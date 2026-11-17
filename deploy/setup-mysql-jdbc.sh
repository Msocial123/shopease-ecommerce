#!/usr/bin/env bash
# =============================================================================
#  ShopEase - MySQL + JDBC setup for an Ubuntu server running Tomcat 11
#
#  What it does (safe to re-run):
#    1. Checks Java 17+ is available (needed by Tomcat 11 / Spring 6)
#    2. Installs MySQL Server (apt) if it is not installed, enables + starts it
#    3. Creates the database (utf8mb4) and an application user with a password
#    4. Tests the JDBC login with that user
#    5. Writes the JDBC settings into Tomcat's bin/setenv.sh
#       (DB_URL / DB_USERNAME / DB_PASSWORD -> read by the app as db.url etc.)
#    6. Optionally restarts Tomcat and calls /shopease/health
#
#  The MySQL JDBC driver (mysql-connector-j) is bundled inside shopease.war
#  (WEB-INF/lib), so no jar has to be copied into Tomcat's lib folder.
#
#  Usage:
#    sudo ./setup-mysql-jdbc.sh                       # defaults, random password
#    sudo DB_PASSWORD='S3cret#Pass' ./setup-mysql-jdbc.sh --restart
#    sudo CATALINA_BASE=/opt/tomcat ./setup-mysql-jdbc.sh --restart
#
#  Options:
#    --restart          restart Tomcat afterwards and check /shopease/health
#    --skip-install     don't install MySQL (use an existing / remote server)
#    -h | --help        show this help
#
#  Settings (environment variables, all optional):
#    DB_NAME            database name                 (default: shopease)
#    DB_USER            application MySQL user        (default: shopease)
#    DB_PASSWORD        its password                  (default: random 24 chars)
#    DB_HOST            MySQL host                    (default: localhost)
#    DB_PORT            MySQL port                    (default: 3306)
#    MYSQL_ADMIN_USER   admin login for a REMOTE server (local uses sudo mysql)
#    MYSQL_ADMIN_PASSWORD
#    CATALINA_BASE      Tomcat directory              (default: auto-detect)
#    TOMCAT_SERVICE     systemd unit name             (default: auto-detect)
#    APP_CONTEXT        context path of the app       (default: shopease)
# =============================================================================
set -Eeuo pipefail

# ----------------------------------------------------------------- settings
DB_NAME="${DB_NAME:-shopease}"
DB_USER="${DB_USER:-shopease}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
APP_CONTEXT="${APP_CONTEXT:-shopease}"
DO_RESTART=false
SKIP_INSTALL=false
CRED_FILE="/root/shopease-db.credentials"
MARK_BEGIN="# >>> shopease jdbc settings (managed by setup-mysql-jdbc.sh) >>>"
MARK_END="# <<< shopease jdbc settings <<<"

# ----------------------------------------------------------------- helpers
c_green=$'\e[32m'; c_yellow=$'\e[33m'; c_red=$'\e[31m'; c_bold=$'\e[1m'; c_off=$'\e[0m'
info() { echo "${c_green}[INFO]${c_off} $*"; }
warn() { echo "${c_yellow}[WARN]${c_off} $*" >&2; }
die()  { echo "${c_red}[FAIL]${c_off} $*" >&2; exit 1; }
step() { echo; echo "${c_bold}==> $*${c_off}"; }
trap 'die "Script stopped at line $LINENO (command: $BASH_COMMAND)"' ERR

usage() { sed -n '2,45p' "$0" | sed 's/^# \{0,1\}//'; exit 0; }

for arg in "$@"; do
    case "$arg" in
        --restart)      DO_RESTART=true ;;
        --skip-install) SKIP_INSTALL=true ;;
        -h|--help)      usage ;;
        *) die "Unknown option: $arg (use --help)" ;;
    esac
done

[[ $EUID -eq 0 ]] || die "Please run as root: sudo $0 $*"
[[ "$DB_NAME" =~ ^[A-Za-z0-9_]+$ ]] || die "DB_NAME may only contain letters, digits and _"
[[ "$DB_USER" =~ ^[A-Za-z0-9_]+$ ]] || die "DB_USER may only contain letters, digits and _"

# Re-use the password from a previous run, otherwise generate one
if [[ -z "${DB_PASSWORD:-}" && -f "$CRED_FILE" ]]; then
    DB_PASSWORD="$( (grep -E '^DB_PASSWORD=' "$CRED_FILE" || true) | head -1 | cut -d= -f2-)"
    [[ -n "$DB_PASSWORD" ]] && info "Re-using the password stored in $CRED_FILE"
fi
if [[ -z "${DB_PASSWORD:-}" ]]; then
    DB_PASSWORD="$(tr -dc 'A-Za-z0-9' </dev/urandom | head -c 20)Aa1"
    info "Generated a random password for MySQL user '$DB_USER'"
fi
(( ${#DB_PASSWORD} >= 8 )) || die "DB_PASSWORD must be at least 8 characters"

# Escape for SQL string literal ('...') and for a single-quoted shell string
sql_escape()   { printf '%s' "$1" | sed -e 's/\\/\\\\/g' -e "s/'/''/g"; }
shell_quote()  { printf "'%s'" "$(printf '%s' "$1" | sed "s/'/'\\\\''/g")"; }

IS_LOCAL=false
[[ "$DB_HOST" == "localhost" || "$DB_HOST" == "127.0.0.1" ]] && IS_LOCAL=true

# Run SQL as a MySQL administrator
mysql_admin() {
    if $IS_LOCAL && [[ -z "${MYSQL_ADMIN_USER:-}" ]]; then
        mysql --protocol=socket -uroot "$@"          # Ubuntu: root uses auth_socket
    else
        [[ -n "${MYSQL_ADMIN_USER:-}" ]] || die "Remote DB_HOST needs MYSQL_ADMIN_USER and MYSQL_ADMIN_PASSWORD"
        MYSQL_PWD="${MYSQL_ADMIN_PASSWORD:-}" mysql -h "$DB_HOST" -P "$DB_PORT" -u "$MYSQL_ADMIN_USER" "$@"
    fi
}

# ----------------------------------------------------------------- 1. Java
step "1/6 Checking Java (Tomcat 11 needs Java 17+)"
JAVA_BIN=""
if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then JAVA_BIN="$JAVA_HOME/bin/java"
elif command -v java >/dev/null 2>&1; then JAVA_BIN="$(command -v java)"; fi

if [[ -z "$JAVA_BIN" ]]; then
    warn "Java not found. Install it with:  sudo apt-get install -y openjdk-17-jdk   (or openjdk-21-jdk)"
else
    JAVA_VER="$("$JAVA_BIN" -version 2>&1 | head -1 | sed -E 's/.*version "([0-9]+).*/\1/')"
    if [[ "$JAVA_VER" =~ ^[0-9]+$ ]] && (( JAVA_VER >= 17 )); then
        info "Java $JAVA_VER found at $JAVA_BIN"
    else
        warn "Java version '$JAVA_VER' found - Tomcat 11 needs 17 or newer (apt-get install openjdk-17-jdk)"
    fi
fi

# ----------------------------------------------------------------- 2. MySQL server
step "2/6 MySQL Server"
if $SKIP_INSTALL || ! $IS_LOCAL; then
    info "Skipping MySQL installation (remote host or --skip-install)"
    command -v mysql >/dev/null 2>&1 || { info "Installing MySQL client only"; apt-get update -qq; DEBIAN_FRONTEND=noninteractive apt-get install -y -qq mysql-client; }
else
    if ! dpkg -s mysql-server >/dev/null 2>&1 && ! dpkg -s mariadb-server >/dev/null 2>&1; then
        info "Installing mysql-server (this can take a minute)..."
        apt-get update -qq
        DEBIAN_FRONTEND=noninteractive apt-get install -y -qq mysql-server
    else
        info "MySQL/MariaDB server already installed"
    fi
    systemctl enable --now mysql >/dev/null 2>&1 || systemctl enable --now mariadb >/dev/null 2>&1 \
        || die "Could not start the MySQL service (check: journalctl -u mysql)"
    for _ in {1..30}; do mysqladmin --protocol=socket -uroot ping >/dev/null 2>&1 && break; sleep 1; done
    mysqladmin --protocol=socket -uroot ping >/dev/null 2>&1 || die "MySQL is not answering"
    info "MySQL is running: $(mysql --protocol=socket -uroot -N -e 'SELECT VERSION()')"
fi

# ----------------------------------------------------------------- 3. database + user
step "3/6 Creating database '$DB_NAME' and user '$DB_USER'"
PW_SQL="$(sql_escape "$DB_PASSWORD")"
if $IS_LOCAL; then USER_HOSTS=("localhost" "127.0.0.1"); else USER_HOSTS=("%"); fi

SQL="CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
for h in "${USER_HOSTS[@]}"; do
    SQL+="
CREATE USER IF NOT EXISTS '$DB_USER'@'$h' IDENTIFIED BY '$PW_SQL';
ALTER USER '$DB_USER'@'$h' IDENTIFIED BY '$PW_SQL';
GRANT ALL PRIVILEGES ON \`$DB_NAME\`.* TO '$DB_USER'@'$h';"
done
SQL+="
FLUSH PRIVILEGES;"
mysql_admin -e "$SQL"
info "Database and user ready (hosts: ${USER_HOSTS[*]})"

# ----------------------------------------------------------------- 4. test JDBC login
step "4/6 Testing login as '$DB_USER'"
MYSQL_PWD="$DB_PASSWORD" mysql -h "$DB_HOST" -P "$DB_PORT" --protocol=tcp -u "$DB_USER" -D "$DB_NAME" -N -e "SELECT 1" >/dev/null \
    || die "Login test failed for $DB_USER@$DB_HOST:$DB_PORT/$DB_NAME"
info "Login OK over TCP ($DB_HOST:$DB_PORT) - this is the same path the JDBC driver uses"

umask 077
cat > "$CRED_FILE" <<EOF
# ShopEase database credentials - created $(date '+%F %T')
DB_HOST=$DB_HOST
DB_PORT=$DB_PORT
DB_NAME=$DB_NAME
DB_USER=$DB_USER
DB_PASSWORD=$DB_PASSWORD
EOF
chmod 600 "$CRED_FILE"
info "Credentials saved to $CRED_FILE (root only)"

# ----------------------------------------------------------------- 5. Tomcat setenv.sh
step "5/6 Configuring Tomcat"
detect_catalina_base() {
    [[ -n "${CATALINA_BASE:-}" ]] && { echo "$CATALINA_BASE"; return; }
    local from_ps
    from_ps="$(ps -eo args 2>/dev/null | grep -o -- '-Dcatalina\.base=[^ ]*' | head -1 | cut -d= -f2- || true)"
    [[ -n "$from_ps" && -d "$from_ps" ]] && { echo "$from_ps"; return; }
    local d
    for d in /opt/tomcat /opt/tomcat11 /opt/apache-tomcat-11* /usr/local/tomcat /usr/share/tomcat11 /var/lib/tomcat11 /var/lib/tomcat10 /opt/tomcat10; do
        [[ -d "$d/conf" ]] && { echo "$d"; return; }
    done
}
TOMCAT_DIR="$(detect_catalina_base || true)"
[[ -n "$TOMCAT_DIR" && -d "$TOMCAT_DIR" ]] || die "Tomcat not found. Re-run with CATALINA_BASE=/path/to/tomcat"
info "Tomcat directory: $TOMCAT_DIR"

TOMCAT_OWNER="$(stat -c '%U' "$TOMCAT_DIR/conf" 2>/dev/null || echo root)"
TOMCAT_GROUP="$(stat -c '%G' "$TOMCAT_DIR/conf" 2>/dev/null || echo root)"
mkdir -p "$TOMCAT_DIR/bin"
SETENV="$TOMCAT_DIR/bin/setenv.sh"
DB_URL="jdbc:mysql://$DB_HOST:$DB_PORT/$DB_NAME?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8"

if [[ -f "$SETENV" ]]; then
    cp -p "$SETENV" "$SETENV.bak.$(date +%Y%m%d%H%M%S)"
    # drop the block from a previous run, keep everything else the admin wrote
    awk -v b="$MARK_BEGIN" -v e="$MARK_END" '$0==b{skip=1;next} $0==e{skip=0;next} !skip' "$SETENV" > "$SETENV.tmp"
    mv "$SETENV.tmp" "$SETENV"
else
    printf '#!/bin/sh\n' > "$SETENV"
fi

{
    echo "$MARK_BEGIN"
    echo "# Spring maps DB_URL -> db.url, DB_USERNAME -> db.username, DB_PASSWORD -> db.password"
    echo "export DB_URL=$(shell_quote "$DB_URL")"
    echo "export DB_USERNAME=$(shell_quote "$DB_USER")"
    echo "export DB_PASSWORD=$(shell_quote "$DB_PASSWORD")"
    echo "$MARK_END"
} >> "$SETENV"

chown "root:$TOMCAT_GROUP" "$SETENV"
chmod 750 "$SETENV"            # readable by the tomcat group only - it holds a password
info "Wrote JDBC settings to $SETENV (owner root:$TOMCAT_GROUP, mode 750)"

WAR="$TOMCAT_DIR/webapps/$APP_CONTEXT.war"
if [[ -f "$WAR" ]] && command -v unzip >/dev/null 2>&1; then
    DRIVER="$(unzip -l "$WAR" | grep -o 'WEB-INF/lib/mysql-connector-j-[^ ]*\.jar' | head -1 || true)"
    [[ -n "$DRIVER" ]] && info "JDBC driver found inside the WAR: $DRIVER" || warn "mysql-connector-j not found inside $WAR"
elif [[ ! -f "$WAR" ]]; then
    info "No $APP_CONTEXT.war deployed yet - copy target/$APP_CONTEXT.war to $TOMCAT_DIR/webapps/ after 'mvn clean package'"
fi

# ----------------------------------------------------------------- 6. restart + health
step "6/6 Tomcat restart"
SERVICE="${TOMCAT_SERVICE:-$(systemctl list-unit-files --type=service --no-legend 2>/dev/null | awk '{print $1}' | grep -E '^tomcat[0-9]*\.service$' | head -1 || true)}"
if $DO_RESTART; then
    if [[ -n "$SERVICE" ]]; then
        info "Restarting $SERVICE ..."
        systemctl restart "$SERVICE"
    elif [[ -x "$TOMCAT_DIR/bin/shutdown.sh" ]]; then
        info "No systemd unit found - restarting with shutdown.sh/startup.sh as $TOMCAT_OWNER"
        sudo -u "$TOMCAT_OWNER" "$TOMCAT_DIR/bin/shutdown.sh" >/dev/null 2>&1 || true
        sleep 5
        sudo -u "$TOMCAT_OWNER" "$TOMCAT_DIR/bin/startup.sh" >/dev/null
    else
        warn "Could not find how to restart Tomcat - please restart it yourself"
    fi

    if [[ -f "$WAR" || -d "$TOMCAT_DIR/webapps/$APP_CONTEXT" ]]; then
        PORT="$(grep -oP '<Connector[^>]*port="\K[0-9]+(?="[^>]*protocol="HTTP/1\.1")' "$TOMCAT_DIR/conf/server.xml" 2>/dev/null | head -1 || true)"
        PORT="${PORT:-8080}"
        URL="http://localhost:$PORT/$APP_CONTEXT/health"
        info "Waiting for $URL ..."
        HEALTH=""
        for _ in {1..60}; do
            HEALTH="$(curl -s --max-time 3 "$URL" || true)"
            [[ "$HEALTH" == *'"database":"UP"'* ]] && break
            sleep 2
        done
        if [[ "$HEALTH" == *'"database":"UP"'* ]]; then
            info "Application is UP and connected to MySQL: $HEALTH"
        else
            warn "Health check did not report database UP yet. Response: ${HEALTH:-<none>}"
            warn "Check the logs: tail -n 100 $TOMCAT_DIR/logs/catalina.out   (or journalctl -u ${SERVICE:-tomcat})"
        fi
    fi
else
    info "Not restarting. Restart Tomcat so it picks up the settings:  sudo systemctl restart ${SERVICE:-tomcat}"
fi

trap - ERR
cat <<EOF

${c_bold}Done.${c_off}
  JDBC URL : $DB_URL
  User     : $DB_USER
  Password : stored in $CRED_FILE
  Settings : $SETENV
  App      : http://<server>:8080/$APP_CONTEXT/      Health: http://<server>:8080/$APP_CONTEXT/health
  Admin    : admin@shopease.com / Admin@123 (created on first start - change it!)
EOF

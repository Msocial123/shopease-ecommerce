# Sourced by backup.sh / restore.sh: picks the compose command for Docker or Podman
# and provides small helpers. Override the tool with:  COMPOSE="podman-compose" ./docker/volumes/backup.sh
if [ -z "${COMPOSE:-}" ]; then
    if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
        COMPOSE="docker compose"
    elif command -v podman-compose >/dev/null 2>&1; then
        COMPOSE="podman-compose"
    elif command -v podman >/dev/null 2>&1 && podman compose version >/dev/null 2>&1; then
        COMPOSE="podman compose"
    else
        echo "No compose tool found (docker compose, podman-compose or podman compose)." >&2
        exit 1
    fi
fi
ENGINE=docker
case "$COMPOSE" in podman*) ENGINE=podman ;; esac
echo "[compose] using: $COMPOSE ($ENGINE)"

# Waits until a container reports "healthy" (max ~3 minutes).
wait_healthy() {
    local name=$1 status=""
    for _ in $(seq 1 90); do
        status=$($ENGINE inspect --format '{{.State.Health.Status}}' "$name" 2>/dev/null || true)
        [ "$status" = "healthy" ] && { echo "[wait] $name is healthy"; return 0; }
        sleep 2
    done
    echo "[wait] $name did not become healthy (last status: ${status:-unknown})" >&2
    return 1
}

# Stops the given services (missing containers are fine, e.g. after "compose down -v").
stop_services() {
    $COMPOSE stop "$@" >/dev/null 2>&1 || true
    echo "[stop] $*"
}

# Starts db first, waits for it, then the app. Falls back to "up -d" when the
# containers do not exist yet (e.g. after "compose down -v").
start_stack() {
    if $ENGINE container exists shopease-db 2>/dev/null || $ENGINE inspect shopease-db >/dev/null 2>&1; then
        $COMPOSE start db >/dev/null && wait_healthy shopease-db
        $COMPOSE start app >/dev/null && wait_healthy shopease-app
    else
        $COMPOSE up -d >/dev/null && wait_healthy shopease-db && wait_healthy shopease-app
    fi
}

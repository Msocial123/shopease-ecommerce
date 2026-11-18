# syntax=docker/dockerfile:1
# =====================================================================================
#  ShopEase - Spring MVC 6 / Servlet 6.1 WAR on Apache Tomcat 11
#
#  Build :  docker build -t shopease:1.0.0 .
#  Run   :  docker run -d -p 8080:8080 \
#             -e DB_URL="jdbc:mysql://mysql-host:3306/shopease?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
#             -e DB_USERNAME=shopease -e DB_PASSWORD=secret \
#             -e APP_ADMIN_PASSWORD=ChangeMe@123 \
#             shopease:1.0.0
#  Health:  http://localhost:8080/health
# =====================================================================================

# -------------------------------------------------------------------------------------
#  1. Choosing a base - official Tomcat 11 image variants (Docker Hub "tomcat")
#
#     tomcat:11.0-jdk21-temurin-noble   full JDK 21 on Ubuntu 24.04 - debugging tools, bigger
#     tomcat:11.0-jre21-temurin-noble   JRE 21 on Ubuntu 24.04      - smaller runtime  <== chosen
#     tomcat:11.0-jdk17-temurin-noble   JDK 17 (app targets release 17, so this also works)
#     tomcat:11.0-jre21-temurin-jammy   same JRE on Ubuntu 22.04
#     tomcat:11.0-jdk21-corretto        Amazon Corretto on Amazon Linux
#
#  Why jre21-temurin-noble:
#    - Tomcat 11 needs Java 17+; Java 21 is the current LTS and runs our release-17 bytecode.
#    - A JRE is enough at runtime: Tomcat compiles JSPs with its bundled Eclipse JDT compiler.
#    - Ubuntu (glibc) avoids the musl surprises of Alpine and gets regular security updates.
#  Override at build time, e.g.:  --build-arg TOMCAT_TAG=11.0.13-jre21-temurin-noble  (pin a patch)
# -------------------------------------------------------------------------------------
ARG MAVEN_TAG=3.9-eclipse-temurin-21
ARG TOMCAT_TAG=11.0-jre21-temurin-noble

# =====================================================================================
#  Stage 1 - BUILD: compile, test and package shopease.war with Maven + JDK 21
# =====================================================================================
FROM maven:${MAVEN_TAG} AS build
WORKDIR /workspace

# Resolve dependencies first so this layer is cached until pom.xml changes
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY src ./src
ARG SKIP_TESTS=false
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q package -DskipTests=${SKIP_TESTS} \
 && mvn -B -q dependency:copy-dependencies \
        -DincludeArtifactIds=mysql-connector-j \
        -DexcludeTransitive=true \
        -DoutputDirectory=target/jdbc

# =====================================================================================
#  Stage 2 - DEPLOY: run the WAR from stage 1 in Tomcat 11
#  Only the built WAR and the JDBC driver are copied over (COPY --from=build); Maven, the
#  JDK and the source code stay behind in the build stage and are not in the final image.
# =====================================================================================
FROM tomcat:${TOMCAT_TAG} AS deploy

LABEL org.opencontainers.image.title="ShopEase" \
      org.opencontainers.image.description="ShopEase e-commerce WAR on Apache Tomcat 11" \
      org.opencontainers.image.version="1.0.0"

# curl is used by HEALTHCHECK (installed only if the base image does not already have it)
RUN if ! command -v curl >/dev/null 2>&1; then \
        apt-get update \
     && apt-get install -y --no-install-recommends curl \
     && rm -rf /var/lib/apt/lists/*; \
    fi

# Non-root user that owns the writable Tomcat directories
RUN groupadd --system --gid 10001 tomcat \
 && useradd  --system --uid 10001 --gid tomcat --home-dir "$CATALINA_HOME" --shell /usr/sbin/nologin tomcat

# -------------------------------------------------------------------------------------
#  3. Custom server.xml + externalised configuration through environment variables
#
#  server.xml uses ${ENV_VAR:-default} placeholders. Telling Tomcat's digester to resolve
#  them from the environment is a one-line setting in catalina.properties.
#  The JDBC driver goes into $CATALINA_HOME/lib because the global JNDI pool is created by
#  Tomcat itself (common class loader), not by the web application.
# -------------------------------------------------------------------------------------
COPY docker/tomcat/server.xml "$CATALINA_HOME/conf/server.xml"
COPY --from=build /workspace/target/jdbc/*.jar "$CATALINA_HOME/lib/"
RUN echo 'org.apache.tomcat.util.digester.PROPERTY_SOURCE=org.apache.tomcat.util.digester.EnvironmentPropertySource' \
        >> "$CATALINA_HOME/conf/catalina.properties"

# -------------------------------------------------------------------------------------
#  2. Packaging the WAR
#
#  The official image ships an empty webapps/ (samples live in webapps.dist/), but we clear
#  it anyway. Deploying as ROOT.war serves the app at "/" (http://host:8080/).
# -------------------------------------------------------------------------------------
RUN rm -rf "$CATALINA_HOME/webapps/"* "$CATALINA_HOME/webapps.dist"
COPY --from=build --chown=tomcat:tomcat /workspace/target/shopease.war "$CATALINA_HOME/webapps/ROOT.war"

# Non-root permissions:
#  - writable by tomcat : webapps (WAR unpack), work (compiled JSPs), temp, logs,
#                         conf/Catalina/localhost (Tomcat creates/uses it at start-up)
#  - read-only to tomcat: everything else in conf/ (root-owned, group tomcat, no write),
#                         so a compromised app cannot rewrite server.xml
RUN mkdir -p "$CATALINA_HOME/conf/Catalina/localhost" \
 && chown -R root:tomcat "$CATALINA_HOME/conf" \
 && chmod -R u=rwX,g=rX,o= "$CATALINA_HOME/conf" \
 && chown -R tomcat:tomcat "$CATALINA_HOME/conf/Catalina" \
                           "$CATALINA_HOME/webapps" "$CATALINA_HOME/work" \
                           "$CATALINA_HOME/temp" "$CATALINA_HOME/logs"

# -------------------------------------------------------------------------------------
#  Runtime configuration (all overridable with `docker run -e NAME=value`)
#
#  Tomcat / server.xml : TOMCAT_HTTP_PORT, TOMCAT_MAX_THREADS, TOMCAT_MIN_SPARE_THREADS,
#                        TOMCAT_ACCEPT_COUNT, TOMCAT_CONNECTION_TIMEOUT, TOMCAT_MAX_POST_SIZE
#  Database (JNDI pool): DB_URL, DB_USERNAME, DB_PASSWORD, DB_POOL_MAX, DB_POOL_MIN_IDLE,
#                        DB_POOL_CONNECTION_TIMEOUT_MS
#  Application         : any app.properties key as UPPER_SNAKE_CASE, e.g. APP_DB_INIT,
#                        APP_ADMIN_EMAIL, APP_ADMIN_PASSWORD, APP_STORE_NAME, APP_TAX_RATE
#  JVM                 : JAVA_OPTS / CATALINA_OPTS
#
#  Secrets (DB_PASSWORD, APP_ADMIN_PASSWORD) are deliberately NOT baked in here - pass them
#  at run time (-e, --env-file, or orchestrator secrets).
# -------------------------------------------------------------------------------------
ENV TOMCAT_HTTP_PORT=8080 \
    TOMCAT_MAX_THREADS=200 \
    DB_USERNAME=shopease \
    DB_POOL_MAX=10 \
    DB_POOL_MIN_IDLE=2 \
    APP_DB_INIT=true \
    TZ=UTC \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom -Dfile.encoding=UTF-8"

# Run as the non-root tomcat user. Numeric UID:GID so Kubernetes `runAsNonRoot: true`
# can verify it is not root. Port 8080 (>1024) needs no root privileges to bind.
USER 10001:10001
EXPOSE 8080

# -------------------------------------------------------------------------------------
#  4. Health check
#
#  /health is the app's own servlet: HTTP 200 {"status":"UP"} when Tomcat, the app and the
#  database are all working; HTTP 503 {"status":"DEGRADED"} when the database is down.
#  curl -f fails on 4xx/5xx, so the container is reported "unhealthy" if Tomcat is not
#  answering, the WAR failed to deploy (404), or MySQL is unreachable (503).
#  start-period gives Tomcat + Spring + schema initialisation time to come up.
# -------------------------------------------------------------------------------------
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS "http://127.0.0.1:${TOMCAT_HTTP_PORT}/health" || exit 1

# Exec form so Tomcat is PID 1 and receives SIGTERM for a graceful shutdown
CMD ["catalina.sh", "run"]

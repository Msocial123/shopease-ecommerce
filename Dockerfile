# syntax=docker/dockerfile:1
# =====================================================================================
#  ShopEase - Spring MVC 6 / Servlet 6.1 WAR on Apache Tomcat 11  (multi-stage build)
#
#    Stage 1 "build"  : Maven + JDK 21 compiles, tests and packages shopease.war
#    Stage 2 "deploy" : official Tomcat 11 image runs the WAR as a non-root user
#
#  Build (Podman)  : podman build --format docker -t shopease:1.0.0 .
#                    (--format docker keeps the HEALTHCHECK; the OCI format drops it)
#  Build (Docker)  : docker build -t shopease:1.0.0 .
#
#  Run             : cp docker/shopease.env.example shopease.env   # fill in the secrets
#                    podman network create shopease-net
#                    podman run -d --name shopease-db  --network shopease-net --env-file shopease.env \
#                           -v shopease-db:/var/lib/mysql docker.io/library/mariadb:11.4
#                    podman run -d --name shopease-app --network shopease-net --env-file shopease.env \
#                           -p 8081:8080 shopease:1.0.0
#                    (docker: the same commands with `docker` instead of `podman`)
#
#  Check           : curl http://localhost:8081/health
#                    podman healthcheck run shopease-app   |   docker inspect --format '{{.State.Health.Status}}' shopease-app
# =====================================================================================


# =====================================================================================
#  1. OFFICIAL TOMCAT 11 IMAGE VARIANTS AND CHOOSING A BASE
#
#     docker.io/library/tomcat:<tag>
#       11.0-jdk21-temurin-noble   JDK 21, Ubuntu 24.04  - compiler + debug tools, bigger
#       11.0-jre21-temurin-noble   JRE 21, Ubuntu 24.04  - runtime only, smaller     <== chosen
#       11.0-jdk17-temurin-noble   JDK 17 (the app targets release 17, so it also runs)
#       11.0-jre21-temurin-jammy   JRE 21, Ubuntu 22.04
#       11.0-jdk21-corretto        Amazon Corretto on Amazon Linux
#
#  Why 11.0-jre21-temurin-noble:
#    - Tomcat 11 requires Java 17+; Java 21 is the current LTS and runs release-17 bytecode.
#    - A JRE is enough at runtime: Tomcat compiles JSPs with its bundled Eclipse JDT compiler.
#    - Ubuntu (glibc) is the best-supported variant; Alpine/musl is not published for Tomcat 11.
#    - "11.0" follows the newest 11.0.x patch on each rebuild (security fixes). For fully
#      reproducible builds pin a patch:  --build-arg TOMCAT_TAG=11.0.29-jre21-temurin-noble
#
#  Images are fully qualified (docker.io/library/...) so Podman/Buildah never has to guess
#  a registry from a short name.
# =====================================================================================
ARG MAVEN_IMAGE=docker.io/library/maven:3.9-eclipse-temurin-21
ARG TOMCAT_IMAGE=docker.io/library/tomcat
ARG TOMCAT_TAG=11.0-jre21-temurin-noble


# =====================================================================================
#  STAGE 1 - BUILD
# =====================================================================================
FROM ${MAVEN_IMAGE} AS build
WORKDIR /workspace

# Dependencies first: this layer stays cached until pom.xml changes
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
# Output: target/shopease.war  +  target/jdbc/mysql-connector-j-<version>.jar


# =====================================================================================
#  STAGE 2 - DEPLOY
#  Only the WAR and the JDBC driver come from stage 1. Maven, the JDK, the sources and the
#  Maven repository are not part of the final image.
# =====================================================================================
FROM ${TOMCAT_IMAGE}:${TOMCAT_TAG} AS deploy

LABEL org.opencontainers.image.title="ShopEase" \
      org.opencontainers.image.description="ShopEase e-commerce WAR on Apache Tomcat 11" \
      org.opencontainers.image.version="1.0.0" \
      org.opencontainers.image.source="https://github.com/Msocial123/shopease-ecommerce"

# curl is needed by the HEALTHCHECK (installed only if the base image lacks it)
RUN if ! command -v curl >/dev/null 2>&1; then \
        apt-get update \
     && apt-get install -y --no-install-recommends curl \
     && rm -rf /var/lib/apt/lists/*; \
    fi

# Non-root user and group, fixed IDs 10001 (no login shell, no home of its own)
RUN groupadd --gid 10001 tomcat \
 && useradd  --uid 10001 --gid tomcat --no-create-home --home-dir "$CATALINA_HOME" \
             --shell /usr/sbin/nologin tomcat


# =====================================================================================
#  2. PACKAGING THE WAR INTO THE IMAGE
#
#  - webapps.dist/ (Tomcat's sample apps) is removed and webapps/ is emptied.
#  - The WAR is copied as ROOT.war, so the shop is served at "/" instead of "/shopease".
#  - Tomcat unpacks it on first start (unpackWARs="true" in server.xml), so webapps/ must be
#    writable by the tomcat user - hence --chown.
# =====================================================================================
RUN rm -rf "$CATALINA_HOME/webapps/"* "$CATALINA_HOME/webapps.dist"
COPY --from=build --chown=tomcat:tomcat /workspace/target/shopease.war "$CATALINA_HOME/webapps/ROOT.war"


# =====================================================================================
#  3. CUSTOM server.xml AND EXTERNALISED CONFIGURATION WITH ENVIRONMENT VARIABLES
#
#  (a) docker/tomcat/server.xml replaces the stock file. Its values are ${ENV_VAR:-default}
#      placeholders: HTTP connector (port, threads, timeouts) and the global JNDI DataSource
#      jdbc/ShopEaseDB (DB_URL, DB_USERNAME, DB_PASSWORD, pool sizes).
#  (b) The MySQL driver goes into $CATALINA_HOME/lib: Tomcat creates the JNDI pool itself,
#      before the web app (and the driver inside the WAR) is loaded.
#  (c) By default Tomcat fills ${...} in server.xml only from Java system properties.
#      EnvironmentPropertySource makes it read OS environment variables as well.
#
#  The app links the pool via src/main/webapp/META-INF/context.xml (<ResourceLink>), and
#  RootConfig looks up java:comp/env/jdbc/ShopEaseDB. Every app.properties key can also be
#  overridden with an UPPER_SNAKE_CASE variable (app.admin.password -> APP_ADMIN_PASSWORD).
#  All variables are listed in docker/shopease.env.example.
# =====================================================================================
COPY docker/tomcat/server.xml "$CATALINA_HOME/conf/server.xml"
COPY --from=build /workspace/target/jdbc/*.jar "$CATALINA_HOME/lib/"
RUN echo 'org.apache.tomcat.util.digester.PROPERTY_SOURCE=org.apache.tomcat.util.digester.EnvironmentPropertySource' \
        >> "$CATALINA_HOME/conf/catalina.properties"

# Non-default, non-secret settings. Override any of them at run time with -e / --env-file.
# Secrets (DB_PASSWORD, APP_ADMIN_PASSWORD) are deliberately NOT set here: ENV values are
# visible to anyone who can pull the image (`inspect`).
ENV TOMCAT_HTTP_PORT=8080 \
    TOMCAT_MAX_THREADS=200 \
    DB_USERNAME=shopease \
    DB_POOL_MAX=10 \
    DB_POOL_MIN_IDLE=2 \
    APP_DB_INIT=true \
    TZ=UTC \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom -Dfile.encoding=UTF-8"


# =====================================================================================
#  NON-ROOT USER
#
#  writable by tomcat : webapps (WAR unpack), work (compiled JSPs), temp, logs,
#                       conf/Catalina/localhost (Tomcat uses it at start-up)
#  read-only          : the rest of conf/ (owner root, group tomcat r-x), so the running
#                       app cannot rewrite server.xml or catalina.properties
#  USER is numeric so Kubernetes `runAsNonRoot: true` can verify it. Port 8080 is above
#  1024, so binding it needs no root privileges.
# =====================================================================================
RUN mkdir -p "$CATALINA_HOME/conf/Catalina/localhost" \
 && chown -R root:tomcat "$CATALINA_HOME/conf" \
 && chmod -R u=rwX,g=rX,o= "$CATALINA_HOME/conf" \
 && chown -R tomcat:tomcat "$CATALINA_HOME/conf/Catalina" \
                           "$CATALINA_HOME/webapps" "$CATALINA_HOME/work" \
                           "$CATALINA_HOME/temp"    "$CATALINA_HOME/logs"

USER 10001:10001
EXPOSE 8080


# =====================================================================================
#  4. HEALTH CHECK
#
#  GET /health is the app's own servlet:
#     200 {"status":"UP","database":"UP",...}           Tomcat + app + database working
#     503 {"status":"DEGRADED","database":"DOWN",...}   database unreachable
#  curl -f fails on any 4xx/5xx or no answer, so the container turns "unhealthy" when Tomcat
#  is down, the WAR failed to deploy (404) or the database is unreachable (503), and turns
#  "healthy" again by itself once the cause is fixed.
#  start-period: failures in the first 60 s (Tomcat + Spring + schema init) are not counted.
#  Podman: build with --format docker, otherwise this instruction is dropped.
# =====================================================================================
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS "http://127.0.0.1:${TOMCAT_HTTP_PORT}/health" || exit 1

# Exec form: Tomcat is PID 1 and receives SIGTERM directly for a graceful shutdown
CMD ["catalina.sh", "run"]

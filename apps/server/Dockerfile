# syntax=docker/dockerfile:1.7

# ============================================================
# Stage 1: Build Spring Boot executable JAR
# ============================================================
FROM maven:3.10-eclipse-temurin-25-noble AS build

WORKDIR /workspace

COPY pom.xml ./

RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B -ntp dependency:go-offline -DskipTests

COPY src ./src

RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B -ntp package -DskipTests && \
    set -- $(find target \
        -maxdepth 1 \
        -type f \
        -name '*.jar' \
        ! -name '*-sources.jar' \
        ! -name '*-javadoc.jar' \
        ! -name '*.original'); \
    if [ "$#" -ne 1 ]; then \
        echo "Expected exactly one executable JAR, found: $#" >&2; \
        find target -maxdepth 1 -type f -name '*.jar' >&2; \
        exit 1; \
    fi && \
    cp "$1" /workspace/application.jar && \
    test -s /workspace/application.jar

# ============================================================
# Stage 2: Extract Spring Boot Layers
# ============================================================
FROM bellsoft/liberica-openjre-debian:25-cds AS extract

WORKDIR /workspace

COPY --from=build /workspace/application.jar .

RUN java -Djarmode=tools \
    -jar application.jar \
    extract \
    --layers \
    --destination extracted

# ============================================================
# Stage 3: Production Runtime
# ============================================================
FROM bellsoft/liberica-openjre-debian:25-cds AS runtime

LABEL maintainer="nghlong3004"
LABEL description="Longnest Persional OS"

WORKDIR /app

RUN apt-get update && \
    apt-get install -y --no-install-recommends wget && \
    rm -rf /var/lib/apt/lists/*

RUN groupadd --gid 10001 longnest && \
    useradd \
        --uid 10001 \
        --gid 10001 \
        --no-create-home \
        --home-dir /app \
        --shell /usr/sbin/nologin \
        olympic_platform && \
    mkdir -p /app/storage && \
    chown -R longnest:longnest /app

COPY --from=extract --chown=10001:10001 /workspace/extracted/dependencies/ ./
COPY --from=extract --chown=10001:10001 /workspace/extracted/spring-boot-loader/ ./
COPY --from=extract --chown=10001:10001 /workspace/extracted/snapshot-dependencies/ ./
COPY --from=extract --chown=10001:10001 /workspace/extracted/application/ ./

USER 10001:10001

EXPOSE 8080

ENV HOME=/app

ENV JAVA_TOOL_OPTIONS="\
-XX:InitialRAMPercentage=25 \
-XX:MaxRAMPercentage=70 \
-XX:+ExitOnOutOfMemoryError \
-Djava.io.tmpdir=/tmp \
-Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["java", "-jar", "application.jar"]

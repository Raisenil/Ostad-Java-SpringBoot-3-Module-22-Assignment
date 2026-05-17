# syntax=docker/dockerfile:1

# Build stage
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

ARG GRADLE_VERSION=9.4.1
ENV GRADLE_HOME=/opt/gradle/gradle-${GRADLE_VERSION}
ENV PATH="${GRADLE_HOME}/bin:${PATH}"

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl unzip \
    && rm -rf /var/lib/apt/lists/* \
    && curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o /tmp/gradle.zip \
    && unzip -q /tmp/gradle.zip -d /opt/gradle \
    && rm -f /tmp/gradle.zip

COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# Cache dependencies
RUN gradle --no-daemon dependencies

COPY src/ src/
RUN gradle --no-daemon bootJar

# Runtime stage
FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /workspace/build/libs/*-SNAPSHOT.jar /app/
RUN set -eux; \
    JAR_FILE=$(ls /app/*-SNAPSHOT.jar | grep -v plain | head -n 1); \
    mv "$JAR_FILE" /app/app.jar; \
    rm -f /app/*-plain.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

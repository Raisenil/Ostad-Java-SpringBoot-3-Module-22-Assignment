# syntax=docker/dockerfile:1

# Build stage
FROM gradle:8.9-jdk25 AS build
WORKDIR /workspace

COPY gradlew gradle/ build.gradle settings.gradle ./
RUN chmod +x gradlew

# Cache dependencies
RUN ./gradlew --no-daemon dependencies

COPY src/ src/
RUN ./gradlew --no-daemon bootJar

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


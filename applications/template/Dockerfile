# Dockerfile for the template Spring Boot application.
#
# This is intentionally local to the application rather than centralized.
# It keeps the packaging flow obvious while still using the same core ideas as
# the shared app-components Dockerfiles:
#   - multi-stage build
#   - Spring Boot jar extraction for better layer caching
#
# Build example:
#   cd applications/template
#   ../../gradlew bootJar
#   docker build -t template:latest .

ARG BASE_IMAGE=eclipse-temurin:25-jre

FROM ${BASE_IMAGE} AS builder
WORKDIR /application

COPY build/libs/*.jar application.jar

RUN java -Djarmode=tools -jar application.jar extract --layers --destination /out --application-filename application.jar

FROM ${BASE_IMAGE}
WORKDIR /application

COPY --from=builder /out/dependencies/ ./
COPY --from=builder /out/snapshot-dependencies/ ./
COPY --from=builder /out/application/ ./

ENTRYPOINT ["java", "-jar", "application.jar"]

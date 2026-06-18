ARG BASE_IMAGE=ghcr.io/ministryofjustice/hmpps-eclipse-temurin:25-jre-jammy

FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jdk AS compile
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/
RUN ./gradlew --no-daemon dependencies 2>/dev/null || true
COPY src/ src/
RUN ./gradlew --no-daemon clean assemble -x test -x generateGitProperties

FROM ${BASE_IMAGE}
WORKDIR /app
COPY --from=compile --chown=appuser:appgroup /app/build/libs/annual-leave-apprenticeship-project-api-*.jar app.jar

ENTRYPOINT ["java", "-XX:+ExitOnOutOfMemoryError", "-XX:+AlwaysActAsServerClassMachine", "-jar", "app.jar"]

# syntax=docker/dockerfile:1

# ---- Build stage: compile the jar with the Maven wrapper ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build

# Copy the wrapper + pom first so dependency resolution is cached as its own
# layer and only re-runs when the pom changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -ntp -DskipTests package

# ---- Runtime stage: slim JRE, non-root ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# curl is used only by the container HEALTHCHECK; temurin JRE ships neither
# curl nor wget by default.
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app && useradd --system --gid app --home /app app

COPY --from=build /build/target/corporate-1.0-SNAPSHOT.jar /app/app.jar
RUN chown -R app:app /app
USER app

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD ["sh", "-c", "curl -fsS http://localhost:8080/actuator/health | grep -q '\"status\":\"UP\"'"]

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

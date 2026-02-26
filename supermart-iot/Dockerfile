# ═══════════════════════════════════════════════════════════════════════════════
# Supermart IoT API – Multi-Stage Dockerfile
# Java 21 + Spring Boot 3.2.3
# ═══════════════════════════════════════════════════════════════════════════════

# ─── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy pom.xml first so Docker caches the dependency layer separately.
# Re-downloads only when pom.xml changes, not on every source change.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build the fat JAR (skip tests – run them in CI)
COPY src ./src
RUN mvn clean package -DskipTests -B

# ─── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy AS runtime

LABEL maintainer="Supermart IoT Team"
LABEL application="supermart-iot-api"
LABEL version="2.0.0"

WORKDIR /app

# Install curl for the Docker health-check probe
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Security: run as a non-root user
RUN groupadd --system appgroup && \
    useradd --system --gid appgroup appuser && \
    chown appuser:appgroup /app

# Copy only the built artifact from the build stage
COPY --from=builder /build/target/supermart-iot-*.jar app.jar

USER appuser

# Application port (matches server.port=8080)
EXPOSE 8080

# JVM flags:
#   UseContainerSupport  – honours cgroup CPU/memory limits (on by default in JDK 11+)
#   MaxRAMPercentage     – cap heap to 75 % of the container's RAM allocation
#   security.egd         – faster SecureRandom on Linux containers
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]

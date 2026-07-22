FROM maven:3.8.4-openjdk-8 as builder

WORKDIR /app

# Copy pom files
COPY pom.xml .
COPY event-gateway/pom.xml event-gateway/

# Build dependencies layer
RUN mvn dependency:go-offline -DskipTests || true

# Copy source code
COPY event-gateway/src event-gateway/src

# Build the application
RUN mvn clean package -DskipTests -f event-gateway/pom.xml

# Runtime stage
FROM openjdk:8-jre-slim

WORKDIR /app

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy built jar from builder
COPY --from=builder /app/event-gateway/target/event-gateway-1.0.0.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# Run application
ENTRYPOINT ["java", "-Xmx256m", "-XX:+UseG1GC", "-jar", "app.jar"]


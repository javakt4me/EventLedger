FROM maven:3.8.4-openjdk-8 as builder

WORKDIR /app

# Copy pom files
COPY pom.xml .
COPY account-service/pom.xml account-service/

# Build dependencies layer
RUN mvn dependency:go-offline -DskipTests || true

# Copy source code
COPY account-service/src account-service/src

# Build the application
RUN mvn clean package -DskipTests -f account-service/pom.xml

# Runtime stage
FROM openjdk:8-jre-slim

WORKDIR /app

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy built jar from builder
COPY --from=builder /app/account-service/target/account-service-1.0.0.jar app.jar

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8081/health || exit 1

# Run application
ENTRYPOINT ["java", "-Xmx256m", "-XX:+UseG1GC", "-jar", "app.jar"]


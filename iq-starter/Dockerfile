# Multi-stage build for IQ Starter
# Produces a lightweight container with MCP server running

FROM openjdk:21-jdk-slim AS builder

WORKDIR /workspace

# Copy entire repo (use .dockerignore to exclude unnecessary files)
COPY . .

# Build iq-apis module only
RUN ./mvnw clean package \
    -pl iq-apis \
    -am \
    -DskipTests \
    -Dquarkus.package.type=uber-jar

# Runtime stage: minimal base image
FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy built JAR
COPY --from=builder /workspace/iq-apis/target/*-runner.jar ./app.jar

# Metadata
LABEL org.opencontainers.image.title="IQ Runtime"
LABEL org.opencontainers.image.description="IQ server with enterprise knowledge graph and workflow support"
LABEL org.opencontainers.image.version="1.0"

# Health check endpoint
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD java -jar app.jar -c "curl -f http://localhost:8080/health || exit 1" 2>/dev/null || exit 1

# Expose HTTP port
EXPOSE 8080

# JVM configuration
ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0 \
               -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

# Run MCP server
CMD ["java", "-jar", "app.jar"]

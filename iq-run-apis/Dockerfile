# Use the official OpenJDK base image
FROM openjdk:21-jdk-slim AS build

# Set the working directory
WORKDIR /app

# Copy the Maven project files
COPY pom.xml .
COPY src ./src

# Build the application using Maven
RUN ./mvnw package -DskipTests

# Use a smaller base image for the final image
FROM openjdk:21-jdk-slim

# Set the working directory in the final image
WORKDIR /app

# Copy the built JAR file from the previous stage
COPY --from=build /app/target/*-runner.jar app.jar

# Expose the port your application runs on
EXPOSE 8080

# Command to run the application
ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

# CMD ["java", "-jar", "app.jar"]

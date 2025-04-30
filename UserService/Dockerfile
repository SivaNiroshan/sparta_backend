# Stage 1: Build the application
FROM maven:3.9.5-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy Maven project files
COPY pom.xml .
COPY src ./src

# Build the project (skip tests for faster build)
RUN mvn clean package -DskipTests

# Stage 2: Minimal runtime image
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the built jar from the build stage
COPY --from=builder /build/target/UserService-0.0.1-SNAPSHOT.jar app.jar

# Expose the user service port
EXPOSE 8081

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

# Stage 1: Build the application using Maven
FROM maven:3.9.5-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy Maven files first to leverage Docker cache
COPY pom.xml .
COPY src ./src

# Package the application (skip tests to save time)
RUN mvn clean package -DskipTests

# Stage 2: Create a minimal runtime image
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the jar from the builder stage
COPY --from=builder /build/target/GatewayService-0.0.1-SNAPSHOT.jar app.jar

# Expose the Gateway port
EXPOSE 8000

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]

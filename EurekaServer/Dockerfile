# Stage 1: Build the JAR using Maven
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run the JAR
FROM openjdk:17
WORKDIR /usr/app
COPY --from=builder /app/target/EurekaServer-0.0.1-SNAPSHOT.jar .
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "EurekaServer-0.0.1-SNAPSHOT.jar"]

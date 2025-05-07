# Stage 1: Build the JAR using Maven
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM openjdk:17
WORKDIR /usr/app
COPY --from=builder /app/target/GatewayService-0.0.1-SNAPSHOT.jar .
EXPOSE 8000
ENTRYPOINT ["java","-jar","GatewayService-0.0.1-SNAPSHOT.jar"]

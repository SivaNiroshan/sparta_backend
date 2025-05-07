FROM openjdk:17
EXPOSE 8761
WORKDIR /usr/app
COPY target/EurekaServer-0.0.1-SNAPSHOT.jar .
ENTRYPOINT ["java", "-jar", "EurekaServer-0.0.1-SNAPSHOT.jar"]

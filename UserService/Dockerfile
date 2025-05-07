FROM openjdk:17
EXPOSE 8081
WORKDIR /usr/app
COPY target/UserService-0.0.1-SNAPSHOT.jar .
ENTRYPOINT ["java","-jar","UserService-0.0.1-SNAPSHOT.jar"]

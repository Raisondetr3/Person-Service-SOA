FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/person-service-*.jar person-service.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "person-service.jar"]
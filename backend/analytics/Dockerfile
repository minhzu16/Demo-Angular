FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/*.jar /app/app.jar
EXPOSE 8093
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

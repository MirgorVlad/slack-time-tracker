FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/slack-time-tracker-0.0.1-SNAPSHOT.jar app.jar

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
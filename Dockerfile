FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app
COPY target/TelegramBot-1.0.jar /app/bot.jar

ENTRYPOINT ["java", "-jar", "/app/bot.jar"]

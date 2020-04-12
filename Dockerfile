FROM openjdk:8-jdk-alpine
EXPOSE 8080
RUN mkdir -p /app/logs/
COPY roles-app/build/libs/roles-app*.jar /app/roles-app.jar
ENTRYPOINT ["java", "-jar", "/app/roles-app.jar"]
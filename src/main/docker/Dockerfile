FROM openjdk:8-jdk-alpine
ADD tng-vnv-executor.jar app.jar
ADD application.properties application.properties
EXPOSE 8080
ENTRYPOINT exec java -Xmx1024m -Xms256m -Dtng-vnv-executor -Djava.security.egd=file:/dev/./urandom -jar /app.jar
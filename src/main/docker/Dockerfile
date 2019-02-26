FROM openjdk:11-jdk-slim

RUN apt update && apt install -y curl

RUN curl -L https://github.com/docker/compose/releases/download/1.23.2/docker-compose-`uname -s`-`uname -m` -o /usr/local/bin/docker-compose && \
    chmod +x /usr/local/bin/docker-compose

ADD wait_for.sh wait_for.sh
ADD tng-vnv-executor.jar app.jar
ADD application.properties application.properties
EXPOSE 8080
ENTRYPOINT exec java -Xmx1024m -Xms256m -Dtng-vnv-executor -Djava.security.egd=file:/dev/./urandom -jar /app.jar
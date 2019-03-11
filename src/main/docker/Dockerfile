FROM openjdk:11-jdk-slim

RUN apt update && apt install -y curl

RUN curl -L https://github.com/docker/compose/releases/download/1.23.2/docker-compose-`uname -s`-`uname -m` -o /usr/local/bin/docker-compose && \
    chmod +x /usr/local/bin/docker-compose

#RUN mkdir /executor && mkdir /executor/bash_scripts && mkdir /executor/compose_files && mkdir /executor/tests
RUN mkdir /executor

ADD wait_for.sh /wait_for.sh
ADD tng-vnv-executor.jar app.jar
ADD application.properties application.properties

#VOLUME /executor/bash_scripts
#VOLUME /executor/compose_files
VOLUME /executor

ENTRYPOINT ["java", "-jar", "-Xmx1024m", "-Xms256m", "-Dtng-vnv-executor", "-Djava.security.egd=file:/dev/./urandom", "/app.jar"]
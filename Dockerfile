FROM openjdk:8-jdk-alpine

COPY ./ /opt/app/

COPY file.txt /opt/app/file.txt

WORKDIR opt/app

RUN javac App.java

RUN javac Machine.java

EXPOSE 7070

EXPOSE 8080

EXPOSE 9090

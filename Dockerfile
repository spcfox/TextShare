FROM openjdk:17.0.1-jdk-slim
ENV LOG_LEVEL INFO
ADD build/libs/ShareText-0.1.0.jar /usr/src/ShareText-0.1.0.jar
WORKDIR /usr/src
EXPOSE 8080
ENTRYPOINT java -jar ShareText-0.1.0.jar

FROM openjdk:8-jre-alpine
ARG JAR_FILE=build/libs/nri-empcon.jar
ARG LOG_CONFIG_FILE=config/logback.xml.docker
RUN mkdir -p /apps/config
WORKDIR /app
COPY ${JAR_FILE} app.jar
COPY ${LOG_CONFIG_FILE} config/logback.xml
ENTRYPOINT ["java","-jar","app.jar","com.newrelic.fit.empcon.Main"]

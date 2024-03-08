FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp
RUN mkdir /tmp/creds ; chmod -R 775 /tmp/creds
COPY service-account-file.json /tmp/creds/service-account-file.json
EXPOSE 8080
ENV GOOGLE_APPLICATION_CREDENTIALS /tmp/creds/service-account-file.json
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]

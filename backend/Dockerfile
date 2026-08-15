 FROM bellsoft/liberica-openjre-alpine:21

WORKDIR /application

COPY build/libs/*.jar application.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "application.jar"]

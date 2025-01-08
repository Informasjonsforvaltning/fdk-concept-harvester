FROM eclipse-temurin:17-jre-alpine

ENV TZ=Europe/Oslo
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

VOLUME /tmp
COPY /target/fdk-concept-harvester.jar app.jar

CMD ["sh", "-c", "java -jar -Xmx3g $JAVA_OPTS app.jar"]

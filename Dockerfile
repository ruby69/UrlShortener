FROM azul/zulu-openjdk:17

VOLUME /tmp
ADD target/urlshortener.jar app.jar

ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ENV JAVA_OPTS="-XX:TieredStopAtLevel=1 -Djava.security.egd=file:/dev/./urandom"
ENTRYPOINT exec java $JAVA_OPTS -jar /app.jar
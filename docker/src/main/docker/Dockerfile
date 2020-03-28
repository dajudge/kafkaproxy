FROM oracle/graalvm-ce:19.3.1-java11

ADD *-runner.jar /app/app.jar
ADD lib/* /app/lib/

WORKDIR /app/

CMD ["java", "-jar", "app.jar"]
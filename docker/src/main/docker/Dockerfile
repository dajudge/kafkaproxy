FROM ubuntu:eoan-20200313
COPY *-runner /work/application
WORKDIR /work/
EXPOSE 8080
USER 1001

CMD ["./application", "-Xmx64m", "-Dquarkus.http.host=0.0.0.0"]
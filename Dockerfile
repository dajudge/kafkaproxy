FROM ubuntu:jammy-20221130@sha256:965fbcae990b0467ed5657caceaec165018ef44a4d2d46c7cdea80a9dff0d1ea
ARG APP_DIR=app/build/
COPY $APP_DIR/*-runner /work/application
WORKDIR /work/
EXPOSE 8080
USER 1001

CMD ["./application", "-Xmx64m", "-Dquarkus.http.host=0.0.0.0"]
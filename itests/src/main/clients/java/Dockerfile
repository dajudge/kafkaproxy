FROM ghcr.io/graalvm/graalvm-ce:java11-20.3

COPY . /app/
WORKDIR /app/

ARG KAFKA_CLIENT_VERSION_JAVA
RUN echo "Kafka client version: ${KAFKA_CLIENT_VERSION_JAVA}"
RUN ./gradlew build

ENTRYPOINT [""]
CMD ["sleep", "600"]
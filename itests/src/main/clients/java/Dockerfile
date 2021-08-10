FROM ghcr.io/graalvm/graalvm-ce:java11-20.3

COPY . /app/
WORKDIR /app/
RUN ./gradlew build

ENTRYPOINT [""]
CMD ["sleep", "600"]
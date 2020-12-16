FROM gradle:5.3.1-jdk11-slim as base

FROM base as build
ADD src /build/src/
ADD *.gradle /build/
WORKDIR /build
USER root
RUN gradle build

FROM base
COPY --from=build /build/build/libs /app
ADD run.sh /app/
CMD ["sleep", "600"]
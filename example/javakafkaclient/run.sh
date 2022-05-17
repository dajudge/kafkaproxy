#!/bin/bash
docker run -it --rm --network host -v gradle-cache:/home/gradle/.gradle -v "$PWD":/work -w /work gradle:jdk8 ./gradlew run

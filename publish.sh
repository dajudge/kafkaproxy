#! /bin/bash

set -e

DIR="$(cd "$(dirname "$0")"; pwd)";

TAG=${GITHUB_REF##*/}
if [ $TAG = "master" ]; then
  TAG="devel"
fi

IMAGE_NAME="dajudge/kafkaproxy:$TAG"

echo "Building $IMAGE_NAME..."
docker build . -t $IMAGE_NAME

echo "Pushing $IMAGE_NAME to Docker Hub..."
docker login -u $DOCKERHUB_USERNAME -p $DOCKERHUB_PASSWORD
docker push $IMAGE_NAME
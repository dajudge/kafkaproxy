#! /bin/sh

DIR="$(cd "$(dirname "$0")"; pwd)";

IMG=$(docker build -q $DIR/.workbench)

DOCKER_ARGS="-it"

if [ "$#" -eq 0 ]; then
    CMD=bash
else
    CMD="$@"
fi

if [ ! -z "$CI_PROJECT_DIR" ]; then
    CONTAINER_OPTS="-v /root:/root -v /${CI_PROJECT_DIR}:/${CI_PROJECT_DIR}"
    USER=root
else
    CONTAINER_OPTS="-v $HOME:$HOME -it"
fi

docker run --rm \
    --net host \
    -v /etc/passwd:/etc/passwd \
    -v /etc/group:/etc/group \
    -v /tmp:/tmp \
    -e HOME:${HOME} \
    -e http_proxy="$http_proxy" \
    -e https_proxy="$https_proxy" \
    -e no_proxy="$no_proxy" \
    -v "${DIR}":/project \
    -v /var/run/docker.sock:/var/run/docker.sock \
    $CONTAINER_OPTS $IMG \
    sudo -u ${USER} -E sh -c "cd ${DIR}; ${CMD}"
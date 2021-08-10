FROM ghcr.io/graalvm/graalvm-ce:java11-20.3

RUN	yum install -y bash curl sudo wget tar

RUN mkdir -p /opt && \
    wget -qO- https://download.docker.com/linux/static/stable/x86_64/docker-17.12.1-ce.tgz | tar xvz -C /opt && \
    ln -s /opt/docker/docker /usr/bin/docker

RUN curl -L "https://github.com/docker/compose/releases/download/1.25.3/docker-compose-$(uname -s)-$(uname -m)" -o /usr/bin/docker-compose && \
    chmod 755 /usr/bin/docker-compose

RUN gu install native-image

RUN yum install -y gcc glibc-devel zlib-devel

CMD ["bash"]
FROM amazoncorretto:8u232

RUN	yum install -y bash curl sudo python-pip wget tar

RUN mkdir -p /opt && \
    wget -qO- https://download.docker.com/linux/static/stable/x86_64/docker-17.12.1-ce.tgz | tar xvz -C /opt && \
    ln -s /opt/docker/docker /usr/bin/docker

RUN pip install --upgrade pip
RUN pip install docker-compose

CMD ["bash"]
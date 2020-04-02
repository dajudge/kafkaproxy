![ci](https://gitlab.com/dajudge/kafkaproxy/badges/master/pipeline.svg)

# kafkaproxy
kafkaproxy is a reverse proxy for the wire protocol of Apache Kafka. 

Since the Kafka wire protocol publishes the set of available broker endpoints from the brokers to the clients during
client bootstrapping, the brokers must be configured to publish endpoints that are reachable from the client side. This
can be a cumbersome restriction in several different situations, such as:
* Network topologies preventing direct access to the broker nodes
* Multiple networks from which broker nodes should be reachable
* DNS resolution restrictions when accessing TLS secured broker nodes
* Using the sidecar pattern for TLS termination in Kubernetes 

This is where kafkaproxy comes into play and allows for transparent relaying of the Kafka wire protocol by rewriting
the relevant parts of the communication where the brokers publish the endpoint names - with user-configurable endpoints
where the the proxy instances can be reached.

# Run kafkaproxy in Docker
kafkaproxy is built to be run in a container. The released versions are available at [Docker Hub](https://hub.docker.com/r/dajudge/kafkaproxy).
## Command line
The following configuration parameters are mandatory:
* `KAFKAPROXY_HOSTNAME`: the hostname at which the proxy can be reached from the clients.
* `KAFKAPROXY_BASE_PORT`: the first port to be used by kafkaproxy.
* `KAFKAPROXY_BOOTSTRAP_SERVERS`: the bootstrap server via which the kafka cluster can be contacted. This is usually a load balancer in front of the kafka cluster.

For example:
```
docker run \
    --net host \
    -e KAFKAPROXY_HOSTNAME=localhost \
    -e KAFKAPROXY_BASE_PORT=4000 \
    -e KAFKAPROXY_BOOTSTRAP_SERVERS=kafka:9092 \
    -d dajudge/kafkaproxy:0.0.3
``` 
*Note:* You will have to make the proxy ports defined in your broker map available from outside the container with `-p PORT:PORT` if you're not using `--net host`.

## Demonstration setup with `docker-compose`
If you have `docker-compose` installed you can try out kafkaproxy by using the demonstration setup provided in the
`example` directory. So clone the [kafkaproxy repo](https://github.com/dajudge/kafkaproxy) and run the following commands:

**Step 1:** Start kafka, zookeeper and kafkaproxy.
```
docker-compose -f example/docker-compose.yml up -d
```
Kafka will take a couple of seconds to fully start and become available.

**Step 2:** Create `my-test-topic`.
```
docker run --rm --net host -i confluentinc/cp-zookeeper:5.2.1 kafka-topics --create --topic my-test-topic --bootstrap-server localhost:4000 --partitions 1 --replication-factor 1
```

**Step 3:** Publish a message to `my-test-topic`.
```
echo "Hello, kafkaproxy" | docker run --rm --net host -i confluentinc/cp-zookeeper:5.2.1 kafka-console-producer --broker-list localhost:4000 --topic my-test-topic
```

**Step 4:** Consume to produced message from `my-test-topic`.
```
docker run --rm --net host -it confluentinc/cp-zookeeper:5.2.1 kafka-console-consumer --bootstrap-server localhost:4000 --topic my-test-topic --from-beginning --max-messages 1
```

**Cleanup:** Stop and remove the demonstration containers.
```
docker-compose -f example/docker-compose.yml rm -sf
```

**Explanation:** The `docker-compose.yml` file starts up a kafka broker (along with it's required zookeeper) that is
only available from within the docker network as `kafka1:9092`. The kafkaproxy is configured to
proxy this kafka instance as `localhost:4000` which is also mapped from outside the docker network.

# Configuration
kafkaproxy is configured using mostly environment variables and a broker map file in YAML format. The following
section describe the configuration options in detail.

## General configuration
kafkaproxy requires some general information to start. 

| Name                           | Default value | Destription
| ------------------------------ | ------------- | -----------
| `KAFKAPROXY_HOSTNAME`          |               | The hostname of the proxy as seen by the clients.
| `KAFKAPROXY_BASE_PORT`         |               | The base of the ports to be used by the proxy. Each new required port is created by incrementing on top of the base port.
| `KAFKAPROXY_BOOTSTRAP_SERVERS` |               | The comma separated list of initially mapped endpoints. This is usually the list of bootstrap brokers or a load balancer in front of the kafka brokers.
| `KAFKAPROXY_LOG_LEVEL`         | `INFO`        | The log level of the root logger. This must be a valid log level for [logback](http://logback.qos.ch/manual/configuration.html).
 
## Client SSL configuration
The client SSL configuration determines how the Kafka clients have to connect to the kafkaproxy instances.
Configuration can be provided using the following environment variables:

| Name                                        | Default value | Destription
| ------------------------------------------- |---------------| -----------
| `KAFKAPROXY_CLIENT_SSL_ENABLED`             | `false`       | Enables SSL encrypted communication between clients and kafkaproxy. 
| `KAFKAPROXY_CLIENT_SSL_TRUSTSTORE_LOCATION` |               | The filesystem location of the trust store to use. If no value is provided the JRE's default trust store will be used.
| `KAFKAPROXY_CLIENT_SSL_TRUSTSTORE_PASSWORD` |               | The password to access the trust store. Provide no value if the trust store is not password protected.
| `KAFKAPROXY_CLIENT_SSL_KEYSTORE_LOCATION`   |               | The filesystem location of the proxy's server key store. If no value is provided the JRE's default key store will be used.
| `KAFKAPROXY_CLIENT_SSL_KEYSTORE_PASSWORD`   |               | The password to access the proxy's server key store. Provide no value if the key store is not password protected.
| `KAFKAPROXY_CLIENT_SSL_KEY_PASSWORD`        |               | The password to access the proxy's server key. Provide no value if the key is not password protected.
| `KAFKAPROXY_CLIENT_SSL_AUTH_REQUIRED`       | `false`       | Require a valid client certificate from clients connecting to the proxy.

## Kafka SSL configuration
The Kafka SSL configuration determines how kafkaproxy connects to the Kafka broker instances.
Configuration can be provided using the following environment variables:

| Name                                        | Default value | Destription
| ------------------------------------------- |---------------| -----------
| `KAFKAPROXY_KAFKA_SSL_ENABLED`              | `false`       | Enables SSL encrypted communication kafkaproxy and the Kafka brokers.
| `KAFKAPROXY_KAFKA_SSL_TRUSTSTORE_LOCATION`  |               | The filesystem location of the trust store to use. If no value is provided the JRE's default trust store will be used.
| `KAFKAPROXY_KAFKA_SSL_TRUSTSTORE_PASSWORD`  |               | The password to access the trust store. Provide no value if the trust store is not password protected.
| `KAFKAPROXY_KAFKA_SSL_VERIFY_HOSTNAME`      | `true`        | Indicates if the hostnames of the Kafka brokers are validated against the SSL certificates they provide when connecting.
| `KAFKAPROXY_KAFKA_SSL_CLIENT_CERT_STRATEGY` | `NONE`        | <ul><li>`NONE`: don't use a client certificate for broker connections.</li><li>`KEYSTORE`: use the client certificate provided by `KAFKAPROXY_KAFKA_SSL_KEYSTORE_LOCATION`</li></ul>  
| `KAFKAPROXY_KAFKA_SSL_KEYSTORE_LOCATION`    |               | The filesystem location of the proxy's client key store. Required only when `KAFKAPROXY_KAFKA_SSL_CLIENT_CERT_STRATEGY` is set to `KEYSTORE`. 
| `KAFKAPROXY_KAFKA_SSL_KEYSTORE_PASSWORD`    |               | The password to access the proxy's client key store. Provide no value if the key store is not password protected.
| `KAFKAPROXY_KAFKA_SSL_KEY_PASSWORD`         |               | The password to access the proxy's client key. Provide no value if the key is not password protected.

# Features
* SSL support from client to proxy
* SSL support from proxy to broker
* TODO: Forwarding 2-way client SSL authentication information via on-the-fly generation of impostor certificates via
  * local KeyPair generation / signing
  * CFSSL
  * AWS Certificate Manager Private CA

# Further Reading
* [A Guide To The Kafka Protocol](https://cwiki.apache.org/confluence/display/KAFKA/A+Guide+To+The+Kafka+Protocol)
* [Kafka protocol guide](http://kafka.apache.org/protocol.html)

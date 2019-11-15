![ci](https://gitlab.com/dajudge/kafkaproxy/badges/master/pipeline.svg)

# kafkaproxy
kafkaproxy is a reverse proxy for the wire protocol of Apache Kafka. 

Since the Kafka wire protocol publishes the set of available broker endpoints from the brokers to the clients during
client bootstrapping, the brokers must be configured to publish endpoints that are reachable from the client side. This
can be a cumbersome restriction in several different situations, such as:
* Network topologies preventing direct access to the broker nodes
* Multiple networks from which broker nodes should be reachable
* DNS resolution restrictions when accessing TLS secured broker nodes

This is where kafkaproxy comes into play and allows for transparent relaying of the Kafka wire protocol by rewriting
the relevant parts of the communication where the brokers publish the endpoint names - with user-configurable endpoints
where the the proxy instances can be reached.

# Run kafkaproxy in Docker
TODO

# Configuration
kafkaproxy is configured using mostly environment variables and a broker map file in YAML format. The following
section describe the configuration options in detail.

## Client SSL configuration
The client SSL configuration determines how the Kafka clients have to connect to the kafkaproxy instances.
Configuration can be provided using the following environment variables:

| Name                                        | Default value | Destription
| ------------------------------------------- |---------------| -----------
| `KAFKAPROXY_CLIENT_SSL_ENABLED`             | `false`       | Enables SSL encrypted communication between clients and kafkaproxy. 
| `KAFKAPROXY_CLIENT_SSL_TRUSTSTORE_LOCATION` |               | The filesystem location of the trust store to use. If no value is provided the JRE's default trust store will be used.
| `KAFKAPROXY_CLIENT_SSL_TRUSTSTORE_PASSWORD` |               | The password to access the trust store. Provide no value if the trust store is not password protected.
| `KAFKAPROXY_CLIENT_SSL_KEYSTORE_LOCATION`   |               | The filesystem location of the key store. If not value is provided the JRE's default key store will be used.
| `KAFKAPROXY_CLIENT_SSL_KEYSTORE_PASSWORD`   |               | The password to access the key store. Provide no value if the key store is not password protected.
| `KAFKAPROXY_CLIENT_SSL_KEY_PASSWORD`        |               | The password to access the key. Provide no value if the key is not password protected.

## Kafka SSL configuration
The Kafka SSL configuration determines how kafkaproxy connects to the Kafka broker instances.
Configuration can be provided using the following environment variables:

| Name                                        | Default value | Destription
| ------------------------------------------- |---------------| -----------
| `KAFKAPROXY_KAFKA_SSL_ENABLED`              | `false`       | Enables SSL encrypted communication kafkaproxy and the Kafka brokers.
| `KAFKAPROXY_KAFKA_SSL_TRUSTSTORE_LOCATION`  |               | The filesystem location of the trust store to use. If no value is provided the JRE's default trust store will be used.
| `KAFKAPROXY_KAFKA_SSL_TRUSTSTORE_PASSWORD`  |               | The password to access the trust store. Provide no value if the trust store is not password protected.
| `KAFKAPROXY_KAFKA_SSL_VERIFY_HOSTNAME`      | `true`        | Indicates if the hostnames of the Kafka brokers are validated against the SSL certificates they provide when connecting.


## Broker map & proxy configuration
kafkaproxy needs a mapping configuration in order to know how to replace the brokers' endpoints with the endpoints
where the kafkaproxy instance(s) are reachable. The broker map is provided as a YAML configuration file.

The location of the broker map & proxy configuration is configured using the following environment variable:

| Name                            | Default value                   | Destription
| ------------------------------- |---------------------------------| -----------
| `KAFKAPROXY_BROKERMAP_LOCATION` | `/etc/kafkaproxy/brokermap.yml` | The filesystem location where the broker map YAML file is located.
| `KAFKAPROXY_PROXIED_BROKERS`    | `*`                             | The list of comma-separated symbolic names of the proxy entries from the broker map YAML file that the kafkaproxy instance should be starting. This setting is useful when different kafkaproxy instances are started to proxy multiple brokers. If you set `*` then all configured proxies will be started.
 
### Format of `brokermap.yml`

| Configuration key         | Value type | Destription
| ------------------------- | ---------- | -----------
| `proxies`                 | `List`     | The list of all proxied brokers.
| `proxies.name`            | `String`   | A unique symbolic name for the proxied broker. This name is used in the `KAFKAPROXY_PROXIED_BROKERS` environment variable to identify the brokers to proxy.
| `proxies.proxy`           | `Object`   | The proxy configuration for the proxied broker.
| `proxies.proxy.hostname`  | `String`   | The hostname of the proxy instance. This hostname must be reachable from the Kafka proxy.
| `proxies.proxy.port`      | `Integer`  | The port of the proxy instance.
| `proxies.broker`          | `Object`   | The proxy configuration for the proxied broker.
| `proxies.broker.hostname` | `String`   | The hostname of the Kafka broker instance. The Kafka broker must be reachable from kafkaproxy under this hostname and identify itself using the hostname in message responses. 
| `proxies.broker.port`     | `Integer`  | The port of the Kafka broker instance.
 
### Example `brokermap.yml`
This is an example with two proxied brokers:
```yaml
proxies:
  - name: broker1
    proxy:
      hostname: kafka.example.com
      port: 39092
    broker:
      hostname: broker1.kafka.local
      port: 9092
  - name: broker2
    proxy:
      hostname: kafka.example.com
      port: 39093
    broker:
      hostname: broker2.kafka.local
      port: 9092
```
# Features
* SSL support from client to proxy
* SSL support from proxy to broker
* TODO: Forwarding 2-way client SSL authentication information via on-the-fly generation of client certificates via
  * local KeyPair generation / signing
  * CFSSL
  * AWS Certificate Manager Private CA

# Further Reading
* [A Guide To The Kafka Protocol](https://cwiki.apache.org/confluence/display/KAFKA/A+Guide+To+The+Kafka+Protocol)
* [Kafka protocol guide](http://kafka.apache.org/protocol.html)

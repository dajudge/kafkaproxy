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

kafkaproxy is based on netty for high performance and low resource consumption.  

# Features
* TODO: TLS encrypted communication from client to proxy
* TLS encrypted communication from proxy to broker
* TODO: Forwarding 2-way client TLS authentication information with on-the-fly generation of client certificates via
  * local KeyPair generation / signing
  * CFSSL
  * AWS Certificate Manager Private CA  

# Broker mapping
kafkaproxy needs a mapping configuration in order to know how to replace the brokers' endpoints with the endpoints
where the kafkaproxy instance(s) are reachable. 
```yaml
proxies:
  - hostname: kafka.example.com
    port: 39092
    broker:
      hostname: broker1.kafka.local
      port: 9092
  - hostname: kafka.example.com
    port: 39093
    broker:
      hostname: broker2.kafka.local
      port: 9092
```

# Run kafkaproxy in Docker
TODO

#! /bin/sh

echo "Waiting for advertised listeners..."
while [ ! -f /tmp/advertisedListeners.txt ]; do sleep 1; done

echo "Advertised listeners: $(cat /tmp/advertisedListeners.txt)"

KAFKA_ADVERTISED_LISTENERS=$(cat /tmp/advertisedListeners.txt) /etc/confluent/docker/run
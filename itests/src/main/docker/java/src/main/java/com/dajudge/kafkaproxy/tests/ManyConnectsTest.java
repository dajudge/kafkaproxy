package com.dajudge.kafkaproxy.tests;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class ManyConnectsTest {

    public static void main(final String[] args) throws ExecutionException, InterruptedException {
        for (int i = 0; i < 100000; i++) {
            if ((i % 100) == 0) {
                System.out.println("Connection attempts #" + (i + 1));
            }
            final Properties props = new Properties();
            props.setProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:4000");
            try (final AdminClient client = AdminClient.create(props)) {
                client.listTopics().names().get();
            }
        }
    }
}

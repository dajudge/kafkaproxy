package com.dajudge.kafkaproxy.tests;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static java.lang.Integer.parseInt;

public class ManyConnectsTest {

    private String bootstrapServers;

    public ManyConnectsTest(final String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public boolean run() throws ExecutionException, InterruptedException {
        final int connectionAttempts = getConnectionAttempts();
        System.out.println("Connection attempts to run: " + connectionAttempts);
        for (int i = 0; i < connectionAttempts; i++) {
            if ((i % 100) == 0) {
                System.out.println("Connection attempts #" + (i + 1));
            }
            final Properties props = new Properties();
            props.setProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            try (final AdminClient client = AdminClient.create(props)) {
                client.listTopics().names().get();
            }
        }
        return true;
    }

    private int getConnectionAttempts() {
        try {
            return parseInt(System.getenv("CONNECTION_ATTEMPTS"));
        } catch (final Exception e) {
            return 100000;
        }
    }


}

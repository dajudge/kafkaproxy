package com.dajudge.kafkaproxy.tests;

import java.util.stream.Stream;

public class Startup {
    public interface Test {
        boolean run() throws Exception;
    }

    public static void main(final String[] args) {
        final boolean succeeded = Stream.<Test>of(
                new ManyConnectsTest(args[0])::run,
                new MultiClientTest(args[0])::run
        ).map(test -> {
            try {
                final boolean result = test.run();
                if (result) {
                    System.out.println("===> Test run succeeded");
                } else {
                    System.out.println("===> Test run failed");
                }
                return result;
            } catch (final Exception e) {
                new RuntimeException("===> Test exception", e).printStackTrace();
                return false;
            }
        }).reduce(true, Boolean::logicalAnd);
        System.exit(succeeded ? 0 : 1);
    }
}

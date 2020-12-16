using System;
using System.Collections.Generic;
using Confluent.Kafka;
using Confluent.Kafka.Admin;

namespace testrunner
{
    public class Startup
    {
        public static void Main(string[] args)
        {
            var bootstrapServers = Environment.GetEnvironmentVariable("BOOTSTRAP_SERVERS");
            if (bootstrapServers == null || bootstrapServers.Trim().Length == 0)
            {
                Console.Error.WriteLine("Please set BOOTSTRAP_SERVERS env variable.");
                Environment.ExitCode = 1;
                return;
            }

            var runner = new TestRunner(bootstrapServers, 10, 10);
            runner.CreateTopic();
            runner.Produce();
            runner.WaitForConsumers();
            var success = runner.Report();
            runner.Dispose();
            var status = success ? "SUCCEEDED" : "FAILED";
            Console.Out.WriteLine("Test run " + status + ".");
            if (!success)
            {
                Environment.ExitCode = 1;
            }
        }
    }
}
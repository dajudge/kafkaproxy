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
            var bootstrapServers = args[0];
            var runner = new TestRunner(bootstrapServers,10, 10);
            runner.CreateTopic();
            runner.Produce();
            runner.WaitForConsumers();
            runner.Report();
            runner.Dispose();
        }
    }
}
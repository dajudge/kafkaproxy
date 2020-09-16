using System;
using System.Collections.Generic;
using System.Threading;
using Confluent.Kafka;
using Confluent.Kafka.Admin;

namespace testrunner
{
    public class TestRunner : IDisposable 
    {
        private readonly ISet<string> _messages = new HashSet<string>();
        private readonly PollMaster _pollMaster;
        private readonly SinkMaster _sinkMaster;

        private readonly string _bootstrapServers;
        private readonly string _topic;
        private readonly string _groupId;

        private int _producedMessages;

        private Sink CreateSink()
        {
            var producer = new ProducerBuilder<string, string>(new ProducerConfig
            {
                BootstrapServers = _bootstrapServers
            }).Build();
            return new ProducerSink(producer, _topic);
        }

        private Poll CreatePoll()
        {
            var consumer = new ConsumerBuilder<string, string>(new ConsumerConfig
            {
                BootstrapServers = _bootstrapServers,
                GroupId = _groupId,
                AutoOffsetReset = AutoOffsetReset.Earliest
            }).Build();
            consumer.Subscribe(_topic);
            return new ConsumerPoll(consumer);
        }

        public TestRunner(
            in string bootstrapServers,
            in int sinkCount,
            in int pollCount
        )
        {
            _bootstrapServers = bootstrapServers;
            _groupId = Guid.NewGuid().ToString();
            _topic = Guid.NewGuid().ToString();
            _pollMaster = new PollMaster(_messages, pollCount, CreatePoll);
            _sinkMaster = new SinkMaster(_messages, sinkCount, CreateSink);
        }

        public void CreateTopic()
        {
            using (var admin = new AdminClientBuilder(new AdminClientConfig
            {
                BootstrapServers = _bootstrapServers
            }).Build())
            {
                Console.WriteLine("Creating topic: " + _topic);
                var topicSpec = new TopicSpecification {NumPartitions = 10, ReplicationFactor = 1, Name = _topic};
                admin.CreateTopicsAsync(new[] {topicSpec}).GetAwaiter().GetResult();
            }
        }

        public void Produce()
        {
            Console.WriteLine("Producing to " + _topic);
            var start = TimeUtils.CurrentTimeMillis();
            var lastStats = TimeUtils.CurrentTimeMillis();
            while (TimeUtils.CurrentTimeMillis() - start < 10000)
            {
                if (TimeUtils.CurrentTimeMillis() - lastStats > 1000)
                {
                    lastStats = TimeUtils.CurrentTimeMillis();
                    Console.WriteLine("sent: " + _producedMessages + " inflight: " + _messages.Count);
                }

                _sinkMaster.RandomSink().Publish("" + _producedMessages);
                _producedMessages++;
                if (_producedMessages % 100 == 0)
                {
                    Thread.Yield();
                }
            }
        }

        public void WaitForConsumers()
        {
            Console.WriteLine("Producing phase complete, waiting for consumers to finish...");
            while (_messages.Count != 0 &&
                   (TimeUtils.CurrentTimeMillis() - _pollMaster.GetLastMessageTimestamp()) < 10000)
            {
                Thread.Sleep(1);
            }
        }

        public void Report()
        {
            Console.WriteLine("Produced messages: " + _producedMessages);
            if (_messages.Count > 20)
            {
                Console.WriteLine("Unseen messages: " + _messages.Count);
            }
            else
            {
                Console.WriteLine("Unseen messages: " + string.Join(",", _messages));
            }
        }

        public void Dispose()
        {
            _pollMaster.Dispose();
            _sinkMaster.Dispose();
        }
    }
}
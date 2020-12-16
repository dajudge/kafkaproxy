using System;
using Confluent.Kafka;

namespace testrunner
{
    public class ConsumerPoll : Poll
    {
        private readonly IConsumer<string, string> _consumer;

        public ConsumerPoll(
            in IConsumer<string, string> consumer
        )
        {
            _consumer = consumer;
        }

        public string Poll()
        {
            return _consumer.Consume(TimeSpan.FromMilliseconds(100))?.Message?.Value;
        }

        public void Cancel()
        {
            _consumer.Close();
        }

        public void Dispose()
        {
            _consumer.Dispose();
        }
    }
}
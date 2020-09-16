using Confluent.Kafka;

namespace testrunner
{
    public class ProducerSink : Sink
    {
        private readonly IProducer<string, string> _producer;
        private readonly string _topic;

        public ProducerSink(
            in IProducer<string, string> producer,
            in string topic
        )
        {
            _producer = producer;
            _topic = topic;
        }
        
        public void Publish(string message)
        {
            _producer.ProduceAsync(_topic, new Message<string, string>
            {
                Key = message,
                Value = message
            }).GetAwaiter().GetResult();
        }

        public void Dispose()
        {
            _producer.Dispose();
        }
    }
}
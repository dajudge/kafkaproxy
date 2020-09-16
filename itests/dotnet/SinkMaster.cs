using System;
using System.Collections.Generic;

namespace testrunner
{
    public delegate Sink SinkFactory();

    internal class SinkWrapper : Sink
    {
        private readonly Sink _wrapped;
        private readonly ISet<string> _messages;

        public SinkWrapper(Sink wrapped, ISet<string> messages)
        {
            _wrapped = wrapped;
            _messages = messages;
        }

        public void Dispose()
        {
            _wrapped.Dispose();
        }

        public void Publish(string message)
        {
            lock (_messages)
            {
                _messages.Add(message);
            }

            _wrapped.Publish(message);
        }
    }

    public class SinkMaster : IDisposable
    {
        private readonly IList<Sink> _sinks = new List<Sink>();
        private readonly Random _rand = new Random();

        public SinkMaster(
            in ISet<string> messages,
            in int sinkCount,
            in SinkFactory sinkFactory
        )
        {
            for (var i = 0; i < sinkCount; i++)
            {
                _sinks.Add(new SinkWrapper(sinkFactory(), messages));
            }
        }

        public Sink RandomSink()
        {
            return _sinks[_rand.Next(_sinks.Count)];
        }

        public void Dispose()
        {
            foreach (var sink in _sinks)
            {
                sink.Dispose();
            }
        }
    }
}
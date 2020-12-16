using System;

namespace testrunner
{
    public interface Sink : IDisposable
    {
        public void Publish(string message);
    }
}
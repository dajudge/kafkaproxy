using System;
using System.Collections.Generic;

namespace testrunner
{
    public delegate Poll PollFactory();

    public class PollMaster : IDisposable
    {
        private readonly IList<PollThread> pollThreads = new List<PollThread>();
        private long _lastMessageTimestamp;

        public PollMaster(
            in ISet<string> messages,
            in int pollThreadCount,
            in PollFactory pollFactory
        )
        {
            for (var i = 0; i < pollThreadCount; i++)
            {
                pollThreads.Add(new PollThread(
                    pollFactory(),
                    messages,
                    UpdateLastMessageTimestamp
                ));
            }

            foreach (var pollThread in pollThreads)
            {
                pollThread.Start();
            }
        }

        private void UpdateLastMessageTimestamp()
        {
            _lastMessageTimestamp = TimeUtils.CurrentTimeMillis();
        }

        public long GetLastMessageTimestamp()
        {
            return _lastMessageTimestamp;
        }

        public void Dispose()
        {
            foreach (var pollThread in pollThreads)
            {
                pollThread.Dispose();
            }
        }
    }
}
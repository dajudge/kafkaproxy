using System;
using System.Collections.Generic;
using System.Threading;

namespace testrunner
{
    public delegate void OnMessageCallback();

    public class PollThread : IDisposable
    {
        private readonly Poll _poll;
        private readonly ISet<string> _messages;
        private readonly OnMessageCallback _onMessageCallback;
        private readonly Thread _thread;
        private bool _exit;

        public PollThread(
            in Poll poll,
            in ISet<string> messages,
            in OnMessageCallback onMessageCallback
        )
        {
            _poll = poll;
            _messages = messages;
            _onMessageCallback = onMessageCallback;
            _thread = new Thread(Run);
        }

        public void Start()
        {
            _thread.Start();
        }

        private void Run(object p)
        {
            try
            {
                while (!_exit)
                {
                    try
                    {
                        var result = _poll.Poll();
                        if (result == null) continue;
                        lock (_messages)
                        {
                            _messages.Remove(result);
                        }

                        _onMessageCallback();
                    }
                    catch (Exception e)
                    {
                        Console.Error.WriteLine(e);
                    }
                }
            }
            finally
            {
                _poll.Dispose();
            }
        }

        public void Dispose()
        {
            _exit = true;
        }
    }
}
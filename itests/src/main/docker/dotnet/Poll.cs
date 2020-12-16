using System;

namespace testrunner
{
    public interface Poll : IDisposable
    {
        string Poll();
    }
}
using System;

namespace testrunner
{
    public class TimeUtils
    {
        private static readonly DateTime Epoch = new DateTime
            (1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);

        public static long CurrentTimeMillis()
        {
            return (long) (DateTime.UtcNow - Epoch).TotalMilliseconds;
        }
    }
}
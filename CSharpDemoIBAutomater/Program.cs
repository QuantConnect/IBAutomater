using System;
using System.Threading;
using QuantConnect.IBAutomater;

namespace CSharpDemoIBAutomater
{
    public class Program
    {
        public static void Main(string[] args)
        {
            // IBAutomater settings
            var ibDirectory = IsLinux ? "~/Jts" : "C:\\Jts";
            var ibVersion = "974";
            var ibUserName = "myusername";
            var ibPassword = "mypassword";
            var ibTradingMode = "paper";
            var ibPort = 4002;

            // Create a new instance of the IBAutomater class
            var automater = new IBAutomater(ibDirectory, ibVersion, ibUserName, ibPassword, ibTradingMode, ibPort);

            // Attach the event handlers
            automater.OutputDataReceived += (s, e) => Console.WriteLine($"{DateTime.UtcNow:O} {e.Data}");
            automater.ErrorDataReceived += (s, e) => Console.WriteLine($"{DateTime.UtcNow:O} {e.Data}");
            automater.Exited += (s, e) => Console.WriteLine($"{DateTime.UtcNow:O} IBAutomater exited [ExitCode:{e.ExitCode}]");

            // Start the IBAutomater
            if (!automater.Start(false))
            {
                Console.WriteLine("Failed to start IBAutomater");
            }

            // Wait a few seconds for startup
            Console.WriteLine("IBAutomater started, waiting 30 seconds");
            Thread.Sleep(30000);

            Console.WriteLine("IBAutomater is " + (automater.IsRunning() ? "" : "not ") + "running");

            // Stop the IBAutomater
            automater.Stop();

            Console.WriteLine("IBAutomater stopped");

            Console.WriteLine("IBAutomater is " + (automater.IsRunning() ? "" : "not ") + "running");
        }

        private static bool IsLinux
        {
            get
            {
                var p = (int)Environment.OSVersion.Platform;
                return p == 4 || p == 6 || p == 128;
            }
        }
    }
}

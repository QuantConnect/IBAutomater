/*
 * QUANTCONNECT.COM - Democratizing Finance, Empowering Individuals.
 * IBAutomater v1.0. Copyright 2019 QuantConnect Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

using System;
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
            using var automater = new IBAutomater(ibDirectory, ibVersion, ibUserName, ibPassword, ibTradingMode, ibPort, false);

            // Attach the event handlers
            automater.OutputDataReceived += (s, e) => Console.WriteLine($"{DateTime.UtcNow:O} {e.Data}");
            automater.ErrorDataReceived += (s, e) => Console.WriteLine($"{DateTime.UtcNow:O} {e.Data}");
            automater.Exited += (s, e) => Console.WriteLine($"{DateTime.UtcNow:O} IBAutomater exited [ExitCode:{e.ExitCode}]");

            // Start the IBAutomater
            var result = automater.Start(false);
            if (result.HasError)
            {
                Console.WriteLine($"Failed to start IBAutomater - Code: {result.ErrorCode}, Message: {result.ErrorMessage}");
                automater.Stop();
                return;
            }

            // Stop the IBAutomater
            automater.Stop();

            Console.WriteLine("IBAutomater stopped");
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

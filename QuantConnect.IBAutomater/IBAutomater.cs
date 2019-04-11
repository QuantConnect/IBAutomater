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
using System.Diagnostics;
using System.IO;
using Newtonsoft.Json.Linq;

namespace QuantConnect.IBAutomater
{
    public class IBAutomater
    {
        public event EventHandler<DataReceivedEventArgs> OutputDataReceived;
        public event EventHandler<DataReceivedEventArgs> ErrorDataReceived;
        public event EventHandler<int> Exited;

        private readonly string _ibDirectory;
        private readonly string _userName;
        private readonly string _password;
        private readonly string _tradingMode;
        private readonly int _portNumber;

        public static void Main(string[] args)
        {
            var json = File.ReadAllText("config.json");
            var config = JObject.Parse(json);

            var ibDirectory = config["ib-tws-dir"].ToString();
            var userName = config["ib-user-name"].ToString();
            var password = config["ib-password"].ToString();
            var tradingMode = config["ib-trading-mode"].ToString();
            var portNumber = config["ib-port"].ToObject<int>();

            var automater = new IBAutomater(ibDirectory, userName, password, tradingMode, portNumber);

            automater.OutputDataReceived += (s, e) => Console.WriteLine($"{DateTime.UtcNow:O} {e.Data}");
            automater.ErrorDataReceived += (s, e) => Console.WriteLine($"{DateTime.UtcNow:O} {e.Data}");
            automater.Exited += (s, e) => Console.WriteLine($"{DateTime.UtcNow:O} IBAutomater exited [{e}]");

            automater.Start(true);
        }

        public IBAutomater(string ibDirectory, string userName, string password, string tradingMode, int portNumber)
        {
            _ibDirectory = ibDirectory;
            _userName = userName;
            _password = password;
            _tradingMode = tradingMode;
            _portNumber = portNumber;
        }

        public Process Start(bool waitForExit)
        {
            const string ibVersion = "974";

            var fileName = IsWindows ? "IBAutomater.bat" : "IBAutomater.sh";
            var arguments = $"{_ibDirectory} {ibVersion} {_userName} {_password} {_tradingMode} {_portNumber}";

            var process = new Process
            {
                StartInfo = new ProcessStartInfo(fileName, arguments)
                {
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    UseShellExecute = false,
                    WindowStyle = ProcessWindowStyle.Hidden,
                    CreateNoWindow = true
                },
                EnableRaisingEvents = true
            };

            process.OutputDataReceived += (sender, e) =>
            {
                OutputDataReceived?.Invoke(this, e);
            };

            process.ErrorDataReceived += (sender, e) =>
            {
                ErrorDataReceived?.Invoke(this, e);
            };

            process.Exited += (sender, e) =>
            {
                Exited?.Invoke(this, process.ExitCode);
            };

            process.Start();

            process.BeginErrorReadLine();
            process.BeginOutputReadLine();

            if (waitForExit)
            {
                process.WaitForExit();
            }

            return process;
        }

        private static bool IsLinux
        {
            get
            {
                var p = (int)Environment.OSVersion.Platform;
                return p == 4 || p == 6 || p == 128;
            }
        }

        private static bool IsWindows => !IsLinux;
    }
}

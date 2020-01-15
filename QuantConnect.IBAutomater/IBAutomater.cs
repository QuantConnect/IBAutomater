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
using System.Threading;
using Newtonsoft.Json.Linq;

namespace QuantConnect.IBAutomater
{
    /// <summary>
    /// The IB Automater is responsible for automating the configuration/logon process
    /// to the IB Gateway application and for handling/dismissing its popup windows.
    /// </summary>
    public class IBAutomater
    {
        private readonly string _ibDirectory;
        private readonly string _ibVersion;
        private readonly string _userName;
        private readonly string _password;
        private readonly string _tradingMode;
        private readonly int _portNumber;

        private readonly object _locker = new object();
        private bool _isStarting;
        private Process _process;

        /// <summary>
        /// Event fired when the process writes to the output stream
        /// </summary>
        public event EventHandler<OutputDataReceivedEventArgs> OutputDataReceived;

        /// <summary>
        /// Event fired when the process writes to the error stream
        /// </summary>
        public event EventHandler<ErrorDataReceivedEventArgs> ErrorDataReceived;

        /// <summary>
        /// Event fired when the process exits
        /// </summary>
        public event EventHandler<ExitedEventArgs> Exited;

        /// <summary>
        /// Main program for testing and/or standalone execution
        /// </summary>
        public static void Main(string[] args)
        {
            var json = File.ReadAllText("config.json");
            var config = JObject.Parse(json);

            var ibDirectory = config["ib-tws-dir"].ToString();
            var userName = config["ib-user-name"].ToString();
            var password = config["ib-password"].ToString();
            var tradingMode = config["ib-trading-mode"].ToString();
            var portNumber = config["ib-port"].ToObject<int>();
            var ibVersion = "974";
            if (config["ib-version"] != null)
            {
                ibVersion = config["ib-version"].ToString();
            }

            var automater = new IBAutomater(ibDirectory, ibVersion, userName, password, tradingMode, portNumber);

            automater.OutputDataReceived += (s, e) => Console.WriteLine($"{DateTime.UtcNow:O} {e}");
            automater.ErrorDataReceived += (s, e) => Console.WriteLine($"{DateTime.UtcNow:O} {e}");
            automater.Exited += (s, e) => Console.WriteLine($"{DateTime.UtcNow:O} IBAutomater exited [{e}]");

            if (!automater.Start(true))
            {
                Console.WriteLine("Error starting IBAutomater process.");
            }
        }

        /// <summary>
        /// Creates a new instance of the <see cref="IBAutomater"/> class
        /// </summary>
        /// <param name="ibDirectory">The root directory of IB Gateway</param>
        /// <param name="ibVersion">The IB Gateway version to launch</param>
        /// <param name="userName">The user name</param>
        /// <param name="password">The password</param>
        /// <param name="tradingMode">The trading mode ('paper' or 'live')</param>
        /// <param name="portNumber">The API port number</param>
        public IBAutomater(string ibDirectory, string ibVersion, string userName, string password, string tradingMode, int portNumber)
        {
            _ibDirectory = ibDirectory;
            _ibVersion = ibVersion;
            _userName = userName;
            _password = password;
            _tradingMode = tradingMode;
            _portNumber = portNumber;
        }

        /// <summary>
        /// Starts the IB Automater
        /// </summary>
        /// <param name="waitForExit">true if it should wait for the IB Gateway process to exit</param>
        /// <remarks>The IB Gateway application will be launched</remarks>
        public bool Start(bool waitForExit)
        {
            lock (_locker)
            {
                if (IsRunning())
                {
                    return true;
                }

                if (_isStarting)
                {
                    OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs("IBAutomater is already starting."));
                    return true;
                }

                _process = null;
                _isStarting = true;

                try
                {
                    if (IsLinux)
                    {
                        // debug testing
                        if (!File.Exists("IBAutomater.sh"))
                        {
                            throw new Exception($"IBAutomater.sh file not found - current directory: {Directory.GetCurrentDirectory()}");
                        }

                        if (!File.Exists("IBAutomater.jar"))
                        {
                            throw new Exception($"IBAutomater.jar file not found - current directory: {Directory.GetCurrentDirectory()}");
                        }

                        // need permission for execution
                        OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs("Setting execute permissions on IBAutomater.sh"));
                        ExecuteProcessAndWaitForExit("chmod", "+x IBAutomater.sh");
                    }

                    var fileName = IsWindows ? "IBAutomater.bat" : "IBAutomater.sh";
                    var arguments = $"{_ibDirectory} {_ibVersion} {_userName} {_password} {_tradingMode} {_portNumber}";

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
                        if (e.Data != null)
                        {
                            OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs(e.Data));
                        }
                    };

                    process.ErrorDataReceived += (sender, e) =>
                    {
                        if (e.Data != null)
                        {
                            ErrorDataReceived?.Invoke(this, new ErrorDataReceivedEventArgs(e.Data));
                        }
                    };

                    process.Exited += (sender, e) =>
                    {
                        Exited?.Invoke(this, new ExitedEventArgs(process.ExitCode));
                    };

                    var started = process.Start();
                    if (!started)
                    {
                        throw new Exception("IBAutomater was unable to start the IBGateway process.");
                    }

                    OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs($"IBAutomater process started - Id:{process.Id}"));

                    _process = process;
                    _isStarting = false;

                    process.BeginErrorReadLine();
                    process.BeginOutputReadLine();

                    if (waitForExit)
                    {
                        process.WaitForExit();
                    }
                }
                catch (Exception)
                {
                    _isStarting = false;
                    throw;
                }
            }

            return true;
        }

        /// <summary>
        /// Stops the IB Automater
        /// </summary>
        /// <remarks>The IB Gateway application will be terminated</remarks>
        public void Stop()
        {
            var stopped = false;

            lock (_locker)
            {
                if (!IsRunning())
                {
                    return;
                }

                if (IsWindows)
                {
                    foreach (var process in Process.GetProcesses())
                    {
                        try
                        {
                            if (process.MainWindowTitle.ToLower().Contains("ib gateway"))
                            {
                                process.Kill();
                                stopped = true;
                            }
                        }
                        catch (Exception)
                        {
                            // ignored
                        }
                    }
                }
                else
                {
                    try
                    {
                        Process.Start("pkill", "xvfb-run");
                        Process.Start("pkill", "java");
                        Process.Start("pkill", "Xvfb");
                        stopped = true;
                    }
                    catch (Exception)
                    {
                        // ignored
                    }
                }

                _process = null;
            }

            if (stopped)
            {
                Thread.Sleep(2500);
            }
        }

        /// <summary>
        /// Returns whether the IBGateway is running
        /// </summary>
        /// <returns>true if the IBGateway is running</returns>
        public bool IsRunning()
        {
            lock (_locker)
            {
                if (_isStarting)
                {
                    return true;
                }

                if (_process == null)
                {
                    return false;
                }

                var exited = _process.HasExited;
                if (exited)
                {
                    _process = null;
                }

                return !exited;
            }
        }

        private void ExecuteProcessAndWaitForExit(string fileName, string arguments)
        {
            var p = new Process
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

            p.OutputDataReceived += (sender, e) =>
            {
                if (e.Data != null)
                {
                    OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs($"{fileName}: {e.Data}"));
                }
            };
            p.ErrorDataReceived += (sender, e) =>
            {
                if (e.Data != null)
                {
                    ErrorDataReceived?.Invoke(this, new ErrorDataReceivedEventArgs($"{fileName}: {e.Data}"));
                }
            };

            p.Start();
            p.BeginErrorReadLine();
            p.BeginOutputReadLine();
            p.WaitForExit();

            OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs($"{fileName} {arguments}: process exit code: {p.ExitCode}"));
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

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
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Threading;
using System.Threading.Tasks;
using Newtonsoft.Json.Linq;
using NodaTime;

namespace QuantConnect.IBAutomater
{
    /// <summary>
    /// The IB Automater is responsible for automating the configuration/logon process
    /// to the IB Gateway application and for handling/dismissing its popup windows.
    /// </summary>
    public class IBAutomater : IDisposable
    {
        private readonly TimeSpan _initializationTimeout = TimeSpan.FromMinutes(15);

        private readonly string _ibDirectory;
        private readonly string _ibVersion;
        private readonly string _userName;
        private readonly string _password;
        private readonly string _tradingMode;
        private readonly int _portNumber;
        private readonly bool _exportIbGatewayLogs;

        private volatile bool _isDisposeCalled;

        private readonly object _locker = new object();
        private Process _process;
        private StartResult _lastStartResult = StartResult.Success;
        private readonly AutoResetEvent _ibAutomaterInitializeEvent = new AutoResetEvent(false);
        private bool _isRestartInProgress;
        private bool _isFirstStart = true;
        private volatile bool _isAuthenticating;

        private enum Region { America, Europe, Asia }

        // Source: https://ibkr.info/article/2816
        private readonly Dictionary<string, Region> _ibServerMap = new Dictionary<string, Region>
        {
            { "gdc1.ibllc.com", Region.America },
            { "ndc1.ibllc.com", Region.America },
            { "ndc1_hb1.ibllc.com", Region.America },
            { "cdc1.ibllc.com", Region.America },
            { "cdc1_hb1.ibllc.com", Region.America },

            { "zdc1.ibllc.com", Region.Europe },
            { "zdc1_hb1.ibllc.com", Region.Europe },

            { "hdc1.ibllc.com", Region.Asia },
            { "hdc1_hb1.ibllc.com", Region.Asia },
            { "mcgw1.ibllc.com.cn", Region.Asia },
            { "mcgw1_hb1.ibllc.com.cn", Region.Asia }
        };

        private string _ibServerName;
        private Region _ibServerRegion = Region.America;

        private static readonly DateTimeZone TimeZoneNewYork = DateTimeZoneProviders.Tzdb["America/New_York"];
        private static readonly DateTimeZone TimeZoneZurich = DateTimeZoneProviders.Tzdb["Europe/Zurich"];
        private static readonly DateTimeZone TimeZoneHongKong = DateTimeZoneProviders.Tzdb["Asia/Hong_Kong"];

        // used to limit logging
        private bool _isWithinScheduledServerResetTimesLastValue;

        private string _ibGatewayLogFileName;
        private int _logLinesRead;
        private readonly Timer _timerLogReader;
        private readonly object _logLocker = new object();

        private static readonly TimeSpan _maxExpectedGatewayRestartTime = TimeSpan.FromMinutes(10);
        private int _gatewaySoftRestartCount;
        private bool _gatewaySoftRestartTimedOut;
        private CancellationTokenSource _gatewaySoftRestartTokenSource;

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
        /// Event fired when the process exits
        /// </summary>
        public event EventHandler Restarted;

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
            var exportIbGatewayLogs = config["ib-export-ibgateway-logs"].ToObject<bool>();

            // Create a new instance of the IBAutomater class
            using var automater = new IBAutomater(ibDirectory, ibVersion, userName, password, tradingMode, portNumber, exportIbGatewayLogs);

            // Attach the event handlers
            automater.OutputDataReceived += (s, e) => Console.WriteLine($"{DateTime.UtcNow:O} {e.Data}");
            automater.ErrorDataReceived += (s, e) => Console.WriteLine($"{DateTime.UtcNow:O} {e.Data}");
            automater.Exited += (s, e) => Console.WriteLine($"{DateTime.UtcNow:O} IBAutomater exited [ExitCode:{e.ExitCode}]");

            // Start the IBAutomater
            Console.WriteLine("===> Starting IBAutomater");
            var result = automater.Start(false);
            if (result.HasError)
            {
                Console.WriteLine($"Failed to start IBAutomater - Code: {result.ErrorCode}, Message: {result.ErrorMessage}");
                automater.Stop();
                return;
            }

            // Restart the IBAutomater
            Console.WriteLine("===> Restarting IBAutomater");
            result = automater.Restart();
            if (result.HasError)
            {
                Console.WriteLine($"Failed to restart IBAutomater - Code: {result.ErrorCode}, Message: {result.ErrorMessage}");
                automater.Stop();
                return;
            }

            // Stop the IBAutomater
            Console.WriteLine("===> Stopping IBAutomater");
            automater.Stop();
            Console.WriteLine("IBAutomater stopped");
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
        /// <param name="exportIbGatewayLogs">Export IB Gateway logs if true</param>
        public IBAutomater(string ibDirectory, string ibVersion, string userName, string password, string tradingMode, int portNumber, bool exportIbGatewayLogs)
        {
            _ibDirectory = ibDirectory;
            _ibVersion = ibVersion;
            _userName = userName;
            _password = password;
            _tradingMode = tradingMode;
            _portNumber = portNumber;
            _exportIbGatewayLogs = exportIbGatewayLogs;

            _timerLogReader = new Timer(LogReaderTimerCallback, null, Timeout.Infinite, Timeout.Infinite);


            Restarted += (sender, e) =>
            {
                StopGatewayRestartTimeoutMonitor();
            };

            StartGatewayRestartCountResetTask();
        }

        public void Dispose()
        {
            if (_isDisposeCalled)
            {
                return;
            }

            _isDisposeCalled = true;

            StopGatewayRestartTimeoutMonitor();

            // remove Java agent setting from IB configuration file
            UpdateIbGatewayConfiguration(GetIbGatewayVersionPath(), false);
        }

        /// <summary>
        /// Starts the IB Gateway
        /// </summary>
        /// <param name="waitForExit">true if it should wait for the IB Gateway process to exit</param>
        /// <remarks>The IB Gateway application will be launched</remarks>
        public StartResult Start(bool waitForExit)
        {
            lock (_locker)
            {
                if (_lastStartResult.HasError)
                {
                    // IBAutomater errors are unrecoverable
                    return _lastStartResult;
                }

                if (IsRunning())
                {
                    return StartResult.Success;
                }

                _process = null;
                _ibAutomaterInitializeEvent.Reset();

                if (IsLinux)
                {
                    // need permission for execution
                    OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs("Setting execute permissions on IBAutomater.sh"));
                    ExecuteProcessAndWaitForExit("chmod", "+x IBAutomater.sh");
                }

                var ibGatewayVersionPath = GetIbGatewayVersionPath();

                OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs($"Loading IBGateway - Version: {_ibVersion} - Path: {ibGatewayVersionPath} - User: {_userName}"));

                if (!Directory.Exists(ibGatewayVersionPath))
                {
                    return new StartResult(ErrorCode.IbGatewayVersionNotInstalled, $"Version: {_ibVersion} - Path: {ibGatewayVersionPath}");
                }

                var jreInstallPath = GetJreInstallPath(ibGatewayVersionPath);
                if (string.IsNullOrWhiteSpace(jreInstallPath))
                {
                    return new StartResult(ErrorCode.JavaNotFound);
                }

                UpdateIbGatewayIniFile();
                var javaAgent = UpdateIbGatewayConfiguration(ibGatewayVersionPath, true);

                _timerLogReader.Change(Timeout.Infinite, Timeout.Infinite);

                _ibGatewayLogFileName = Path.Combine(ibGatewayVersionPath, "IBAutomater.log");

                lock (_logLocker)
                {
                    if (File.Exists(_ibGatewayLogFileName))
                    {
                        File.Delete(_ibGatewayLogFileName);
                    }
                }

                _timerLogReader.Change(TimeSpan.Zero, TimeSpan.FromSeconds(1));

                string fileName;
                string arguments;
                if (IsWindows)
                {
                    fileName = $"{ibGatewayVersionPath}/ibgateway.exe";
                    arguments = string.Empty;
                }
                else
                {
                    fileName = "IBAutomater.sh";
                    arguments = $"{ibGatewayVersionPath} {javaAgent}";
                }

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

                process.OutputDataReceived += SendTraceLog;
                process.ErrorDataReceived += SendErrorLog;
                process.Exited += OnProcessExited;

                try
                {
                    var started = process.Start();
                    if (!started)
                    {
                        return new StartResult(ErrorCode.ProcessStartFailed);
                    }
                }
                catch (Exception exception)
                {
                    return new StartResult(
                        ErrorCode.ProcessStartFailed,
                        exception.Message.Replace(_password, "***"));
                }

                OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs($"IBAutomater process started - Id:{process.Id} - Name:{process.ProcessName} - InitializationTimeout:{_initializationTimeout}"));

                _process = process;

                process.BeginErrorReadLine();
                process.BeginOutputReadLine();

                if (waitForExit)
                {
                    process.WaitForExit();
                }
                else
                {
                    // wait for completion of IBGateway login and configuration
                    string message;
                    if (_ibAutomaterInitializeEvent.WaitOne(_initializationTimeout))
                    {
                        var processName = IsWindows ? "ibgateway" : "java";

                        var p = Process.GetProcessesByName(processName).FirstOrDefault();
                        OutputDataReceived?.Invoke(this,
                            p != null
                                ? new OutputDataReceivedEventArgs($"IBGateway process found - Id:{p.Id} - Name:{p.ProcessName}")
                                : new OutputDataReceivedEventArgs($"IBGateway process not found: {processName}"));

                        message = "IB Automater initialized.";
                    }
                    else
                    {
                        TraceIbLauncherLogFile();

                        var additionalMessage = string.Empty;
                        if (_isFirstStart && _isAuthenticating)
                        {
                            // unable to complete logon because IB weekend server reset is in progress
                            additionalMessage = "The logon process could not be completed because the IB server is busy or being reset for the weekend, please try again later.";
                        }

                        _lastStartResult = new StartResult(ErrorCode.InitializationTimeout, additionalMessage);
                        message = "IB Automater initialization timeout. " + additionalMessage;
                    }

                    _isFirstStart = false;

                    OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs(message));

                    if (_lastStartResult.HasError)
                    {
                        message = $"IBAutomater error - Code: {_lastStartResult.ErrorCode} Message: {_lastStartResult.ErrorMessage}";
                        OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs(message));

                        return _lastStartResult;
                    }
                }
            }

            return StartResult.Success;
        }

        private void LogReaderTimerCallback(object _)
        {
            try
            {
                lock (_logLocker)
                {
                    if (string.IsNullOrWhiteSpace(_ibGatewayLogFileName) || !File.Exists(_ibGatewayLogFileName))
                    {
                        return;
                    }

                    using (var fileStream = new FileStream(_ibGatewayLogFileName, FileMode.Open, FileAccess.Read, FileShare.ReadWrite))
                    {
                        fileStream.Seek(0, SeekOrigin.Begin);

                        using (var reader = new StreamReader(fileStream))
                        {
                            var lines = new List<string>();
                            string line;
                            while ((line = reader.ReadLine()) != null)
                            {
                                lines.Add(line);
                            }

                            var totalLines = lines.Count;
                            if (totalLines < _logLinesRead)
                            {
                                // log file was rewritten by a restart of IBGateway
                                _logLinesRead = 0;
                            }

                            var newLinesCount = totalLines - _logLinesRead;
                            var newLines = lines.Skip(_logLinesRead).Take(newLinesCount);
                            _logLinesRead = totalLines;

                            if (newLinesCount > 0)
                            {
                                foreach (var newLine in newLines)
                                {
                                    OnProcessOutputDataReceived(newLine);
                                }
                            }
                        }
                    }
                }
            }
            catch (Exception exception)
            {
                var message = $"IBAutomater error in timer - Message: {exception.Message}";
                OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs(message));
            }
        }

        private void OnProcessOutputDataReceived(string text)
        {
            if (text != null)
            {
                OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs(text));

                // login failed
                if (text.Contains("Login failed"))
                {
                    if (!IsWithinScheduledServerResetTimes())
                    {
                        _lastStartResult = new StartResult(ErrorCode.LoginFailed);
                    }

                    _ibAutomaterInitializeEvent.Set();
                }

                // an existing session was detected
                else if (text.Contains("Existing session detected"))
                {
                    _lastStartResult = new StartResult(ErrorCode.ExistingSessionDetected);
                    _ibAutomaterInitializeEvent.Set();
                }

                // a security dialog (2FA) was detected by IBAutomater
                else if (text.Contains("Second Factor Authentication"))
                {
                    if (text.Contains("[WINDOW_OPENED]"))
                    {
                        // waiting for 2FA confirmation on IBKR mobile app
                        const string message = "Waiting for 2FA confirmation on IBKR mobile app (to be confirmed within 3 minutes).";
                        OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs(message));
                    }
                }

                // 2FA timed out for the maximum number of attempts
                else if (text.Contains("2FA maximum attempts reached"))
                {
                    const string message = "IB Automater 2FA timeout.";
                    OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs(message));

                    _lastStartResult = new StartResult(ErrorCode.TwoFactorConfirmationTimeout);
                    _ibAutomaterInitializeEvent.Set();
                }

                // a security dialog (code card) was detected by IBAutomater
                else if (text.Contains("Security Code Card Authentication") || text.Contains("Enter security code"))
                {
                    _lastStartResult = new StartResult(ErrorCode.SecurityDialogDetected);
                    _ibAutomaterInitializeEvent.Set();
                }

                // the IBGateway version is no longer supported
                else if (text.Contains("is no longer supported"))
                {
                    _lastStartResult = new StartResult(ErrorCode.UnsupportedVersion);
                    _ibAutomaterInitializeEvent.Set();
                }

                // a Java exception was thrown
                if (text.StartsWith("Exception"))
                {
                    TraceIbLauncherLogFile();

                    _lastStartResult = new StartResult(ErrorCode.JavaException, text);
                    _ibAutomaterInitializeEvent.Set();
                }

                // API support is not available for accounts that support free trading
                else if (text.Contains("API support is not available"))
                {
                    _lastStartResult = new StartResult(ErrorCode.ApiSupportNotAvailable);
                    _ibAutomaterInitializeEvent.Set();
                }

                // an unknown message window was detected
                else if (text.StartsWith("Unknown message window detected"))
                {
                    TraceIbLauncherLogFile();

                    _lastStartResult = new StartResult(ErrorCode.UnknownMessageWindowDetected, text);
                    _ibAutomaterInitializeEvent.Set();
                }

                // initialization completed
                else if (text.Contains("Configuration settings updated"))
                {
                    // load server name and region
                    LoadIbServerInformation();

                    _ibAutomaterInitializeEvent.Set();
                }

                // daily restart with no authentication required
                else if (text.Contains("Restart in progress") || text.Contains("The application will automatically restart in"))
                {
                    _isRestartInProgress = true;
                }

                // weekly restart with full authentication
                else if (text.Contains("Auto-restart token expired"))
                {
                    _isRestartInProgress = false;
                    _ibAutomaterInitializeEvent.Set();
                }

                // authentication/connection in progress
                if (text.Contains("Window event:"))
                {
                    _isAuthenticating = text.Contains("Authenticating", StringComparison.InvariantCultureIgnoreCase) ||
                                        text.Contains("Connecting to server", StringComparison.InvariantCultureIgnoreCase) ||
                                        text.Contains("server error, will retry", StringComparison.InvariantCultureIgnoreCase);
                }
            }
        }

        private void OnProcessExited(object sender, EventArgs e)
        {
            OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs("IBGateway process exited"));

            if (_isRestartInProgress)
            {
                _ibAutomaterInitializeEvent.Reset();

                OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs("Waiting for IBGateway auto-restart"));
                if (!_ibAutomaterInitializeEvent.WaitOne(_initializationTimeout))
                {
                    TraceIbLauncherLogFile();

                    _lastStartResult = new StartResult(ErrorCode.InitializationTimeout);
                    return;
                }

                if (_isRestartInProgress)
                {
                    OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs("IB Automater initialized."));

                    // find new IBGateway process (created by auto-restart)

                    var process = GetIbGatewayProcess();
                    if (process == null)
                    {
                        OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs($"IBGateway restarted process not found"));

                        TraceIbLauncherLogFile();

                        _lastStartResult = new StartResult(ErrorCode.RestartedProcessNotFound);
                    }
                    else
                    {
                        OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs($"IBGateway restarted process found: Id:{process.Id} - Name:{process.ProcessName}. Arguments: {GetProcessArguments(process)}"));

                        // fire Restarted event so the client can reconnect only (without starting IBGateway)
                        Restarted?.Invoke(this, new EventArgs());

                        process.Exited -= OnProcessExited;

                        // replace process
                        _process = process;

                        // we cannot add output/error redirection event handlers here as we didn't start the process

                        process.Exited += OnProcessExited;
                        process.EnableRaisingEvents = true;
                    }

                    _isRestartInProgress = false;
                }
                else
                {
                    Exited?.Invoke(this, new ExitedEventArgs(GetProcessExitCode(_process)));
                }
            }
            else
            {
                Exited?.Invoke(this, new ExitedEventArgs(GetProcessExitCode(_process)));
            }
        }

        /// <summary>
        /// Stops the IB Gateway
        /// </summary>
        /// <remarks>The IB Gateway application will be terminated</remarks>
        public void Stop()
        {
            lock (_locker)
            {
                if (!IsRunning())
                {
                    return;
                }

                _timerLogReader.Change(Timeout.Infinite, Timeout.Infinite);

                if (IsWindows)
                {
                    foreach (var process in Process.GetProcesses())
                    {
                        try
                        {
                            if (process.MainWindowTitle.ToLower().Contains("gateway"))
                            {
                                process.Kill();
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
                        Process.Start("pkill", "java");
                        Process.Start("pkill", "Xvfb");
                    }
                    catch (Exception)
                    {
                        // ignored
                    }
                }

                _process = null;

                // stop any restart threads
                StopGatewayRestartTimeoutMonitor();
            }
        }

        /// <summary>
        /// Restarts the IB Gateway
        /// </summary>
        /// <remarks>The IB Gateway application will be restarted</remarks>
        public StartResult Restart()
        {
            lock (_locker)
            {
                Stop();

                Thread.Sleep(2500);

                return Start(false);
            }
        }

        /// <summary>
        /// Triggers a restart from within the IB Gateway using it's daily restart functionality.
        /// This is different than normal restart because it will not require a 2FA call
        /// </summary>
        public void SoftRestart()
        {

            if (_isDisposeCalled || _gatewaySoftRestartTokenSource != null && !_gatewaySoftRestartTokenSource.IsCancellationRequested)
            {
                return;
            }

            // We increment the restart delay each time we try to restart the gateway within an hour to limit the number of restarts.
            var currentRestartCount = Interlocked.Increment(ref _gatewaySoftRestartCount);
            var delay = TimeSpan.FromMinutes(5) * (currentRestartCount - 1);

            // we take the lock to avoid it getting disposed while we are evaluating it
            lock (_gatewaySoftRestartTokenSource ?? new object())
            {
                _gatewaySoftRestartTokenSource?.Dispose();
                _gatewaySoftRestartTokenSource = new CancellationTokenSource();
            }

            Task.Delay(delay, _gatewaySoftRestartTokenSource.Token).ContinueWith(_ =>
            {
                if (_isDisposeCalled)
                {
                    return;
                }

                lock (_locker)
                {
                    var ibGatewayVersionPath = GetIbGatewayVersionPath();
                    var restartFilePath = Path.Combine(ibGatewayVersionPath, "restart");
                    File.WriteAllBytes(restartFilePath, Array.Empty<byte>());

                    StartGatewayRestartTimeoutMonitor();
                };
            });
        }

        /// <summary>
        /// Gets the last <see cref="StartResult"/> instance
        /// </summary>
        /// <returns>Returns the last start result instance</returns>
        public StartResult GetLastStartResult()
        {
            return _lastStartResult;
        }

        /// <summary>
        /// This function is used to decide whether or not we should kill an algorithm
        /// when we lose contact with IB servers. IB performs server resets nightly
        /// and on Fridays they take everything down, so we'll prevent killing algos
        /// during the scheduled reset times.
        /// </summary>
        public bool IsWithinScheduledServerResetTimes()
        {
            // Use schedule based on server region:
            // https://www.interactivebrokers.com/en/index.php?f=2225

            bool result;
            var utcTime = DateTime.UtcNow;
            var newYorkTime = utcTime.ConvertFromUtc(TimeZoneNewYork);
            var newYorkTimeOfDay = newYorkTime.TimeOfDay;

            if (IsWithinWeekendServerResetTimes(utcTime))
            {
                result = true;
            }
            else
            {
                switch (_ibServerRegion)
                {
                    case Region.Europe:
                    {
                        // Saturday - Thursday: 05:45 - 06:45 CET
                        var euTime = utcTime.ConvertFromUtc(TimeZoneZurich);
                        var euTimeOfDay = euTime.TimeOfDay;
                        result = euTimeOfDay > new TimeSpan(5, 30, 0) && euTimeOfDay < new TimeSpan(7, 0, 0);
                    }
                        break;

                    case Region.Asia:
                    {
                        // Saturday - Thursday: First reset: 16:30 - 17:00 ET
                        if (newYorkTimeOfDay > new TimeSpan(16, 15, 0) && newYorkTimeOfDay < new TimeSpan(17, 15, 0))
                        {
                            result = true;
                        }
                        else
                        {
                            // Saturday - Thursday: Second reset: 20:15 - 21:00 HKT
                            var hkTime = utcTime.ConvertFromUtc(TimeZoneHongKong);
                            var hkTimeOfDay = hkTime.TimeOfDay;
                            result = hkTimeOfDay > new TimeSpan(20, 0, 0) && hkTimeOfDay < new TimeSpan(21, 15, 0);
                        }
                    }
                        break;

                    case Region.America:
                    default:
                    {
                        // Saturday - Thursday: 23:45 - 00:45 ET
                        result = newYorkTimeOfDay > new TimeSpan(23, 30, 0) || newYorkTimeOfDay < new TimeSpan(1, 0, 0);
                    }
                        break;
                }
            }

            if (result != _isWithinScheduledServerResetTimesLastValue)
            {
                _isWithinScheduledServerResetTimesLastValue = result;

                OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs(
                    $"IBAutomater.IsWithinScheduledServerResetTimes(): {result}"));
            }

            return result;
        }

        /// <summary>
        /// Returns whether the current time is within the IB weekend server reset period.
        /// </summary>
        public bool IsWithinWeekendServerResetTimes()
        {
            return IsWithinWeekendServerResetTimes(DateTime.UtcNow);
        }

        /// <summary>
        /// Returns whether the current time is within the IB weekend server reset period.
        /// </summary>
        public bool IsWithinWeekendServerResetTimes(DateTime utcTime)
        {
            // Use schedule based on server region:
            // https://www.interactivebrokers.com/en/index.php?f=2225

            bool result = false;
            var newYorkTime = utcTime.ConvertFromUtc(TimeZoneNewYork);
            var newYorkTimeOfDay = newYorkTime.TimeOfDay;

            // Note: we add 15 minutes *before* and *after* all time ranges for safety margin
            // During the Friday evening reset period, all services will be unavailable in all regions for the duration of the reset.
            if (newYorkTime.DayOfWeek == DayOfWeek.Friday && newYorkTimeOfDay > new TimeSpan(22, 45, 0) ||
                // Occasionally the disconnection due to the IB reset period might last
                // much longer than expected during weekends (even up to the cash sync time).
                newYorkTime.DayOfWeek == DayOfWeek.Saturday ||
                // Occasionally disconnection on the first hours of Sunday
                newYorkTime.DayOfWeek == DayOfWeek.Sunday && newYorkTimeOfDay < new TimeSpan(2, 0, 0))
            {
                // Friday: 23:00 - 03:00 ET for all regions
                result = true;
            }

            return result;
        }

        /// <summary>
        /// Returns whether the IBGateway is running
        /// </summary>
        /// <returns>true if the IBGateway is running</returns>
        public bool IsRunning()
        {
            lock (_locker)
            {
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

        private void LoadIbServerInformation()
        {
            // After a successful login, IBGateway saves the connected/redirected host name to the Peer key in the jts.ini file.
            var iniFileName = GetIbGatewayIniFile();

            // Note: Attempting to connect to a different server via jts.ini will not change anything.
            // IB will route you back to the server they have set for you on their server side.
            // You need to request a server change and only then will your system connect to the changed server address.

            if (File.Exists(iniFileName))
            {
                const string key = "Peer=";
                foreach (var line in File.ReadLines(iniFileName))
                {
                    if (line.StartsWith(key))
                    {
                        var value = line.Substring(key.Length);
                        _ibServerName = value.Substring(0, value.IndexOf(':'));

                        if (!_ibServerMap.TryGetValue(_ibServerName, out _ibServerRegion))
                        {
                            _ibServerRegion = Region.America;

                            ErrorDataReceived?.Invoke(this, new ErrorDataReceivedEventArgs(
                                $"LoadIbServerInformation(): Unknown server name: {_ibServerName}, region set to {_ibServerRegion}"));
                        }

                        // known server name and region
                        OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs(
                            $"LoadIbServerInformation(): ServerName: {_ibServerName}, ServerRegion: {_ibServerRegion}"));
                        return;
                    }
                }

                _ibServerRegion = Region.America;

                ErrorDataReceived?.Invoke(this, new ErrorDataReceivedEventArgs(
                    $"LoadIbServerInformation(): Unable to find the server name in the IB ini file: {iniFileName}, region set to {_ibServerRegion}"));
            }
            else
            {
                ErrorDataReceived?.Invoke(this, new ErrorDataReceivedEventArgs(
                    $"LoadIbServerInformation(): IB ini file not found: {iniFileName}"));
            }

            OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs(
                $"LoadIbServerInformation(): ServerName: {_ibServerName}, ServerRegion: {_ibServerRegion}"));
        }

        private string GetJreInstallPath(string ibGatewayVersionPath)
        {
            OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs("Searching for TWS JRE path"));

            // Find TWS Java location (depends on OS and IBGateway version)
            var install4JPath = $"{ibGatewayVersionPath}/.install4j";
            OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs($"Install4J path: {install4JPath}"));

            foreach (var fileName in new[] { "pref_jre.cfg", "inst_jre.cfg" })
            {
                var install4JConfigFileName = $"{install4JPath}/{fileName}";
                if (File.Exists(install4JConfigFileName))
                {
                    OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs($"File found: {install4JConfigFileName}"));

                    var jreInstallPath = File.ReadAllText(install4JConfigFileName)
                        .Replace("\r", "")
                        .Replace("\n", "");

                    if (Directory.Exists(jreInstallPath))
                    {
                        OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs($"Directory found: {jreInstallPath}"));
                        return jreInstallPath;
                    }
                    else
                    {
                        OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs($"Directory not found: {jreInstallPath}"));
                    }
                }
                else
                {
                    OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs($"File not found: {install4JConfigFileName}"));
                }
            }

            return null;
        }

        private string GetIbGatewayVersionPath()
        {
            var ibGatewayVersionPath = $"{_ibDirectory}/ibgateway/{_ibVersion}";

            if (IsLinux && Convert.ToInt32(_ibVersion) >= 984)
            {
                ibGatewayVersionPath = _ibDirectory.Replace("Jts", "ibgateway");
            }

            return ibGatewayVersionPath;
        }

        private string GetIbGatewayIniFile()
        {
            return Path.Combine(IsWindows ? GetIbGatewayVersionPath() : _ibDirectory, "jts.ini");
        }

        private void UpdateIbGatewayIniFile()
        {
            var ibGatewayIniFile = GetIbGatewayIniFile();
            OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs($"Updating IBGateway ini file: {ibGatewayIniFile}"));

            // The "Use SSL" checkbox in the IBGateway UX has inconsistent behavior,
            // so we preset the "UseSSL=true" value in jts.ini
            if (File.Exists(ibGatewayIniFile))
            {
                var iniText = File.ReadAllText(ibGatewayIniFile);
                if (iniText.Contains("UseSSL=false"))
                {
                    iniText = iniText.Replace("UseSSL=false", "UseSSL=true");
                    File.WriteAllText(ibGatewayIniFile, iniText);
                }
            }
            else
            {
                File.WriteAllText(ibGatewayIniFile, $"[Logon]{Environment.NewLine}UseSSL=true");
            }
        }

        private string UpdateIbGatewayConfiguration(string ibGatewayVersionPath, bool enableJavaAgent)
        {
            // update IBGateway configuration file with Java agent entry
            var ibGatewayConfigFile = $"{ibGatewayVersionPath}/ibgateway.vmoptions";
            OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs($"Updating IBGateway configuration file: {ibGatewayConfigFile}"));

            var jarPath = Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location);
            var javaAgentConfigFileName = Path.Combine(jarPath, "IBAutomater.json");
            var javaAgentConfig = $"-javaagent:{jarPath}/IBAutomater.jar={javaAgentConfigFileName}";

            // for linux we will use an env, since the options file is not respected
            if (!IsLinux)
            {
                var lines = File.ReadAllLines(ibGatewayConfigFile).ToList();
                var existing = false;
                for (var i = 0; i < lines.Count; i++)
                {
                    var line = lines[i];

                    if (line.StartsWith("-javaagent:") && line.Contains("IBAutomater"))
                    {
                        if (enableJavaAgent)
                        {
                            lines[i] = javaAgentConfig;
                        }
                        else
                        {
                            lines.RemoveAt(i--);
                        }

                        existing = true;
                    }
                }

                if (enableJavaAgent && !existing)
                {
                    lines.Add(javaAgentConfig);
                }

                File.WriteAllLines(ibGatewayConfigFile, lines);
            }

            if (enableJavaAgent)
            {
                File.WriteAllText(javaAgentConfigFileName, $"{_userName}\n{_password}\n{_tradingMode}\n{_portNumber}\n{_exportIbGatewayLogs}");
            }
            else
            {
                File.Delete(javaAgentConfigFileName);
            }

            return javaAgentConfig;
        }

        private static int GetProcessExitCode(Process process)
        {
            // The IBGateway auto-restarted process is a non-child process
            // System.InvalidOperationException: Cannot get the exit code from a non-child process on Unix
            if (process == null || IsLinux)
            {
                return 0;
            }

            return process.ExitCode;
        }

        private void TraceIbLauncherLogFile()
        {
            var ibLauncherLogFile = Path.Combine(_ibDirectory, "launcher.log");

            if (File.Exists(ibLauncherLogFile))
            {
                OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs($"IB launcher log file: {ibLauncherLogFile}"));

                try
                {
                    using (var fileStream = new FileStream(ibLauncherLogFile, FileMode.Open, FileAccess.Read, FileShare.ReadWrite))
                    {
                        fileStream.Seek(0, SeekOrigin.Begin);

                        using (var reader = new StreamReader(fileStream))
                        {
                            string line;
                            while ((line = reader.ReadLine()) != null)
                            {
                                OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs($"[IB Launcher] {line}"));
                            }
                        }
                    }
                }
                catch (Exception exception)
                {
                    OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs($"Error reading IB launcher log file: {exception.Message}"));
                }
            }
        }

        private string GetProcessArguments(Process process)
        {
            if (IsWindows)
            {
                // we don't need this and supporting it requires dependencies so let's just skip it
                return "Not supported";
            }

            try
            {
                return File.ReadAllText($"/proc/{process.Id}/cmdline");
            }
            catch (Exception exception)
            {
                OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs($"Error reading process cmdline: {exception.Message}"));
            }
            return string.Empty;
        }

        private Process GetIbGatewayProcess()
        {
            var processName = IsWindows ? "ibgateway" : "java";

            var processes = Process.GetProcessesByName(processName);
            if (processes == null || processes.Length == 0)
            {
                return null;
            }
            else if (processes.Length > 1)
            {
                OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs($"Found multiple processes named: '{processName}'. Processes: [{string.Join(",", processes.Select(p => $"pid:{p.Id}\ncmdline:\n{GetProcessArguments(p)}"))}]"));

                // in linux there's a short lived java launcher process but it doesn't have our jar as argument
                var filteredProcesses = processes.Where(p => GetProcessArguments(p).Contains("IBAutomater.jar", StringComparison.InvariantCultureIgnoreCase)).ToList();
                if (filteredProcesses.Count != 1)
                {
                    filteredProcesses = processes.Where(p => GetProcessArguments(p).Contains("-Drestart=", StringComparison.InvariantCultureIgnoreCase)).ToList();
                    if (filteredProcesses.Count != 1)
                    {
                        return null;
                    }
                }
                return filteredProcesses.Single();
            }
            else
            {
                // happy case
                return processes.Single();
            }
        }

        private void StartGatewayRestartTimeoutMonitor()
        {
            // Detect rare gateway restart timeouts that could leave the gateway in a stale state
            Task.Delay(_maxExpectedGatewayRestartTime, _gatewaySoftRestartTokenSource.Token).ContinueWith(_ =>
            {
                if (_isDisposeCalled || !IsRunning())
                {
                    return;
                }


                // Restart timeout
                if (!_gatewaySoftRestartTokenSource.IsCancellationRequested)
                {
                    // Let's cancel just for the next SoftRestart call to be able to schedule another restart
                    _gatewaySoftRestartTokenSource.Cancel();

                    // The gateway should have restarted by now, if it didn't we will try to restart it again
                    OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs($"Soft restart timed out after {_maxExpectedGatewayRestartTime}. Triggering a new restart..."));

                    if (_gatewaySoftRestartTimedOut)
                    {
                        // Restart timed out again, let's send an error
                        _lastStartResult = new StartResult(ErrorCode.SoftRestartTimeout);
                        Restarted?.Invoke(this, new EventArgs());
                    }
                    else
                    {
                        _gatewaySoftRestartTimedOut = true;

                        // Try to restart again
                        SoftRestart();
                    }
                }
                else
                {
                    _gatewaySoftRestartTimedOut = false;
                }
            });
        }

        private void StopGatewayRestartTimeoutMonitor()
        {
            if (!_isDisposeCalled && _gatewaySoftRestartTokenSource != null && !_gatewaySoftRestartTokenSource.IsCancellationRequested)
            {
                _gatewaySoftRestartTokenSource.Cancel();
            }
        }

        /// <summary>
        /// Recurring task to reset the gateway restart count.
        /// We keep track of the restart count to limit the number of times the gateway is consecutively restarted.
        /// </summary>
        private void StartGatewayRestartCountResetTask()
        {
            Task.Delay(TimeSpan.FromHours(1)).ContinueWith(_ =>
            {
                Interlocked.Exchange(ref _gatewaySoftRestartCount, 0);
                StartGatewayRestartCountResetTask();
            });
        }

        private void SendErrorLog(object sender, DataReceivedEventArgs e)
        {
            if (e.Data != null)
            {
                if(e.Data.Contains("JAVA_TOOL_OPTIONS"))
                {
                    // this is not an error
                    SendTraceLog(sender, e);
                }
                else
                {
                    ErrorDataReceived?.Invoke(this, new ErrorDataReceivedEventArgs(e.Data.Replace(_password, "***")));
                }
            }
        }

        private void SendTraceLog(object sender, DataReceivedEventArgs e)
        {
            if (e.Data != null)
            {
                OutputDataReceived?.Invoke(this, new OutputDataReceivedEventArgs(e.Data.Replace(_password, "***")));
            }
        }
    }
}

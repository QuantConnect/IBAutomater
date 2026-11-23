/*
 * QUANTCONNECT.COM - Democratizing Finance, Empowering Individuals.
 * Lean Algorithmic Trading Engine v2.0. Copyright 2014 QuantConnect Corporation.
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
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using NUnit.Framework;

namespace QuantConnect.IBAutomater.Tests
{
    [TestFixture, Platform("Linux"), NonParallelizable]
    public class IBAutomaterShutdownTests
    {
        private string _directory;
        private string _workingDirectory;
        private IBAutomater _automater;
        private ManualResetEventSlim _waitingForLogin;

        [SetUp]
        public void SetUp()
        {
            _directory = Path.Combine(Path.GetTempPath(), $"ibautomater-shutdown-tests-{Guid.NewGuid():N}");
            var gatewayDirectory = Path.Combine(_directory, "ibgateway");
            var jreDirectory = Path.Combine(_directory, "jre");
            Directory.CreateDirectory(Path.Combine(gatewayDirectory, ".install4j"));
            Directory.CreateDirectory(jreDirectory);
            File.WriteAllText(Path.Combine(gatewayDirectory, ".install4j", "pref_jre.cfg"), jreDirectory);
            File.WriteAllText(Path.Combine(gatewayDirectory, "ibgateway"), string.Empty);
            File.WriteAllText(Path.Combine(gatewayDirectory, "ibgateway.vmoptions"), string.Empty);

            // the start runs the IBAutomater.sh of the working directory first: this one stands for a gateway waiting
            // for its login, such as for a 2FA confirmation, until it is stopped, without touching any other process
            _workingDirectory = Path.Combine(_directory, "working");
            Directory.CreateDirectory(_workingDirectory);
            File.WriteAllText(Path.Combine(_workingDirectory, "IBAutomater.sh"),
                "#!/bin/bash\ndirectory=$(dirname \"$1\")\nwhile [ ! -f \"$directory/shutdown\" ]; do sleep 0.1; done\n");
            Environment.CurrentDirectory = _workingDirectory;

            _automater = new IBAutomater(gatewayDirectory, "1034", "user", "password", "paper", 4002, false);
            _waitingForLogin = new ManualResetEventSlim();
            _automater.OutputDataReceived += (_, e) =>
            {
                if (e.Data.StartsWith("IBAutomater process started", StringComparison.InvariantCulture))
                {
                    _waitingForLogin.Set();
                }
            };
        }

        [TearDown]
        public void TearDown()
        {
            _automater.Shutdown();
            _automater.Dispose();
            Environment.CurrentDirectory = Path.GetTempPath();
            Directory.Delete(_directory, true);
        }

        [Test]
        public void ShutdownEndsAStartWaitingForTheLogin()
        {
            var start = Task.Run(() => _automater.Start(false));
            Assert.That(_waitingForLogin.Wait(TimeSpan.FromSeconds(30)), Is.True);

            // the start waits up to its 15 minutes initialization timeout, holding the stop until then
            var shutdown = Task.Run(_automater.Shutdown);

            Assert.That(shutdown.Wait(TimeSpan.FromSeconds(30)), Is.True, "Shutdown() waited for the login");
            Assert.That(start.Wait(TimeSpan.FromSeconds(5)), Is.True);
            Assert.That(start.Result.ErrorCode, Is.EqualTo(ErrorCode.Shutdown));
            Assert.That(_automater.IsRunning(), Is.False);
        }

        [Test]
        public void StartAfterShutdownFails()
        {
            _automater.Shutdown();

            Assert.That(_automater.Start(false).ErrorCode, Is.EqualTo(ErrorCode.Shutdown));
            Assert.That(_waitingForLogin.IsSet, Is.False);
            Assert.That(_automater.IsRunning(), Is.False);
        }
    }
}

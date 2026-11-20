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

using System.IO;
using NUnit.Framework;

namespace QuantConnect.IBAutomater.Tests
{
    [TestFixture]
    public class JreInstallPathTests
    {
        private string _gatewayDirectory;
        private string _configuredJre;
        private string _bundledJre;

        [SetUp]
        public void SetUp()
        {
            _gatewayDirectory = Directory.CreateTempSubdirectory("ibgateway").FullName;
            Directory.CreateDirectory(Path.Combine(_gatewayDirectory, ".install4j"));
            _configuredJre = Directory.CreateTempSubdirectory("i4j_jre").FullName;
            _bundledJre = Path.Combine(_gatewayDirectory, "jre");
        }

        [TearDown]
        public void TearDown()
        {
            Directory.Delete(_gatewayDirectory, true);
            Directory.Delete(_configuredJre, true);
        }

        // 10.39: the shared JRE the installer put in pref_jre.cfg, even if a bundled one exists
        [TestCase("pref_jre.cfg", true)]
        [TestCase("pref_jre.cfg", false)]
        // only inst_jre.cfg names an existing JRE
        [TestCase("inst_jre.cfg", true)]
        [TestCase("inst_jre.cfg", false)]
        public void ConfiguredJreTakesPrecedence(string configFileName, bool withBundledJre)
        {
            WriteConfig(configFileName, _configuredJre);
            if (withBundledJre)
            {
                Directory.CreateDirectory(_bundledJre);
            }

            Assert.That(GetJreInstallPath(), Is.EqualTo(_configuredJre));
        }

        [Test]
        public void PrefJreConfigTakesPrecedenceOverInstJreConfig()
        {
            var otherJre = Directory.CreateTempSubdirectory("i4j_jre_other").FullName;
            try
            {
                WriteConfig("pref_jre.cfg", _configuredJre);
                WriteConfig("inst_jre.cfg", otherJre);

                Assert.That(GetJreInstallPath(), Is.EqualTo(_configuredJre));
            }
            finally
            {
                Directory.Delete(otherJre, true);
            }
        }

        // 10.50: no pref_jre.cfg, inst_jre.cfg names the installer's deleted temporary JRE, the JRE is bundled
        [TestCase(true)]
        [TestCase(false)]
        public void FallsBackToTheBundledJre(bool withStaleInstJreConfig)
        {
            if (withStaleInstJreConfig)
            {
                WriteConfig("inst_jre.cfg", Path.Combine(_gatewayDirectory, "ibgateway.sh.8.dir", "jre"));
            }
            Directory.CreateDirectory(_bundledJre);

            Assert.That(GetJreInstallPath(), Is.EqualTo(_bundledJre));
        }

        [TestCase(true)]
        [TestCase(false)]
        public void ReturnsNullWithoutAnyJre(bool withStaleInstJreConfig)
        {
            if (withStaleInstJreConfig)
            {
                WriteConfig("inst_jre.cfg", Path.Combine(_gatewayDirectory, "ibgateway.sh.8.dir", "jre"));
            }

            Assert.That(GetJreInstallPath(), Is.Null);
        }

        private void WriteConfig(string fileName, string jrePath)
        {
            File.WriteAllText(Path.Combine(_gatewayDirectory, ".install4j", fileName), jrePath + "\n");
        }

        private string GetJreInstallPath()
        {
            var automater = new IBAutomater(_gatewayDirectory, "1050", "user", "password", "paper", 4002, false);
            return automater.GetJreInstallPath(_gatewayDirectory);
        }
    }
}

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
using NUnit.Framework;

namespace QuantConnect.IBAutomater.Tests
{
    [TestFixture]
    public class FinancialAdvisorConfigurationTests
    {
        [Test]
        public void PreservesFinancialAdvisorErrorCodeValue()
        {
            Assert.That((int)ErrorCode.FinancialAdvisorAllocationGroupsConfigurationUnavailable, Is.EqualTo(16));
        }

        [Test]
        public void PreservesOriginalPublicConstructor()
        {
            Assert.That(
                typeof(IBAutomater).GetConstructor(new[]
                {
                    typeof(string),
                    typeof(string),
                    typeof(string),
                    typeof(string),
                    typeof(string),
                    typeof(int),
                    typeof(bool)
                }),
                Is.Not.Null);
        }

        [Test]
        public void DescribesFinancialAdvisorDesiredStateFailure()
        {
            var result = new StartResult(ErrorCode.FinancialAdvisorAllocationGroupsConfigurationUnavailable);

            Assert.Multiple(() =>
            {
                Assert.That(result.ErrorMessage, Does.Contain("requested Use Account Groups with Allocation Methods setting"));
                Assert.That(result.ErrorMessage, Does.Contain("could not be applied or verified"));
            });
        }
    }
}

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

package ibautomater;

public class Settings {
    private final String userName;
    private final String password;
    private final String ibDirectory;
    private final String tradingMode;
    private final int portNumber;

    public Settings(String ibDirectory, String userName, String password, String tradingMode, int portNumber) {
        this.ibDirectory = ibDirectory;
        this.userName = userName;
        this.password = password;
        this.tradingMode = tradingMode;
        this.portNumber = portNumber;
    }

    public String getUserName() {
        return this.userName;
    }

    public String getPassword() {
        return this.password;
    }

    public String getIbDirectory() {
        return this.ibDirectory;
    }

    public String getTradingMode() {
        return this.tradingMode;
    }

    public int getPortNumber() {
        return this.portNumber;
    }
}


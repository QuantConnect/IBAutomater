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

/**
 * Contains all settings required by IBAutomater.
 *
 * @author QuantConnect Corporation
*/
public class Settings {
    private final String userName;
    private final String password;
    private final String tradingMode;
    private final int portNumber;
    private final boolean exportIbGatewayLogs;
    private final boolean restarting;

    /**
     * Creates a new instance of the {@link Settings} class.
     *
     * @param userName The IB user name
     * @param password The IB password
     * @param tradingMode The trading mode (allowed values are "live" and "paper")
     * @param portNumber The socket port number to be used for API connections
     * @param exportIbGatewayLogs If true, IBGateway logs will be exported at predefined times
     * (currently at startup and when unknown windows are detected)
     * @param restarting If true, the automater will assume the gateway is starting after a 
     * soft daily restart and won't try to log in
     */
    public Settings(String userName, String password, String tradingMode, int portNumber, boolean exportIbGatewayLogs, boolean restarting) {
        this.userName = userName;
        this.password = password;
        this.tradingMode = tradingMode;
        this.portNumber = portNumber;
        this.exportIbGatewayLogs = exportIbGatewayLogs;
        this.restarting = restarting;
    }

    /**
     * Gets the IB user name.
     *
     * @return Returns the IB user name
     */
    public String getUserName() {
        return this.userName;
    }

    /**
     * Gets the IB password.
     *
     * @return Returns the IB password
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * Gets the trading mode ("live" for Real trading, "paper" for Paper trading).
     *
     * @return Returns the trading mode
     */
    public String getTradingMode() {
        return this.tradingMode;
    }

    /**
     * Gets the socket port number for API connections.
     *
     * @return Returns the socket port number
     */
    public int getPortNumber() {
        return this.portNumber;
    }

    /**
     * Gets whether IBGateway logs should be exported.
     *
     * @return Returns true if IBGateway logs should be exported, false otherwise
     */
    public boolean getExportIbGatewayLogs() {
        return this.exportIbGatewayLogs;
    }

    /**
     * Gets whether IBGateway is starting after a soft daily restart
     *
     * @return Returns true if IBGateway is starting after a soft daily restart
     */
    public boolean getRestarting() {
        return this.restarting;
    }
}


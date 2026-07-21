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

import java.awt.Toolkit;
import java.awt.Window;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * IBAutomater is the component responsible for the interaction with the IBGateway user interface.
 *
 * @author QuantConnect Corporation
 */
public final class IBAutomater {
    private final Settings settings;
    private PrintWriter printWriter = null;
    private Window mainWindow;

    /**
     * The Java agent premain method is called before the IBGateway main method.
     *
     * @param args The name of a text file containing the values of the IBAutomater settings
     */
    public static void premain(String args) throws Exception {

        String fileContent = new String(Files.readAllBytes(Paths.get(args)), StandardCharsets.UTF_8);
        String[] argValues = fileContent.split("\n");

        String userName = argValues[0];
        String password = argValues[1];
        String tradingMode = argValues[2];
        int portNumber = Integer.parseInt(argValues[3]);
        boolean exportIbGatewayLogs = Boolean.parseBoolean(argValues[4]);
        boolean restarting = Boolean.parseBoolean(argValues[5]);

        IBAutomater automater = new IBAutomater(userName, password, tradingMode, portNumber, exportIbGatewayLogs, restarting);
    }

    /**
     * Creates a new instance of the {@link IBAutomater} class.
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
    public IBAutomater(String userName, String password, String tradingMode, int portNumber, boolean exportIbGatewayLogs, boolean restarting) {
        this.settings = new Settings(userName, password, tradingMode, portNumber, exportIbGatewayLogs, restarting);

        try
        {
            this.printWriter = new PrintWriter(new FileWriter("IBAutomater.log"), true);
        }
        catch (IOException exception)
        {
            System.out.println(exception.getMessage());
        }

        Toolkit.getDefaultToolkit().addAWTEventListener(new WindowEventListener(this), 64L);

        this.logMessage("IBGateway started");
    }

    /**
     * Writes the text message to the log file.
     *
     * @param text The text message to be logged
     */
    public void logMessage(String text) {
        try
        {
            this.printWriter.println(text);
        }
        catch (Exception exception)
        {
            System.out.println(exception.getMessage());
        }
    }

    /**
     * Writes the exception message to the log file.
     *
     * @param exception The exception to be logged
     */
    public void logError(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);

        this.logMessage("Error: " + sw.toString());
    }

    /**
     * Gets the IBGateway main window.
     *
     * @return Returns the IBGateway main window
     */
    public Window getMainWindow() {
        return this.mainWindow;
    }

    /**
     * Sets the IBGateway main window.
     *
     * @param window The IBGateway main window
     */
    public void setMainWindow(Window window) {
        this.mainWindow = window;
    }

    /**
     * Gets the IBAutomater settings.
     *
     * @return Returns the {@link Settings} instance
     */
    public Settings getSettings() {
        return this.settings;
    }
}

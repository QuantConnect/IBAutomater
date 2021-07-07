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

public final class IBAutomater {
    private final Settings settings;
    private PrintWriter printWriter = null;
    private Window mainWindow;

    public static void premain(String args) throws Exception {
        String[] argValues = args.split(" ");

        String userName = argValues[0];
        String password = argValues[1];
        String tradingMode = argValues[2];
        int portNumber = Integer.parseInt(argValues[3]);
        boolean exportIbGatewayLogs = Boolean.parseBoolean(argValues[4]);

        IBAutomater automater = new IBAutomater(userName, password, tradingMode, portNumber, exportIbGatewayLogs);
    }

    public IBAutomater(String userName, String password, String tradingMode, int portNumber, boolean exportIbGatewayLogs) {
        this.settings = new Settings(userName, password, tradingMode, portNumber, exportIbGatewayLogs);

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

    public void logError(String text) {
        this.logMessage("Error: " + text);
    }

    public void logError(Exception exception) {
        this.logError(exception.getMessage());
    }

    public Window getMainWindow() {
        return this.mainWindow;
    }

    public void setMainWindow(Window window) {
        this.mainWindow = window;
    }

    public Settings getSettings() {
        return this.settings;
    }
}

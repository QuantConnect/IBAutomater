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

import ibgateway.GWClient;
import java.awt.Toolkit;
import java.awt.Window;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class IBAutomater {
    private final Settings settings;
    private Window mainWindow;

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addOption(new Option("ibdir", true, "IB Gateway path"));
        options.addOption(new Option("user", true, "User name"));
        options.addOption(new Option("pwd", true, "Password"));
        options.addOption(new Option("mode", true, "Trading mode"));
        options.addOption(new Option("port", true, "IB socket port"));

        DefaultParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args);

        if (!(commandLine.hasOption("ibdir") && commandLine.hasOption("user") && commandLine.hasOption("pwd") && commandLine.hasOption("mode") && commandLine.hasOption("port"))) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("IBAutomater", options);
            System.exit(1);
        }

        String ibDirectory = commandLine.getOptionValue("ibdir");
        String userName = commandLine.getOptionValue("user");
        String password = commandLine.getOptionValue("pwd");
        String tradingMode = commandLine.getOptionValue("mode");
        int portNumber = Integer.parseInt(commandLine.getOptionValue("port"));

        IBAutomater automater = new IBAutomater(ibDirectory, userName, password, tradingMode, portNumber);
        automater.startIBGateway();
    }

    public IBAutomater(String ibDirectory, String userName, String password, String tradingMode, int portNumber) {
        this.settings = new Settings(ibDirectory, userName, password, tradingMode, portNumber);
    }

    public void startIBGateway() {
        this.logMessage("StartIBGateway(): starting IBGateway");

        Toolkit.getDefaultToolkit().addAWTEventListener(new WindowEventListener(this), 64L);

        String[] args = new String[] { this.settings.getIbDirectory() };
        try {
            GWClient.main(args);
        }
        catch (Exception exception) {
            this.logError("StartIBGateway(): Error launching IBGateway: " + exception.toString());
            System.exit(1);
            return;
        }
        this.logMessage("StartIBGateway(): IBGateway started");
    }

    public void logMessage(String text) {
        System.out.println(text);
    }

    public void logError(String text) {
        System.out.println("Error: " + text);
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

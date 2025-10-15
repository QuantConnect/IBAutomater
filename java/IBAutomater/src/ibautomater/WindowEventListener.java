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

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

/**
 * The event listener implementation handles the detection and handling of known and supported IBGateway windows.
 *
 * @author QuantConnect Corporation
 */
public class WindowEventListener implements AWTEventListener {
    private final IBAutomater automater;
    private final HashMap<Integer, String> handledEvents = new HashMap<Integer, String>(){
        {
            this.put(WindowEvent.WINDOW_OPENED, "WINDOW_OPENED");
            this.put(WindowEvent.WINDOW_ACTIVATED, "WINDOW_ACTIVATED");
            this.put(WindowEvent.WINDOW_DEACTIVATED, "WINDOW_DEACTIVATED");
            this.put(WindowEvent.WINDOW_CLOSING, "WINDOW_CLOSING");
            this.put(WindowEvent.WINDOW_CLOSED, "WINDOW_CLOSED");
        }
    };
    private boolean isAutoRestartTokenExpired = false;
    private boolean restartNow = false;
    private Window viewLogsWindow = null;

    private Instant twoFactorConfirmationRequestTime;
    private int twoFactorConfirmationAttempts = 0;
    private final int maxTwoFactorConfirmationAttempts = 3;

    /**
     * Creates a new instance of the {@link WindowEventListener} class.
     *
     * @param automater The {@link IBAutomater} instance
     */
    WindowEventListener(IBAutomater automater) {
        this.automater = automater;
    }

    /**
     * Invoked when an event is dispatched in the AWT.
     *
     * @param awtEvent The event to be processed
     */
    @Override
    public void eventDispatched(AWTEvent awtEvent) {
        int eventId = awtEvent.getID();
        Window window = ((WindowEvent)awtEvent).getWindow();

        if (this.handledEvents.containsKey(eventId)) {
            this.automater.logMessage("Window event: [" + this.handledEvents.get(eventId) + "] - Window title: [" + Common.getTitle(window) + "] - Window name: [" + window.getName() + "]");
        }
        else {
            return;
        }

        try {
            if (this.HandleLoginWindow(window, eventId)) {
                return;
            }
            if (this.HandleLoginFailedWindow(window, eventId)) {
                return;
            }
            if (this.HandleServerDisconnectedWindow(window, eventId)) {
                return;
            }
            if (this.HandleTooManyFailedLoginAttemptsWindow(window, eventId)) {
                return;
            }
            if (this.HandlePasswordNoticeWindow(window, eventId)) {
                return;
            }
            if (this.HandleInitializationWindow(window, eventId)) {
                return;
            }
            if (this.HandlePaperTradingAccountWindow(window, eventId)) {
                return;
            }
            if (this.HandleUnsupportedVersionWindow(window, eventId)) {
                return;
            }
            if (this.HandleConfigurationWindow(window, eventId)) {
                return;
            }
            if (this.HandleExistingSessionDetectedWindow(window, eventId)) {
                return;
            }
            if (this.HandleReloginRequiredWindow(window, eventId)) {
                return;
            }
            if (this.HandleFinancialAdvisorWarningWindow(window, eventId)) {
                return;
            }
            if (this.HandleExitSessionSettingWindow(window, eventId)) {
                return;
            }
            if (this.HandleApiNotAvailableWindow(window, eventId)) {
                return;
            }
            if (this.HandleEnableAutoRestartConfirmationWindow(window, eventId)) {
                return;
            }
            if (this.HandleAutoRestartTokenExpiredWindow(window, eventId)) {
                return;
            }
            if (this.HandleViewLogsWindow(window, eventId)) {
                return;
            }
            if (this.HandleExportFileNameWindow(window, eventId)) {
                return;
            }
            if (this.HandleExportFinishedWindow(window, eventId)) {
                return;
            }
            if (this.HandleAutoRestartNowWindow(window, eventId)) {
                return;
            }
            if (this.HandleTwoFactorAuthenticationWindow(window, eventId)) {
                return;
            }
            if (this.HandleDisplayMarketDataWindow(window, eventId)) {
                return;
            }
            if (this.HandleUseSslEncryptionWindow(window, eventId)) {
                return;
            }
            if (this.HandleLoginMessages(window, eventId)) {
                return;
            }

            HandleUnknownMessageWindow(window, eventId);
        }
        catch (Exception e) {
            this.automater.logError(e);
        }
    }

    /**
     * Detects and handles the login messages window.
     *
     * @param window The window instance
     * @param eventId The id of the window event
     *
     * @return Returns true if the window was detected and handled
     */
    private boolean HandleLoginMessages(Window window, int eventId) {
        if (eventId != WindowEvent.WINDOW_OPENED) {
            return false;
        }

        String title = Common.getTitle(window);

        if (title != null && title.equals("Login Messages")) {
            // this window has custom IB obfuscated components, we can't click around nor understand
            this.automater.logMessage("Login failed: a user account-task is required. Please download"
                    + " the IB Gateway and follow the instructions provided https://www.interactivebrokers.com/en/trading/ibgateway-stable.php.");
            return true;
        }
        return false;
    }

    /**
     * Detects and handles the main login window.
     * - selects the "IB API" toggle button
     * - selects the "Live Trading" or "Paper Trading" toggle button
     * - enters the IB user name and password
     * - selects the "Use SSL" check box
     * - clicks the "Log In" or "Paper Log In" button
     *
     * @param window The window instance
     * @param eventId The id of the window event
     *
     * @return Returns true if the window was detected and handled
     */
    private boolean HandleLoginWindow(Window window, int eventId) throws Exception {
        if (eventId != WindowEvent.WINDOW_OPENED || 
            // restarting, no need to manually log in
            this.automater.getSettings().getRestarting()) {
            return false;
        }

        String title = Common.getTitle(window);

        if (title == null ||
            !Common.isFrame(window) ||
            (
            // v1023
            !title.equals("IBKR Gateway") &&
            !title.equals("IB Gateway") &&
             // v981
             !title.equals("Interactive Brokers Gateway"))) {
            return false;
        }

        this.automater.setMainWindow(window);
        this.automater.logMessage("Main window - Window title: [" + title + "] - Window name: [" + window.getName() + "]");

        boolean isLiveTradingMode = this.automater.getSettings().getTradingMode().equals("live");

        String buttonIbApiText = "IB API";
        JToggleButton ibApiButton = Common.getToggleButton(window, buttonIbApiText);
        if (ibApiButton == null) {
            this.automater.logMessage("Unexpected window found");
            LogWindowContents(window);
            throw new Exception("IB API toggle button not found");
        }
        if (!ibApiButton.isSelected()) {
            this.automater.logMessage("Click button: [" + buttonIbApiText + "]");
            ibApiButton.doClick();
        }

        String buttonTradingModeText = isLiveTradingMode ? "Live Trading" : "Paper Trading";
        JToggleButton tradingModeButton = Common.getToggleButton(window, buttonTradingModeText);
        if (tradingModeButton == null) {
            throw new Exception("Trading Mode toggle button not found");
        }
        if (!tradingModeButton.isSelected()) {
            this.automater.logMessage("Click button: [" + buttonTradingModeText + "]");
            tradingModeButton.doClick();
        }

        this.automater.logMessage("Trading mode: " + this.automater.getSettings().getTradingMode());

        JTextField userNameTextField = Common.getTextField(window, 0);
        if (userNameTextField == null) {
            throw new Exception("IB API user name text field not found");
        }
        userNameTextField.setText(this.automater.getSettings().getUserName());

        JTextField passwordTextField = Common.getTextField(window, 1);
        if (passwordTextField == null) {
            throw new Exception("IB API password text field not found");
        }
        passwordTextField.setText(this.automater.getSettings().getPassword());

        String useSslText = "Use SSL";
        JCheckBox useSslCheckbox = Common.getCheckBox(window, useSslText);
        if (useSslCheckbox == null) {
            this.automater.logMessage("Use SSL checkbox not found");
        }
        else {
            if (!useSslCheckbox.isSelected()) {
                this.automater.logMessage("Select checkbox: [" + useSslText + "]");
                useSslCheckbox.setSelected(true);
            }
        }

        String loginButtonText = isLiveTradingMode ? "Log In" : "Paper Log In";
        JButton loginButton = Common.getButton(window, loginButtonText);
        if (loginButton == null) {
            throw new Exception("Login button not found");
        }

        if (!loginButton.isEnabled()) {
            this.automater.logMessage("Login failed: invalid characters in credentials");
            return false;
        }

        this.automater.logMessage("Click button: [" + loginButtonText + "]");
        loginButton.doClick();

        return true;
    }

    /**
     * Detects and handles the login failed window.
     * - logs the error message text
     * - clicks the "OK" button
     *
     * @param window The window instance
     * @param eventId The id of the window event
     *
     * @return Returns true if the window was detected and handled
     */
    private boolean HandleLoginFailedWindow(Window window, int eventId) {
        if (eventId != WindowEvent.WINDOW_OPENED) {
            return false;
        }

        String title = Common.getTitle(window);

        if (title != null && (title.equals("Login failed")
                || title.equals("Unrecognized Username or Password"))) {
            JTextPane textPane = Common.getTextPane(window);
            String text = "";
            if (textPane != null) {
                text = textPane.getText().replaceAll("\\<.*?>", " ").trim();
            }

            this.automater.logMessage("Login failed: " + text);

            JButton button = Common.getButton(window, "OK");
            if (button != null) {
                this.automater.logMessage("Click button: [OK]");
                button.doClick();
            }

            return true;
        }

        return false;
    }

    /**
     * Detects and handles the Server Disconnected window.
     * - clicks the "OK" button
     * - closes the main window
     *
     * @param window The window instance
     * @param eventId The id of the window event
     *
     * @return Returns true if the window was detected and handled
     */
    private boolean HandleServerDisconnectedWindow(Window window, int eventId) {
        if (eventId != WindowEvent.WINDOW_OPENED) {
            return false;
        }

        String text = GetWindowText(window);

        if (text != null && text.contains("Connection to server failed: Server disconnected, please try again")) {
            this.automater.logMessage(text);

            JButton button = Common.getButton(window, "OK");
            if (button != null) {
                this.automater.logMessage("Click button: [OK]");
                button.doClick();
            }

            this.automater.logMessage("Server disconnection detected, closing IBGateway.");

            CloseMainWindow();

            return true;
        }

        return false;
    }

    /**
     * Detects and handles the "Too many failed login attempts" window.
     * - clicks the "OK" button
     * - closes the main window
     *
     * @param window The window instance
     * @param eventId The id of the window event
     *
     * @return Returns true if the window was detected and handled
     */
    private boolean HandleTooManyFailedLoginAttemptsWindow(Window window, int eventId) {
        if (eventId != WindowEvent.WINDOW_OPENED) {
            return false;
        }

        String text = GetWindowText(window);

        if (text != null && text.contains("Too many failed login attempts")) {
            this.automater.logMessage(text);

            JButton button = Common.getButton(window, "OK");
            if (button != null) {
                this.automater.logMessage("Click button: [OK]");
                button.doClick();
            }

            this.automater.logMessage("Too many failed login attempts, closing IBGateway.");

            CloseMainWindow();

            return true;
        }

        return false;
    }

    /**
     * Detects and handles the Password Notice window.
     * - logs the error message text
     * - clicks the "OK" button
     *
     * @param window The window instance
     * @param eventId The id of the window event
     *
     * @return Returns true if the window was detected and handled
     */
    private boolean HandlePasswordNoticeWindow(Window window, int eventId) {
        if (eventId != WindowEvent.WINDOW_OPENED) {
            return false;
        }

        String title = Common.getTitle(window);

        if (title != null && title.contains("Password Notice")) {
            JTextPane textPane = Common.getTextPane(window);
            String text = "";
            if (textPane != null) {
                text = textPane.getText().replaceAll("\\<.*?>", " ").trim();
            }

            this.automater.logMessage("Login failed: " + text);

            JButton button = Common.getButton(window, "OK");
            if (button != null) {
                this.automater.logMessage("Click button: [OK]");
                button.doClick();
            }

            return true;
        }

        return false;
    }

    /**
     * Detects and handles the Initialization window.
     * - starts the {@link GetMainWindowTask} task to find the main window
     *
     * @param window The window instance
     * @param eventId The id of the window event
     *
     * @return Returns true if the window was detected and handled
     */
    private boolean HandleInitializationWindow(Window window, int eventId) {
        if (eventId != WindowEvent.WINDOW_CLOSED) {
            return false;
        }

        String title = Common.getTitle(window);

        if (title != null && title.contains("Starting application...")) {
            // The main window might not be completely initialized at this point,
            // so we start a task and wait 30 seconds maximum for the window to be ready.

            RunInitializationUsingThread();
            RunRestartWatcher();
            RunShutdownWatcher();

            return true;
        }

        return false;
    }

    /**
     * Will start a thread which will get the main window and setup our settings
     *
     */
    private void RunInitializationUsingThread() {
        new Thread(()-> {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<Window> future = executor.submit(new GetMainWindowTask(this.automater));
            try {
                future.get(30, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                this.automater.logError(e);
            }
            executor.shutdown();
        }).start();
    }

    /**
     * Will start a thread which will monitor for restart requests and trigger a restart when detected
     *
     */
    @SuppressWarnings("SleepWhileInLoop")
    private void RunRestartWatcher() {
        new Thread(()-> {
            this.automater.logMessage("Start running restart watcher thread...");
            while (true) {
                try {
                    File file = new File("restart");
                    if(file.exists()) {
                        file.delete();
                        this.automater.logMessage("Restart request detected, starting restart...");
                        this.restartNow = true;
                        RunInitializationUsingThread();
                    }

                    Thread.sleep(1000 * 10);
                } catch (InterruptedException ex) {
                    // stopped
                }
            }
        }).start();
    }

    /**
     * Will start a thread which will get the main window and trigger shutdown
     */
    private void RunShutdownUsingThread() {
        new Thread(()-> {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<Window> future = executor.submit(new ShutdownTask(this.automater));
            try {
                future.get(30, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                this.automater.logError(e);
            }
            executor.shutdown();
        }).start();
    }

    /**
     * Will start a thread which will monitor for and trigger shutdown
     */
    @SuppressWarnings("SleepWhileInLoop")
    private void RunShutdownWatcher() {
        new Thread(()-> {
            this.automater.logMessage("Start running shutdown watcher thread...");
            while (true) {
                try {
                    File file = new File("shutdown");
                    if (file.exists()) {
                        file.delete();
                        this.automater.logMessage("Shutdown request detected. Shutting down...");
                        RunShutdownUsingThread();
                        break;
                    }

                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    // stopped
                }
            }
        }).start();
    }

    /**
     * Detects and handles the Paper Trading warning window.
     * - clicks the "I understand and accept" button
     *
     * @param window The window instance
     * @param eventId The id of the window event
     *
     * @return Returns true if the window was detected and handled
     */
    private boolean HandlePaperTradingAccountWindow(Window window, int eventId) throws Exception {
        if (eventId != WindowEvent.WINDOW_OPENED) {
            return false;
        }

        if (Common.getLabel(window, "This is not a brokerage account") == null) {
            return false;
        }

        String buttonText = "I understand and accept";
        JButton button = Common.getButton(window, buttonText);

        if (button != null) {
            this.automater.logMessage("Click button: [" + buttonText + "]");
            button.doClick();
        }
        else {
            throw new Exception("Button not found: [" + buttonText + "]");
        }

        return true;
    }

    /**
     * Detects and handles the Unsupported Version window.
     * - logs the error message text
     * - clicks the "OK" button
     *
     * @param window The window instance
     * @param eventId The id of the window event
     *
     * @return Returns true if the window was detected and handled
     */
    private boolean HandleUnsupportedVersionWindow(Window window, int eventId) throws Exception {
        if (eventId != WindowEvent.WINDOW_OPENED) {
            return false;
        }

        String message;
        JOptionPane optionPane = Common.getOptionPane(window, "is no longer supported");
        if (optionPane == null) {
            message = GetWindowText(window).toLowerCase().replaceAll("\\<.*?>","").replace("\n", " ");
            if (!message.contains("minimum supported version") && !message.contains("will be desupported on")) {
                return false;
            }
        }
        else {
            message = optionPane.getMessage().toString().toLowerCase().replaceAll("\\<.*?>","").replace("\n", " ");
        }

        this.automater.logMessage("IBGateway message: [" + message + "]");

        String buttonText = "OK";
        JButton button = Common.getButton(window, buttonText);

        if (button != null) {
            this.automater.logMessage("Click button: [" + buttonText + "]");
            button.doClick();
        }
        else {
            throw new Exception("Button not found: [" + buttonText + "]");
        }

        return true;
    }

    /**
     * Detects and handles the Configuration window.
     * - in the Configuration/API/Settings panel:
     *   - deselects the "Read-Only API" check box
     *   - sets the API Port Number
     *   - selects the "Create API message log file" check box
     *   - deselects the "Use Account Groups with Allocation Methods" check box
     * - in the Configuration/API/Precautions panel:
     *   - selects the "Bypass Order Precautions for API Orders" check box
     * - in the Configuration/Lock and Exit panel:
     *   - selects the "Auto restart" check box
     * - if requested, opens the Export IB logs window
     * - clicks the "OK" button
     *
     * @param window The window instance
     * @param eventId The id of the window event
     *
     * @return Returns true if the window was detected and handled
     */
    private boolean HandleConfigurationWindow(Window window, int eventId) throws Exception {
        if (eventId != WindowEvent.WINDOW_OPENED) {
            return false;
        }

        String title = Common.getTitle(window);
        if (title == null || !title.contains(" Configuration")) {
            return false;
        }

        JTree tree = Common.getTree(window);
        if (tree == null) {
            throw new Exception("Configuration tree not found");
        }

        Common.selectTreeNode(tree, new TreePath(new String[]{"Configuration", "API", "Settings"}));

        String readOnlyApiText = "Read-Only API";
        JCheckBox readOnlyApi = Common.getCheckBox(window, readOnlyApiText);
        if (readOnlyApi == null) {
            throw new Exception("Read-Only API check box not found");
        }
        if (readOnlyApi.isSelected()) {
            this.automater.logMessage("Unselect checkbox: [" + readOnlyApiText + "]");
            readOnlyApi.setSelected(false);
        }

        JTextField portNumber = Common.getTextField(window, 0);
        if (portNumber == null) {
            throw new Exception("API Port Number text field not found");
        }
        String portText = Integer.toString(this.automater.getSettings().getPortNumber());
        this.automater.logMessage("Set API port textbox value: [" + portText + "]");
        portNumber.setText(portText);

        String createApiLogText = "Create API message log file";
        JCheckBox createApiLog = Common.getCheckBox(window, createApiLogText);
        if (createApiLog == null) {
            throw new Exception("'Create API message log file' check box not found");
        }
        if (!createApiLog.isSelected()) {
            this.automater.logMessage("Select checkbox: [" + createApiLogText + "]");
            createApiLog.setSelected(true);
        }

        // v983+
        String faText = "Use Account Groups with Allocation Methods";
        JCheckBox faCheckBox = Common.getCheckBox(window, faText);
        if (faCheckBox != null && faCheckBox.isSelected()) {
            this.automater.logMessage("Unselect checkbox: [" + faText + "]");
            faCheckBox.setSelected(false);
        }

        Common.selectTreeNode(tree, new TreePath(new String[]{"Configuration", "API", "Precautions"}));

        String bypassOrderPrecautionsText = "Bypass Order Precautions for API Orders";
        JCheckBox bypassOrderPrecautions = Common.getCheckBox(window, bypassOrderPrecautionsText);
        if (bypassOrderPrecautions == null) {
            throw new Exception("Bypass Order Precautions check box not found");
        }
        if (!bypassOrderPrecautions.isSelected()) {
            this.automater.logMessage("Select checkbox: [" + bypassOrderPrecautionsText + "]");
            bypassOrderPrecautions.setSelected(true);
        }

        Common.selectTreeNode(tree, new TreePath(new String[]{"Configuration", "Lock and Exit"}));

        String autoRestartText = "Auto restart";
        JRadioButton autoRestart = Common.getRadioButton(window, autoRestartText);
        if (autoRestart == null) {
            throw new Exception("Auto restart radio button not found");
        }
        if (!autoRestart.isSelected()) {
            this.automater.logMessage("Select radio button: [" + autoRestartText + "]");
            autoRestart.setSelected(true);
        }

        JRadioButton amButton = Common.getRadioButton(window, "AM");
        if (amButton == null) {
            throw new Exception("Auto restart AM button not found");
        }
        JRadioButton pmButton = Common.getRadioButton(window, "PM");
        if (pmButton == null) {
            throw new Exception("Auto restart PM button not found");
        }

        JTextField restartTimeField = Common.getTextField(window, 0);
        if (restartTimeField == null) {
            throw new Exception("Restart time text field not found");
        }
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("hh:mma");
        // defaults
        String restartTime = "11:45";
        JRadioButton timeButton = pmButton;
        if(this.restartNow) {
            this.restartNow = false;
            // will restart in 2 minutes
            LocalDateTime now = LocalDateTime.now().plusMinutes(2);
            String completeTime = dtf.format(now);
            restartTime = completeTime.substring(0, 5);
            if("am".equals(completeTime.substring(5).toLowerCase())){
                timeButton = amButton;
            }
        }

        this.automater.logMessage("Set restart time value: [" + restartTime + "]");
        restartTimeField.setText(restartTime);
        if (!timeButton.isSelected()) {
            this.automater.logMessage("Select radio button: [" + timeButton.getText()+ "]");
            timeButton.setSelected(true);
        }
        else {
            this.automater.logMessage("Radio button: [" + timeButton.getText()+ "] already selected");
        }

        JButton okButton = Common.getButton(window, "OK");
        if (okButton == null) {
            throw new Exception("OK button not found");
        }
        this.automater.logMessage("Click button: [OK]");
        okButton.doClick();

        if (this.automater.getSettings().getExportIbGatewayLogs()) {
            SaveIBLogs();
        }

        this.automater.logMessage("Configuration settings updated.");

        return true;
    }

    /**
     * Detects and handles the Existing Session Detected window.
     * - clicks the "Exit Application" button
     *
     * @param window The window instance
     * @param eventId The id of the window event
     *
     * @return Returns true if the window was detected and handled
     */
    private boolean HandleExistingSessionDetectedWindow(Window window, int eventId) throws Exception {
        if (eventId != WindowEvent.WINDOW_OPENED) {
            return false;
        }

        String title = Common.getTitle(window);

        if (title != null && title.equals("Existing session detected")) {
            String buttonText = "Exit Application";
            JButton button = Common.getButton(window, buttonText);

            if (button != null) {
                this.automater.logMessage("Click button: [" + buttonText + "]");
                button.doClick();
            }
            else {
                throw new Exception("Button not found: [" + buttonText + "]");
            }

            return true;
        }

        return false;
    }

    /**
     * Detects and handles the Re-login Required window.
     * - clicks the "Re-login" button
     *
     * @param window The window instance
     * @param eventId The id of the window event
     *
     * @return Returns true if the window was detected and handled
     */
    private boolean HandleReloginRequiredWindow(Window window, int eventId) throws Exception {
        if (eventId != WindowEvent.WINDOW_OPENED) {
            return false;
        }

        String title = Common.getTitle(window);

        if (title != null && title.equals("Re-login is required")) {
            if (this.twoFactorConfirmationAttempts >= this.maxTwoFactorConfirmationAttempts) {
                this.automater.logMessage("Skipping Re-login, maximum attempts reached");

                JButton cancel = Common.getButton(window, "Cancel");
                if (cancel != null) {
                    this.automater.logMessage("Click button: [Cancel]");
                    cancel.doClick();
                }
                else {
                    throw new Exception("Button not found: [Cancel]");
                }
                return true;
            }

            String buttonText = "Re-login";
            JButton button = Common.getButton(window, buttonText);

            if (button != null) {
                this.automater.logMessage("Click button: [" + buttonText + "]");
                button.doClick();
            }
            else {
                throw new Exception("Button not found: [" + buttonText + "]");
            }

            return true;
        }

        return false;
    }

    /**
     * Detects and handles the Financial Advisor warning window.
     * - clicks the "Yes" button
     *
     * @param window The window instance
     * @param eventId The id of the window event
     *
     * @return Returns true if the window was detected and handled
     */
    private boolean HandleFinancialAdvisorWarningWindow(Window window, int eventId) throws Exception {
        if (eventId != WindowEvent.WINDOW_OPENED) {
            return false;
        }

        String title = Common.getTitle(window);

        if (title != null && title.contains("Financial Advisor Warning")) {
            String buttonText = "Yes";
            JButton button = Common.getButton(window, buttonText);

            if (button != null) {
                this.automater.logMessage("Click button: [" + buttonText + "]");
                button.doClick();
            }
            else {
                throw new Exception("Button not found: [" + buttonText + "]");
            }

            return true;
        }

        return false;
    }

    /**
     * Detects and handles the Exit Session Setting window.
     * - clicks the "OK" button
     *
     * @param window The window instance
     * @param eventId The id of the window event
     *
     * @return Returns true if the window was detected and handled
     */
    private boolean HandleExitSessionSettingWindow(Window window, int eventId) throws Exception {
        if (eventId != WindowEvent.WINDOW_ACTIVATED) {
            return false;
        }

        String title = Common.getTitle(window);

        if (title != null && title.contains("Exit Session Setting")) {
            String text = String.join(" ", Common.getLabelTextLines(window));
            this.automater.logMessage("Content: " + text);

            String buttonText = "OK";
            JButton button = Common.getButton(window, buttonText);

            if (button != null) {
                this.automater.logMessage("Click button: [" + buttonText + "]");
                button.doClick();
            }
            else {
                throw new Exception("Button not found: [" + buttonText + "]");
            }

            return true;
        }

        return false;
    }

    /**
     * Detects and handles the API support not available window (e.g. for IBKR Lite accounts).
     * - clicks the "OK" button
     *
     * @param window The window instance
     * @param eventId The id of the window event
     *
     * @return Returns true if the window was detected and handled
     */
    private boolean HandleApiNotAvailableWindow(Window window, int eventId) {
        if (eventId != WindowEvent.WINDOW_OPENED) {
            return false;
        }

        String title = Common.getTitle(window);

        if (title == null) {
            JTextPane textPane = Common.getTextPane(window);
            String text = "";
            if (textPane != null) {
                text = textPane.getText().replaceAll("\\<.*?>", " ").trim();
            }

            if (!text.contains("API support is not available for accounts that support free trading."))
            {
                return false;
            }

            this.automater.logMessage(text);

            JButton button = Common.getButton(window, "OK");
            if (button != null) {
                this.automater.logMessage("Click button: [OK]");
                button.doClick();
            }

            return true;
        }

        return false;
    }

    /**
     * Detects and handles the AutoRestart confirmation window.
     * - clicks the "OK" button
     *
     * @param window The window instance
     * @param eventId The id of the window event
     *
     * @return Returns true if the window was detected and handled
     */
    private boolean HandleEnableAutoRestartConfirmationWindow(Window window, int eventId) {
        if (eventId != WindowEvent.WINDOW_OPENED) {
            return false;
        }

        JTextPane textPane = Common.getTextPane(window);
        String text = "";
        if (textPane != null) {
            text = textPane.getText().replaceAll("\\<.*?>", " ").trim();
        }

        if (!text.contains("You have elected to have your trading platform restart automatically"))
        {
            return false;
        }

        this.automater.logMessage(text);

        JButton button = Common.getButton(window, "OK");
        if (button != null) {
            this.automater.logMessage("Click button: [OK]");
            button.doClick();
        }

        return true;
    }

    /**
     * Detects and handles the AutoRestart Token Expired window.
     * - clicks the "OK" button
     * - closes the main window
     *
     * @param window The window instance
     * @param eventId The id of the window event
     *
     * @return Returns true if the window was detected and handled
     */
    private boolean HandleAutoRestartTokenExpiredWindow(Window window, int eventId) throws Exception {
        if (eventId != WindowEvent.WINDOW_OPENED) {
            return false;
        }

        if (Common.getLabel(window, "Soft token=0 received instead of expected permanent") == null) {
            return false;
        }

        String buttonText = "OK";
        JButton button = Common.getButton(window, buttonText);

        if (button != null) {
            this.automater.logMessage("Click button: [" + buttonText + "]");
            button.doClick();
        }
        else {
            throw new Exception("Button not found: [" + buttonText + "]");
        }

        // we can do this only once, to avoid closing the restarted process
        if (!this.isAutoRestartTokenExpired)
        {
            this.isAutoRestartTokenExpired = true;

            this.automater.logMessage("Auto-restart token expired, closing IBGateway");

            CloseMainWindow();
        }

        return true;
    }

    /**
     * Detects and handles the AutoRestart Now window.
     * - clicks the "No" button
     *
     * @param window The window instance
     * @param eventId The id of the window event
     *
     * @return Returns true if the window was detected and handled
     */
    private boolean HandleAutoRestartNowWindow(Window window, int eventId) throws Exception {
        if (eventId != WindowEvent.WINDOW_OPENED) {
            return false;
        }

        String text = GetWindowText(window);

        if (text != null && text.contains("Would you like to restart now?"))
        {
            this.automater.logMessage(text);

            JButton button = Common.getButton(window, "No");
            if (button != null) {
                this.automater.logMessage("Click button: [No]");
                button.doClick();
            }

            return true;
        }

        return false;
    }

    /**
     * Detects and handles the Two Factor Authentication window.
     * - if the window is closed within 150 seconds since it was opened, 2FA confirmation was successful,
     * otherwise it is considered a timeout and other two attempts to login are performed
     *
     * @param window The window instance
     * @param eventId The id of the window event
     *
     * @return Returns true if the window was detected and handled
     */
    private boolean HandleTwoFactorAuthenticationWindow(Window window, int eventId) throws Exception {
        if (eventId != WindowEvent.WINDOW_OPENED && eventId != WindowEvent.WINDOW_CLOSED) {
            return false;
        }

        String title = Common.getTitle(window);
        if (title != null && title.equalsIgnoreCase("Second Factor Authentication")) {
            JTextArea textArea = Common.getTextArea(window);
            if(textArea != null && textArea.getText().equalsIgnoreCase("Select second factor device")) {
                if (eventId == WindowEvent.WINDOW_OPENED) {
                    // we need to select the 2fa method
                    JButton button = Common.getButton(window, "OK");
                    if(button != null) {
                        JList list = Common.getList(window);
                        if(list != null) {
                            ListModel listModel = ((JList) list).getModel();
                            boolean foundIbKey = false;
                            for (int i = 0; i < listModel.getSize(); i++) {
                                String entry = listModel.getElementAt(i).toString().trim();
                                this.automater.logMessage("2FA method: " + entry);
                                if (entry.equalsIgnoreCase("IB Key")) {
                                    foundIbKey = true;
                                    list.setSelectedIndex(i);
                                }
                            }

                            if(foundIbKey) {
                                button.doClick();
                            } else {
                                throw new Exception("Failed to find supported 2FA method 'IB Key'");
                            }
                            return true;
                        }
                    }
                    // unexpected
                    return false;
                }
                // 2fa selection window closed
                return true;
            }
            else if (eventId == WindowEvent.WINDOW_OPENED) {
                this.twoFactorConfirmationRequestTime = Instant.now();
                this.twoFactorConfirmationAttempts++;
                this.automater.logMessage("twoFactorConfirmationAttempts: " + this.twoFactorConfirmationAttempts + "/" + this.maxTwoFactorConfirmationAttempts);
                return true;
            }
            else if (eventId == WindowEvent.WINDOW_CLOSED) {
                Duration delta = Duration.between(this.twoFactorConfirmationRequestTime, Instant.now());
                // the timeout can be a few seconds earlier than 3 minutes, so we use 150 seconds to be safe
                if (delta.compareTo(Duration.ofSeconds(150)) >= 0) {
                    this.automater.logMessage("2FA confirmation timeout");
                    if (this.twoFactorConfirmationAttempts >= this.maxTwoFactorConfirmationAttempts) {
                        this.automater.logMessage("2FA maximum attempts reached");
                    }
                    else {
                        this.automater.logMessage("New login attempt with 2FA");

                        new Thread(()-> {
                            try {
                                int delay = 10000 * this.twoFactorConfirmationAttempts;

                                // IB considers a 2FA timeout as a failed login attempt
                                // so we wait before retrying to avoid the "Too many failed login attempts" error
                                Thread.sleep(delay);

                                // execute asynchronously on the AWT event dispatching thread
                                SwingUtilities.invokeLater(() -> {
                                    try {
                                        Window mainWindow = automater.getMainWindow();
                                        HandleLoginWindow(mainWindow, WindowEvent.WINDOW_OPENED);
                                    } catch (Exception e) {
                                        automater.logMessage("HandleLoginWindow error: " + e.getMessage());
                                    }
                                });
                            } catch (Exception e) {
                                automater.logMessage("HandleLoginWindow error: " + e.getMessage());
                            }
                        }).start();
                    }
                }
                else {
                    this.automater.logMessage("2FA confirmation success");
                    this.twoFactorConfirmationAttempts = 0;
                }
                return true;
            }
        }

        return false;
    }

    /**
     * Detects and handles the Display Market Data window.
     * - clicks the "I understand - display market data" button
     *
     * @param window The window instance
     * @param eventId The id of the window event
     *
     * @return Returns true if the window was detected and handled
     */
    private boolean HandleDisplayMarketDataWindow(Window window, int eventId) throws Exception {
        if (eventId != WindowEvent.WINDOW_OPENED) {
            return false;
        }

        String text = GetWindowText(window);

        if (text != null && text.contains("Bid, Ask and Last Size Display Update"))
        {
            this.automater.logMessage(text);

            String buttonText = "I understand - display market data";
            JButton button = Common.getButton(window, buttonText);
            if (button != null) {
                this.automater.logMessage("Click button: [" + buttonText + "]");
                button.doClick();
            }

            return true;
        }

        return false;
    }

    /**
     * Detects and handles the Use SSL Encryption window.
     * - clicks the "Reconnect using SSL" button
     *
     * @param window The window instance
     * @param eventId The id of the window event
     *
     * @return Returns true if the window was detected and handled
     */
    private boolean HandleUseSslEncryptionWindow(Window window, int eventId) {
        if (eventId != WindowEvent.WINDOW_OPENED) {
            return false;
        }

        String title = Common.getTitle(window);

        if (title != null && title.contains("Use SSL encryption")) {

            String buttonText = "Reconnect using SSL";
            JButton button = Common.getButton(window, buttonText);
            if (button != null) {
                this.automater.logMessage("Click button: [" + buttonText + "]");
                button.doClick();
            }

            return true;
        }

        return false;
    }

    /**
     * Returns whether the given window title is known.
     *
     * @param title The window title
     *
     * @return Returns true if the window title is known, false otherwise
     */
    private boolean IsKnownWindowTitle(String title) {
        if (title == null) {
            return false;
        }

        if (title.equals("Second Factor Authentication") ||
            title.equals("Security Code Card Authentication") ||
            title.equals("Enter security code")) {
            return true;
        }

        return false;
    }

    /**
     * Gets the text content of the window (labels, text panes and text areas only).
     *
     * @param window The window instance
     *
     * @return Returns the text content of the window
     */
    private String GetWindowText(Window window) {
        String text = "";

        JTextPane textPane = Common.getTextPane(window);
        if (textPane != null) {
            String t = textPane.getText();
            if (t != null) {
                text += t.replaceAll("\\<.*?>", " ").trim();
            }
        }

        JTextArea textArea = Common.getTextArea(window);
        if (textArea != null) {
            String t = textArea.getText();
            if (t != null) {
                text += " " + t.replaceAll("\\<.*?>", " ").trim();
            }
        }

        text += " " + String.join(" ", Common.getLabelTextLines(window));

        return text;
    }

    /**
     * Detects and handles an unknown message window.
     * - if requested, opens the Export IB logs window
     * - logs the window structure
     * - clicks the "OK" button
     *
     * @param window The window instance
     * @param eventId The id of the window event
     *
     * @return Returns true if the window was detected and handled
     */
    private boolean HandleUnknownMessageWindow(Window window, int eventId) {
        if (eventId != WindowEvent.WINDOW_OPENED) {
            return false;
        }

        String title = Common.getTitle(window);
        String windowName = window.getName();

        if (windowName != null && windowName.startsWith("dialog") && !IsKnownWindowTitle(title))
        {
            LogWindowContents(window);

            String text = GetWindowText(window);

            if (this.automater.getSettings().getExportIbGatewayLogs()) {
                SaveIBLogs();
            }

            this.automater.logMessage("Unknown message window detected: " + text);

            return true;
        }

        return false;
    }

    /**
     * Detects and handles the View Logs window.
     * - clicks the "Export Today Logs..." button
     *
     * @param window The window instance
     * @param eventId The id of the window event
     *
     * @return Returns true if the window was detected and handled
     */
    private boolean HandleViewLogsWindow(Window window, int eventId) {
        if (eventId != WindowEvent.WINDOW_OPENED) {
            return false;
        }

        String title = Common.getTitle(window);

        if (title != null && title.contains("View Logs")) {

            String buttonText = "Export Today Logs...";
            JButton button = Common.getButton(window, buttonText);
            if (button != null) {
                if (button.isEnabled()) {
                    this.viewLogsWindow = window;
                    this.automater.logMessage("Click button: [" + buttonText + "]");
                    button.doClick();
                }
                else {
                    buttonText = "Cancel";
                    button = Common.getButton(window, buttonText);
                    if (button != null) {
                        this.automater.logMessage("Click button: [" + buttonText + "]");
                        button.doClick();
                    }
                }
            }

            return true;
        }

        return false;
    }

    /**
     * Detects and handles the Enter Export Filename window.
     * - clicks the "Open" button
     *
     * @param window The window instance
     * @param eventId The id of the window event
     *
     * @return Returns true if the window was detected and handled
     */
    private boolean HandleExportFileNameWindow(Window window, int eventId) {
        if (eventId != WindowEvent.WINDOW_OPENED) {
            return false;
        }

        String title = Common.getTitle(window);

        if (title != null && title.contains("Enter export filename")) {

            String buttonText = "Open";
            JButton button = Common.getButton(window, buttonText);
            if (button != null) {
                this.automater.logMessage("Click button: [" + buttonText + "]");
                button.doClick();
            }

            return true;
        }

        return false;
    }

    /**
     * Detects and handles the Export Finished window.
     * - clicks the "OK" button
     * - clicks the "Cancel" button on the parent window (View Logs)
     *
     * @param window The window instance
     * @param eventId The id of the window event
     *
     * @return Returns true if the window was detected and handled
     */
    private boolean HandleExportFinishedWindow(Window window, int eventId) throws Exception {
        if (eventId != WindowEvent.WINDOW_OPENED) {
            return false;
        }

        if (Common.getOptionPane(window, "Finished exporting logs") == null) {
            return false;
        }

        JButton button = Common.getButton(window, "OK");
        if (button != null) {
            this.automater.logMessage("Click button: [OK]");
            button.doClick();
        }
        this.automater.logMessage("Finished exporting logs, closing export logs window");

        String buttonText = "Cancel";
        JButton cancelButton = Common.getButton(this.viewLogsWindow, buttonText);
        if (cancelButton != null) {
            this.viewLogsWindow = null;
            this.automater.logMessage("Click button: [" + buttonText + "]");
            cancelButton.doClick();
        }

        return true;
    }

    /**
     * Closes the main window.
     */
    private void CloseMainWindow()
    {
        new Thread(()-> {
            this.automater.logMessage("CloseMainWindow thread started");

            ExecutorService executor = Executors.newSingleThreadExecutor();

            executor.execute(() -> {
                try {
                    Window mainWindow = this.automater.getMainWindow();
                    this.automater.logMessage("Closing main window - Window title: [" + Common.getTitle(mainWindow) + "] - Window name: [" + mainWindow.getName() + "]");
                    ((JFrame) this.automater.getMainWindow()).setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    WindowEvent closingEvent = new WindowEvent(this.automater.getMainWindow(), WindowEvent.WINDOW_CLOSING);
                    Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(closingEvent);
                    this.automater.logMessage("Close main window message sent");
                } catch (Exception e) {
                    this.automater.logMessage("CloseMainWindow execute error: " + e.getMessage());
                }
            });

            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS))
                {
                    this.automater.logMessage("Timeout in execution of CloseMainWindow");
                }
            } catch (InterruptedException e) {
                this.automater.logMessage("CloseMainWindow await error: " + e.getMessage());
            }

            this.automater.logMessage("CloseMainWindow thread ended");

        }).start();
    }

    /**
     * Logs the structure of the specified window.
     *
     * @param window The window instance
     */
    private void LogWindowContents(Window window) {
        List<Component> components = Common.getComponents(window);

        this.automater.logMessage("DEBUG: Window title: [" + Common.getTitle(window) + "] - Window name: [" + window.getName() + "]");

        components.forEach((component) -> {
            String text = "";
            if (component instanceof JLabel)
            {
                text = " - JLabel Text: [" + ((JLabel) component).getText() + "]";
            }
            else if (component instanceof JTextPane)
            {
                text = " - JTextPane Text: [" + ((JTextPane) component).getText() + "]";
            }
            else if (component instanceof JTextField)
            {
                text = " - JTextField Text: [" + ((JTextField) component).getText() + "]";
            }
            else if (component instanceof JTextArea)
            {
                text = " - JTextArea Text: [" + ((JTextArea) component).getText() + "]";
            }
            else if (component instanceof JCheckBox)
            {
                text = " - JCheckBox Text: [" + ((JCheckBox) component).getText() + "]";
            }
            else if (component instanceof JOptionPane)
            {
                text = " - JOptionPane Message: [" + ((JOptionPane) component).getMessage().toString() + "]";
            }
            this.automater.logMessage("DEBUG: - Component: [" + component.toString() + "]" + text);
        });
    }

    /**
     * Clicks the File/Gateway Logs menu item.
     */
    private void SaveIBLogs()
    {
        if (this.viewLogsWindow != null) {
            // we are already exporting the logs, let's not request it again because we keep state through 'this.viewLogsWindow'
            // trying to export twice at the same time will overrite this variable and the export window will remain open.
            // seen this happen when we detect an unknown window at start where we are already storing the logs
            this.automater.logMessage("Skip export gateway logs request, it's already being executed!");
            return;
        }

        Window window = this.automater.getMainWindow();
        if (window != null) {
            JMenuItem menuItem = Common.getMenuItem(window, "File", "Gateway Logs");
            if (menuItem != null) {
                menuItem.doClick();
            }
            else {
                this.automater.logMessage("Gateway Logs menu not found.");
            }

            JMenuItem apiLogsButton = Common.getMenuItem(window, "File", "API Logs");
            if (apiLogsButton != null) {
                apiLogsButton.doClick();
            }
            else {
                this.automater.logMessage("API Logs menu not found.");
            }
        }
    }
}

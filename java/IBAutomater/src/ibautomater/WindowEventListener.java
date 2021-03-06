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
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

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
    private Window viewLogsWindow = null;

    WindowEventListener(IBAutomater automater) {
        this.automater = automater;
    }

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

            HandleUnknownMessageWindow(window, eventId);
        }
        catch (Exception e) {
            this.automater.logError(e.toString());
        }
    }

    private boolean HandleLoginWindow(Window window, int eventId) throws Exception {
        if (eventId != WindowEvent.WINDOW_OPENED) {
            return false;
        }

        String title = Common.getTitle(window);

        if (title == null ||
            !Common.isFrame(window) ||
            (!title.equals("IB Gateway") &&
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

        String loginButtonText = isLiveTradingMode ? "Log In" : "Paper Log In";
        JButton loginButton = Common.getButton(window, loginButtonText);
        if (loginButton == null) {
            throw new Exception("Login button not found");
        }

        this.automater.logMessage("Click button: [" + loginButtonText + "]");
        loginButton.doClick();

        return true;
    }

    private boolean HandleLoginFailedWindow(Window window, int eventId) {
        if (eventId != WindowEvent.WINDOW_OPENED) {
            return false;
        }

        String title = Common.getTitle(window);

        if (title != null && title.equals("Login failed")) {
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

    private boolean HandleInitializationWindow(Window window, int eventId) {
        if (eventId != WindowEvent.WINDOW_CLOSED) {
            return false;
        }

        String title = Common.getTitle(window);

        if (title != null && title.contains("Starting application...")) {
            // The main window might not be completely initialized at this point,
            // so we start a task and wait 30 seconds maximum for the window to be ready.

            new Thread(()-> {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<Window> future = executor.submit(new GetMainWindowTask(this.automater));
                try {
                    future.get(30, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    this.automater.logError(e.toString());
                }
                executor.shutdown();
            }).start();
        }

        return false;
    }

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

    private boolean HandleUnsupportedVersionWindow(Window window, int eventId) throws Exception {
        if (eventId != WindowEvent.WINDOW_OPENED) {
            return false;
        }

        String title = Common.getTitle(window);
        if (title != null) {
            return false;
        }

        JOptionPane optionPane = Common.getOptionPane(window, "is no longer supported");
        if (optionPane == null) {
            return false;
        }

        String message = optionPane.getMessage().toString().replaceAll("\\<.*?>","").replace("\n", " ");
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
        if (faCheckBox != null) {
            if (faCheckBox.isSelected()) {
                this.automater.logMessage("Unselect checkbox: [" + faText + "]");
                faCheckBox.setSelected(false);
            }
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

    private boolean HandleReloginRequiredWindow(Window window, int eventId) throws Exception {
        if (eventId != WindowEvent.WINDOW_OPENED) {
            return false;
        }

        String title = Common.getTitle(window);

        if (title != null && title.equals("Re-login is required")) {
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

    private boolean IsKnownWindowTitle(String title) {
        if (title.equals("Second Factor Authentication") ||
            title.equals("Security Code Card Authentication") ||
            title.equals("Enter security code")) {
            return true;
        }

        return false;
    }

    private String GetWindowText(Window window) {
        String text;

        JTextPane textPane = Common.getTextPane(window);
        if (textPane != null) {
            text = textPane.getText().replaceAll("\\<.*?>", " ").trim();
        }
        else {
            text = String.join(" ", Common.getLabelTextLines(window));
        }

        return text;
    }

    private boolean HandleUnknownMessageWindow(Window window, int eventId) {
        if (eventId != WindowEvent.WINDOW_OPENED) {
            return false;
        }

        String title = Common.getTitle(window);
        String windowName = window.getName();

        if (windowName != null && windowName.startsWith("dialog") && !IsKnownWindowTitle(title))
        {
            String text = GetWindowText(window);

            if (text != null && text.length() > 0)
            {
                SaveIBLogs();

                this.automater.logMessage("Unknown message window detected: " + text);
            }

            JButton button = Common.getButton(window, "OK");
            if (button != null) {
                this.automater.logMessage("Click button: [OK]");
                button.doClick();
            }

            return true;
        }

        return false;
    }

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

        String buttonText = "Cancel";
        JButton cancelButton = Common.getButton(this.viewLogsWindow, buttonText);
        if (cancelButton != null) {
            this.viewLogsWindow = null;
            this.automater.logMessage("Click button: [" + buttonText + "]");
            cancelButton.doClick();
        }

        return true;
    }

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

    private void LogWindowContents(Window window) {
        List<Component> components = Common.getComponents(window);

        this.automater.logMessage("DEBUG: Window title: [" + Common.getTitle(window) + "] - Window name: [" + window.getName() + "]");

        components.forEach((component) -> {
            this.automater.logMessage("DEBUG: - Component: [" + component.toString() + "]");
        });
    }

    private void SaveIBLogs()
    {
        Window window = this.automater.getMainWindow();
        if (window != null) {
            JMenuItem menuItem = Common.getMenuItem(window, "File", "Gateway Logs");
            if (menuItem != null) {
                menuItem.doClick();
            }
            else {
                this.automater.logMessage("Gateway Logs menu not found.");
            }
        }
    }
}


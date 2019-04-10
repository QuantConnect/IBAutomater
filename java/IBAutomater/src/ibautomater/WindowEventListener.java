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
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JMenuItem;
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

    WindowEventListener(IBAutomater automater) {
        this.automater = automater;
    }

    @Override
    public void eventDispatched(AWTEvent awtEvent) {
        int eventId = awtEvent.getID();
        Window window = ((WindowEvent)awtEvent).getWindow();
        if (this.handledEvents.containsKey(eventId)) {
            this.automater.logMessage("Window event: [" + this.handledEvents.get(eventId) + "] - Window title: [" + Common.getTitle(window) + "]");
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
            if (this.HandleMainWindow(window, eventId)) {
                return;
            }
            if (this.HandleInitializationWindow(window, eventId)) {
                return;
            }
            if (this.HandlePaperTradingAccountWindow(window, eventId)) {
                return;
            }
            if (this.HandleConfigurationWindow(window, eventId)) {
                return;
            }
            if (this.HandleExistingSessionDetectedWindow(window, eventId)) {
                return;
            }
        }
        catch (Exception e) {
            this.automater.logError(e);
        }
    }

    private boolean HandleLoginWindow(Window window, int eventId) throws Exception {
        if (eventId != WindowEvent.WINDOW_OPENED) {
            return false;
        }
        
        boolean isLiveTradingMode = this.automater.getSettings().getTradingMode().equals("live");
        String buttonText = isLiveTradingMode ? "Log In" : "Paper Log In";
        
        JButton loginButton = Common.getButton(window, buttonText);
        if (loginButton == null) {
            return false;
        }
        
        JToggleButton ibApiButton = Common.getToggleButton(window, "IB API");
        if (ibApiButton == null) {
            throw new Exception("IB API toggle button not found");
        }
        
        ibApiButton.doClick();
        
        String buttonTradingModeText = isLiveTradingMode ? "Live Trading" : "Paper Trading";
        JToggleButton tradingModeButton = Common.getToggleButton(window, buttonTradingModeText);
        if (tradingModeButton == null) {
            throw new Exception("Trading Mode toggle button not found");
        }
        if (!tradingModeButton.isSelected()) {
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
                button.doClick();
            }
            
            return true;
        }
        
        return false;
    }

    private boolean HandleMainWindow(Window window, int eventId) {
        if (eventId != WindowEvent.WINDOW_OPENED) {
            return false;
        }
        
        String title = Common.getTitle(window);
        
        if (title != null && title.contains("IB Gateway") && title.contains("API Account")) {
            this.automater.setMainWindow(window);
            
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
            JMenuItem menuItem = Common.getMenuItem(this.automater.getMainWindow(), "Configure", "Settings");
            menuItem.doClick();
            
            return true;
        }
        
        return false;
    }

    private boolean HandlePaperTradingAccountWindow(Window window, int eventId) {
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
        
        JCheckBox readOnlyApi = Common.getCheckBox(window, "Read-Only API");
        if (readOnlyApi == null) {
            throw new Exception("Read-Only API check box not found");
        }
        if (readOnlyApi.isSelected()) {
            readOnlyApi.setSelected(false);
        }
        
        JTextField portNumber = Common.getTextField(window, 0);
        if (portNumber == null) {
            throw new Exception("Port Number text field not found");
        }
        portNumber.setText(Integer.toString(this.automater.getSettings().getPortNumber()));
        
        Common.selectTreeNode(tree, new TreePath(new String[]{"Configuration", "API", "Precautions"}));
        
        JCheckBox bypassOrderPrecautions = Common.getCheckBox(window, "Bypass Order Precautions for API Orders");
        if (bypassOrderPrecautions == null) {
            throw new Exception("Bypass Order Precautions check box not found");
        }
        if (!bypassOrderPrecautions.isSelected()) {
            bypassOrderPrecautions.setSelected(true);
        }

        JButton okButton = Common.getButton(window, "OK");
        if (okButton == null) {
            throw new Exception("OK button not found");
        }
        okButton.doClick();
        
        return true;
    }

    private boolean HandleExistingSessionDetectedWindow(Window window, int eventId) {
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
            
            return true;
        }
        
        return false;
    }

}


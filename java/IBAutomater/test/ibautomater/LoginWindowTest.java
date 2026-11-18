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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Drives the login window handler with windows built like the gateway's, never shown on screen
 */
public class LoginWindowTest {

    /**
     * A login window: the IB API and trading mode toggles, the credentials, Use SSL and the login button
     */
    private static class LoginFrame extends JFrame {
        final JToggleButton ibApi = new JToggleButton("IB API");
        final JToggleButton fixCtci = new JToggleButton("FIX CTCI", true);
        final JToggleButton liveTrading = new JToggleButton("Live Trading", true);
        final JToggleButton paperTrading = new JToggleButton("Paper Trading");
        final JTextField userName = new JTextField();
        final JPasswordField password = new JPasswordField();
        final JCheckBox useSsl = new JCheckBox("Use SSL");
        final JButton loginButton;
        final AtomicInteger loginClicks = new AtomicInteger();

        LoginFrame(String loginButtonText, boolean withIbApiToggle) {
            super("IBKR Gateway");
            this.loginButton = new JButton(loginButtonText);
            this.loginButton.addActionListener(e -> this.loginClicks.incrementAndGet());

            // each pair is exclusive, as in the gateway
            ButtonGroup apiGroup = new ButtonGroup();
            apiGroup.add(this.ibApi);
            apiGroup.add(this.fixCtci);
            ButtonGroup tradingModeGroup = new ButtonGroup();
            tradingModeGroup.add(this.liveTrading);
            tradingModeGroup.add(this.paperTrading);

            JPanel panel = new JPanel();
            if (withIbApiToggle) {
                panel.add(this.ibApi);
            }
            panel.add(this.fixCtci);
            panel.add(this.liveTrading);
            panel.add(this.paperTrading);
            panel.add(this.userName);
            panel.add(this.password);
            panel.add(this.useSsl);
            panel.add(this.loginButton);
            this.setContentPane(panel);
        }
    }

    /**
     * The window 10.50 opens after the login, with the login window's title
     */
    private static JFrame createPostLoginFrame() {
        JFrame frame = new JFrame("IBKR Gateway");
        JPanel panel = new JPanel();
        panel.add(new JLabel("Authenticating..."));
        frame.setContentPane(panel);
        return frame;
    }

    private static IBAutomater createAutomater(String tradingMode, boolean restarting) {
        return new IBAutomater("user", "password", tradingMode, 4002, false, restarting);
    }

    @ParameterizedTest
    @ValueSource(strings = { "paper", "live" })
    public void LogsInOnTheLoginWindow(String tradingMode) throws Exception {
        IBAutomater automater = createAutomater(tradingMode, false);
        WindowEventListener listener = new WindowEventListener(automater);
        boolean isLive = tradingMode.equals("live");
        LoginFrame loginFrame = new LoginFrame(isLive ? "Log In" : "Paper Log In", true);

        assertTrue(listener.HandleLoginWindow(loginFrame, WindowEvent.WINDOW_OPENED));

        assertSame(loginFrame, automater.getMainWindow());
        assertTrue(loginFrame.ibApi.isSelected());
        assertEquals(isLive, loginFrame.liveTrading.isSelected());
        assertEquals(!isLive, loginFrame.paperTrading.isSelected());
        assertEquals("user", loginFrame.userName.getText());
        assertEquals("password", new String(loginFrame.password.getPassword()));
        assertTrue(loginFrame.useSsl.isSelected());
        assertEquals(1, loginFrame.loginClicks.get());
    }

    @Test
    public void LeavesThePostLoginWindowWithTheSameTitleAlone() throws Exception {
        IBAutomater automater = createAutomater("paper", false);
        WindowEventListener listener = new WindowEventListener(automater);
        LoginFrame loginFrame = new LoginFrame("Paper Log In", true);
        listener.HandleLoginWindow(loginFrame, WindowEvent.WINDOW_OPENED);

        assertFalse(listener.HandleLoginWindow(createPostLoginFrame(), WindowEvent.WINDOW_OPENED));

        // the 2FA retry logs in again on the window kept as the main window, which must still be the login window
        assertSame(loginFrame, automater.getMainWindow());
        assertTrue(listener.HandleLoginWindow(automater.getMainWindow(), WindowEvent.WINDOW_OPENED));
        assertEquals(2, loginFrame.loginClicks.get());
    }

    @Test
    public void ThrowsWhenTheLoginWindowHasNoIbApiToggle() {
        IBAutomater automater = createAutomater("paper", false);
        WindowEventListener listener = new WindowEventListener(automater);
        LoginFrame loginFrame = new LoginFrame("Paper Log In", false);

        Exception exception = assertThrows(Exception.class, () -> listener.HandleLoginWindow(loginFrame, WindowEvent.WINDOW_OPENED));

        assertEquals("IB API toggle button not found", exception.getMessage());
        assertEquals(0, loginFrame.loginClicks.get());
    }

    @Test
    public void IgnoresTheLoginWindowWhenRestarting() throws Exception {
        IBAutomater automater = createAutomater("paper", true);
        WindowEventListener listener = new WindowEventListener(automater);
        LoginFrame loginFrame = new LoginFrame("Paper Log In", true);

        assertFalse(listener.HandleLoginWindow(loginFrame, WindowEvent.WINDOW_OPENED));
        assertEquals(0, loginFrame.loginClicks.get());
    }

    @Test
    public void IgnoresOtherWindowEvents() throws Exception {
        IBAutomater automater = createAutomater("paper", false);
        WindowEventListener listener = new WindowEventListener(automater);
        LoginFrame loginFrame = new LoginFrame("Paper Log In", true);

        assertFalse(listener.HandleLoginWindow(loginFrame, WindowEvent.WINDOW_ACTIVATED));
        assertEquals(0, loginFrame.loginClicks.get());
    }
}

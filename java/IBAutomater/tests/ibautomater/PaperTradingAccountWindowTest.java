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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import org.junit.jupiter.api.Test;

/**
 * Drives the paper trading warning handler with windows built like the gateway's (v1039) warnings, never shown on screen
 */
public class PaperTradingAccountWindowTest {

    // the gateway's login warning holds its message in an HTML label
    private static final String LOGIN_WARNING = "<html>This is not a brokerage account. <br>This is a \"paper\" trading account in which you <br>"
        + "can engage in simulated trading. <br><br>Please confirm that you understand and wish <br>"
        + "to conduct simulated trading before proceeding.</html>";

    // the gateway's warnings on orders hold their message in an HTML text pane, whose text comes back wrapped and indented
    private static final String PREVIEW_MODE_WARNING_HTML = "<html>\n  <head>\n    \n  </head>\n  <body>\n"
        + "    Your Paper Trading account is no longer operating in &quot;Preview&quot; \n"
        + "    mode<br>&#8226; Simulated order handling/trades now uses real-time market data instead of delayed data.\n"
        + "  </body>\n</html>\n";

    /**
     * A warning window laid out like the gateway's: an icon, the message, the title label and the buttons
     */
    private static class WarningFrame extends JFrame {
        final AtomicInteger acceptClicks = new AtomicInteger();
        final AtomicInteger cancelClicks = new AtomicInteger();

        WarningFrame(Component message) {
            super("Warning");
            JButton accept = new JButton("I understand and accept");
            accept.addActionListener(e -> this.acceptClicks.incrementAndGet());
            JButton cancel = new JButton("Cancel");
            cancel.addActionListener(e -> this.cancelClicks.incrementAndGet());

            JPanel panel = new JPanel();
            panel.add(new JLabel((String) null));
            panel.add(message);
            panel.add(accept);
            panel.add(cancel);
            panel.add(new JLabel(""));
            panel.add(new JLabel("Warning"));
            panel.add(new JLabel(""));
            this.setContentPane(panel);
        }
    }

    private static WindowEventListener createListener() {
        return new WindowEventListener(new IBAutomater("user", "password", "paper", 4002, false, false));
    }

    private static JTextPane createTextPane(String text) {
        JTextPane textPane = new JTextPane();
        textPane.setContentType("text/html");
        textPane.setText(text);
        textPane.setEditable(false);
        return textPane;
    }

    @Test
    public void AcceptsTheLoginPaperTradingWarning() throws Exception {
        WarningFrame frame = new WarningFrame(new JLabel(LOGIN_WARNING));

        assertTrue(createListener().HandlePaperTradingAccountWindow(frame, WindowEvent.WINDOW_OPENED));

        assertEquals(1, frame.acceptClicks.get());
        assertEquals(0, frame.cancelClicks.get());
    }

    @Test
    public void AcceptsThePreviewModeWarningInATextPane() throws Exception {
        WarningFrame frame = new WarningFrame(createTextPane(PREVIEW_MODE_WARNING_HTML));

        assertTrue(createListener().HandlePaperTradingAccountWindow(frame, WindowEvent.WINDOW_OPENED));

        assertEquals(1, frame.acceptClicks.get());
        assertEquals(0, frame.cancelClicks.get());
    }

    @Test
    public void AcceptsThePreviewModeWarningInALabel() throws Exception {
        WarningFrame frame = new WarningFrame(new JLabel("<html>Your Paper Trading account is no longer operating in \"Preview\" mode<br>"
            + "&bull; Simulated order handling/trades now uses real-time market data instead of delayed data.</html>"));

        assertTrue(createListener().HandlePaperTradingAccountWindow(frame, WindowEvent.WINDOW_OPENED));

        assertEquals(1, frame.acceptClicks.get());
        assertEquals(0, frame.cancelClicks.get());
    }

    @Test
    public void AcceptsThePreviewModeWarningWithNonBreakingSpaces() throws Exception {
        WarningFrame frame = new WarningFrame(new JLabel("<html>Your Paper&nbsp;Trading account is no\u00a0longer operating&#160;in "
            + "&ldquo;Preview&rdquo; mode</html>"));

        assertTrue(createListener().HandlePaperTradingAccountWindow(frame, WindowEvent.WINDOW_OPENED));

        assertEquals(1, frame.acceptClicks.get());
        assertEquals(0, frame.cancelClicks.get());
    }

    @Test
    public void IgnoresOtherWarnings() throws Exception {
        WarningFrame frame = new WarningFrame(new JLabel("<html>Market data farm connection is broken</html>"));

        assertFalse(createListener().HandlePaperTradingAccountWindow(frame, WindowEvent.WINDOW_OPENED));

        assertEquals(0, frame.acceptClicks.get());
    }

    @Test
    public void IgnoresThePreviewModeWarningClosing() throws Exception {
        WarningFrame frame = new WarningFrame(createTextPane(PREVIEW_MODE_WARNING_HTML));

        assertFalse(createListener().HandlePaperTradingAccountWindow(frame, WindowEvent.WINDOW_CLOSED));

        assertEquals(0, frame.acceptClicks.get());
    }
}

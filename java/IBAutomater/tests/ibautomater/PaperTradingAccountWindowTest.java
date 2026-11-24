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
 * Drives the paper trading warning handler with windows built like the gateway's, never shown on screen
 */
public class PaperTradingAccountWindowTest {

    private static final String PREVIEW_MODE_WARNING = "<html>Your Paper Trading account is no longer operating in &quot;Preview&quot; mode<br>"
        + "&bull; Simulated order handling/trades now uses real-time market data instead of delayed data.</html>";

    /**
     * A warning window holding the given message and the "I understand and accept" and "Cancel" buttons
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
            panel.add(message);
            panel.add(accept);
            panel.add(cancel);
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
        return textPane;
    }

    @Test
    public void AcceptsTheLoginPaperTradingWarning() throws Exception {
        WarningFrame frame = new WarningFrame(new JLabel("<html>This is not a brokerage account. This is a paper trading account.</html>"));

        assertTrue(createListener().HandlePaperTradingAccountWindow(frame, WindowEvent.WINDOW_OPENED));

        assertEquals(1, frame.acceptClicks.get());
        assertEquals(0, frame.cancelClicks.get());
    }

    @Test
    public void AcceptsThePreviewModeWarningInALabel() throws Exception {
        WarningFrame frame = new WarningFrame(new JLabel(PREVIEW_MODE_WARNING));

        assertTrue(createListener().HandlePaperTradingAccountWindow(frame, WindowEvent.WINDOW_OPENED));

        assertEquals(1, frame.acceptClicks.get());
        assertEquals(0, frame.cancelClicks.get());
    }

    @Test
    public void AcceptsThePreviewModeWarningInATextPane() throws Exception {
        WarningFrame frame = new WarningFrame(createTextPane(PREVIEW_MODE_WARNING));

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
        WarningFrame frame = new WarningFrame(new JLabel(PREVIEW_MODE_WARNING));

        assertFalse(createListener().HandlePaperTradingAccountWindow(frame, WindowEvent.WINDOW_CLOSED));

        assertEquals(0, frame.acceptClicks.get());
    }
}

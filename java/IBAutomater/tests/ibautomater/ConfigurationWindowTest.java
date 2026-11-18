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

import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Drives the configuration window handler with a window holding the settings it changes, never shown on screen
 */
public class ConfigurationWindowTest {

    private static class ConfigurationFrame extends JFrame {
        final JCheckBox readOnlyApi = new JCheckBox("Read-Only API", true);
        final JCheckBox resubmitOrders;
        final JCheckBox accountGroups;
        final JCheckBox bypassOrderPrecautions = new JCheckBox("Bypass Order Precautions for API Orders");
        final AtomicInteger okClicks = new AtomicInteger();

        ConfigurationFrame(String resubmitOrdersText, boolean resubmitOrdersSelected, String accountGroupsText) {
            super("Trader Workstation Configuration (Simulated Trading)");
            this.resubmitOrders = new JCheckBox(resubmitOrdersText);
            this.accountGroups = new JCheckBox(accountGroupsText, true);
            DefaultMutableTreeNode root = new DefaultMutableTreeNode("Configuration");
            DefaultMutableTreeNode api = new DefaultMutableTreeNode("API");
            api.add(new DefaultMutableTreeNode("Settings"));
            api.add(new DefaultMutableTreeNode("Precautions"));
            root.add(api);
            root.add(new DefaultMutableTreeNode("Lock and Exit"));

            JRadioButton am = new JRadioButton("AM");
            JRadioButton pm = new JRadioButton("PM", true);
            ButtonGroup amPm = new ButtonGroup();
            amPm.add(am);
            amPm.add(pm);
            JButton ok = new JButton("OK");
            ok.addActionListener(e -> this.okClicks.incrementAndGet());
            this.resubmitOrders.setSelected(resubmitOrdersSelected);

            JPanel panel = new JPanel();
            panel.add(new JTree(root));
            panel.add(this.readOnlyApi);
            panel.add(new JTextField());
            panel.add(new JCheckBox("Create API message log file"));
            if (accountGroupsText != null) {
                panel.add(this.accountGroups);
            }
            if (resubmitOrdersText != null) {
                panel.add(this.resubmitOrders);
            }
            panel.add(this.bypassOrderPrecautions);
            panel.add(new JRadioButton("Auto restart"));
            panel.add(am);
            panel.add(pm);
            panel.add(ok);
            this.setContentPane(panel);
        }
    }

    private static WindowEventListener createListener() {
        return new WindowEventListener(new IBAutomater("user", "password", "paper", 4002, false, false));
    }

    // 10.50 labels it with a trailing period
    @ParameterizedTest
    @CsvSource({
        "'Maintain and resubmit orders when connection is restored.', true",
        "'Maintain and resubmit orders when connection is restored.', false",
        "'Maintain and resubmit orders when connection is restored', true",
    })
    public void TurnsOffOrderResubmissionAfterReconnect(String label, boolean selected) throws Exception {
        ConfigurationFrame frame = new ConfigurationFrame(label, selected, null);

        assertTrue(createListener().HandleConfigurationWindow(frame, WindowEvent.WINDOW_OPENED));

        assertFalse(frame.resubmitOrders.isSelected());
        assertFalse(frame.readOnlyApi.isSelected());
        assertTrue(frame.bypassOrderPrecautions.isSelected());
        assertEquals(1, frame.okClicks.get());
    }

    // selected, IB rejects an order whose FA method differs from its group's; 10.39 and 10.50 label it with a trailing period
    @ParameterizedTest
    @CsvSource({
        "'Use Account Groups with Allocation Methods.'",
        "'Use Account Groups with Allocation Methods'",
    })
    public void UnselectsAccountGroupsWithAllocationMethods(String label) throws Exception {
        ConfigurationFrame frame = new ConfigurationFrame("Maintain and resubmit orders when connection is restored.", true, label);

        assertTrue(createListener().HandleConfigurationWindow(frame, WindowEvent.WINDOW_OPENED));

        assertFalse(frame.accountGroups.isSelected());
        assertFalse(frame.resubmitOrders.isSelected());
        assertEquals(1, frame.okClicks.get());
    }

    // gateways before 10.40 don't have the setting
    @Test
    public void AppliesTheSettingsWithoutOrderResubmissionSetting() throws Exception {
        ConfigurationFrame frame = new ConfigurationFrame(null, false, null);

        assertTrue(createListener().HandleConfigurationWindow(frame, WindowEvent.WINDOW_OPENED));

        assertFalse(frame.readOnlyApi.isSelected());
        assertEquals(1, frame.okClicks.get());
    }
}

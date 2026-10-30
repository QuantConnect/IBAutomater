/*
 * QUANTCONNECT.COM - Democratizing Finance, Empowering Individuals.
 * IBAutomater v1.0. Copyright 2019 QuantConnect Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package ibautomater;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * Dependency-free tests for Financial Advisor Gateway configuration helpers.
 * The Ant test target fails on the first error.
 */
public final class IBAutomaterSelfTest {
    private static int assertions;

    public static void main(String[] args) {
        TestFinancialAdvisorAllocationGroupsCheckBox();
        TestGroupAllocationWarningMessage();

        if (assertions == 0) {
            throw new AssertionError("No self-test assertions executed");
        }
        System.out.println("IBAutomater Java self-tests passed: " + assertions + " assertions");
    }

    private static void TestGroupAllocationWarningMessage() {
        List<String> messages = new ArrayList<>();
        JPanel panel = new JPanel();
        JTable table = CreateMessagesTable(Boolean.TRUE);
        panel.add(table);

        WindowEventListener.DisableGroupAllocationWarning(panel, messages::add);
        AssertEquals(Boolean.FALSE, table.getValueAt(1, 2));
        AssertEquals(Boolean.TRUE, table.getValueAt(0, 2));
        AssertEquals("Message: [Group Allocation Warning] - Enabled: [false]",
            messages.get(messages.size() - 1));

        messages.clear();
        WindowEventListener.DisableGroupAllocationWarning(panel, messages::add);
        AssertEquals(Boolean.FALSE, table.getValueAt(1, 2));
        AssertEquals(1, messages.size());
        AssertEquals("Message: [Group Allocation Warning] - Enabled: [false]", messages.get(0));

        panel = new JPanel();
        table = CreateMessagesTable(Boolean.TRUE);
        table.moveColumn(2, 0);
        panel.add(table);
        WindowEventListener.DisableGroupAllocationWarning(panel, messages::add);
        AssertEquals(Boolean.FALSE, table.getValueAt(1, 0));

        panel = new JPanel();
        table = new JTable(new Object[][]{{"group allocation warning", Boolean.TRUE}},
            new Object[]{"Message", "Enabled"});
        panel.add(table);
        WindowEventListener.DisableGroupAllocationWarning(panel, messages::add);
        AssertEquals(Boolean.FALSE, table.getValueAt(0, 1));

        messages.clear();
        panel = new JPanel();
        table = new JTable(new Object[][]{{"Other Warning", Boolean.TRUE}},
            new Object[]{"Message", "Enabled"});
        panel.add(table);
        WindowEventListener.DisableGroupAllocationWarning(panel, messages::add);
        AssertEquals(Boolean.TRUE, table.getValueAt(0, 1));
        AssertEquals("Message setting not found: [Group Allocation Warning]", messages.get(0));
    }

    private static JTable CreateMessagesTable(Boolean groupAllocationWarningEnabled) {
        return new JTable(new DefaultTableModel(
            new Object[][]{
                {"Other Warning", "Ask", Boolean.TRUE},
                {"Group Allocation Warning", "Ask", groupAllocationWarningEnabled}
            },
            new Object[]{"Message Name", "Default Action", "Enabled"}));
    }

    private static void TestFinancialAdvisorAllocationGroupsCheckBox() {
        String checkBoxText = "Use Account Groups with Allocation Methods";
        List<String> messages = new ArrayList<>();

        JPanel panel = new JPanel();
        JCheckBox checkBox = new JCheckBox(checkBoxText);
        panel.add(checkBox);
        AssertEquals(null, WindowEventListener.ConfigureFinancialAdvisorAllocationGroupsCheckBox(
            panel, true, messages::add));
        AssertTrue(checkBox.isSelected());
        AssertEquals("Checkbox: [" + checkBoxText + "] - Selected: [true]",
            messages.get(messages.size() - 1));

        panel = new JPanel();
        checkBox = new JCheckBox("use account groups with allocation methods (recommended)", true);
        panel.add(checkBox);
        AssertEquals(null, WindowEventListener.ConfigureFinancialAdvisorAllocationGroupsCheckBox(
            panel, true, messages::add));
        AssertTrue(checkBox.isSelected());

        AssertEquals("check box not found",
            WindowEventListener.ConfigureFinancialAdvisorAllocationGroupsCheckBox(
                new JPanel(), true, messages::add));
        AssertEquals(null, WindowEventListener.ConfigureFinancialAdvisorAllocationGroupsCheckBox(
            new JPanel(), false, messages::add));

        panel = new JPanel();
        JCheckBox first = new JCheckBox(checkBoxText);
        JCheckBox second = new JCheckBox(checkBoxText + ".");
        panel.add(first);
        panel.add(second);
        AssertEquals("multiple matching check boxes found",
            WindowEventListener.ConfigureFinancialAdvisorAllocationGroupsCheckBox(
                panel, true, messages::add));
        AssertTrue(!first.isSelected() && !second.isSelected());
        first.setSelected(true);
        second.setSelected(true);
        AssertEquals(null, WindowEventListener.ConfigureFinancialAdvisorAllocationGroupsCheckBox(
            panel, false, messages::add));
        AssertTrue(!first.isSelected() && !second.isSelected());

        panel = new JPanel();
        checkBox = new JCheckBox(checkBoxText);
        checkBox.setEnabled(false);
        panel.add(checkBox);
        AssertEquals("check box is disabled and unchecked",
            WindowEventListener.ConfigureFinancialAdvisorAllocationGroupsCheckBox(
                panel, true, messages::add));
        checkBox.setSelected(true);
        AssertEquals(null, WindowEventListener.ConfigureFinancialAdvisorAllocationGroupsCheckBox(
            panel, true, messages::add));
        AssertTrue(checkBox.isSelected());

        panel = new JPanel();
        checkBox = new NonSelectableCheckBox(checkBoxText, false);
        panel.add(checkBox);
        AssertEquals("check box did not retain the requested state",
            WindowEventListener.ConfigureFinancialAdvisorAllocationGroupsCheckBox(
                panel, true, messages::add));

        panel = new JPanel();
        checkBox = new NonSelectableCheckBox(checkBoxText, true);
        panel.add(checkBox);
        AssertEquals("check box did not retain the requested state",
            WindowEventListener.ConfigureFinancialAdvisorAllocationGroupsCheckBox(
                panel, false, messages::add));
    }

    private static void AssertTrue(boolean condition) {
        assertions++;
        if (!condition) {
            throw new AssertionError("Expected condition to be true");
        }
    }

    private static void AssertEquals(Object expected, Object actual) {
        assertions++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Expected [" + expected + "] but found [" + actual + "]");
        }
    }

    private static final class NonSelectableCheckBox extends JCheckBox {
        private boolean blockSelection;

        private NonSelectableCheckBox(String text, boolean selected) {
            super(text, selected);
            blockSelection = true;
        }

        @Override
        public void setSelected(boolean selected) {
            if (!blockSelection) {
                super.setSelected(selected);
            }
        }
    }
}

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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 * Contains various helper functions.
 *
 * @author QuantConnect Corporation
 */
public class Common {

    /**
     * Gets whether the window is a Frame window.
     *
     * @param window The window to be checked
     *
     * @return Returns true if the window is a Frame window, false otherwise
     */
    public static boolean isFrame(Window window) {
        return window instanceof Frame;
    }

    /**
     * Gets whether the window is a Dialog window.
     *
     * @param window The window to be checked
     *
     * @return Returns true if the window is a Dialog window, false otherwise
     */
    public static boolean isDialog(Window window) {
        return window instanceof Dialog;
    }

    /**
     * Gets the title of the window.
     *
     * @param window The window to be queried
     *
     * @return Returns the title of the window
     */
    public static String getTitle(Window window) {
        String title = "";
        if (isFrame(window)) {
            title = ((Frame)window).getTitle();
        } else if (isDialog(window)) {
            title = ((Dialog)window).getTitle();
        }
        return title;
    }

    /**
     * Gets a JButton instance with the specified text.
     *
     * @param container The container to be queried
     * @param text The button text to find
     *
     * @return Returns a JButton instance in the given container with the specified text, null if the button is not found
     */
    public static JButton getButton(Container container, String text) {
        ArrayList<Component> buttons = new ArrayList<>();
        Common.loadComponents(container, JButton.class, buttons);
        for (Component component : buttons) {
            JButton button = (JButton)component;
            String buttonText = button.getText();
            if (buttonText == null || !buttonText.equalsIgnoreCase(text)) continue;
            return button;
        }
        return null;
    }

    /**
     * Gets a JToggleButton instance with the specified text.
     *
     * @param container The container to be queried
     * @param text The toggle button text to find
     *
     * @return Returns a JToggleButton instance in the given container with the specified text, null if the toggle button is not found
     */
    public static JToggleButton getToggleButton(Container container, String text) {
        ArrayList<Component> buttons = new ArrayList<>();
        Common.loadComponents(container, JToggleButton.class, buttons);
        for (Component component : buttons) {
            JToggleButton button = (JToggleButton)component;
            String buttonText = button.getText();
            if (buttonText == null || !buttonText.equalsIgnoreCase(text)) continue;
            return button;
        }
        return null;
    }

    /**
     * Gets a JRadioButton instance with the specified text.
     *
     * @param container The container to be queried
     * @param text The radio button text to find
     *
     * @return Returns a JRadioButton instance in the given container with the specified text, null if the radio button is not found
     */
    public static JRadioButton getRadioButton(Container container, String text) {
        ArrayList<Component> buttons = new ArrayList<>();
        Common.loadComponents(container, JRadioButton.class, buttons);
        for (Component component : buttons) {
            JRadioButton button = (JRadioButton)component;
            String buttonText = button.getText();
            if (buttonText == null || !buttonText.equalsIgnoreCase(text)) continue;
            return button;
        }
        return null;
    }

    /**
     * Gets a JLabel instance containing the specified text.
     *
     * @param container The container to be queried
     * @param text The label text to find
     *
     * @return Returns a JLabel instance in the given container containing the specified text, null if the label is not found
     */
    public static JLabel getLabel(Container container, String text) {
        ArrayList<Component> labels = new ArrayList<>();
        Common.loadComponents(container, JLabel.class, labels);
        for (Component component : labels) {
            JLabel label = (JLabel)component;
            String labelText = label.getText();
            if (labelText == null || !labelText.toLowerCase().contains(text.toLowerCase())) continue;
            return label;
        }
        return null;
    }

    /**
     * Gets a JOptionPane instance containing the specified text.
     *
     * @param container The container to be queried
     * @param text The option pane text to find
     *
     * @return Returns a JOptionPane instance in the given container containing the specified text, null if the option pane is not found
     */
    public static JOptionPane getOptionPane(Container container, String text) {
        ArrayList<Component> optionPanes = new ArrayList<>();
        Common.loadComponents(container, JOptionPane.class, optionPanes);
        for (Component component : optionPanes) {
            JOptionPane optionPane = (JOptionPane)component;
            String optionPaneText = optionPane.getMessage().toString();
            if (optionPaneText == null || !optionPaneText.toLowerCase().contains(text.toLowerCase())) continue;
            return optionPane;
        }
        return null;
    }

    /**
     * Gets a JTextField instance at the specified position in the container.
     *
     * @param container The container to be queried
     * @param index The index of the text field to return
     *
     * @return Returns a JTextField instance at the specified position in the list of text fields in the container, null if the index is not valid
     */
    public static JTextField getTextField(Container container, int index) {
        ArrayList<Component> textFields = new ArrayList<>();
        Common.loadComponents(container, JTextField.class, textFields);
        return textFields.size() > index ? (JTextField)textFields.get(index) : null;
    }

    /**
     * Gets a JMenuItem instance in the container with the specified menu item text in the menu with the specified menu text.
     *
     * @param container The container to be queried
     * @param menuText The menu item text to find
     * @param menuItemText The menu text to find
     *
     * @return Returns a JMenuItem instance in the given container with the specified text, null if the menu item is not found
     */
    public static JMenuItem getMenuItem(Container container, String menuText, String menuItemText) {
        if (container == null) return null;
        JMenuBar menuBar = ((JFrame) container).getJMenuBar();
        if (menuBar == null) return null;
        for (int i = 0; i < menuBar.getMenuCount(); ++i) {
            JMenu menu = menuBar.getMenu(i);
            if (!menu.getText().equals(menuText)) continue;
            for (int j = 0; j < menu.getItemCount(); ++j) {
                JMenuItem menuItem = menu.getItem(j);
                if (menuItem == null || !menuItem.getText().equalsIgnoreCase(menuItemText)) continue;
                return menuItem;
            }
        }
        return null;
    }

    /**
     * Gets a JCheckBox instance with the specified text.
     *
     * @param container The container to be queried
     * @param text The check box text to find
     *
     * @return Returns a JCheckBox instance in the given container with the specified text, null if the check box is not found
     */
    public static JCheckBox getCheckBox(Container container, String text) {
        ArrayList<Component> checkBoxes = new ArrayList<>();
        Common.loadComponents(container, JCheckBox.class, checkBoxes);
        for (Component component : checkBoxes) {
            JCheckBox checkBox = (JCheckBox)component;
            String checkBoxText = checkBox.getText();
            if (checkBoxText == null || !checkBoxText.equalsIgnoreCase(text)) continue;
            return checkBox;
        }
        return null;
    }

    /**
     * Gets the first JTextPane instance in the container.
     *
     * @param container The container to be queried
     *
     * @return Returns the first JTextPane instance in the given container, null if the text pane is not found
     */
    public static JTextPane getTextPane(Container container) {
        ArrayList<Component> textPanes = new ArrayList<>();
        Common.loadComponents(container, JTextPane.class, textPanes);
        return textPanes.size() > 0 ? (JTextPane)textPanes.get(0) : null;
    }

    /**
     * Gets a list of label text lines in the container.
     *
     * @param container The container to be queried
     *
     * @return Returns a list of label text lines found in the given container
     */
    public static List<String> getLabelTextLines(Container container) {
        List<String> lines = new ArrayList<>();

        ArrayList<Component> labels = new ArrayList<>();
        Common.loadComponents(container, JLabel.class, labels);
        for (Component component : labels) {
            JLabel label = (JLabel)component;
            String labelText = label.getText();
            if (labelText != null && labelText.length() > 0) {
                lines.add(labelText.replaceAll("\\<.*?>", " ").trim());
            }
        }

        return lines;
    }

    /**
     * Gets the first JTree instance in the container.
     *
     * @param container The container to be queried
     *
     * @return Returns the first JTree instance in the given container, null if the tree is not found
     */
    public static JTree getTree(Container container) {
        ArrayList<Component> trees = new ArrayList<>();
        Common.loadComponents(container, JTree.class, trees);
        return trees.size() > 0 ? (JTree)trees.get(0) : null;
    }

    /**
     * Selects the tree node in the given tree with the specified tree path.
     *
     * @param tree The tree for which the node is to be selected
     * @param path The tree path for the tree node to be selected
     *
     * @return Returns false
     */
    public static boolean selectTreeNode(JTree tree, TreePath path) {
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode)tree.getModel().getRoot();
        Common.selectNode(tree, rootNode, path);
        return false;
    }

    /**
     * Selects the tree node in the given tree with the specified tree path.
     *
     * @param tree The tree for which the node is to be selected
     * @param parentNode The parent node for the tree node to be selected
     * @param path The tree path for the tree node to be selected
     *
     * @return Returns true if the tree node was selected, false otherwise
     */
    private static boolean selectNode(JTree tree, DefaultMutableTreeNode parentNode, TreePath path) {
        for (int i = 0; i < parentNode.getChildCount(); ++i) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)parentNode.getChildAt(i);
            TreePath treePath = new TreePath(node.getPath());
            if (treePath.toString().equalsIgnoreCase(path.toString())) {
                tree.setSelectionPath(treePath);
                return true;
            }
            if (!Common.selectNode(tree, node, path)) continue;
            return true;
        }
        return false;
    }

    /**
     * Recursively loads components of the specified type in the given container into the specified list.
     *
     * @param container The container to be queried
     * @param type The type of the components to be loaded
     * @param components The list to be loaded with the components
     */
    private static void loadComponents(Container container, Class<?> type, List<Component> components) {
        for (Component component : container.getComponents()) {
            if (type.isAssignableFrom(component.getClass())) {
                components.add(component);
                continue;
            }
            if (!(component instanceof Container)) continue;
            Common.loadComponents((Container)component, type, components);
        }
    }

    /**
     * Recursively loads all components in the given container into the specified list.
     *
     * @param container The container to be queried
     * @param components The list to be loaded with the components
     */
    public static void loadAllComponents(Container container, List<Component> components) {
        for (Component component : container.getComponents()) {
            if (component instanceof Container) {
                Common.loadAllComponents((Container)component, components);
            }
            components.add(component);
        }
    }

    /**
     * Recursively gets all components in the given container.
     *
     * @param container The container to be queried
     *
     * @return Returns a list of all components contained in the given container
     */
    public static List<Component> getComponents(Container container) {
        List<Component> components = new ArrayList<>();

        for (Component component : container.getComponents()) {
            if (component instanceof Container) {
                Common.loadAllComponents((Container)component, components);
            }
            components.add(component);
        }

        return components;
    }
}

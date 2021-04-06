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

public class Common {
    public static String getTitle(Window window) {
        String title = "";
        if (window instanceof Frame) {
            title = ((Frame)window).getTitle();
        } else if (window instanceof Dialog) {
            title = ((Dialog)window).getTitle();
        }
        return title;
    }

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

    public static JTextField getTextField(Container container, int index) {
        ArrayList<Component> textFields = new ArrayList<>();
        Common.loadComponents(container, JTextField.class, textFields);
        return textFields.size() > index ? (JTextField)textFields.get(index) : null;
    }

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

    public static JTextPane getTextPane(Container container) {
        ArrayList<Component> textPanes = new ArrayList<>();
        Common.loadComponents(container, JTextPane.class, textPanes);
        return textPanes.size() > 0 ? (JTextPane)textPanes.get(0) : null;
    }

    public static JTree getTree(Container container) {
        ArrayList<Component> trees = new ArrayList<>();
        Common.loadComponents(container, JTree.class, trees);
        return trees.size() > 0 ? (JTree)trees.get(0) : null;
    }

    public static boolean selectTreeNode(JTree tree, TreePath path) {
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode)tree.getModel().getRoot();
        Common.selectNode(tree, rootNode, path);
        return false;
    }

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

    public static void loadAllComponents(Container container, List<Component> components) {
        for (Component component : container.getComponents()) {
            if (component instanceof Container) {
                Common.loadAllComponents((Container)component, components);
            }
            components.add(component);
        }
    }

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


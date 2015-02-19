package org.halfway.grapple.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Logger;

public class ProgressWindow extends JDialog {
    private final static Logger logger = Logger.getLogger(ProgressWindow.class.getSimpleName());

    private JProgressBar mainProgressBar;
    private JPanel panel1;
    private JPanel spinningPanel;
    private JLabel statusLabel;

    public ProgressWindow() {
        setContentPane(panel1);
        setModal(true);
        setMinimumSize(new Dimension(400, 60));

        setLocationRelativeTo(null);

        mainProgressBar.setMinimum(0);
        mainProgressBar.setMaximum(100);
        mainProgressBar.setValue(0);

        spinningPanel.setLayout(new BoxLayout(spinningPanel, BoxLayout.PAGE_AXIS));
        spinningPanel.setPreferredSize(new Dimension(30, 30));

        final CircularSpinner spinner = new CircularSpinner(40, 3);
        spinningPanel.add(spinner);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                spinner.getTimer().stop();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                spinner.getTimer().stop();
            }
        });
    }

    public void setProgressPercentage(final int newPercentage) {
        if (newPercentage >= mainProgressBar.getValue() && newPercentage <= 100) {
            mainProgressBar.setValue(newPercentage);
        } else {
            logger.warning("Attempted to make percentage backwards: current=" + mainProgressBar.getValue() +
                    ", attempted=" + newPercentage);
        }
    }

    public void setStatusMessage(final String message) {
        this.statusLabel.setText(message);
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        mainProgressBar = new JProgressBar();
        panel1.add(mainProgressBar, BorderLayout.SOUTH);
        statusLabel = new JLabel();
        statusLabel.setText("");
        panel1.add(statusLabel, BorderLayout.WEST);
        spinningPanel = new JPanel();
        spinningPanel.setLayout(new BorderLayout(0, 0));
        panel1.add(spinningPanel, BorderLayout.EAST);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }
}

package org.halfway.grapple.gui;

public class RunBacktraceWindow {

    public static void main(String[] args) {
        BacktraceWindow frame = new BacktraceWindow(true);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

}

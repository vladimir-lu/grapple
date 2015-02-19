package org.halfway.grapple.gui;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * Public interface to the GUI display of Grapple launcher progress.
 * <p/>
 * All the methods on this interface are thread-safe.
 */
public interface GrappleGuiApi {

    /**
     * Show the main progress window. This method should only be called once upon application startup.
     */
    public void showProgressWindow();

    /**
     * Display a backtrace window with an exit button and the application log leading up to the event.
     * <p/>
     * The main window will be closed and the application will exit after the backtrace window is closed.
     *
     * @param message    The final exit message to display
     * @param scrollback The list of log messages leading up to the failure
     */
    public void displayBacktrace(String message, ImmutableList<String> scrollback);

    /**
     * Wait until the progress window is closed. This method should only be called upon application shutdown.
     */
    public void waitForProgressWindowClose();

    /**
     * Notify the progress window of progress made during updating. The optional message will be displayed in the
     * progress window
     *
     * @param percentage The percentage completion as of now
     * @param message    The optional status message to display
     */
    public void notifyProgress(int percentage, Optional<String> message);

}

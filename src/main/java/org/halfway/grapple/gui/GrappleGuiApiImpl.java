package org.halfway.grapple.gui;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import org.halfway.grapple.model.configuration.Configuration;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

public class GrappleGuiApiImpl implements GrappleGuiApi {

    private final Configuration configuration;
    private final ProgressWindow progressWindow;
    private final BacktraceWindow backtraceWindow;

    public GrappleGuiApiImpl(Configuration configuration, ProgressWindow progressWindow, BacktraceWindow backtraceWindow) {
        Verify.verifyNotNull(configuration, "configuration must not be null");
        Verify.verifyNotNull(progressWindow, "progress window must not be null");
        Verify.verifyNotNull(backtraceWindow, "backtrace window must not be null");

        this.configuration = configuration;
        this.progressWindow = progressWindow;
        this.backtraceWindow = backtraceWindow;
    }

    @Override
    public void showProgressWindow() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                progressWindow.setTitle("launching " + configuration.getApplicationName() + "...");
                progressWindow.setVisible(true);
            }
        });
    }

    @Override
    public void displayBacktrace(final String explanation, final ImmutableList<String> scrollback) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                progressWindow.dispose();
                backtraceWindow.setScrollbackWithExplanation(explanation, scrollback);
                backtraceWindow.setVisible(true);
            }
        });
    }

    @Override
    public void waitForProgressWindowClose() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    progressWindow.dispose();
                    backtraceWindow.dispose();
                }
            });
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("bug");
        } catch (final InvocationTargetException e) {
            throw new AssertionError("bug", e);
        }
    }

    @Override
    public void notifyProgress(final int percentage, final Optional<String> message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                progressWindow.setProgressPercentage(percentage);
                if (message.isPresent()) {
                    progressWindow.setStatusMessage(message.get());
                }
            }
        });
    }
}

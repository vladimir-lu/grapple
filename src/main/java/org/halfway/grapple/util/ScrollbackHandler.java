package org.halfway.grapple.util;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;

import java.util.LinkedList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Custom {@link java.util.logging.Handler} that keeps a buffer of log messages in memory and is able to produce the list
 * in reverse order of occurrence.
 */
public class ScrollbackHandler extends Handler {
    private final String[] fifo;
    private int index = 0;

    public ScrollbackHandler(final int scrollback) {
        Verify.verify(scrollback > 0, "scrollback must be bigger than zero");
        this.fifo = new String[scrollback];
    }

    @Override
    public void publish(final LogRecord record) {
        if (getFormatter() == null) {
            return;
        } else if (!isLoggable(record)) {
            return;
        }
        addRecord(record);
    }

    private synchronized void addRecord(final LogRecord record) {
        fifo[index] = getFormatter().format(record);
        index += 1;
        index %= fifo.length;
    }

    @Override
    public void flush() {
        // no-op
    }

    @Override
    public void close() throws SecurityException {
        // no-op
    }

    public synchronized ImmutableList<String> getScrollback() {
        LinkedList<String> list = new LinkedList<String>();
        for (int i = index; i < index + 1; i--) {
            if (i < 0) {
                i = fifo.length - 1;
            }
            if (fifo[i] != null) {
                list.add(fifo[i]);
            }

        }
        return ImmutableList.copyOf(list);
    }
}

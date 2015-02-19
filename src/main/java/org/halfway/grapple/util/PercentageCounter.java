package org.halfway.grapple.util;

import com.google.common.base.Function;
import com.google.common.base.Verify;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A simple incrementing counter that reports the percentage to the known total to any callback passed in.
 */
public class PercentageCounter {
    private final long total;
    private final AtomicLong current = new AtomicLong(0);
    private final Function<Double, Object> callback;

    public PercentageCounter(final long total, final Function<Double, Object> callback) {
        Verify.verifyNotNull(callback, "callback must not be null");
        this.total = total;
        this.callback = callback;
    }

    public void addToTotal(final long summand) {
        final long sum = current.addAndGet(summand);
        callback.apply(sum / (double) total);
    }

    public long getCurrent() {
        return current.get();
    }


}

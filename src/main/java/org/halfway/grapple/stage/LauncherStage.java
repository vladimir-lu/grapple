package org.halfway.grapple.stage;

import com.google.common.collect.Range;
import org.halfway.grapple.impl.RuntimeContext;

/**
 * A launcher stage is a discrete sequential step in the process of launching an application.
 */
public interface LauncherStage {
    /**
     * Return the progress range that is covered by this stage
     *
     * @return The range, in percentage points
     */
    Range<Integer> progressRange();

    /**
     * Burn a stage, hopefully doing some work in the process
     *
     * @param context The runtime context that is populated by previous stages
     */
    void burn(RuntimeContext context);
}

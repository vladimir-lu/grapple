package org.halfway.grapple.impl;

import com.google.common.base.Verify;
import org.halfway.grapple.model.GrappleAsset;
import org.halfway.grapple.model.manifest.GrappleManifest;

/**
 * Container for the outcome of verifying an asset file that links the outcome, asset and manifest together for easy
 * access.
 */
public class AssetVerificationResult {

    private final GrappleManifest manifest;
    private final GrappleAsset asset;
    private final Outcome outcome;

    public AssetVerificationResult(final GrappleManifest manifest, final GrappleAsset asset, final Outcome outcome) {
        Verify.verifyNotNull(manifest, "manifest must not be null");
        Verify.verifyNotNull(asset, "asset must not be null");
        Verify.verifyNotNull(outcome, "outcome must not be null");

        this.manifest = manifest;
        this.asset = asset;
        this.outcome = outcome;
    }

    public GrappleManifest getManifest() {
        return manifest;
    }

    public GrappleAsset getAsset() {
        return asset;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    /**
     * Enumeration of possible outcomes asset verification. Obviously, only
     * {@link org.halfway.grapple.impl.AssetVerificationResult.Outcome#OK} is a successful outcome.
     */
    public static enum Outcome {
        HashDiffers,
        MissingFile,
        OK,
        SizeDiffers,
    }
}


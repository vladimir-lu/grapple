package org.halfway.grapple.impl;

import com.google.common.base.Verify;
import org.halfway.grapple.model.GrappleAsset;
import org.halfway.grapple.model.manifest.GrappleManifest;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * A function that can verify a single asset inside a manifest by checking the existence, size and hash of the asset
 * file on the file system and returning a result accordingly.
 */
class AssetVerificationCallable implements Callable<AssetVerificationResult> {

    private final GrappleManifest manifest;
    private final GrappleAsset asset;
    private final File file;
    private final ManifestHasher manifestHasher;

    public AssetVerificationCallable(final GrappleManifest manifest, final GrappleAsset asset, final File file,
                                     final ManifestHasher manifestHasher) {
        Verify.verifyNotNull(manifest, "manifest must not be null");
        Verify.verifyNotNull(asset, "application file must not be null");
        Verify.verifyNotNull(file, "file must not be null");
        Verify.verifyNotNull(manifestHasher, "hasher must not be null");

        this.manifest = manifest;
        this.asset = asset;
        this.file = file;
        this.manifestHasher = manifestHasher;
    }

    @Override
    public AssetVerificationResult call() throws Exception {
        if (!file.isFile()) {
            return new AssetVerificationResult(manifest, asset, AssetVerificationResult.Outcome.MissingFile);
        }

        if (file.length() != asset.getSize()) {
            return new AssetVerificationResult(manifest, asset, AssetVerificationResult.Outcome.SizeDiffers);
        }

        if (!manifestHasher.hashFile(file).equals(asset.getHash())) {
            return new AssetVerificationResult(manifest, asset, AssetVerificationResult.Outcome.HashDiffers);
        }

        return new AssetVerificationResult(manifest, asset, AssetVerificationResult.Outcome.OK);
    }
}

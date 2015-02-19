package org.halfway.grapple.impl;

import com.google.common.base.MoreObjects;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.halfway.grapple.model.GrappleAsset;
import org.halfway.grapple.model.manifest.GrappleManifest;

import java.io.File;

/**
 * Utility class whose responsibility is to allow for easy verification of assets on the file system
 */
public class ManifestAssetVerifier {

    private final GrappleManifest manifest;
    private final File contentRoot;
    private final ManifestHasher manifestHasher;

    public ManifestAssetVerifier(final GrappleManifest manifest, final File contentRoot, final ManifestHasher manifestHasher) {
        Verify.verifyNotNull(manifest, "manifest must not be null");
        Verify.verifyNotNull(contentRoot, "content root must not be null");
        Verify.verifyNotNull(manifestHasher, "manifest hasher must not be null");

        this.manifest = manifest;
        this.contentRoot = contentRoot;
        this.manifestHasher = manifestHasher;
    }

    /**
     * Verify all the files using the {@link org.halfway.grapple.impl.ManifestHasher} set up during construction. Use the
     * {@link java.util.concurrent.ExecutorService} to allow for parallel verification.
     */
    public ImmutableList<ListenableFuture<AssetVerificationResult>> verifyAll(final ListeningExecutorService service) {
        final ImmutableList.Builder<ListenableFuture<AssetVerificationResult>> builder = ImmutableList.builder();
        for (final GrappleAsset asset : manifest.getAssets()) {
            final ListenableFuture<AssetVerificationResult> future = service.submit(
                    new AssetVerificationCallable(manifest, asset, combineContentRootWith(asset), manifestHasher));
            builder.add(future);
        }
        return builder.build();
    }

    private File combineContentRootWith(GrappleAsset asset) {
        return new File(this.contentRoot, asset.getPath());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper("FileVerifier")
                .add("manifest", manifest)
                .add("contentRoot", contentRoot)
                .add("hasher", manifestHasher)
                .toString();
    }
}

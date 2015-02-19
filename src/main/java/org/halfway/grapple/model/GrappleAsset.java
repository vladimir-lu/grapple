package org.halfway.grapple.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Verify;
import com.google.common.hash.HashCode;

/**
 * Object representing an asset of a Grapple application. An asset is anything that may be stored on disk as a single
 * file.
 *
 * @see org.halfway.grapple.model.manifest.GrappleManifest
 */
public class GrappleAsset {

    private final String path;
    private final long size;
    private final HashCode hash;

    public GrappleAsset(String path, long size, HashCode hash) {
        Verify.verifyNotNull(path, "path must not be null");
        Verify.verify(size >= 0, "size must be >= 0");
        Verify.verifyNotNull(hash, "hash must not be null");

        this.path = path;
        this.size = size;
        this.hash = hash;
    }

    /**
     * @return The path of of the asset relative to the application home directory
     */
    public String getPath() {
        return path;
    }

    /**
     * @return The size of the asset in bytes
     */
    public long getSize() {
        return size;
    }

    /**
     * @return The hash code of the asset. The hash algorithm is specified in the
     * {@link org.halfway.grapple.model.manifest.GrappleManifest#getHashAlgorithm()}
     */
    public HashCode getHash() {
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GrappleAsset that = (GrappleAsset) o;

        return Objects.equal(path, that.path) &&
                Objects.equal(size, that.size) &&
                Objects.equal(hash, that.hash);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(path, size, hash);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper("GrappleAsset")
                .add("path", path)
                .add("size", size)
                .add("hash", hash)
                .toString();
    }
}

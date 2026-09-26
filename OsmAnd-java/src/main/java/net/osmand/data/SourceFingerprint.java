package net.osmand.data;

import java.io.File;

/**
 * State of a file as the filesystem reports it. Not a content identity: a replacement with the
 * same timestamp and size is indistinguishable from the original, so a fingerprint can tell a
 * derived cache that its input changed, never that it did not.
 */
public final class SourceFingerprint {

	/** Used when no source file is available. */
	public static final SourceFingerprint EMPTY = new SourceFingerprint(0, 0);

	private final long lastModified;
	private final long length;

	public SourceFingerprint(long lastModified, long length) {
		this.lastModified = lastModified;
		this.length = length;
	}

	public static SourceFingerprint of(File file) {
		return new SourceFingerprint(file.lastModified(), file.length());
	}

	public long getLastModified() {
		return lastModified;
	}

	public long getLength() {
		return length;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof SourceFingerprint)) {
			return false;
		}
		SourceFingerprint other = (SourceFingerprint) obj;
		return lastModified == other.lastModified && length == other.length;
	}

	@Override
	public int hashCode() {
		return 31 * Long.hashCode(lastModified) + Long.hashCode(length);
	}
}

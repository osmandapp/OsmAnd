package net.osmand.apkdelta;

import java.nio.charset.StandardCharsets;

/**
 * A delta turns an APK a phone already has into another one, byte for byte, so the signature of
 * the new APK stays valid.
 *
 * <pre>
 * magic, new APK size (long), SHA-256 of the new APK, metadata length (long)
 * metadata, bzip2:
 *   length (int) and bytes of everything in the new APK that is not entry data:
 *     local headers, signing block, central directory
 *   number of operations (int), then for each entry of the new APK in file order:
 *     length of the bytes before the entry data (int), kind (byte), and by kind:
 *       COPY:  name in the old APK
 *       FULL:  payload offset (long), payload length (long)
 *       DELTA: name in the old APK, method of the new entry (byte), its uncompressed size (long),
 *              compressed size (long), CRC of the compressed bytes (long),
 *              payload offset (long), payload length (long)
 *   length of the bytes after the last entry (int)
 * payload: BSDIFF40 patches of uncompressed entries, compressed bytes of FULL entries
 * </pre>
 */
public final class ApkDeltaFormat {

	public static final byte[] MAGIC = "OADELTA1".getBytes(StandardCharsets.US_ASCII);
	public static final int SHA256_LENGTH = 32;

	public static final byte KIND_COPY = 0;
	public static final byte KIND_FULL = 1;
	public static final byte KIND_DELTA = 2;

	/** The level the build machine compresses with; re-compressing must give the same bytes. */
	public static final int DEFLATE_LEVEL = 6;

	private ApkDeltaFormat() {
	}
}

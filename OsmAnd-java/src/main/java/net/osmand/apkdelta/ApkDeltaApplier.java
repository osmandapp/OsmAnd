package net.osmand.apkdelta;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.utils.BoundedInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * Builds the new APK from the old one and a delta. Entries are streamed; only the patch of one
 * entry is kept in memory, and an old entry that is compressed goes through a temporary file.
 *
 * Any failure is an IOException, and the caller takes the whole APK instead. The result depends on
 * the zlib of the device, because changed entries are compressed again.
 */
public final class ApkDeltaApplier {

	private static final int BUFFER_SIZE = 64 * 1024;
	private static final byte[] BSDIFF_MAGIC = "BSDIFF40".getBytes(StandardCharsets.US_ASCII);
	private static final int BSDIFF_HEADER_SIZE = 32;
	private static final int CONTROL_SIZE = 24;

	public interface Cancellation {
		boolean isCancelled();
	}

	/** The delta was applied and the result is not the expected APK: retrying gives the same. */
	public static final class MismatchException extends IOException {
		MismatchException() {
			super("The built APK differs from the expected one");
		}
	}

	private final File tempDir;
	private final Cancellation cancellation;
	private final byte[] buffer = new byte[BUFFER_SIZE];
	private final byte[] oldBuffer = new byte[BUFFER_SIZE];

	private ApkDeltaApplier(File tempDir, Cancellation cancellation) {
		this.tempDir = tempDir;
		this.cancellation = cancellation;
	}

	public static void apply(File oldApk, File delta, File newApk, File tempDir, Cancellation cancellation)
			throws IOException {
		boolean done = false;
		try {
			new ApkDeltaApplier(tempDir, cancellation).apply(oldApk, delta, newApk);
			done = true;
		} catch (RuntimeException e) {
			// that is how lengths from a broken delta end
			throw new IOException("Broken delta", e);
		} finally {
			if (!done) {
				newApk.delete();
			}
		}
	}

	private void apply(File oldApk, File deltaFile, File newApk) throws IOException, RuntimeException {
		MessageDigest digest = sha256();
		byte[] expectedHash = new byte[ApkDeltaFormat.SHA256_LENGTH];
		// the file channel, not the file: ZipFile(File) needs java.nio.file of API 26
		try (DataInputStream delta = new DataInputStream(new BufferedInputStream(new FileInputStream(deltaFile), BUFFER_SIZE));
		     RandomAccessFile oldFile = new RandomAccessFile(oldApk, "r");
		     ZipFile oldZip = new ZipFile(oldFile.getChannel());
		     OutputStream output = new DigestOutputStream(
				     new BufferedOutputStream(new FileOutputStream(newApk), BUFFER_SIZE), digest)) {
			byte[] magic = new byte[ApkDeltaFormat.MAGIC.length];
			delta.readFully(magic);
			if (!Arrays.equals(magic, ApkDeltaFormat.MAGIC)) {
				throw new IOException("Not an APK delta");
			}
			delta.readFully(expectedHash);
			byte[] metadata = new byte[Math.toIntExact(delta.readLong())];
			delta.readFully(metadata);
			try (DataInputStream meta = new DataInputStream(new BufferedInputStream(
					new BZip2CompressorInputStream(new ByteArrayInputStream(metadata))))) {
				byte[] container = new byte[meta.readInt()];
				meta.readFully(container);
				int position = 0;
				for (int left = meta.readInt(); left > 0; left--) {
					int headerLength = meta.readInt();
					output.write(container, position, headerLength);
					position += headerLength;
					checkCancelled();
					writeEntry(meta, delta, oldZip, oldFile, output);
				}
				output.write(container, position, meta.readInt());
			}
		}
		if (!MessageDigest.isEqual(expectedHash, digest.digest())) {
			throw new MismatchException();
		}
	}

	private void writeEntry(DataInputStream meta, DataInputStream delta, ZipFile oldZip, RandomAccessFile oldFile,
	                        OutputStream output) throws IOException {
		byte kind = meta.readByte();
		switch (kind) {
			case ApkDeltaFormat.KIND_COPY:
				try (InputStream raw = oldZip.getRawInputStream(oldEntry(oldZip, meta.readUTF()))) {
					IOUtils.copy(raw, output, BUFFER_SIZE);
				}
				break;
			case ApkDeltaFormat.KIND_FULL:
				IOUtils.copy(new BoundedInputStream(delta, meta.readLong()), output, BUFFER_SIZE);
				break;
			case ApkDeltaFormat.KIND_DELTA:
				ZipArchiveEntry oldEntry = oldEntry(oldZip, meta.readUTF());
				byte method = meta.readByte();
				byte[] patch = new byte[Math.toIntExact(meta.readLong())];
				delta.readFully(patch);
				if (method == ZipArchiveEntry.DEFLATED) {
					Deflater deflater = new Deflater(ApkDeltaFormat.DEFLATE_LEVEL, true);
					try {
						DeflaterOutputStream deflated = new DeflaterOutputStream(output, deflater, BUFFER_SIZE);
						patch(oldZip, oldFile, oldEntry, patch, deflated);
						deflated.finish();
					} finally {
						deflater.end();
					}
				} else {
					patch(oldZip, oldFile, oldEntry, patch, output);
				}
				break;
			default:
				throw new IOException("Unknown entry kind " + kind);
		}
	}

	private static ZipArchiveEntry oldEntry(ZipFile zip, String name) throws IOException {
		ZipArchiveEntry entry = zip.getEntry(name);
		if (entry == null) {
			throw new IOException("The old APK has no " + name);
		}
		return entry;
	}

	private void patch(ZipFile oldZip, RandomAccessFile oldFile, ZipArchiveEntry oldEntry, byte[] patch,
	                   OutputStream output) throws IOException {
		if (oldEntry.getMethod() == ZipArchiveEntry.STORED) {
			bspatch(oldFile, oldEntry.getDataOffset(), oldEntry.getSize(), patch, output);
			return;
		}
		File inflated = File.createTempFile("apk-delta", ".tmp", tempDir);
		try {
			try (InputStream input = oldZip.getInputStream(oldEntry);
			     OutputStream file = new FileOutputStream(inflated)) {
				IOUtils.copy(input, file, BUFFER_SIZE);
			}
			try (RandomAccessFile file = new RandomAccessFile(inflated, "r")) {
				bspatch(file, 0, file.length(), patch, output);
			}
		} finally {
			inflated.delete();
		}
	}

	private void bspatch(RandomAccessFile old, long oldOffset, long oldSize, byte[] patch, OutputStream output)
			throws IOException {
		if (!Arrays.equals(Arrays.copyOf(patch, BSDIFF_MAGIC.length), BSDIFF_MAGIC)) {
			throw new IOException("Broken patch");
		}
		int controlStart = BSDIFF_HEADER_SIZE;
		int diffStart = controlStart + Math.toIntExact(number(patch, 8));
		int extraStart = diffStart + Math.toIntExact(number(patch, 16));
		long newSize = number(patch, 24);
		try (InputStream control = bzip2(patch, controlStart, diffStart);
		     InputStream diff = bzip2(patch, diffStart, extraStart);
		     InputStream extra = bzip2(patch, extraStart, patch.length)) {
			byte[] numbers = new byte[CONTROL_SIZE];
			long position = 0;
			for (long written = 0; written < newSize; ) {
				checkCancelled();
				if (IOUtils.readFully(control, numbers) != CONTROL_SIZE) {
					throw new EOFException();
				}
				long diffLength = number(numbers, 0);
				long extraLength = number(numbers, 8);
				addOld(old, oldOffset, oldSize, position, diffLength, diff, output);
				IOUtils.copy(new BoundedInputStream(extra, extraLength), output, BUFFER_SIZE);
				position += diffLength + number(numbers, 16);
				written += diffLength + extraLength;
			}
		}
	}

	/** Adds diff bytes to old bytes; positions outside of the old entry count as zeros. */
	private void addOld(RandomAccessFile old, long oldOffset, long oldSize, long oldPosition, long length,
	                    InputStream diff, OutputStream output) throws IOException {
		long position = oldPosition;
		for (long left = length; left > 0; ) {
			int chunk = (int) Math.min(left, buffer.length);
			if (IOUtils.readFully(diff, buffer, 0, chunk) != chunk) {
				throw new EOFException();
			}
			long from = Math.max(position, 0);
			int oldLength = (int) (Math.min(position + chunk, oldSize) - from);
			if (oldLength > 0) {
				old.seek(oldOffset + from);
				old.readFully(oldBuffer, 0, oldLength);
				int shift = (int) (from - position);
				for (int i = 0; i < oldLength; i++) {
					buffer[shift + i] += oldBuffer[i];
				}
			}
			output.write(buffer, 0, chunk);
			position += chunk;
			left -= chunk;
		}
	}

	private void checkCancelled() throws IOException {
		if (cancellation.isCancelled()) {
			throw new IOException("Cancelled");
		}
	}

	private static InputStream bzip2(byte[] bytes, int from, int to) throws IOException {
		return new BZip2CompressorInputStream(new ByteArrayInputStream(bytes, from, to - from));
	}

	/** bsdiff numbers: little endian, the top bit of the last byte is the sign. */
	private static long number(byte[] bytes, int position) {
		long value = 0;
		for (int i = 7; i >= 0; i--) {
			value = (value << 8) | (bytes[position + i] & 0xff);
		}
		return (value & Long.MIN_VALUE) != 0 ? -(value & Long.MAX_VALUE) : value;
	}

	private static MessageDigest sha256() {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}

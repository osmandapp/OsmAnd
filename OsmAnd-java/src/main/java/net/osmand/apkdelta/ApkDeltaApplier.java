package net.osmand.apkdelta;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Builds the new APK from the old one and a delta. Entries are streamed, so memory use does not
 * depend on their size; an old entry that is compressed goes through a temporary file.
 *
 * Fails with an IOException on any difference, the caller then takes the whole APK. The result
 * is compared with the SHA-256 in the delta: re-compression depends on the zlib of the device.
 */
public final class ApkDeltaApplier {

	private static final int BUFFER_SIZE = 64 * 1024;
	private static final byte[] BSDIFF_MAGIC = "BSDIFF40".getBytes(StandardCharsets.US_ASCII);
	private static final int BSDIFF_HEADER_SIZE = 32;

	public interface Progress {
		void onProgress(long written, long total);

		boolean isCancelled();
	}

	private ApkDeltaApplier() {
	}

	public static void apply(File oldApk, File delta, File newApk, File tempDir, Progress progress) throws IOException {
		boolean done = false;
		try (RandomAccessFile oldFile = new RandomAccessFile(oldApk, "r");
		     RandomAccessFile deltaFile = new RandomAccessFile(delta, "r")) {
			new Applier(oldFile, deltaFile, newApk, tempDir, progress).apply();
			done = true;
		} finally {
			if (!done) {
				newApk.delete();
			}
		}
	}

	private static final class Applier {
		private final RandomAccessFile oldFile;
		private final RandomAccessFile deltaFile;
		private final File newApk;
		private final File tempDir;
		private final Progress progress;
		private final byte[] buffer = new byte[BUFFER_SIZE];
		private long written;
		private long total;

		Applier(RandomAccessFile oldFile, RandomAccessFile deltaFile, File newApk, File tempDir, Progress progress) {
			this.oldFile = oldFile;
			this.deltaFile = deltaFile;
			this.newApk = newApk;
			this.tempDir = tempDir;
			this.progress = progress;
		}

		void apply() throws IOException {
			byte[] magic = new byte[ApkDeltaFormat.MAGIC.length];
			deltaFile.readFully(magic);
			if (!Arrays.equals(magic, ApkDeltaFormat.MAGIC)) {
				throw new IOException("Not an APK delta");
			}
			total = deltaFile.readLong();
			byte[] expectedHash = new byte[ApkDeltaFormat.SHA256_LENGTH];
			deltaFile.readFully(expectedHash);
			byte[] metadata = new byte[Math.toIntExact(deltaFile.readLong())];
			deltaFile.readFully(metadata);
			long payloadStart = deltaFile.getFilePointer();

			Map<String, ApkArchive.Entry> oldEntries = new HashMap<>();
			for (ApkArchive.Entry entry : ApkArchive.readEntries(oldFile)) {
				oldEntries.put(entry.name, entry);
			}
			MessageDigest digest = sha256();
			try (OutputStream output = new DigestOutputStream(
					new BufferedOutputStream(new FileOutputStream(newApk), BUFFER_SIZE), digest);
			     DataInputStream meta = new DataInputStream(new BufferedInputStream(
					     new BZip2CompressorInputStream(new ByteArrayInputStream(metadata))))) {
				byte[] container = new byte[meta.readInt()];
				meta.readFully(container);
				int position = 0;
				int operations = meta.readInt();
				for (int i = 0; i < operations; i++) {
					int headerLength = meta.readInt();
					write(output, container, position, headerLength);
					position += headerLength;
					byte kind = meta.readByte();
					switch (kind) {
						case ApkDeltaFormat.KIND_COPY:
							copyEntry(oldEntry(oldEntries, meta.readUTF()), output);
							break;
						case ApkDeltaFormat.KIND_FULL:
							long offset = meta.readLong();
							long length = meta.readLong();
							copyRange(deltaFile, payloadStart + offset, length, output);
							break;
						case ApkDeltaFormat.KIND_DELTA:
							patchEntry(oldEntry(oldEntries, meta.readUTF()), meta, payloadStart, output);
							break;
						default:
							throw new IOException("Unknown operation " + kind);
					}
				}
				int tailLength = meta.readInt();
				write(output, container, position, tailLength);
				if (position + tailLength != container.length) {
					throw new IOException("Delta does not cover the APK");
				}
			}
			if (newApk.length() != total || !MessageDigest.isEqual(expectedHash, digest.digest())) {
				throw new IOException("The built APK differs from the expected one");
			}
		}

		private static ApkArchive.Entry oldEntry(Map<String, ApkArchive.Entry> entries, String name) throws IOException {
			ApkArchive.Entry entry = entries.get(name);
			if (entry == null) {
				throw new IOException("The old APK has no " + name);
			}
			return entry;
		}

		private void copyEntry(ApkArchive.Entry entry, OutputStream output) throws IOException {
			copyRange(oldFile, ApkArchive.dataOffset(oldFile, entry), entry.compressedSize, output);
		}

		private void copyRange(RandomAccessFile file, long offset, long length, OutputStream output) throws IOException {
			file.seek(offset);
			long left = length;
			while (left > 0) {
				int read = file.read(buffer, 0, (int) Math.min(left, buffer.length));
				if (read < 0) {
					throw new EOFException();
				}
				write(output, buffer, 0, read);
				left -= read;
			}
		}

		private void patchEntry(ApkArchive.Entry oldEntry, DataInputStream meta, long payloadStart,
		                        OutputStream output) throws IOException {
			byte method = meta.readByte();
			long size = meta.readLong();
			long compressedSize = meta.readLong();
			long compressedCrc = meta.readLong();
			long patchOffset = payloadStart + meta.readLong();
			long patchLength = meta.readLong();

			File inflated = null;
			try {
				FileChannel oldBytes;
				long oldOffset;
				RandomAccessFile inflatedFile = null;
				if (oldEntry.method == ApkArchive.DEFLATED) {
					inflated = File.createTempFile("apk-delta", ".tmp", tempDir);
					inflate(oldEntry, inflated);
					inflatedFile = new RandomAccessFile(inflated, "r");
					oldBytes = inflatedFile.getChannel();
					oldOffset = 0;
				} else {
					oldBytes = oldFile.getChannel();
					oldOffset = ApkArchive.dataOffset(oldFile, oldEntry);
				}
				try {
					OutputStream counted = new ProgressStream(output);
					CRC32 crc = new CRC32();
					CheckedOutputStream checked = new CheckedOutputStream(counted, crc);
					long before = written;
					if (method == ApkArchive.DEFLATED) {
						Deflater deflater = new Deflater(ApkDeltaFormat.DEFLATE_LEVEL, true);
						try (OutputStream deflated = new UnclosedDeflaterStream(checked, deflater)) {
							bspatch(oldBytes, oldOffset, oldEntry.size, patchOffset, patchLength, size, deflated);
						} finally {
							deflater.end();
						}
					} else {
						bspatch(oldBytes, oldOffset, oldEntry.size, patchOffset, patchLength, size, checked);
					}
					checked.flush();
					if (written - before != compressedSize || crc.getValue() != compressedCrc) {
						throw new IOException("Re-compressed entry differs: " + oldEntry.name);
					}
				} finally {
					if (inflatedFile != null) {
						inflatedFile.close();
					}
				}
			} finally {
				if (inflated != null) {
					inflated.delete();
				}
			}
		}

		private void inflate(ApkArchive.Entry entry, File target) throws IOException {
			long offset = ApkArchive.dataOffset(oldFile, entry);
			Inflater inflater = new Inflater(true);
			try (InputStream input = new InflaterInputStream(
					new RangeInputStream(oldFile.getChannel(), offset, entry.compressedSize), inflater, BUFFER_SIZE);
			     OutputStream output = new FileOutputStream(target)) {
				int read;
				while ((read = input.read(buffer)) != -1) {
					output.write(buffer, 0, read);
				}
			} finally {
				inflater.end();
			}
		}

		/** The patch has three bzip2 streams after its header; they are read at the same time. */
		private void bspatch(FileChannel oldBytes, long oldOffset, long oldSize, long patchOffset, long patchLength,
		                     long newSize, OutputStream output) throws IOException {
			ByteBuffer header = ByteBuffer.allocate(BSDIFF_HEADER_SIZE);
			deltaFile.seek(patchOffset);
			deltaFile.readFully(header.array());
			byte[] magic = Arrays.copyOf(header.array(), BSDIFF_MAGIC.length);
			if (!Arrays.equals(magic, BSDIFF_MAGIC)) {
				throw new IOException("Broken patch");
			}
			long controlLength = offset(header.array(), 8);
			long diffLength = offset(header.array(), 16);
			long patchedSize = offset(header.array(), 24);
			long dataStart = patchOffset + BSDIFF_HEADER_SIZE;
			long extraLength = patchLength - BSDIFF_HEADER_SIZE - controlLength - diffLength;
			if (patchedSize != newSize || controlLength < 0 || diffLength < 0 || extraLength < 0) {
				throw new IOException("Broken patch");
			}
			FileChannel channel = deltaFile.getChannel();
			try (InputStream control = bzip2(channel, dataStart, controlLength);
			     InputStream diff = bzip2(channel, dataStart + controlLength, diffLength);
			     InputStream extra = bzip2(channel, dataStart + controlLength + diffLength, extraLength)) {
				byte[] triple = new byte[24];
				long newPosition = 0;
				long oldPosition = 0;
				while (newPosition < newSize) {
					checkCancelled();
					readFully(control, triple, triple.length);
					long diffBytes = offset(triple, 0);
					long extraBytes = offset(triple, 8);
					long seek = offset(triple, 16);
					if (diffBytes < 0 || extraBytes < 0 || newPosition + diffBytes + extraBytes > newSize) {
						throw new IOException("Broken patch");
					}
					addOld(oldBytes, oldOffset, oldSize, oldPosition, diffBytes, diff, output);
					oldPosition += diffBytes;
					newPosition += diffBytes;
					long left = extraBytes;
					while (left > 0) {
						int chunk = (int) Math.min(left, buffer.length);
						readFully(extra, buffer, chunk);
						output.write(buffer, 0, chunk);
						left -= chunk;
					}
					newPosition += extraBytes;
					oldPosition += seek;
				}
			}
		}

		/** Adds diff bytes to old bytes; positions past the end of the old file count as zeros. */
		private void addOld(FileChannel oldBytes, long oldOffset, long oldSize, long oldPosition, long length,
		                    InputStream diff, OutputStream output) throws IOException {
			long left = length;
			long position = oldPosition;
			byte[] old = new byte[BUFFER_SIZE];
			while (left > 0) {
				int chunk = (int) Math.min(left, buffer.length);
				readFully(diff, buffer, chunk);
				Arrays.fill(old, 0, chunk, (byte) 0);
				long from = Math.max(position, 0);
				long to = Math.min(position + chunk, oldSize);
				if (from < to) {
					ByteBuffer target = ByteBuffer.wrap(old, (int) (from - position), (int) (to - from));
					long at = oldOffset + from;
					while (target.hasRemaining()) {
						int read = oldBytes.read(target, at);
						if (read < 0) {
							throw new EOFException();
						}
						at += read;
					}
				}
				for (int i = 0; i < chunk; i++) {
					buffer[i] += old[i];
				}
				output.write(buffer, 0, chunk);
				position += chunk;
				left -= chunk;
			}
		}

		private void write(OutputStream output, byte[] bytes, int offset, int length) throws IOException {
			output.write(bytes, offset, length);
			advance(length);
		}

		private void advance(int length) throws IOException {
			written += length;
			checkCancelled();
			if (progress != null) {
				progress.onProgress(written, total);
			}
		}

		private void checkCancelled() throws IOException {
			if (progress != null && progress.isCancelled()) {
				throw new IOException("Cancelled");
			}
		}

		private final class ProgressStream extends FilterOutputStream {
			ProgressStream(OutputStream output) {
				super(output);
			}

			@Override
			public void write(int b) throws IOException {
				out.write(b);
				advance(1);
			}

			@Override
			public void write(byte[] bytes, int offset, int length) throws IOException {
				out.write(bytes, offset, length);
				advance(length);
			}

			@Override
			public void close() throws IOException {
				flush();
			}
		}
	}

	/** Finishes the compressed stream without closing the APK behind it. */
	private static final class UnclosedDeflaterStream extends DeflaterOutputStream {
		UnclosedDeflaterStream(OutputStream output, Deflater deflater) {
			super(output, deflater, BUFFER_SIZE);
		}

		@Override
		public void close() throws IOException {
			finish();
			out.flush();
		}
	}

	private static InputStream bzip2(FileChannel channel, long offset, long length) throws IOException {
		return new BZip2CompressorInputStream(new BufferedInputStream(new RangeInputStream(channel, offset, length), BUFFER_SIZE));
	}

	private static void readFully(InputStream input, byte[] bytes, int length) throws IOException {
		int position = 0;
		while (position < length) {
			int read = input.read(bytes, position, length - position);
			if (read < 0) {
				throw new EOFException();
			}
			position += read;
		}
	}

	/** bsdiff numbers: little endian, the top bit of the last byte is the sign. */
	private static long offset(byte[] bytes, int position) {
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

	/** Reads a part of a file by position, so several of them can share one channel. */
	private static final class RangeInputStream extends InputStream {
		private final FileChannel channel;
		private long position;
		private long left;

		RangeInputStream(FileChannel channel, long offset, long length) {
			this.channel = channel;
			this.position = offset;
			this.left = length;
		}

		@Override
		public int read() throws IOException {
			byte[] one = new byte[1];
			return read(one, 0, 1) < 0 ? -1 : one[0] & 0xff;
		}

		@Override
		public int read(byte[] bytes, int offset, int length) throws IOException {
			if (left <= 0) {
				return -1;
			}
			int read = channel.read(ByteBuffer.wrap(bytes, offset, (int) Math.min(length, left)), position);
			if (read > 0) {
				position += read;
				left -= read;
			}
			return read;
		}
	}
}

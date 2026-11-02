package net.osmand.apkdelta;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * The central directory of an APK, read without touching the entries, and the manifest made of it
 * that a phone sends to the server: what is inside the APK it already has.
 *
 * A manifest line needs the CRC. ZIP dates are fixed in an APK, and most changed entries keep
 * their size, so neither of them tells whether an entry has changed.
 */
public final class ApkArchive {

	public static final int STORED = 0;
	public static final int DEFLATED = 8;

	private static final int END_SIGNATURE = 0x06054b50;
	private static final int CENTRAL_SIGNATURE = 0x02014b50;
	private static final int LOCAL_SIGNATURE = 0x04034b50;
	private static final int END_SIZE = 22;
	private static final int CENTRAL_SIZE = 46;
	private static final int LOCAL_SIZE = 30;
	private static final int MAX_COMMENT = 0xffff;
	private static final long ZIP64_MARKER = 0xffffffffL;

	private static final int MAX_MANIFEST_ENTRIES = 200_000;
	private static final int MAX_MANIFEST_LINE = 2048;

	public static final class Entry {
		public final String name;
		public final long crc;
		public final long size;
		public final long compressedSize;
		public final int method;
		public final long headerOffset;

		public Entry(String name, long crc, long size, long compressedSize, int method, long headerOffset) {
			this.name = name;
			this.crc = crc;
			this.size = size;
			this.compressedSize = compressedSize;
			this.method = method;
			this.headerOffset = headerOffset;
		}

		/** The same content, however it is compressed. */
		public String contentKey() {
			return contentKey(name, crc, size);
		}

		/** Equal entries are the same bytes in the APK, so one can be copied for the other. */
		public boolean isSameAs(Entry other) {
			return crc == other.crc && size == other.size && compressedSize == other.compressedSize
					&& method == other.method && name.equals(other.name);
		}

		public static String contentKey(String name, long crc, long size) {
			return crc + ":" + size + ":" + name;
		}
	}

	private ApkArchive() {
	}

	/** In the order of the central directory. */
	public static List<Entry> readEntries(RandomAccessFile file) throws IOException {
		ByteBuffer end = findEnd(file);
		int count = end.getShort(10) & 0xffff;
		long size = end.getInt(12) & ZIP64_MARKER;
		long offset = end.getInt(16) & ZIP64_MARKER;
		if (count == 0xffff || offset == ZIP64_MARKER) {
			throw new IOException("ZIP64 is not supported");
		}
		byte[] central = new byte[Math.toIntExact(size)];
		file.seek(offset);
		file.readFully(central);
		ByteBuffer buffer = ByteBuffer.wrap(central).order(ByteOrder.LITTLE_ENDIAN);
		List<Entry> entries = new ArrayList<>(count);
		int position = 0;
		for (int i = 0; i < count; i++) {
			if (position + CENTRAL_SIZE > central.length || buffer.getInt(position) != CENTRAL_SIGNATURE) {
				throw new IOException("Broken central directory");
			}
			int method = buffer.getShort(position + 10) & 0xffff;
			long crc = buffer.getInt(position + 16) & ZIP64_MARKER;
			long compressedSize = buffer.getInt(position + 20) & ZIP64_MARKER;
			long entrySize = buffer.getInt(position + 24) & ZIP64_MARKER;
			int nameLength = buffer.getShort(position + 28) & 0xffff;
			int extraLength = buffer.getShort(position + 30) & 0xffff;
			int commentLength = buffer.getShort(position + 32) & 0xffff;
			long headerOffset = buffer.getInt(position + 42) & ZIP64_MARKER;
			if (position + CENTRAL_SIZE + nameLength > central.length) {
				throw new IOException("Broken central directory");
			}
			String name = new String(central, position + CENTRAL_SIZE, nameLength, StandardCharsets.UTF_8);
			entries.add(new Entry(name, crc, entrySize, compressedSize, method, headerOffset));
			position += CENTRAL_SIZE + nameLength + extraLength + commentLength;
		}
		return entries;
	}

	/** Where the compressed bytes of the entry start: the local header has its own extra field. */
	public static long dataOffset(RandomAccessFile file, Entry entry) throws IOException {
		byte[] header = new byte[LOCAL_SIZE];
		file.seek(entry.headerOffset);
		file.readFully(header);
		ByteBuffer buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
		if (buffer.getInt(0) != LOCAL_SIGNATURE) {
			throw new IOException("Broken local header of " + entry.name);
		}
		int nameLength = buffer.getShort(26) & 0xffff;
		int extraLength = buffer.getShort(28) & 0xffff;
		return entry.headerOffset + LOCAL_SIZE + nameLength + extraLength;
	}

	public static byte[] writeManifest(List<Entry> entries) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (Writer writer = new OutputStreamWriter(new GZIPOutputStream(bytes), StandardCharsets.UTF_8)) {
			for (Entry entry : entries) {
				writer.write(entry.crc + " " + entry.size + " " + entry.compressedSize + " "
						+ entry.method + " " + entry.name + "\n");
			}
		}
		return bytes.toByteArray();
	}

	/** Reads what a client has sent: limits and format are checked, an offset is not sent at all. */
	public static List<Entry> readManifest(InputStream gzipped) throws IOException {
		List<Entry> entries = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new GZIPInputStream(gzipped), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (entries.size() >= MAX_MANIFEST_ENTRIES || line.length() > MAX_MANIFEST_LINE) {
					throw new IOException("Manifest is too large");
				}
				String[] fields = line.split(" ", 5);
				if (fields.length != 5) {
					throw new IOException("Broken manifest line");
				}
				try {
					entries.add(new Entry(fields[4], Long.parseLong(fields[0]), Long.parseLong(fields[1]),
							Long.parseLong(fields[2]), Integer.parseInt(fields[3]), -1));
				} catch (NumberFormatException e) {
					throw new IOException("Broken manifest line");
				}
			}
		}
		return entries;
	}

	private static ByteBuffer findEnd(RandomAccessFile file) throws IOException {
		long length = file.length();
		int tail = (int) Math.min(length, END_SIZE + MAX_COMMENT);
		if (tail < END_SIZE) {
			throw new EOFException("Not a ZIP file");
		}
		byte[] bytes = new byte[tail];
		file.seek(length - tail);
		file.readFully(bytes);
		ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
		for (int position = tail - END_SIZE; position >= 0; position--) {
			if (buffer.getInt(position) == END_SIGNATURE) {
				return ByteBuffer.wrap(bytes, position, END_SIZE).slice().order(ByteOrder.LITTLE_ENDIAN);
			}
		}
		throw new IOException("Not a ZIP file");
	}
}

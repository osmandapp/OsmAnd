package net.osmand.apkdelta;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * What is inside an APK, and the manifest made of it that a phone sends to the server.
 *
 * A manifest line needs the CRC. ZIP dates are fixed in an APK, and most changed entries keep
 * their size, so neither of them tells whether an entry has changed.
 */
public final class ApkArchive {

	public static final class Entry {
		public final String name;
		public final long crc;
		public final long size;
		public final long compressedSize;
		public final int method;

		public Entry(String name, long crc, long size, long compressedSize, int method) {
			this.name = name;
			this.crc = crc;
			this.size = size;
			this.compressedSize = compressedSize;
			this.method = method;
		}

		/** The same content, however it is compressed. */
		public String contentKey() {
			return crc + ":" + size + ":" + name;
		}

		/** Equal entries are the same bytes in the APK, so one can be copied for the other. */
		public boolean isSameAs(Entry other) {
			return contentKey().equals(other.contentKey()) && compressedSize == other.compressedSize
					&& method == other.method;
		}
	}

	private ApkArchive() {
	}

	/** Reads only the central directory, whatever the size of the APK. */
	public static List<Entry> readEntries(File apk) throws IOException {
		List<Entry> entries = new ArrayList<>();
		try (ZipFile zip = new ZipFile(apk)) {
			for (ZipEntry entry : Collections.list(zip.entries())) {
				entries.add(new Entry(entry.getName(), entry.getCrc(), entry.getSize(),
						entry.getCompressedSize(), entry.getMethod()));
			}
		}
		return entries;
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
}

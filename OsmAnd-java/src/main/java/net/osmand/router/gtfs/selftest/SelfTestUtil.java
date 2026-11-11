package net.osmand.router.gtfs.selftest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.osmand.data.LatLon;

final class SelfTestUtil {

	private SelfTestUtil() {
	}

	static String sha256(File f) throws IOException {
		try (InputStream in = new FileInputStream(f)) {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] buf = new byte[1 << 16];
			int n;
			while ((n = in.read(buf)) > 0) {
				md.update(buf, 0, n);
			}
			StringBuilder sb = new StringBuilder();
			for (byte b : md.digest()) {
				sb.append(String.format(Locale.ROOT, "%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e);
		}
	}

	static void deleteTree(File f) {
		File[] kids = f.listFiles();
		if (kids != null) {
			for (File k : kids) {
				deleteTree(k);
			}
		}
		f.delete();
	}

	/** Names of files below dir (one level of subdirectories) that look like leftovers of an interrupted job. */
	static List<String> leftovers(File dir) {
		List<String> res = new ArrayList<>();
		collect(dir, res, 0);
		Collections.sort(res);
		return res;
	}

	private static void collect(File dir, List<String> res, int depth) {
		File[] kids = dir.listFiles();
		if (kids == null) {
			return;
		}
		for (File k : kids) {
			String n = k.getName();
			if (k.isDirectory() && depth < 2) {
				collect(k, res, depth + 1);
			} else if (n.contains(".tmp") || n.endsWith(".part") || n.endsWith(".removed")) {
				res.add(n);
			}
		}
	}

	static void copy(File from, File to) throws IOException {
		try (InputStream in = new FileInputStream(from); OutputStream out = new FileOutputStream(to)) {
			byte[] buf = new byte[1 << 16];
			int n;
			while ((n = in.read(buf)) > 0) {
				out.write(buf, 0, n);
			}
		}
	}

	/** A zip with the given entries (name, text). */
	static File zip(File file, String... nameAndText) throws IOException {
		file.getParentFile().mkdirs();
		try (ZipOutputStream z = new ZipOutputStream(new FileOutputStream(file))) {
			for (int i = 0; i + 1 < nameAndText.length; i += 2) {
				z.putNextEntry(new ZipEntry(nameAndText[i]));
				z.write(nameAndText[i + 1].getBytes(StandardCharsets.UTF_8));
				z.closeEntry();
			}
		}
		return file;
	}

	static File firstBytes(File from, File to, int n) throws IOException {
		byte[] buf = new byte[n];
		try (InputStream in = new FileInputStream(from)) {
			int got = 0, r;
			while (got < n && (r = in.read(buf, got, n - got)) > 0) {
				got += r;
			}
			try (OutputStream out = new FileOutputStream(to)) {
				out.write(buf, 0, got);
			}
		}
		return to;
	}

	static File named(List<File> zips, String name) {
		for (File f : zips) {
			if (f.getName().equals(name)) {
				return f;
			}
		}
		return null;
	}

	/** The two smallest zips (fast to import); fewer if fewer exist. */
	static List<File> smallZips(List<File> zips, int count) {
		List<File> sorted = new ArrayList<>(zips);
		Collections.sort(sorted, (a, b) -> Long.compare(a.length(), b.length()));
		return new ArrayList<>(sorted.subList(0, Math.min(count, sorted.size())));
	}

	static long epoch(LocalDate day, LocalTime time, ZoneId zone) {
		return ZonedDateTime.of(day, time, zone).toEpochSecond();
	}

	static double percentile(long[] sorted, double p) {
		if (sorted.length == 0) {
			return -1;
		}
		int i = (int) Math.min(sorted.length - 1, Math.max(0, Math.ceil(p * sorted.length) - 1));
		return sorted[i];
	}

	static long[] sortedCopy(List<Long> l) {
		long[] a = new long[l.size()];
		for (int i = 0; i < a.length; i++) {
			a[i] = l.get(i);
		}
		Arrays.sort(a);
		return a;
	}

	/** Polls Java heap use on a thread and keeps the maximum. */
	static final class HeapSampler implements Runnable {
		private volatile boolean stop;
		private volatile long peak;
		private final Thread thread = new Thread(this, "selftest-heap-sampler");

		static HeapSampler start() {
			HeapSampler s = new HeapSampler();
			s.sample();
			s.thread.setDaemon(true);
			s.thread.start();
			return s;
		}

		private void sample() {
			Runtime rt = Runtime.getRuntime();
			long used = rt.totalMemory() - rt.freeMemory();
			if (used > peak) {
				peak = used;
			}
		}

		@Override
		public void run() {
			while (!stop) {
				sample();
				try {
					Thread.sleep(40);
				} catch (InterruptedException e) {
					return;
				}
			}
		}

		long stop() {
			stop = true;
			sample();
			return peak;
		}
	}

	static String latLon(LatLon p) {
		return String.format(Locale.ROOT, "%.4f,%.4f", p.getLatitude(), p.getLongitude());
	}

	static long mb(long bytes) {
		return bytes >> 20;
	}
}

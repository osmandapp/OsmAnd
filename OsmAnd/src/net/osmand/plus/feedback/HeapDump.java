package net.osmand.plus.feedback;

import android.os.Build;
import android.os.Debug;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * Takes a heap dump, turns it into a class histogram and keeps only the histogram.
 * <p>
 * The dump itself never leaves the device and is deleted as soon as it has been read: it contains
 * every string the app holds, which on a map is coordinates, track names and search queries. The
 * histogram is class names and counters, and that is what the crash report carries.
 * <p>
 * Dumping stops the world for seconds and writes hundreds of megabytes, so this runs only when a
 * person asks for it.
 */
public class HeapDump {

	private static final Log log = PlatformUtil.getLog(HeapDump.class);

	public static final String HISTOGRAM_NAME = "heap_histogram.txt";
	private static final String DUMP_NAME = "heap.hprof";
	// dumping into a filesystem that cannot hold the dump leaves a truncated file and no answer
	private static final long REQUIRED_FREE_BYTES = 700L * 1024 * 1024;

	@NonNull
	public static File getHistogramFile(@NonNull OsmandApplication app) {
		return new File(app.getFilesDir(), HISTOGRAM_NAME);
	}

	/**
	 * @return the histogram that was written, for showing to the person who asked
	 * @throws IOException when the dump could not be taken or read
	 */
	@NonNull
	public static String collect(@NonNull OsmandApplication app) throws IOException {
		File cacheDir = app.getCacheDir();
		if (cacheDir == null) {
			throw new IOException("No cache directory");
		}
		long free = cacheDir.getUsableSpace();
		if (free < REQUIRED_FREE_BYTES) {
			throw new IOException("Only " + free / (1024 * 1024) + " MB free, a heap dump needs more");
		}
		File dump = new File(cacheDir, DUMP_NAME);
		File histogramFile = getHistogramFile(app);
		try {
			long start = SystemClock.uptimeMillis();
			Debug.dumpHprofData(dump.getAbsolutePath());
			long dumped = SystemClock.uptimeMillis() - start;

			long analyzeStart = SystemClock.uptimeMillis();
			String histogram = HeapHistogram.analyze(dump);
			long analyzed = SystemClock.uptimeMillis() - analyzeStart;

			try (Writer out = new OutputStreamWriter(new FileOutputStream(histogramFile), StandardCharsets.UTF_8)) {
				out.write(header(app, dump.length(), dumped, analyzed));
				out.write(histogram);
			}
			return header(app, dump.length(), dumped, analyzed) + histogram;
		} finally {
			if (dump.exists() && !dump.delete()) {
				log.error("Could not delete the heap dump at " + dump.getAbsolutePath());
			}
		}
	}

	@NonNull
	private static String header(@NonNull OsmandApplication app, long dumpSize, long dumpMs, long analyzeMs) {
		Runtime runtime = Runtime.getRuntime();
		long used = runtime.totalMemory() - runtime.freeMemory();
		StringBuilder sb = new StringBuilder();
		sb.append("--- heap histogram ").append(Version.getAppVersion(app));
		sb.append(" sdk=").append(Build.VERSION.SDK_INT).append('\n');
		sb.append("dump=").append(dumpSize / (1024 * 1024)).append("MB");
		sb.append(" dumpMs=").append(dumpMs).append(" analyzeMs=").append(analyzeMs).append('\n');
		sb.append("heap=").append(mb(used)).append('/').append(mb(runtime.maxMemory()));
		sb.append(" nat=").append(mb(Debug.getNativeHeapAllocatedSize()));
		sb.append('/').append(mb(Debug.getNativeHeapSize())).append('\n');
		return sb.toString();
	}

	private static long mb(long bytes) {
		return bytes / (1024 * 1024);
	}
}

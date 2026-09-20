package net.osmand.plus.feedback;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;

import org.apache.commons.logging.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Memory samples written to disk while the app runs, so that a crash report sent after the
 * process died carries the numbers from before the death. The state built at upload time
 * describes the fresh process and says nothing about the one that hung or was killed.
 * <p>
 * Measured on a device running 5.5.0: the java heap stayed at 57-91 MB of 512 while the
 * process grew to 1.5 GB, so a sample that only knows the java heap says nothing. The
 * native heap is written as allocated/size/free because the gap between size and allocated
 * is what the allocator holds and never returns, and the graphics total is written because
 * GPU textures are counted in neither heap.
 * <p>
 * Numbers only: heap sizes, garbage collector counters, how many objects a few subsystems
 * hold and how many destroyed activities are still alive. No file names, no coordinates,
 * nothing that identifies a user.
 */
public class MemoryLog {

	private static final Log log = PlatformUtil.getLog(MemoryLog.class);

	public static final String MEMORY_LOG_NAME = "memory_log.txt";

	private static final long SAMPLE_INTERVAL = 30 * 1000L;
	// once the heap is filling up the interesting part is minutes long, not half an hour
	private static final long BUSY_SAMPLE_INTERVAL = 5 * 1000L;
	private static final float BUSY_HEAP_RATIO = 0.7f;
	// a process that grew this much between two samples is worth watching closely
	private static final long BUSY_RSS_GROWTH_KB = 100 * 1024;
	// an activity destroyed longer ago than this and still not collected is counted as retained
	private static final long RETAINED_AFTER = 60 * 1000L;
	// a sample is a few hundred bytes, so this holds days of them, and as text it compresses to
	// a couple of hundred kilobytes inside the crash report
	private static final long MAX_FILE_SIZE = 4 * 1024 * 1024;
	private static final int ROTATE_BUFFER = 256 * 1024;
	private static final int THREAD_GROUPS = 8;
	private static final long CLOCK_TICKS_PER_SECOND = 100;
	private static final int PROCESS_SUMMARY_LIMIT = 128;
	// Debug.getMemoryInfo() walks smaps, which is not free on a process of this size
	private static final int SUMMARY_EVERY = 2;
	private static final long SUMMARY_BUDGET_MS = 200;
	// reading every mapping costs far more than the Debug counters, so it happens rarely
	private static final int SMAPS_EVERY = 10;
	// the first walk runs interpreted and costs ~270 ms, later ones settle around 70 ms
	private static final long SMAPS_BUDGET_MS = 600;
	private static final int SMAPS_BUFFER = 64 * 1024;
	private static final int SMAPS_DALVIK = 0;
	private static final int SMAPS_NATIVE = 1;
	private static final int SMAPS_GPU = 2;
	private static final int SMAPS_SO = 3;
	private static final int SMAPS_CODE = 4;
	private static final int SMAPS_OBF = 5;
	private static final int SMAPS_FONT = 6;
	private static final int SMAPS_STACK = 7;
	private static final int SMAPS_ANON = 8;
	private static final int SMAPS_OTHER = 9;
	// graphics mostly does not appear here: EGL and GL buffers are counted by mtrack rather
	// than mapped into the process, which is what Debug.getMemoryStat("summary.graphics") reads
	private static final String[] SMAPS_CATEGORIES =
			{"dalvik", "native", "gpu", "so", "code", "obf", "font", "stack", "anon", "other"};
	// a heap smaller than this explains itself; below it a histogram is not worth seconds of freeze
	private static final long HISTOGRAM_HEAP_THRESHOLD = 350L * 1024 * 1024;
	// many devices cap the heap at 256 MB, where the absolute threshold can never be reached,
	// so a heap that is nearly full counts as well
	private static final float HISTOGRAM_HEAP_RATIO = 0.8f;
	private static final long HISTOGRAM_INTERVAL = 15 * 60 * 1000L;
	// worth knowing what the smaps walk actually costs on a device, not only when it is too slow
	private static final long SUMMARY_REPORT_MS = 25;

	// runtime stat and the short name it is written with
	private static final String[][] RUNTIME_STATS = {
			{"art.gc.gc-count", "gc"},
			{"art.gc.gc-time", "gcms"},
			{"art.gc.bytes-allocated", "alloc"},
			{"art.gc.bytes-freed", "freed"},
			{"art.gc.blocking-gc-count", "bgc"},
			{"art.gc.blocking-gc-time", "bgcms"},
	};

	private static final ReferenceQueue<Activity> DESTROYED_QUEUE = new ReferenceQueue<>();
	private static final List<DestroyedActivity> DESTROYED = new ArrayList<>();

	private static long lastSampleTime;
	private static long peakUsed;
	private static boolean sessionStarted;
	private static boolean activitiesWatched;
	private static int sampleCount;
	private static boolean summaryAffordable = true;
	private static boolean smapsAffordable = true;
	private static int smapsCount;
	private static long previousAllocated;
	private static long previousBlockingGcTime;
	private static long lastRss;
	private static long previousRss;
	private static long previousCpu;
	private static long previousRead;
	private static long previousWrite;
	private static boolean summaryDue;
	private static long lastHistogramTime;
	private static int trimLevel = -1;
	private static int trimCount;

	// called from a background thread, at most once per SAMPLE_INTERVAL
	public static synchronized void sample(@NonNull OsmandApplication app) {
		long time = SystemClock.elapsedRealtime();
		Runtime runtime = Runtime.getRuntime();
		long used = runtime.totalMemory() - runtime.freeMemory();
		long max = runtime.maxMemory();
		// the peak between two samples matters more than the value at the sample itself:
		// a route calculation can take the heap up and give it back within one interval
		peakUsed = Math.max(peakUsed, used);
		// the java heap is not what fills up first: measured on 5.5.0 it stayed at a tenth of
		// its limit while the process went past 1.5 GB, so a growing process is a reason to
		// sample more often just as a filling heap is
		boolean busy = (max > 0 && used > max * BUSY_HEAP_RATIO)
				|| (previousRss > 0 && lastRss - previousRss > BUSY_RSS_GROWTH_KB);
		long interval = busy ? BUSY_SAMPLE_INTERVAL : SAMPLE_INTERVAL;
		if (lastSampleTime != 0 && time - lastSampleTime < interval) {
			return;
		}
		lastSampleTime = time;
		try {
			StringBuilder sb = new StringBuilder();
			if (!sessionStarted) {
				sessionStarted = true;
				sb.append("--- start ").append(Version.getAppVersion(app));
				sb.append(" sdk=").append(Build.VERSION.SDK_INT).append('\n');
			}
			String sample = buildSample(app, time, used, max);
			peakUsed = 0;
			sb.append(sample).append('\n');
			append(getFile(app), sb.toString());
			setProcessStateSummary(app, buildProcessSummary(time, used, max));
		} catch (RuntimeException e) {
			log.error(e);
		}
	}

	@NonNull
	public static File getFile(@NonNull OsmandApplication app) {
		return new File(app.getFilesDir(), MEMORY_LOG_NAME);
	}

	// the system asking for memory back is the clearest sign that the process is in trouble,
	// and it arrives on the main thread, so it is only remembered here and written by the sample
	public static synchronized void onTrimMemory(int level) {
		trimLevel = Math.max(trimLevel, level);
		trimCount++;
	}

	@NonNull
	private static String buildSample(@NonNull OsmandApplication app, long time, long used, long max) {
		StringBuilder sb = new StringBuilder();
		sb.append("t=").append(time / 1000);
		sb.append(" heap=").append(mb(used)).append('/').append(mb(max));
		sb.append(" peak=").append(mb(peakUsed));
		// allocated/size/free: size minus allocated is what the allocator keeps mapped
		sb.append(" nat=").append(mb(Debug.getNativeHeapAllocatedSize()));
		sb.append('/').append(mb(Debug.getNativeHeapSize()));
		sb.append('/').append(mb(Debug.getNativeHeapFreeSize()));
		appendProcStatus(sb);
		appendCpuAndIo(sb);
		appendRuntimeStats(sb);
		appendVmCounts(sb);
		appendProcessSummary(sb);
		appendSmaps(sb);
		appendThreadNames(sb);
		String held = buildHeld(app);
		if (held != null) {
			sb.append(" held=").append(held);
		}
		String retained = countRetained(time);
		if (retained != null) {
			sb.append(' ').append(retained);
		}
		if (trimCount > 0) {
			sb.append(" trim=").append(trimLevel).append('x').append(trimCount);
			trimLevel = -1;
			trimCount = 0;
		}
		String busy = busyWith(app);
		if (busy != null) {
			sb.append(" busy=").append(busy);
		}
		String histogram = maybeCollectHistogram(app, time, used);
		if (histogram != null) {
			sb.append(' ').append(histogram);
		}
		return sb.toString();
	}

	// Thread.activeCount() only counts the thread group of the caller and never sees a thread
	// started by native code, and the map renderer, the Qt pool and hwui all run on those.
	// One read of /proc/self/status gives the real count plus the resident size and the swap,
	// none of which any Debug counter carries and none of which costs a walk of smaps.
	private static void appendProcStatus(@NonNull StringBuilder sb) {
		long rss = 0;
		long swap = 0;
		long threads = 0;
		try (BufferedReader reader = new BufferedReader(new FileReader("/proc/self/status"))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("VmRSS:")) {
					rss = parseStatusKb(line);
				} else if (line.startsWith("VmSwap:")) {
					swap = parseStatusKb(line);
				} else if (line.startsWith("Threads:")) {
					threads = parseStatusKb(line);
				}
			}
		} catch (IOException | RuntimeException e) {
			// /proc is not a promise, the rest of the sample is still worth writing
		}
		if (rss > 0) {
			previousRss = lastRss;
			lastRss = rss;
			sb.append(" rss=").append(rss / 1024);
		}
		if (swap > 0) {
			sb.append(" swap=").append(swap / 1024);
		}
		if (threads > 0) {
			sb.append(" thr=").append(threads);
		}
	}

	private static long parseStatusKb(@NonNull String line) {
		int from = line.indexOf(':') + 1;
		int to = line.indexOf(" kB");
		return parse(line.substring(from, to > from ? to : line.length()).trim());
	}


	// cpu time and disk traffic since the previous sample: a process that allocates hard also
	// burns cpu, and heavy writes are how track recording shows up
	private static void appendCpuAndIo(@NonNull StringBuilder sb) {
		long cpu = readCpuMillis();
		if (cpu > 0) {
			if (previousCpu > 0 && cpu >= previousCpu) {
				sb.append(" cpums=").append(cpu - previousCpu);
			}
			previousCpu = cpu;
		}
		long[] io = readIoBytes();
		if (io != null) {
			if (previousRead > 0 && io[0] >= previousRead) {
				sb.append(" rdkb=").append((io[0] - previousRead) / 1024);
			}
			if (previousWrite > 0 && io[1] >= previousWrite) {
				sb.append(" wrkb=").append((io[1] - previousWrite) / 1024);
			}
			previousRead = io[0];
			previousWrite = io[1];
		}
	}

	private static long readCpuMillis() {
		try (BufferedReader reader = new BufferedReader(new FileReader("/proc/self/stat"))) {
			String line = reader.readLine();
			if (line == null) {
				return 0;
			}
			// the command name is in brackets and may contain spaces, the fields follow it
			int from = line.lastIndexOf(')');
			if (from < 0) {
				return 0;
			}
			String[] fields = line.substring(from + 1).trim().split(" ");
			// utime and stime are the 12th and 13th field after the state
			if (fields.length < 13) {
				return 0;
			}
			long ticks = parse(fields[11]) + parse(fields[12]);
			return ticks * 1000 / CLOCK_TICKS_PER_SECOND;
		} catch (IOException | RuntimeException e) {
			return 0;
		}
	}

	@Nullable
	private static long[] readIoBytes() {
		long read = 0;
		long write = 0;
		try (BufferedReader reader = new BufferedReader(new FileReader("/proc/self/io"))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("rchar:")) {
					read = parse(line.substring(6).trim());
				} else if (line.startsWith("wchar:")) {
					write = parse(line.substring(6).trim());
				}
			}
		} catch (IOException | RuntimeException e) {
			return null;
		}
		return read > 0 || write > 0 ? new long[] {read, write} : null;
	}

	// binder proxies and loaded classes are counted by the runtime itself, so reading them is
	// a native call rather than a heap walk; both grow when something is held that should not be
	private static void appendVmCounts(@NonNull StringBuilder sb) {
		try {
			sb.append(" bnd=").append(Debug.getBinderLocalObjectCount());
			sb.append('/').append(Debug.getBinderProxyObjectCount());
			sb.append('/').append(Debug.getBinderDeathObjectCount());
			sb.append(" cls=").append(Debug.getLoadedClassCount());
		} catch (RuntimeException e) {
			// counters are optional, the sample is not
		}
	}

	// a thread count says a pool leaked, the names say which pool
	private static void appendThreadNames(@NonNull StringBuilder sb) {
		if (!summaryDue) {
			return;
		}
		try {
			File[] tasks = new File("/proc/self/task").listFiles();
			if (tasks == null || tasks.length == 0) {
				return;
			}
			Map<String, Integer> byPrefix = new HashMap<>();
			for (File task : tasks) {
				String name = readFirstLine(new File(task, "comm"));
				if (name == null) {
					continue;
				}
				String prefix = threadPrefix(name);
				Integer was = byPrefix.get(prefix);
				byPrefix.put(prefix, was == null ? 1 : was + 1);
			}
			List<Map.Entry<String, Integer>> entries = new ArrayList<>(byPrefix.entrySet());
			Collections.sort(entries, (a, b) -> b.getValue() - a.getValue());
			StringBuilder names = new StringBuilder();
			for (int i = 0; i < entries.size() && i < THREAD_GROUPS; i++) {
				if (names.length() > 0) {
					names.append(',');
				}
				names.append(entries.get(i).getKey()).append(':').append(entries.get(i).getValue());
			}
			if (names.length() > 0) {
				sb.append(" thrnames=").append(names);
			}
		} catch (RuntimeException e) {
			// /proc is not a promise
		}
	}

	// "pool-3-thread-1", "Binder:8423_2" and "OsmAndCore#4" are all one group each
	@NonNull
	private static String threadPrefix(@NonNull String name) {
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (Character.isDigit(c) || c == '-' || c == '#' || c == '_' || c == ':') {
				return i > 0 ? name.substring(0, i) : name;
			}
		}
		return name;
	}

	@Nullable
	private static String readFirstLine(@NonNull File file) {
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line = reader.readLine();
			return line != null ? line.trim() : null;
		} catch (IOException | RuntimeException e) {
			return null;
		}
	}

	// the counters are totals since the process started; the delta since the previous sample
	// is what tells a quiet minute apart from one that churned a gigabyte
	private static void appendRuntimeStats(@NonNull StringBuilder sb) {
		long allocated = 0;
		long blockingGcTime = 0;
		for (String[] stat : RUNTIME_STATS) {
			String value = Debug.getRuntimeStat(stat[0]);
			sb.append(' ').append(stat[1]).append('=').append(value != null ? value : "?");
			if ("alloc".equals(stat[1])) {
				allocated = parse(value);
			} else if ("bgcms".equals(stat[1])) {
				blockingGcTime = parse(value);
			}
		}
		if (previousAllocated > 0 && allocated >= previousAllocated) {
			sb.append(" dalloc=").append(mb(allocated - previousAllocated));
		}
		if (previousBlockingGcTime > 0 && blockingGcTime >= previousBlockingGcTime) {
			sb.append(" dbgcms=").append(blockingGcTime - previousBlockingGcTime);
		}
		previousAllocated = allocated;
		previousBlockingGcTime = blockingGcTime;
	}

	// graphics and swap are counted in neither heap, and on a map screen graphics is the
	// largest single number, so it is worth the cost of reading smaps every few samples
	private static void appendProcessSummary(@NonNull StringBuilder sb) {
		summaryDue = sampleCount++ % SUMMARY_EVERY == 0;
		if (!summaryAffordable || !summaryDue) {
			return;
		}
		long start = SystemClock.uptimeMillis();
		Debug.MemoryInfo info = new Debug.MemoryInfo();
		Debug.getMemoryInfo(info);
		long spent = SystemClock.uptimeMillis() - start;
		sb.append(" pss=").append(kbStat(info, "summary.total-pss"));
		sb.append(" gfx=").append(kbStat(info, "summary.graphics"));
		sb.append(" code=").append(kbStat(info, "summary.code"));
		sb.append(" stk=").append(kbStat(info, "summary.stack"));
		sb.append(" oth=").append(kbStat(info, "summary.private-other"));
		sb.append(" sys=").append(kbStat(info, "summary.system"));
		if (spent >= SUMMARY_REPORT_MS) {
			sb.append(" pssMs=").append(spent);
		}
		if (spent > SUMMARY_BUDGET_MS) {
			// reading it costs more than it is worth on this device, stop for this session
			summaryAffordable = false;
			sb.append(" summaryMs=").append(spent);
		}
	}

	/**
	 * The Debug counters say how much, /proc/self/smaps says where: every mapping is named, and
	 * Android names the anonymous ones too, so "native" stops being one number and becomes scudo,
	 * the GPU driver, the mapped libraries and the maps a person opened.
	 * <p>
	 * Only the categories are written, never the names of the mapped files: those are the maps
	 * somebody downloaded, which is where they live and where they travel.
	 */
	private static void appendSmaps(@NonNull StringBuilder sb) {
		if (!smapsAffordable || !summaryDue || smapsCount++ % SMAPS_EVERY != 0) {
			return;
		}
		long start = SystemClock.uptimeMillis();
		String categories = readSmapsByCategory();
		long spent = SystemClock.uptimeMillis() - start;
		if (categories != null) {
			sb.append(" smaps=").append(categories);
			sb.append(" smapsMs=").append(spent);
		}
		if (spent > SMAPS_BUDGET_MS) {
			// a process with tens of thousands of mappings can make this cost more than it says
			smapsAffordable = false;
		}
	}

	/**
	 * smaps is a couple of megabytes of text and tens of thousands of lines, so it is scanned as
	 * bytes: decoding it into strings costs several times more than the kernel spends producing it.
	 */
	@Nullable
	private static String readSmapsByCategory() {
		long[] totals = new long[SMAPS_CATEGORIES.length];
		int category = SMAPS_OTHER;
		byte[] buffer = new byte[SMAPS_BUFFER];
		int filled = 0;
		try (FileInputStream in = new FileInputStream("/proc/self/smaps")) {
			while (true) {
				int read = in.read(buffer, filled, buffer.length - filled);
				if (read > 0) {
					filled += read;
				}
				int lineStart = 0;
				int i = 0;
				while (i < filled) {
					if (buffer[i] != '\n') {
						i++;
						continue;
					}
					// only two lines out of the twenty-five a mapping prints are worth looking at,
					// and the rest are told apart by their first byte alone
					byte first = buffer[lineStart];
					if (first == 'R' && lineStart + 1 < i && buffer[lineStart + 1] == 's') {
						totals[category] += parseKb(buffer, lineStart + 4, i);
					} else if (first >= '0' && first <= '9' || first >= 'a' && first <= 'f') {
						category = categoryOf(new String(buffer, lineStart, i - lineStart,
								StandardCharsets.US_ASCII));
					}
					i++;
					lineStart = i;
				}
				if (read < 0) {
					// smaps always ends with a newline, so nothing is left behind here
					break;
				}
				if (lineStart > 0) {
					System.arraycopy(buffer, lineStart, buffer, 0, filled - lineStart);
					filled -= lineStart;
				} else if (filled == buffer.length) {
					// a line longer than the buffer can only be a path we do not need in full
					filled = 0;
				}
			}
		} catch (IOException | RuntimeException e) {
			log.error(e);
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < totals.length; i++) {
			long mb = totals[i] / 1024;
			if (mb > 0) {
				if (sb.length() > 0) {
					sb.append(',');
				}
				sb.append(SMAPS_CATEGORIES[i]).append(':').append(mb);
			}
		}
		return sb.length() > 0 ? sb.toString() : null;
	}

	private static long parseKb(@NonNull byte[] b, int from, int to) {
		long value = 0;
		for (int i = from; i < to; i++) {
			byte c = b[i];
			if (c >= '0' && c <= '9') {
				value = value * 10 + (c - '0');
			} else if (value > 0) {
				break;
			}
		}
		return value;
	}

	private static int categoryOf(@NonNull String header) {
		// address perms offset dev inode [name] — an anonymous mapping simply has no sixth field,
		// and taking the last one instead would read the inode as a name
		int i = 0;
		int length = header.length();
		for (int field = 0; field < 5 && i < length; field++) {
			while (i < length && header.charAt(i) != ' ') {
				i++;
			}
			while (i < length && header.charAt(i) == ' ') {
				i++;
			}
		}
		String name = i < length ? header.substring(i) : "";
		if (name.isEmpty()) {
			return SMAPS_ANON;
		}
		if (name.startsWith("[anon:dalvik") || name.startsWith("[anon:Zygote")) {
			return SMAPS_DALVIK;
		}
		if (name.startsWith("[anon:scudo") || name.startsWith("[anon:libc_malloc")
				|| name.startsWith("[anon:GWP-ASan") || name.startsWith("[anon:bionic")) {
			return SMAPS_NATIVE;
		}
		if (name.startsWith("[stack") || name.startsWith("[anon:stack_and_tls")
				|| name.startsWith("[anon:thread")) {
			return SMAPS_STACK;
		}
		if (name.startsWith("/dev/kgsl") || name.startsWith("/dev/mali") || name.startsWith("/dev/dri")
				|| name.startsWith("/dev/dma_heap") || name.startsWith("/dmabuf")
				|| name.contains("gralloc") || name.startsWith("/dev/ashmem")) {
			return SMAPS_GPU;
		}
		if (name.endsWith(".so")) {
			return SMAPS_SO;
		}
		if (name.endsWith(".apk") || name.endsWith(".jar") || name.endsWith(".dex")
				|| name.endsWith(".odex") || name.endsWith(".vdex") || name.endsWith(".oat")
				|| name.endsWith(".art")) {
			return SMAPS_CODE;
		}
		if (name.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)) {
			return SMAPS_OBF;
		}
		if (name.endsWith(".ttf") || name.endsWith(".otf")) {
			return SMAPS_FONT;
		}
		return SMAPS_OTHER;
	}

	// what a few subsystems hold right now, in objects rather than bytes: counting bytes needs
	// a heap walk, counting what a cache already knows costs nothing. Only non empty values are
	// written so that a line stays short.
	@Nullable
	private static String buildHeld(@NonNull OsmandApplication app) {
		StringBuilder sb = new StringBuilder();
		try {
			ResourceManager manager = app.getResourceManager();
			appendCount(sb, "obf", manager.getFileReadersCount());
			appendCount(sb, "tiles", manager.getBitmapTilesCache().size());
		} catch (RuntimeException e) {
			// a subsystem that is not up yet must not cost the whole sample
		}
		try {
			int files = 0;
			long points = 0;
			for (SelectedGpxFile selected : app.getSelectedGpxHelper().getSelectedGPXFiles()) {
				files++;
				points += selected.getPointsToDisplayCount();
			}
			appendCount(sb, "gpx", files);
			appendCount(sb, "gpxpt", points);
		} catch (RuntimeException e) {
			// the list is written by another thread, a sample is not worth a lock
		}
		try {
			appendCount(sb, "rec", app.getSavingTrackHelper().getTrkPoints());
		} catch (RuntimeException e) {
		}
		try {
			RouteCalculationResult route = app.getRoutingHelper().getRoute();
			appendCount(sb, "rtseg", route.getImmutableAllSegments().size());
			appendCount(sb, "rtloc", route.getImmutableAllLocations().size());
		} catch (RuntimeException e) {
		}
		try {
			ResourceManager manager = app.getResourceManager();
			appendCount(sb, "addr", manager.getAddressRepositories().size());
		} catch (RuntimeException e) {
		}
		try {
			appendCount(sb, "layer", app.getOsmandMap().getMapView().getLayers().size());
		} catch (RuntimeException e) {
		}
		try {
			appendCount(sb, "marker", app.getMapMarkersHelper().getMapMarkers().size());
		} catch (RuntimeException e) {
		}
		try {
			appendCount(sb, "plugin", PluginsHelper.getEnabledPlugins().size());
		} catch (RuntimeException e) {
		}
		try {
			appendCount(sb, "dl", app.getDownloadThread().getCurrentDownloadingItems().size());
		} catch (RuntimeException e) {
		}
		return sb.length() > 0 ? sb.toString() : null;
	}

	private static void appendCount(@NonNull StringBuilder sb, @NonNull String name, long value) {
		if (value <= 0) {
			return;
		}
		if (sb.length() > 0) {
			sb.append(',');
		}
		sb.append(name).append(':').append(value);
	}

	// a histogram is worth taking when the heap is large enough to be worth explaining, and it
	// costs seconds, so it happens rarely and only when a person turned it on
	@Nullable
	private static String maybeCollectHistogram(@NonNull OsmandApplication app, long time, long used) {
		long max = Runtime.getRuntime().maxMemory();
		if (used < Math.min(HISTOGRAM_HEAP_THRESHOLD, (long) (max * HISTOGRAM_HEAP_RATIO))) {
			return null;
		}
		if (lastHistogramTime != 0 && time - lastHistogramTime < HISTOGRAM_INTERVAL) {
			return null;
		}
		try {
			OsmandDevelopmentPlugin plugin = PluginsHelper.getActivePlugin(OsmandDevelopmentPlugin.class);
			if (plugin == null || !plugin.AUTO_HEAP_HISTOGRAM.get()) {
				return null;
			}
		} catch (RuntimeException e) {
			return null;
		}
		lastHistogramTime = time;
		try {
			HeapDump.collect(app);
			return "histogram=taken";
		} catch (IOException | RuntimeException e) {
			log.error(e);
			return "histogram=failed";
		}
	}

	// what the app was doing when the sample was taken, so that a heap spike can be told
	// apart from a leak: a route calculation and a search allocate a lot on purpose
	@Nullable
	private static String busyWith(@NonNull OsmandApplication app) {
		StringBuilder sb = new StringBuilder();
		RoutingHelper routingHelper = app.getRoutingHelper();
		if (routingHelper.isRouteBeingCalculated()) {
			append(sb, "routing");
		}
		if (routingHelper.isFollowingMode()) {
			append(sb, "navigation");
		}
		if (app.getDownloadThread().isDownloading()) {
			append(sb, "download");
		}
		return sb.length() > 0 ? sb.toString() : null;
	}

	private static void append(@NonNull StringBuilder sb, @NonNull String value) {
		if (sb.length() > 0) {
			sb.append(',');
		}
		sb.append(value);
	}

	// activities are destroyed by the system, so any that outlive their onDestroy by a while is
	// either a leak or a slow collection; both are worth knowing about before the process died
	public static synchronized void watchActivities(@NonNull OsmandApplication app) {
		if (activitiesWatched) {
			// diagnostics restart every time the app comes back to the foreground
			return;
		}
		activitiesWatched = true;
		app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
			@Override
			public void onActivityDestroyed(@NonNull Activity activity) {
				synchronized (DESTROYED) {
					DESTROYED.add(new DestroyedActivity(activity, DESTROYED_QUEUE));
				}
			}

			@Override
			public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle state) {
			}

			@Override
			public void onActivityStarted(@NonNull Activity activity) {
			}

			@Override
			public void onActivityResumed(@NonNull Activity activity) {
			}

			@Override
			public void onActivityPaused(@NonNull Activity activity) {
			}

			@Override
			public void onActivityStopped(@NonNull Activity activity) {
			}

			@Override
			public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle state) {
			}
		});
	}

	@Nullable
	private static String countRetained(long time) {
		synchronized (DESTROYED) {
			for (Reference<?> collected = DESTROYED_QUEUE.poll(); collected != null; collected = DESTROYED_QUEUE.poll()) {
				DESTROYED.remove(collected);
			}
			int retained = 0;
			long oldest = 0;
			for (Iterator<DestroyedActivity> it = DESTROYED.iterator(); it.hasNext(); ) {
				DestroyedActivity destroyed = it.next();
				if (destroyed.get() == null) {
					it.remove();
				} else if (time - destroyed.destroyedAt > RETAINED_AFTER) {
					retained++;
					oldest = Math.max(oldest, time - destroyed.destroyedAt);
				}
			}
			return retained > 0 ? "retained=" + retained + " retainedFor=" + oldest / 1000 + "s" : null;
		}
	}

	private static class DestroyedActivity extends WeakReference<Activity> {

		private final long destroyedAt = SystemClock.elapsedRealtime();

		DestroyedActivity(@NonNull Activity activity, @NonNull ReferenceQueue<Activity> queue) {
			super(activity, queue);
		}
	}

	// the summary is attached to the exit record of this very process and survives its death,
	// but it is capped at 128 bytes, so it carries the totals rather than a cut off sample
	@NonNull
	private static String buildProcessSummary(long time, long used, long max) {
		return "t=" + time / 1000
				+ " h=" + mb(used) + '/' + mb(max)
				+ " n=" + mb(Debug.getNativeHeapAllocatedSize()) + '/' + mb(Debug.getNativeHeapSize());
	}

	private static void setProcessStateSummary(@NonNull OsmandApplication app, @NonNull String sample) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			setProcessStateSummaryApi30(app, sample);
		}
	}

	@RequiresApi(api = Build.VERSION_CODES.R)
	private static void setProcessStateSummaryApi30(@NonNull OsmandApplication app, @NonNull String sample) {
		ActivityManager manager = app.getSystemService(ActivityManager.class);
		if (manager == null) {
			return;
		}
		try {
			byte[] bytes = sample.getBytes(StandardCharsets.US_ASCII);
			if (bytes.length > PROCESS_SUMMARY_LIMIT) {
				byte[] cut = new byte[PROCESS_SUMMARY_LIMIT];
				System.arraycopy(bytes, 0, cut, 0, PROCESS_SUMMARY_LIMIT);
				bytes = cut;
			}
			manager.setProcessStateSummary(bytes);
		} catch (RuntimeException e) {
			log.error(e);
		}
	}

	// keeps the newest MAX_FILE_SIZE bytes, the older samples are dropped. The tail is moved
	// in small chunks: the log is tens of megabytes and reading it into one array would be a
	// larger allocation than anything this class is meant to observe.
	private static void append(@NonNull File file, @NonNull String text) {
		try (RandomAccessFile out = new RandomAccessFile(file, "rw")) {
			long length = out.length();
			if (length > MAX_FILE_SIZE) {
				rotate(out, length - MAX_FILE_SIZE / 2);
			} else {
				out.seek(length);
			}
			out.write(text.getBytes(StandardCharsets.US_ASCII));
		} catch (IOException | RuntimeException e) {
			log.error(e);
		}
	}

	private static void rotate(@NonNull RandomAccessFile out, long from) throws IOException {
		byte[] buffer = new byte[ROTATE_BUFFER];
		long read = from;
		long write = 0;
		boolean trimmed = false;
		int count;
		while (true) {
			out.seek(read);
			count = out.read(buffer);
			if (count <= 0) {
				break;
			}
			read += count;
			int offset = 0;
			if (!trimmed) {
				// start at a line boundary so that the first sample kept is not a fragment
				trimmed = true;
				offset = indexOfLineStart(buffer, count);
			}
			out.seek(write);
			out.write(buffer, offset, count - offset);
			write += count - offset;
		}
		out.setLength(write);
		out.seek(write);
	}

	private static int indexOfLineStart(@NonNull byte[] bytes, int length) {
		for (int i = 0; i < length; i++) {
			if (bytes[i] == '\n') {
				return i + 1;
			}
		}
		return 0;
	}

	private static long parse(@Nullable String value) {
		try {
			return value != null ? Long.parseLong(value) : 0;
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long kbStat(@NonNull Debug.MemoryInfo info, @NonNull String name) {
		return parse(info.getMemoryStat(name)) / 1024;
	}

	private static long mb(long bytes) {
		return bytes / (1024 * 1024);
	}
}

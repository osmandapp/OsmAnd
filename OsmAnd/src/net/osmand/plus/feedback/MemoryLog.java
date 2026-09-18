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

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.routing.RoutingHelper;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Memory samples written to disk while the app runs, so that a crash report sent after the
 * process died carries the numbers from before the death. The state built at upload time
 * describes the fresh process and says nothing about the one that hung or was killed.
 * <p>
 * Numbers only: heap sizes, garbage collector counters and how many destroyed activities are
 * still alive. No file names, no coordinates, nothing that identifies a user.
 */
public class MemoryLog {

	private static final Log log = PlatformUtil.getLog(MemoryLog.class);

	public static final String MEMORY_LOG_NAME = "memory_log.txt";

	private static final long SAMPLE_INTERVAL = 30 * 1000L;
	// once the heap is filling up the interesting part is minutes long, not half an hour
	private static final long BUSY_SAMPLE_INTERVAL = 5 * 1000L;
	private static final float BUSY_HEAP_RATIO = 0.7f;
	// an activity destroyed longer ago than this and still not collected is counted as retained
	private static final long RETAINED_AFTER = 60 * 1000L;
	private static final long MAX_FILE_SIZE = 64 * 1024;
	private static final int PROCESS_SUMMARY_LIMIT = 128;

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

	// called from a background thread, at most once per SAMPLE_INTERVAL
	public static synchronized void sample(@NonNull OsmandApplication app) {
		long time = SystemClock.elapsedRealtime();
		Runtime runtime = Runtime.getRuntime();
		long used = runtime.totalMemory() - runtime.freeMemory();
		long max = runtime.maxMemory();
		// the peak between two samples matters more than the value at the sample itself:
		// a route calculation can take the heap up and give it back within one interval
		peakUsed = Math.max(peakUsed, used);
		long interval = max > 0 && used > max * BUSY_HEAP_RATIO ? BUSY_SAMPLE_INTERVAL : SAMPLE_INTERVAL;
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
			setProcessStateSummary(app, sample);
		} catch (RuntimeException e) {
			log.error(e);
		}
	}

	@NonNull
	public static File getFile(@NonNull OsmandApplication app) {
		return new File(app.getFilesDir(), MEMORY_LOG_NAME);
	}

	@NonNull
	private static String buildSample(@NonNull OsmandApplication app, long time, long used, long max) {
		StringBuilder sb = new StringBuilder();
		sb.append("t=").append(time / 1000);
		sb.append(" heap=").append(mb(used)).append('/').append(mb(max));
		sb.append(" peak=").append(mb(peakUsed));
		sb.append(" nat=").append(mb(Debug.getNativeHeapAllocatedSize()));
		sb.append(" thr=").append(Thread.activeCount());
		for (String[] stat : RUNTIME_STATS) {
			String value = Debug.getRuntimeStat(stat[0]);
			sb.append(' ').append(stat[1]).append('=').append(value != null ? value : "?");
		}
		String retained = countRetained(time);
		if (retained != null) {
			sb.append(' ').append(retained);
		}
		String busy = busyWith(app);
		if (busy != null) {
			sb.append(" busy=").append(busy);
		}
		return sb.toString();
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
	public static void watchActivities(@NonNull OsmandApplication app) {
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

	// the summary is attached to the exit record of this very process and survives its death
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

	// keeps the newest MAX_FILE_SIZE bytes, the older samples are dropped
	private static void append(@NonNull File file, @NonNull String text) {
		try (RandomAccessFile out = new RandomAccessFile(file, "rw")) {
			long length = out.length();
			if (length > MAX_FILE_SIZE) {
				byte[] tail = new byte[(int) (MAX_FILE_SIZE / 2)];
				out.seek(length - tail.length);
				out.readFully(tail);
				int from = indexOfLineStart(tail);
				out.setLength(0);
				out.write(tail, from, tail.length - from);
			} else {
				out.seek(length);
			}
			out.write(text.getBytes(StandardCharsets.US_ASCII));
		} catch (IOException | RuntimeException e) {
			log.error(e);
		}
	}

	private static int indexOfLineStart(@NonNull byte[] bytes) {
		for (int i = 0; i < bytes.length; i++) {
			if (bytes[i] == '\n') {
				return i + 1;
			}
		}
		return 0;
	}

	private static long mb(long bytes) {
		return bytes / (1024 * 1024);
	}
}

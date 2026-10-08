package net.osmand.plus.feedback;

import android.app.ActivityManager;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Debug;
import android.os.Process;
import android.os.SystemClock;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.resources.BinaryMapReaderResource;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.IndexConstants;

import org.apache.commons.logging.Log;

import java.io.File;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Runtime state attached to a crash report, to tell an out of memory hang from an ordinary one:
 * heap and PSS sizes, the ART garbage collector counters, how many maps are installed, how many
 * GPX tracks are stored and shown, which plugins are on and whether navigation was running.
 * <p>
 * Counts and flags only. No file names, no coordinates, no profile names, no account data —
 * nothing that identifies a user or where they are.
 */
public class CrashReportState {

	private static final Log log = PlatformUtil.getLog(CrashReportState.class);

	private static final int MAX_TRACKS_DEPTH = 8;

	private static final String[] RUNTIME_STATS = {
			"art.gc.gc-count",
			"art.gc.gc-time",
			"art.gc.bytes-allocated",
			"art.gc.bytes-freed",
			"art.gc.blocking-gc-count",
			"art.gc.blocking-gc-time",
	};

	@NonNull
	public static String build(@NonNull OsmandApplication app) {
		StringBuilder sb = new StringBuilder();
		sb.append("# OsmAnd crash report state (counts and flags only, no personal data)\n");
		try {
			appendApp(sb, app);
			appendDevice(sb, app);
			appendMemory(sb, app);
			appendMaps(sb, app);
			appendTracks(sb, app);
			appendState(sb, app);
		} catch (RuntimeException e) {
			log.error(e);
			sb.append("error: ").append(e.getClass().getSimpleName()).append('\n');
		}
		return sb.toString();
	}

	private static void appendApp(@NonNull StringBuilder sb, @NonNull OsmandApplication app) {
		PackageInfo info = null;
		try {
			info = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
		} catch (Exception e) {
			// ignore, the version is reported by the upload parameters as well
		}
		sb.append("app: ").append(info != null ? info.versionName : Version.getAppVersion(app));
		if (info != null) {
			sb.append(" (").append(info.versionCode).append(')');
		}
		sb.append(" package=").append(app.getPackageName());
		sb.append(" paid=").append(Version.isPaidVersion(app));
		sb.append(" uptime=").append(uptimeSeconds()).append("s\n");
	}

	private static void appendDevice(@NonNull StringBuilder sb, @NonNull OsmandApplication app) {
		ActivityManager manager = app.getSystemService(ActivityManager.class);
		sb.append("device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL);
		sb.append(" android=").append(Build.VERSION.RELEASE).append(" sdk=").append(Build.VERSION.SDK_INT);
		sb.append(" abi=").append(Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "?");
		if (manager != null) {
			sb.append(" lowRamDevice=").append(manager.isLowRamDevice());
			sb.append(" heapLimit=").append(manager.getMemoryClass()).append("MB");
			sb.append(" largeHeapLimit=").append(manager.getLargeMemoryClass()).append("MB");
		}
		sb.append('\n');
	}

	private static void appendMemory(@NonNull StringBuilder sb, @NonNull OsmandApplication app) {
		Runtime runtime = Runtime.getRuntime();
		sb.append("java heap: max=").append(mb(runtime.maxMemory()));
		sb.append(" total=").append(mb(runtime.totalMemory()));
		sb.append(" used=").append(mb(runtime.totalMemory() - runtime.freeMemory())).append('\n');

		sb.append("native heap: size=").append(mb(Debug.getNativeHeapSize()));
		sb.append(" allocated=").append(mb(Debug.getNativeHeapAllocatedSize())).append('\n');

		Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();
		Debug.getMemoryInfo(memoryInfo);
		sb.append("pss: total=").append(kb(memoryInfo.getTotalPss()));
		sb.append(" dalvik=").append(kb(memoryInfo.dalvikPss));
		sb.append(" native=").append(kb(memoryInfo.nativePss));
		sb.append(" other=").append(kb(memoryInfo.otherPss));
		sb.append(" graphics=").append(memoryStat(memoryInfo, "summary.graphics"));
		sb.append(" swap=").append(memoryStat(memoryInfo, "summary.total-swap")).append('\n');

		ActivityManager manager = app.getSystemService(ActivityManager.class);
		if (manager != null) {
			ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
			manager.getMemoryInfo(info);
			sb.append("device memory: total=").append(mb(info.totalMem));
			sb.append(" available=").append(mb(info.availMem));
			sb.append(" threshold=").append(mb(info.threshold));
			sb.append(" low=").append(info.lowMemory).append('\n');
		}

		sb.append("gc:");
		for (String stat : RUNTIME_STATS) {
			String value = Debug.getRuntimeStat(stat);
			sb.append(' ').append(stat.substring(stat.lastIndexOf('.') + 1)).append('=')
					.append(value != null ? value : "?");
		}
		sb.append('\n');
	}

	private static void appendMaps(@NonNull StringBuilder sb, @NonNull OsmandApplication app) {
		int standard = 0;
		int wiki = 0;
		int srtm = 0;
		int travel = 0;
		int road = 0;
		Collection<BinaryMapReaderResource> readers = app.getResourceManager().getFileReaders();
		for (BinaryMapReaderResource reader : readers) {
			String name = reader.getFileName();
			if (name.endsWith(IndexConstants.BINARY_WIKI_MAP_INDEX_EXT)) {
				wiki++;
			} else if (name.endsWith(IndexConstants.BINARY_SRTM_MAP_INDEX_EXT)
					|| name.endsWith(IndexConstants.BINARY_SRTM_FEET_MAP_INDEX_EXT)) {
				srtm++;
			} else if (name.endsWith(IndexConstants.BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT)) {
				travel++;
			} else if (name.endsWith(IndexConstants.BINARY_ROAD_MAP_INDEX_EXT)) {
				road++;
			} else {
				standard++;
			}
		}
		sb.append("maps: open=").append(readers.size());
		sb.append(" standard=").append(standard);
		sb.append(" road=").append(road);
		sb.append(" wiki=").append(wiki);
		sb.append(" srtm=").append(srtm);
		sb.append(" travel=").append(travel).append('\n');
	}

	private static void appendTracks(@NonNull StringBuilder sb, @NonNull OsmandApplication app) {
		DirStats stats = new DirStats();
		collect(app.getAppPath(IndexConstants.GPX_INDEX_DIR), stats, 0);
		sb.append("tracks: files=").append(stats.files);
		sb.append(" size=").append(mb(stats.bytes)).append('\n');

		long points = 0;
		int loaded = 0;
		List<SelectedGpxFile> selected = app.getSelectedGpxHelper().getSelectedGPXFiles();
		for (SelectedGpxFile selectedGpxFile : selected) {
			if (selectedGpxFile.isLoaded()) {
				loaded++;
			}
			points += selectedGpxFile.getPointsToDisplayCount();
		}
		sb.append("tracks shown: files=").append(selected.size());
		sb.append(" loaded=").append(loaded);
		sb.append(" points=").append(points).append('\n');
	}

	// counts only, never a file name; bounded depth so a deep import tree cannot stall the report
	private static void collect(@Nullable File dir, @NonNull DirStats stats, int depth) {
		File[] files = depth > MAX_TRACKS_DEPTH || dir == null ? null : dir.listFiles();
		if (files == null) {
			return;
		}
		for (File file : files) {
			if (file.isDirectory()) {
				collect(file, stats, depth + 1);
			} else if (file.getName().endsWith(IndexConstants.GPX_FILE_EXT)) {
				stats.files++;
				stats.bytes += file.length();
			}
		}
	}

	private static class DirStats {
		int files;
		long bytes;
	}

	private static void appendState(@NonNull StringBuilder sb, @NonNull OsmandApplication app) {
		RoutingHelper routingHelper = app.getRoutingHelper();
		sb.append("state: renderer=").append(app.useOpenGlRenderer() ? "opengl" : "legacy");
		sb.append(" navigation=").append(routingHelper.isFollowingMode());
		sb.append(" route=").append(routingHelper.isRouteCalculated());
		sb.append(" profile=").append(baseProfile(app.getSettings().getApplicationMode()));
		sb.append('\n');

		List<String> plugins = new ArrayList<>();
		for (OsmandPlugin plugin : PluginsHelper.getEnabledPlugins()) {
			plugins.add(plugin.getId());
		}
		sb.append("plugins: ").append(plugins.isEmpty() ? "-" : TextUtils.join(",", plugins)).append('\n');
	}

	// a custom profile can be named by the user, so only the built-in profile it derives from is reported
	@NonNull
	private static String baseProfile(@Nullable ApplicationMode mode) {
		if (mode == null) {
			return "?";
		}
		ApplicationMode parent = mode.getParent();
		return parent != null ? parent.getStringKey() : mode.getStringKey();
	}

	private static long uptimeSeconds() {
		return (SystemClock.uptimeMillis() - Process.getStartUptimeMillis()) / 1000;
	}

	@NonNull
	private static String memoryStat(@NonNull Debug.MemoryInfo memoryInfo, @NonNull String key) {
		try {
			String value = memoryInfo.getMemoryStat(key);
			return value != null ? kb(Long.parseLong(value)) : "?";
		} catch (RuntimeException e) {
			return "?";
		}
	}

	@NonNull
	private static String mb(long bytes) {
		return (bytes / (1024 * 1024)) + "MB";
	}

	@NonNull
	private static String kb(long kilobytes) {
		return (kilobytes / 1024) + "MB";
	}
}

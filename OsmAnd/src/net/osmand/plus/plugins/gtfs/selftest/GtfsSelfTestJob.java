package net.osmand.plus.plugins.gtfs.selftest;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Debug;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.gtfs.GtfsTransitPlugin;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.router.gtfs.selftest.ScenarioResult;
import net.osmand.router.gtfs.selftest.SelfTestEnv;
import net.osmand.router.gtfs.selftest.SelfTestReport;
import net.osmand.router.gtfs.selftest.SelfTestRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs the self-test scenarios inside the app process and writes report.json and report.txt. Used by the
 * foreground service (and by the receiver as a fallback). Never touches a view or an activity; the only UI
 * thread work is the plugin switch of the plugin-state scenario, which has no visual effect.
 * <p>
 * Output: {@code <out>/report.json}, {@code <out>/report.txt}, {@code <out>/progress.log}; default
 * {@code <externalFilesDir>/selftest/<timestamp>/}. The two report files are also copied to
 * {@code /sdcard/Download/gtfs-selftest/}. Zips are read from {@code <externalFilesDir>/selftest/zips/} first
 * and from {@code /sdcard/Download/gtfs_*.zip} second.
 */
public final class GtfsSelfTestJob {

	public static final String TAG = "GtfsSelfTest";
	private static final AtomicBoolean RUNNING = new AtomicBoolean();

	private GtfsSelfTestJob() {
	}

	public static void startThread(@NonNull OsmandApplication app, @Nullable String scenarios, @Nullable String out,
	                               @Nullable String options, @NonNull String via) {
		startThread(app, scenarios, out, options, via, null);
	}

	public static void startThread(@NonNull OsmandApplication app, @Nullable String scenarios, @Nullable String out,
	                               @Nullable String options, @NonNull String via, @Nullable Runnable whenDone) {
		if (!RUNNING.compareAndSet(false, true)) {
			Log.w(TAG, "a self-test run is already in progress, request ignored");
			if (whenDone != null) {
				whenDone.run();
			}
			return;
		}
		Thread t = new Thread(() -> {
			PowerManager.WakeLock lock = null;
			try {
				PowerManager pm = (PowerManager) app.getSystemService(android.content.Context.POWER_SERVICE);
				if (pm != null) {
					lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "osmand:gtfs-selftest");
					lock.acquire(60 * 60 * 1000L);
				}
				run(app, scenarios, out, options, via);
			} catch (Throwable e) {
				Log.e(TAG, "self-test crashed", e);
			} finally {
				if (lock != null && lock.isHeld()) {
					lock.release();
				}
				RUNNING.set(false);
				if (whenDone != null) {
					whenDone.run();
				}
			}
		}, "GtfsSelfTest");
		t.start();
	}

	private static File filesDir(OsmandApplication app) {
		File f = app.getExternalFilesDir(null);
		return f != null ? f : app.getFilesDir();
	}

	static void run(OsmandApplication app, String scenarios, String outArg, String optionsArg, String via) throws Exception {
		long startedAt = System.currentTimeMillis();
		String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date(startedAt));
		File root = new File(filesDir(app), "selftest");
		File outDir = outArg != null && !outArg.trim().isEmpty() ? new File(outArg.trim()) : new File(root, stamp);
		if (!outDir.isDirectory() && !outDir.mkdirs()) {
			Log.e(TAG, "cannot create the output directory " + outDir);
			outDir = new File(root, stamp);
			outDir.mkdirs();
		}
		final File out = outDir;
		final File progress = new File(out, "progress.log");
		SelfTestEnv.Sink sink = line -> {
			Log.i(TAG, line);
			appendLine(progress, new SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(new Date()) + " " + line);
		};
		sink.log("start via " + via + ", scenarios=" + scenarios + ", out=" + out + ", options=" + optionsArg);

		waitForApp(app, sink);

		File work = new File(root, "work");
		deleteTree(work);
		work.mkdirs();
		List<File> zips = findZips(root, sink);
		SelfTestEnv host = new SelfTestEnv(work, zips);
		host.sink = sink;
		host.locale = Locale.getDefault();
		parseOptions(optionsArg, host.options);
		host.memoryProbe = () -> {
			Map<String, Long> m = new LinkedHashMap<>();
			m.put("nativeHeapAllocatedMb", Debug.getNativeHeapAllocatedSize());
			m.put("nativeHeapSizeMb", Debug.getNativeHeapSize());
			m.put("pssMb", Debug.getPss() * 1024L);
			return m;
		};
		final android.content.Context ctx = app;
		host.deviceClock = new SelfTestEnv.ClockProbe() {
			@Override
			public boolean is24Hour() {
				return DateFormat.is24HourFormat(ctx);
			}

			@Override
			public String format(long epochSec, ZoneId zone) {
				return OsmAndFormatter.getTransportClockFormat(ctx).format(epochSec, zone);
			}
		};
		GtfsTransitPlugin plugin = PluginsHelper.getPlugin(GtfsTransitPlugin.class);
		if (plugin != null && plugin.getStore().hasIndex()) {
			host.preferredIndex = plugin.getStore().getIndexFile();
		}
		fillInfo(app, host, via, plugin);

		SelfTestRunner runner = new SelfTestRunner();
		runner.registerFirst("plugin-state", (h, r) -> new PluginStateScenario(app).run(h, r));
		SelfTestReport report = runner.run(host, scenarios, (partial, last) -> {
			// keep a readable partial text next to the progress log
			writeText(new File(out, "report.partial.txt"), partial.toText());
		});
		report.header.put("outDir", out.getPath());
		report.header.put("durationMs", report.endedMillis - report.startedMillis);

		String json = report.toJson(), text = report.toText();
		File jsonFile = new File(out, "report.json"), textFile = new File(out, "report.txt");
		writeText(textFile, text);
		File tmp = new File(out, "report.json.tmp");
		writeText(tmp, json);
		if (!tmp.renameTo(jsonFile)) { // the JSON is written last and atomically: a host polls for it
			writeText(jsonFile, json);
		}
		new File(out, "report.partial.txt").delete();
		sink.log("report written: " + jsonFile + " pass=" + report.count(ScenarioResult.Status.PASS) + " fail="
				+ report.count(ScenarioResult.Status.FAIL) + " skip=" + report.count(ScenarioResult.Status.SKIP));
		copyToDownloads(app, stamp, json, text, sink);
		if (!host.option("keep")) {
			deleteTree(work);
		}
		sink.log("done");
	}

	private static void waitForApp(OsmandApplication app, SelfTestEnv.Sink sink) throws InterruptedException {
		long deadline = System.currentTimeMillis() + 90_000;
		while (System.currentTimeMillis() < deadline) {
			if (app.getTransportRoutingHelper() != null && !app.isApplicationInitializing()) {
				return;
			}
			Thread.sleep(500);
		}
		sink.log("warning: the application did not finish initialising within 90 s");
	}

	private static void parseOptions(@Nullable String s, Map<String, String> into) {
		if (s == null) {
			return;
		}
		for (String kv : s.split(",")) {
			int eq = kv.indexOf('=');
			if (eq > 0) {
				into.put(kv.substring(0, eq).trim(), kv.substring(eq + 1).trim());
			} else if (!kv.trim().isEmpty()) {
				into.put(kv.trim(), "1");
			}
		}
	}

	private static List<File> findZips(File root, SelfTestEnv.Sink sink) {
		Map<String, File> byName = new LinkedHashMap<>();
		File[] dirs = {new File(root, "zips"), Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)};
		for (File dir : dirs) {
			File[] files = dir.listFiles((d, n) -> n.startsWith("gtfs_") && n.endsWith(".zip"));
			sink.log("zip search " + dir + ": " + (files == null ? "not listable" : files.length + " file(s)"));
			if (files != null) {
				for (File f : files) {
					if (f.canRead() && !byName.containsKey(f.getName())) {
						byName.put(f.getName(), f);
					}
				}
			}
		}
		List<File> zips = new ArrayList<>(byName.values());
		Collections.sort(zips);
		return zips;
	}

	private static void fillInfo(OsmandApplication app, SelfTestEnv host, String via, GtfsTransitPlugin plugin) {
		Map<String, Object> i = host.info;
		i.put("startedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(new Date()));
		i.put("via", via);
		i.put("device", Build.MANUFACTURER + " " + Build.MODEL);
		i.put("android", Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")");
		i.put("abis", String.join(",", Build.SUPPORTED_ABIS));
		i.put("package", app.getPackageName());
		try {
			i.put("appVersion", app.getPackageManager().getPackageInfo(app.getPackageName(), 0).versionName);
		} catch (Exception ignored) {
			// the version is only informative
		}
		i.put("debuggable", (app.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
		i.put("locale", Locale.getDefault().toLanguageTag());
		i.put("timeZone", java.util.TimeZone.getDefault().getID());
		i.put("device24h", DateFormat.is24HourFormat(app));
		android.app.ActivityManager am = (android.app.ActivityManager) app.getSystemService(android.content.Context.ACTIVITY_SERVICE);
		if (am != null) {
			i.put("memoryClassMb", am.getMemoryClass());
			i.put("largeMemoryClassMb", am.getLargeMemoryClass());
		}
		i.put("javaMaxHeapMb", Runtime.getRuntime().maxMemory() >> 20);
		PowerManager pm = (PowerManager) app.getSystemService(android.content.Context.POWER_SERVICE);
		if (pm != null) {
			i.put("screenInteractiveAtStart", pm.isInteractive());
		}
		i.put("freeSpaceMb", filesDir(app).getUsableSpace() >> 20);
		i.put("pluginPresent", plugin != null);
		if (plugin != null) {
			i.put("pluginEnabledAtStart", plugin.isEnabled());
			i.put("realIndexPresent", plugin.getStore().hasIndex());
			i.put("realFeeds", plugin.getStore().listFeeds().size());
		}
	}

	private static void copyToDownloads(OsmandApplication app, String stamp, String json, String text, SelfTestEnv.Sink sink) {
		File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "gtfs-selftest");
		try {
			dir.mkdirs();
			writeBytes(new File(dir, "report-" + stamp + ".json"), json);
			writeBytes(new File(dir, "report-" + stamp + ".txt"), text);
			sink.log("copied the report to " + dir);
			return;
		} catch (IOException e) {
			sink.log("direct copy to Downloads failed (" + e.getMessage() + "), trying MediaStore");
		}
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
			return;
		}
		try {
			insertDownload(app, "report-" + stamp + ".json", "application/json", json);
			insertDownload(app, "report-" + stamp + ".txt", "text/plain", text);
			sink.log("copied the report to Download/gtfs-selftest through MediaStore");
		} catch (IOException | RuntimeException e) {
			sink.log("copy to Downloads failed: " + e);
		}
	}

	private static void insertDownload(OsmandApplication app, String name, String mime, String body) throws IOException {
		ContentResolver cr = app.getContentResolver();
		ContentValues v = new ContentValues();
		v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
		v.put(MediaStore.MediaColumns.MIME_TYPE, mime);
		v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/gtfs-selftest");
		Uri uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
		if (uri == null) {
			throw new IOException("MediaStore insert returned null");
		}
		try (OutputStream o = cr.openOutputStream(uri)) {
			if (o == null) {
				throw new IOException("no output stream");
			}
			o.write(body.getBytes(StandardCharsets.UTF_8));
		}
	}

	private static void writeBytes(File f, String body) throws IOException {
		try (FileOutputStream o = new FileOutputStream(f)) {
			o.write(body.getBytes(StandardCharsets.UTF_8));
		}
	}

	private static void writeText(File f, String body) {
		try {
			writeBytes(f, body);
		} catch (IOException e) {
			Log.w(TAG, "cannot write " + f + ": " + e);
		}
	}

	private static void appendLine(File f, String line) {
		try (FileOutputStream o = new FileOutputStream(f, true)) {
			o.write((line + "\n").getBytes(StandardCharsets.UTF_8));
		} catch (IOException ignored) {
			// logcat has the same line
		}
	}

	private static void deleteTree(File f) {
		File[] kids = f.listFiles();
		if (kids != null) {
			for (File k : kids) {
				deleteTree(k);
			}
		}
		f.delete();
	}
}

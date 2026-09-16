package net.osmand.plus.feedback;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.FileUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class NativeCrashHandler {

	private static final Log log = PlatformUtil.getLog(NativeCrashHandler.class);

	private static final int MAX_CRASH_LOGS = 5;
	private static final String CRASH_LOG_EXTENSION = ".pb";
	private static final String CRASH_LOG_NAME = "native_exception";
	private static final String ANR_LOG_EXTENSION = ".txt";
	private static final String ANR_LOG_NAME = "anr_trace";
	private static final Comparator<File> NEWEST_FIRST = Comparator.comparingLong(File::lastModified).reversed();

	private final OsmandApplication app;

	NativeCrashHandler(@NonNull OsmandApplication app) {
		this.app = app;
	}

	boolean hasCrashLogs() {
		return !getSavedCrashLogs().isEmpty() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !getExitReasonsApi30().isEmpty());
	}

	// time of the newest native crash or ANR known to the system, 0 if none
	long getLastCrashTimestamp() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			List<ApplicationExitInfo> exitReasons = getExitReasonsApi30();
			return exitReasons.isEmpty() ? 0 : exitReasons.get(0).getTimestamp();
		}
		return 0;
	}

	// newest first
	@NonNull
	List<File> collectCrashLogs() {
		List<File> files = getSavedCrashLogs();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			saveCrashLogsApi30(files);
		}
		return files;
	}

	@NonNull
	private List<File> getSavedCrashLogs() {
		List<File> files = FileUtils.collectFiles(app.getAppPath(), CRASH_LOG_EXTENSION, new ArrayList<>());
		files.removeIf(file -> !file.getName().startsWith(CRASH_LOG_NAME));
		List<File> anrFiles = FileUtils.collectFiles(app.getAppPath(), ANR_LOG_EXTENSION, new ArrayList<>());
		anrFiles.removeIf(file -> !file.getName().startsWith(ANR_LOG_NAME));
		files.addAll(anrFiles);
		files.removeIf(file -> !FileUtils.isNonEmptyFile(file));
		files.sort(NEWEST_FIRST);
		return new ArrayList<>(files.subList(0, Math.min(files.size(), MAX_CRASH_LOGS)));
	}

	// native crash traces (tombstones) are available since API 31, ANR traces since API 30
	@RequiresApi(api = Build.VERSION_CODES.R)
	@NonNull
	private List<ApplicationExitInfo> getExitReasonsApi30() {
		ActivityManager activityManager = app.getSystemService(ActivityManager.class);
		if (activityManager == null) {
			return Collections.emptyList();
		}
		try {
			List<ApplicationExitInfo> exitReasons = new ArrayList<>(activityManager.getHistoricalProcessExitReasons(null, 0, 0));
			exitReasons.removeIf(exitInfo -> !isCrashWithTrace(exitInfo));
			return exitReasons.subList(0, Math.min(exitReasons.size(), MAX_CRASH_LOGS));
		} catch (RuntimeException e) {
			log.error(e);
		}
		return Collections.emptyList();
	}

	@RequiresApi(api = Build.VERSION_CODES.R)
	private static boolean isCrashWithTrace(@NonNull ApplicationExitInfo exitInfo) {
		int reason = exitInfo.getReason();
		return reason == ApplicationExitInfo.REASON_ANR
				|| (reason == ApplicationExitInfo.REASON_CRASH_NATIVE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S);
	}

	@RequiresApi(api = Build.VERSION_CODES.R)
	private void saveCrashLogsApi30(@NonNull List<File> savedLogs) {
		for (ApplicationExitInfo exitInfo : getExitReasonsApi30()) {
			if (savedLogs.stream().anyMatch(file -> Math.abs(file.lastModified() - exitInfo.getTimestamp()) < 1000)) {
				continue;
			}
			File file = createCrashLogFile(exitInfo.getReason() == ApplicationExitInfo.REASON_ANR);
			if (saveCrashLog(exitInfo, file)) {
				savedLogs.add(file);
				savedLogs.sort(NEWEST_FIRST);

				if (savedLogs.size() > MAX_CRASH_LOGS) {
					Algorithms.removeAllFiles(savedLogs.remove(savedLogs.size() - 1));
				}
			}
		}
	}

	@NonNull
	private File createCrashLogFile(boolean anr) {
		String name = anr ? ANR_LOG_NAME : CRASH_LOG_NAME;
		String extension = anr ? ANR_LOG_EXTENSION : CRASH_LOG_EXTENSION;
		String fileName = FileUtils.createUniqueFileName(app, name, "", extension);
		return app.getAppPath(fileName + extension);
	}

	@RequiresApi(api = Build.VERSION_CODES.R)
	private boolean saveCrashLog(@NonNull ApplicationExitInfo exitInfo, @NonNull File file) {
		File parent = file.getParentFile();
		if (parent == null || !parent.canWrite()) {
			return false;
		}
		try (InputStream inputStream = exitInfo.getTraceInputStream()) {
			if (inputStream == null) {
				return false;
			}
			try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
				Algorithms.streamCopy(inputStream, outputStream);
			}
			if (FileUtils.isNonEmptyFile(file) && file.setLastModified(exitInfo.getTimestamp())) {
				return true;
			}
		} catch (IOException | RuntimeException e) {
			log.error(e);
		}
		Algorithms.removeAllFiles(file);
		return false;
	}
}

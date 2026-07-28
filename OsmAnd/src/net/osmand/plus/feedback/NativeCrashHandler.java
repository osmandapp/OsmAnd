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
	private static final Comparator<File> NEWEST_FIRST = Comparator.comparingLong(File::lastModified).reversed();

	private final OsmandApplication app;

	NativeCrashHandler(@NonNull OsmandApplication app) {
		this.app = app;
	}

	boolean hasCrashLogs() {
		return !getSavedCrashLogs().isEmpty() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !getExitReasonsApi31().isEmpty());
	}

	@NonNull
	List<File> collectCrashLogs() {
		List<File> files = getSavedCrashLogs();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			saveCrashLogsApi31(files);
		}
		return files;
	}

	@NonNull
	private List<File> getSavedCrashLogs() {
		List<File> files = FileUtils.collectFiles(app.getAppPath(), CRASH_LOG_EXTENSION, new ArrayList<>());
		files.removeIf(file -> !file.getName().startsWith(CRASH_LOG_NAME) || !FileUtils.isNonEmptyFile(file));
		files.sort(NEWEST_FIRST);
		return new ArrayList<>(files.subList(0, Math.min(files.size(), MAX_CRASH_LOGS)));
	}

	@RequiresApi(api = Build.VERSION_CODES.S)
	@NonNull
	private List<ApplicationExitInfo> getExitReasonsApi31() {
		ActivityManager activityManager = app.getSystemService(ActivityManager.class);
		if (activityManager == null) {
			return Collections.emptyList();
		}
		try {
			List<ApplicationExitInfo> exitReasons = new ArrayList<>(activityManager.getHistoricalProcessExitReasons(null, 0, 0));
			exitReasons.removeIf(exitInfo -> exitInfo.getReason() != ApplicationExitInfo.REASON_CRASH_NATIVE);
			return exitReasons.subList(0, Math.min(exitReasons.size(), MAX_CRASH_LOGS));
		} catch (RuntimeException e) {
			log.error(e);
		}
		return Collections.emptyList();
	}

	@RequiresApi(api = Build.VERSION_CODES.S)
	private void saveCrashLogsApi31(@NonNull List<File> savedLogs) {
		for (ApplicationExitInfo exitInfo : getExitReasonsApi31()) {
			if (savedLogs.stream().anyMatch(file -> Math.abs(file.lastModified() - exitInfo.getTimestamp()) < 1000)) {
				continue;
			}
			File file = createCrashLogFile();
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
	private File createCrashLogFile() {
		String fileName = FileUtils.createUniqueFileName(app, CRASH_LOG_NAME, "", CRASH_LOG_EXTENSION);
		return app.getAppPath(fileName + CRASH_LOG_EXTENSION);
	}

	@RequiresApi(api = Build.VERSION_CODES.S)
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
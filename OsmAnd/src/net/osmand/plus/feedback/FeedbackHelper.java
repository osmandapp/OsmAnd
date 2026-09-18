package net.osmand.plus.feedback;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.PlatformUtil;
import net.osmand.StreamWriter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.plus.utils.AndroidNetworkUtils.NetworkResult;
import net.osmand.plus.utils.AndroidNetworkUtils.OnFileUploadCallback;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FileUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FeedbackHelper {

	private static final Log log = PlatformUtil.getLog(FeedbackHelper.class);

	public static final String EXCEPTION_PATH = "exception.log";
	private static final String STATE_PATH = "state.txt";
	private static final String EXIT_INFO_PATH = "exit_info.txt";
	private static final String CRASH_REPORT_URL = "https://osmand.net/api/crash-report";
	private static final int MAX_SYSTEM_CRASH_LOGS_IN_REPORT = 3;
	private static final long MAX_MEMORY_LOG_IN_REPORT = 64 * 1024;
	private static final long MAX_EXCEPTION_LOG_IN_REPORT = 10 * 1024 * 1024;

	private final OsmandApplication app;
	private final ExceptionHandler exceptionHandler;
	private final NativeCrashHandler nativeCrashHandler;

	public FeedbackHelper(@NonNull OsmandApplication app) {
		this.app = app;
		exceptionHandler = new ExceptionHandler(app);
		nativeCrashHandler = new NativeCrashHandler(app);
	}

	@Nullable
	public File getCrashLog() {
		return exceptionHandler.getCrashLog();
	}

	public boolean hasCrashLogs() {
		return getCrashLog() != null || nativeCrashHandler.hasCrashLogs();
	}

	@NonNull
	private List<File> collectCrashLogFiles() {
		List<File> files = nativeCrashHandler.collectCrashLogs();
		File crashLog = getCrashLog();
		if (crashLog != null) {
			files.add(0, crashLog);
		}
		return files;
	}

	public void sendCrashLog() {
		sendCrashLog(collectCrashLogFiles());
	}

	public void sendCrashLog(@NonNull File file) {
		sendCrashLog(Collections.singletonList(file));
	}

	private void sendCrashLog(@NonNull List<File> files) {
		if (files.isEmpty()) {
			app.showToastMessage(R.string.data_is_not_available);
			return;
		}
		String deviceInfo = getDeviceInfo();
		Intent intent = new Intent(files.size() > 1 ? Intent.ACTION_SEND_MULTIPLE : Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"crash@osmand.net"});

		if (files.size() == 1) {
			intent.putExtra(Intent.EXTRA_STREAM, AndroidUtils.getUriForFile(app, files.get(0)));
		} else {
			ArrayList<Uri> uris = new ArrayList<>(files.size());
			for (File file : files) {
				uris.add(AndroidUtils.getUriForFile(app, file));
			}
			intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
		}
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		intent.setType("vnd.android.cursor.dir/email");
		intent.putExtra(Intent.EXTRA_SUBJECT, "OsmAnd bug");
		intent.putExtra(Intent.EXTRA_TEXT, deviceInfo);
		Intent chooserIntent = Intent.createChooser(intent, app.getString(R.string.send_report));
		chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		AndroidUtils.startActivityIfSafe(app, intent, chooserIntent);
	}

	// a native crash or ANR happened after the dialog was shown last time (and after the app was installed / updated)
	public boolean hasNewSystemCrash() {
		return nativeCrashHandler.getLastCrashTimestamp() > getShownSystemCrashTime();
	}

	public void markSystemCrashesShown() {
		long timestamp = nativeCrashHandler.getLastCrashTimestamp();
		if (timestamp > 0) {
			app.getSettings().LAST_SHOWN_SYSTEM_CRASH_TIME.set(timestamp);
		}
	}

	private long getShownSystemCrashTime() {
		long shown = app.getSettings().LAST_SHOWN_SYSTEM_CRASH_TIME.get();
		PackageInfo info = getPackageInfo();
		return info != null ? Math.max(shown, info.lastUpdateTime) : shown;
	}

	// zip of exception.log and the newest system crash traces (tombstones, ANR traces) uploaded to osmand.net
	public void sendCrashReport(@Nullable CallbackWithObject<Boolean> callback) {
		Map<String, String> params = new LinkedHashMap<>();
		PackageInfo info = getPackageInfo();
		params.put("platform", "android");
		params.put("version", info != null && info.versionName != null ? info.versionName : Version.getAppVersion(app));
		params.put("osversion", Build.VERSION.RELEASE);

		StreamWriter writer = (outputStream, progress) -> writeCrashReport(outputStream);
		AndroidNetworkUtils.uploadFileAsync(CRASH_REPORT_URL, writer, "crash_report.zip", false, params, null,
				new OnFileUploadCallback() {
					@Override
					public void onFileUploadDone(@NonNull NetworkResult result) {
						if (result.getError() != null) {
							log.error("Crash report upload failed: " + result.getError());
						}
						if (callback != null) {
							callback.processResult(result.getError() == null);
						}
					}
				});
	}

	private void writeCrashReport(@NonNull OutputStream outputStream) throws IOException {
		ZipOutputStream zip = new ZipOutputStream(outputStream);
		putZipEntry(zip, STATE_PATH, CrashReportState.build(app).getBytes());
		String exitInfo = nativeCrashHandler.buildExitInfo();
		if (!Algorithms.isEmpty(exitInfo)) {
			putZipEntry(zip, EXIT_INFO_PATH, exitInfo.getBytes());
		}
		File memoryLog = MemoryLog.getFile(app);
		if (FileUtils.isNonEmptyFile(memoryLog)) {
			putZipEntry(zip, memoryLog, MAX_MEMORY_LOG_IN_REPORT);
		}
		File crashLog = getCrashLog();
		if (crashLog != null) {
			putZipEntry(zip, crashLog, MAX_EXCEPTION_LOG_IN_REPORT);
		}
		List<File> files = nativeCrashHandler.collectCrashLogs();
		for (File file : files.subList(0, Math.min(files.size(), MAX_SYSTEM_CRASH_LOGS_IN_REPORT))) {
			putZipEntry(zip, file, Long.MAX_VALUE);
		}
		zip.finish();
	}

	private static void putZipEntry(@NonNull ZipOutputStream zip, @NonNull String name, @NonNull byte[] content) throws IOException {
		zip.putNextEntry(new ZipEntry(name));
		zip.write(content);
		zip.closeEntry();
	}

	// writes at most the last maxLength bytes of the file
	private static void putZipEntry(@NonNull ZipOutputStream zip, @NonNull File file, long maxLength) throws IOException {
		zip.putNextEntry(new ZipEntry(file.getName()));
		try (InputStream in = new FileInputStream(file)) {
			long skip = file.length() - maxLength;
			if (skip > 0) {
				in.skip(skip);
			}
			Algorithms.streamCopy(in, zip);
		}
		zip.closeEntry();
	}

	public void sendSupportEmail(@NonNull String screenName) {
		sendSupportEmail(screenName, null);
	}

	public void sendSupportEmail(@NonNull String screenName, @Nullable String additional) {
		String info = getDeviceInfo();
		if (!Algorithms.isEmpty(additional)) {
			info = info + "\n" + additional;
		}
		Intent emailIntent = new Intent(Intent.ACTION_SEND)
				.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				.putExtra(Intent.EXTRA_EMAIL, new String[] {"support@osmand.net"})
				.putExtra(Intent.EXTRA_SUBJECT, screenName)
				.putExtra(Intent.EXTRA_TEXT, info);
		emailIntent.setSelector(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")));
		AndroidUtils.startActivityIfSafe(app, emailIntent);
	}

	public String getDeviceInfo() {
		StringBuilder text = new StringBuilder();
		text.append("Device : ").append(Build.DEVICE);
		text.append("\nBrand : ").append(Build.BRAND);
		text.append("\nManufacturer : ").append(Build.MANUFACTURER);
		text.append("\nModel : ").append(Build.MODEL);
		text.append("\nProduct : ").append(Build.PRODUCT);
		text.append("\nBuild : ").append(Build.DISPLAY);
		text.append("\nVersion : ").append(Build.VERSION.RELEASE);
		text.append("\nApp Version : ").append(Version.getAppName(app));

		PackageInfo info = getPackageInfo();
		if (info != null) {
			text.append("\nApk Version : ").append(info.versionName).append(" ").append(info.versionCode);
		}
		return text.toString();
	}

	public void setupExceptionHandler() {
		exceptionHandler.installAsDefaultHandler();
	}

	public void saveExceptionSilent(@NonNull Thread thread, @NonNull Throwable throwable) {
		try {
			exceptionHandler.saveException(thread, throwable);
		} catch (IOException e) {
			log.error(e);
		}
	}

	@Nullable
	public PackageInfo getPackageInfo() {
		try {
			return app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			log.error(e);
			return null;
		}
	}
}
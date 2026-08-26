package net.osmand.plus.feedback;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FeedbackHelper {

	private static final Log log = PlatformUtil.getLog(FeedbackHelper.class);

	public static final String EXCEPTION_PATH = "exception.log";

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
package net.osmand.plus.feedback;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;

public class FeedbackHelper {

	private static final Log log = PlatformUtil.getLog(FeedbackHelper.class);

	public static final String EXCEPTION_PATH = "exception.log";

	private final OsmandApplication app;

	public FeedbackHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public void sendCrashLog() {
		sendCrashLog(app.getAppPath(EXCEPTION_PATH));
	}

	public void sendCrashLog(@NonNull File file) {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"crash@osmand.net"});
		intent.putExtra(Intent.EXTRA_STREAM, AndroidUtils.getUriForFile(app, file));
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		intent.setType("vnd.android.cursor.dir/email");
		intent.putExtra(Intent.EXTRA_SUBJECT, "OsmAnd bug");
		intent.putExtra(Intent.EXTRA_TEXT, getDeviceInfo());
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
		text.append("\nModel : ").append(Build.MODEL);
		text.append("\nProduct : ").append(Build.PRODUCT);
		text.append("\nBuild : ").append(Build.DISPLAY);
		text.append("\nVersion : ").append(Build.VERSION.RELEASE);
		text.append("\nApp Version : ").append(Version.getAppName(app));
		try {
			PackageInfo info = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
			if (info != null) {
				text.append("\nApk Version : ").append(info.versionName).append(" ").append(info.versionCode);
			}
		} catch (NameNotFoundException e) {
			log.error(e);
		}
		return text.toString();
	}

	public void setExceptionHandler() {
		UncaughtExceptionHandler uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
		if (!(uncaughtExceptionHandler instanceof DefaultExceptionHandler)) {
			Thread.setDefaultUncaughtExceptionHandler(new DefaultExceptionHandler());
		}
	}

	public void saveExceptionSilent(@NonNull Thread thread, @NonNull Throwable throwable) {
		try {
			saveException(thread, throwable);
		} catch (IOException e) {
			log.error(e);
		}
	}

	private void saveException(@NonNull Thread thread, @NonNull Throwable throwable) throws IOException {
		File file = app.getAppPath(EXCEPTION_PATH);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintStream printStream = new PrintStream(out);
		throwable.printStackTrace(printStream);
		StringBuilder msg = new StringBuilder();
		msg.append("Version  ")
				.append(Version.getFullVersion(app))
				.append("\n")
				.append(DateFormat.format("dd.MM.yyyy h:mm:ss", System.currentTimeMillis()));
		try {
			PackageInfo info = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
			if (info != null) {
				msg.append("\nApk Version : ").append(info.versionName).append(" ").append(info.versionCode);
			}
		} catch (Throwable e) {
		}
		msg.append("\n")
				.append("Exception occurred in thread ")
				.append(thread)
				.append(" : \n")
				.append(out);

		if (file.getParentFile().canWrite()) {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
			writer.write(msg.toString());
			writer.close();
		}
	}

	private class DefaultExceptionHandler implements UncaughtExceptionHandler {

		private final UncaughtExceptionHandler defaultHandler;
		private final PendingIntent intent;

		public DefaultExceptionHandler() {
			defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
			intent = PendingIntent.getActivity(app.getBaseContext(), 0,
					new Intent(app.getBaseContext(),
							app.getAppCustomization().getMapActivity()), PendingIntent.FLAG_IMMUTABLE);
		}

		@Override
		public void uncaughtException(@NonNull Thread thread, @NonNull Throwable ex) {
			try {
				saveException(thread, ex);
				if (app.getRoutingHelper().isFollowingMode()) {
					AlarmManager mgr = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
					mgr.setExact(AlarmManager.RTC, System.currentTimeMillis() + 2000, intent);
					System.exit(2);
				}
				defaultHandler.uncaughtException(thread, ex);
			} catch (Exception e) {
				// swallow all exceptions
				android.util.Log.e(PlatformUtil.TAG, "Exception while handle other exception", e);
			}
		}
	}
}
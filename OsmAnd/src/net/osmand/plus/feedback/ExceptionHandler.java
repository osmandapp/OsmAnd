package net.osmand.plus.feedback;

import static net.osmand.plus.feedback.FeedbackHelper.EXCEPTION_PATH;

import android.content.pm.PackageInfo;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.activities.RestartActivity;
import net.osmand.plus.utils.FileUtils;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;

class ExceptionHandler implements UncaughtExceptionHandler {

	private final OsmandApplication app;
	private final UncaughtExceptionHandler defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();

	ExceptionHandler(@NonNull OsmandApplication app) {
		this.app = app;
	}

	@Nullable
	File getCrashLog() {
		File file = app.getAppPath(EXCEPTION_PATH);
		return FileUtils.isNonEmptyFile(file) ? file : null;
	}

	void installAsDefaultHandler() {
		UncaughtExceptionHandler uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
		if (!(uncaughtExceptionHandler instanceof ExceptionHandler)) {
			Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(app));
		}
	}

	@Override
	public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
		try {
			saveException(thread, throwable);
			if (app.getRoutingHelper().isFollowingMode()) {
				RestartActivity.doRestartSilent(app);
			}
			if (defaultExceptionHandler != null) {
				defaultExceptionHandler.uncaughtException(thread, throwable);
			}
		} catch (Exception e) {
			// swallow all exceptions
			android.util.Log.e(PlatformUtil.TAG, "Exception while handle other exception", e);
		}
	}

	public void saveException(@NonNull Thread thread, @NonNull Throwable throwable) throws IOException {
		File file = app.getAppPath(EXCEPTION_PATH);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintStream printStream = new PrintStream(out);
		throwable.printStackTrace(printStream);

		StringBuilder builder = new StringBuilder();
		builder.append("Version  ")
				.append(Version.getFullVersion(app))
				.append("\n")
				.append(DateFormat.format("dd.MM.yyyy h:mm:ss", System.currentTimeMillis()));

		PackageInfo info = app.getFeedbackHelper().getPackageInfo();
		if (info != null) {
			builder.append("\nApk Version : ").append(info.versionName).append(" ").append(info.versionCode);
		}
		builder.append("\n")
				.append("Exception occurred in thread ")
				.append(thread)
				.append(" : \n")
				.append(out);

		if (file.getParentFile().canWrite()) {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
			writer.write(builder.toString());
			writer.close();
		}
	}
}

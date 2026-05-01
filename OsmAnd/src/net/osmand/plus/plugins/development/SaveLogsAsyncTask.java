package net.osmand.plus.plugins.development;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.AndroidUtils;

import org.apache.commons.logging.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.ref.WeakReference;
import java.util.Collection;

public class SaveLogsAsyncTask extends AsyncTask<Void, String, File> {

	private static final Log log = PlatformUtil.getLog(SaveLogsAsyncTask.class);

	public static final String LOGCAT_PATH = "logcat.log";

	private final OsmandApplication app;
	private final WeakReference<BaseLogcatActivity> activityRef;
	private final Collection<String> logs;

	SaveLogsAsyncTask(@NonNull BaseLogcatActivity activity, @NonNull Collection<String> logs) {
		this.app = AndroidUtils.getApp(activity);
		this.activityRef = new WeakReference<>(activity);
		this.logs = logs;
	}

	@Override
	protected void onPreExecute() {
		BaseLogcatActivity activity = activityRef.get();
		if (AndroidUtils.isActivityNotDestroyed(activity)) {
			activity.setSupportProgressBarIndeterminateVisibility(true);
		}
	}

	@Override
	protected File doInBackground(Void... voids) {
		File file = app.getAppPath(LOGCAT_PATH);
		try {
			if (file.exists()) {
				file.delete();
			}
			String deviceInfo = app.getFeedbackHelper().getDeviceInfo();
			StringBuilder builder = new StringBuilder(deviceInfo);
			builder.append("\n");

			for (String log : logs) {
				builder.append(log);
				builder.append("\n");
			}
			if (file.getParentFile().canWrite()) {
				BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
				writer.write(builder.toString());
				writer.close();
			}
		} catch (Exception e) {
			log.error(e);
		}
		return file;
	}

	@Override
	protected void onPostExecute(File file) {
		BaseLogcatActivity activity = activityRef.get();
		if (AndroidUtils.isActivityNotDestroyed(activity)) {
			activity.setSupportProgressBarIndeterminateVisibility(false);
		}
		app.getFeedbackHelper().sendCrashLog(file);
	}
}

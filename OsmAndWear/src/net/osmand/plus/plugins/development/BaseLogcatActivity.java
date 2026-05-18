package net.osmand.plus.plugins.development;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.activities.ActionBarProgressActivity;

import org.apache.commons.logging.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class BaseLogcatActivity extends ActionBarProgressActivity implements LogcatMessageListener {

	private static final Log log = PlatformUtil.getLog(BaseLogcatActivity.class);

	public static final String LOGCAT_PATH = "logcat.log";

	protected final List<String> logs = new ArrayList<>();

	private LogcatAsyncTask logcatAsyncTask;

	@NonNull
	protected abstract String getFilterLevel();

	protected void onLogEntryAdded() {
	}

	protected void startSaveLogsAsyncTask() {
		SaveLogsAsyncTask saveLogsAsyncTask = new SaveLogsAsyncTask(this, logs);
		saveLogsAsyncTask.execute();
	}

	protected void startLogcatAsyncTask() {
		logcatAsyncTask = new LogcatAsyncTask(this, getFilterLevel());
		logcatAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	protected void stopLogcatAsyncTask() {
		if (logcatAsyncTask != null) {
			logcatAsyncTask.stop();
		}
	}

	@Override
	public void onLogcatLogs(String filterLevel, List<String> logs) {
		this.logs.addAll(logs);
		onLogEntryAdded();
	}

	private static class SaveLogsAsyncTask extends AsyncTask<Void, String, File> {

		private final WeakReference<BaseLogcatActivity> activityRef;
		private final Collection<String> logs;

		private SaveLogsAsyncTask(BaseLogcatActivity activity, Collection<String> logs) {
			this.activityRef = new WeakReference<>(activity);
			this.logs = logs;
		}

		@Override
		protected void onPreExecute() {
			activityRef.get().setSupportProgressBarIndeterminateVisibility(true);
		}

		@Override
		protected File doInBackground(Void... voids) {
			File file = activityRef.get().getMyApplication().getAppPath(LOGCAT_PATH);
			try {
				if (file.exists()) {
					file.delete();
				}
				StringBuilder stringBuilder = new StringBuilder();
				for (String log : logs) {
					stringBuilder.append(log);
					stringBuilder.append("\n");
				}
				if (file.getParentFile().canWrite()) {
					BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
					writer.write(stringBuilder.toString());
					writer.close();
				}
			} catch (Exception e) {
				log.error(e);
			}

			return file;
		}

		@Override
		protected void onPostExecute(File file) {
			BaseLogcatActivity activity = this.activityRef.get();
			activity.setSupportProgressBarIndeterminateVisibility(false);
			activity.getMyApplication().getFeedbackHelper().sendCrashLog(file);
		}
	}
}
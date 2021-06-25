package net.osmand.plus.development;

import android.os.AsyncTask;

import net.osmand.PlatformUtil;
import net.osmand.plus.activities.ActionBarProgressActivity;

import org.apache.commons.logging.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import androidx.annotation.NonNull;

public abstract class BaseLogcatActivity extends ActionBarProgressActivity {

	private static final Log log = PlatformUtil.getLog(BaseLogcatActivity.class);

	public static final String LOGCAT_PATH = "logcat.log";
	public static int MAX_BUFFER_LOG = 10000;

	protected final List<String> logs = new ArrayList<>();

	private LogcatAsyncTask logcatAsyncTask;

	@NonNull
	protected abstract String getFilterLevel();

	protected abstract void onLogEntryAdded();

	@Override
	protected void onResume() {
		super.onResume();
		startLogcatAsyncTask();
	}

	@Override
	protected void onPause() {
		super.onPause();
		stopLogcatAsyncTask();
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
		if (logcatAsyncTask != null && logcatAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
			logcatAsyncTask.cancel(false);
			logcatAsyncTask.stopLogging();
		}
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
			activity.getMyApplication().sendCrashLog(file);
			}
		}

	private static class LogcatAsyncTask extends AsyncTask<Void, String, Void> {

		private Process processLogcat;
		private final WeakReference<BaseLogcatActivity> activityRef;
		private final String filterLevel;

		private LogcatAsyncTask(BaseLogcatActivity activity, String filterLevel) {
			this.activityRef = new WeakReference<>(activity);
			this.filterLevel = filterLevel;
		}

		@Override
		protected Void doInBackground(Void... voids) {
			try {
				String pid = String.valueOf(android.os.Process.myPid());
				String[] command =  {"logcat", filterLevel, "--pid=" + pid, "-T", String.valueOf(MAX_BUFFER_LOG)};

				processLogcat = Runtime.getRuntime().exec(command);
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(processLogcat.getInputStream()));

				String line;
				while ((line = bufferedReader.readLine()) != null && activityRef.get() != null) {
					if (isCancelled()) {
						break;
					}
					publishProgress(line);
				}
				stopLogging();
			} catch (IOException e) {
				// ignore
			} catch (Exception e) {
				log.error(e);
			}

			return null;
		}

		@Override
		protected void onProgressUpdate(String... values) {
			if (values.length > 0 && !isCancelled()) {
				BaseLogcatActivity activity = activityRef.get();
				if (activity != null) {
					activity.logs.addAll(Arrays.asList(values));
					activity.onLogEntryAdded();
				}
			}
		}

		private void stopLogging() {
			if (processLogcat != null) {
				processLogcat.destroy();
			}
		}
	}
}
package net.osmand.plus.plugins.development;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;

import org.apache.commons.logging.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public class LogcatAsyncTask extends AsyncTask<Void, String, Void> {

	private static final Log log = PlatformUtil.getLog(LogcatAsyncTask.class);

	public static int MAX_BUFFER_LOG = 10000;

	private LogcatMessageListener mListener;
	private final String mFilterLevel;
	private Process mLogcat;

	public LogcatAsyncTask(@NonNull LogcatMessageListener listener, @NonNull String filterLevel) {
		mListener = listener;
		mFilterLevel = filterLevel;
	}

	@Override
	protected Void doInBackground(Void... voids) {
		try {
			String pid = String.valueOf(android.os.Process.myPid());
			String[] command = {"logcat", mFilterLevel, "--pid=" + pid, "-T", String.valueOf(MAX_BUFFER_LOG)};
			mLogcat = Runtime.getRuntime().exec(command);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(mLogcat.getInputStream()));

			String line;
			while ((line = bufferedReader.readLine()) != null) {
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
			mListener.onLogcatLogs(mFilterLevel, Arrays.asList(values));
		}
	}

	public void stop() {
		if (getStatus() == AsyncTask.Status.RUNNING) {
			cancel(false);
			stopLogging();
			mListener = null;
		}
	}

	private void stopLogging() {
		if (mLogcat != null) {
			mLogcat.destroy();
		}
	}
}

package net.osmand.plus.plugins.development;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.activities.ActionBarProgressActivity;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseLogcatActivity extends ActionBarProgressActivity implements LogcatMessageListener {

	private static final Log log = PlatformUtil.getLog(BaseLogcatActivity.class);

	protected final List<String> logs = new ArrayList<>();

	private LogcatAsyncTask logcatAsyncTask;

	@NonNull
	protected abstract String getFilterLevel();

	protected void onLogEntryAdded() {
	}

	protected void startSaveLogsAsyncTask() {
		OsmAndTaskManager.executeTask(new SaveLogsAsyncTask(this, logs));
	}

	protected void startLogcatAsyncTask() {
		logcatAsyncTask = new LogcatAsyncTask(this, getFilterLevel());
		OsmAndTaskManager.executeTask(logcatAsyncTask);
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
}
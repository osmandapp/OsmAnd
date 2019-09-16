package net.osmand.plus.development;

import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandActionBarActivity;

import org.apache.commons.logging.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LogcatActivity extends OsmandActionBarActivity {

	public static int MAX_BUFFER_LOG = 1000;

	private static final Log log = PlatformUtil.getLog(LogcatActivity.class);

	private LogcatAsyncTask logcatAsyncTask;
	private Map<Integer, String> logsMap;
	private LogcatAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		OsmandApplication app = (OsmandApplication) getApplication();
		app.applyTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.recyclerview);

		ActionBar supportActionBar = getSupportActionBar();
		if (supportActionBar != null) {
			supportActionBar.setTitle(R.string.logcat_buffer);
			supportActionBar.setElevation(5.0f);
		}

		logsMap = new LinkedHashMap<Integer, String>() {
			@Override
			protected boolean removeEldestEntry(Entry<Integer, String> eldest) {
				return size() > MAX_BUFFER_LOG;
			}
		};

		adapter = new LogcatAdapter();

		RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		recyclerView.setAdapter(adapter);

		int colorResId = AndroidUtils.resolveAttribute(app, R.attr.divider_color_basic);
		if (colorResId != 0) {
			DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
			dividerItemDecoration.setDrawable(new ColorDrawable(ContextCompat.getColor(app, colorResId)));

			recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
		}
	}

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

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
			case android.R.id.home:
				finish();
				return true;

		}
		return false;
	}

	private void onNewLogEntry(int index, String logEntry) {
		logsMap.put(index, logEntry);

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				adapter.setLogs(logsMap.values());
			}
		});
	}

	public void startLogcatAsyncTask() {
		logcatAsyncTask = new LogcatAsyncTask(this, "*:E");
		logcatAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void stopLogcatAsyncTask() {
		if (logcatAsyncTask != null && logcatAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
			logcatAsyncTask.cancel(false);
			logcatAsyncTask.stopLogging();
		}
	}

	private class LogcatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

		private List<String> logs = new ArrayList<>();

		@NonNull
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
			View itemView = inflater.inflate(R.layout.bottom_sheet_item_description_long, viewGroup, false);

			return new LogViewHolder(itemView);
		}

		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
			if (holder instanceof LogViewHolder) {
				LogViewHolder logViewHolder = (LogViewHolder) holder;
				String log = getLog(position);

				logViewHolder.logTextView.setText(log);
			}
		}

		@Override
		public int getItemCount() {
			return logs.size();
		}

		private String getLog(int position) {
			return logs.get(position);
		}

		public void setLogs(Collection<String> logs) {
			this.logs.clear();
			if (logs != null && !logs.isEmpty()) {
				this.logs.addAll(logs);
			}
			notifyDataSetChanged();
		}

		private class LogViewHolder extends RecyclerView.ViewHolder {

			final TextView logTextView;

			public LogViewHolder(View itemView) {
				super(itemView);
				this.logTextView = itemView.findViewById(R.id.description);
			}
		}
	}

	public static class LogcatAsyncTask extends AsyncTask<Void, String, Void> {

		private Process processLogcat;
		private WeakReference<LogcatActivity> logcatActivity;
		private String filterLevel;
		private int index = 0;

		LogcatAsyncTask(LogcatActivity logcatActivity, String filterLevel) {
			this.logcatActivity = new WeakReference<>(logcatActivity);
			this.filterLevel = filterLevel;
		}

		@Override
		protected Void doInBackground(Void... voids) {
			try {
				String filter = String.valueOf(android.os.Process.myPid());
				String[] command = {"logcat", filterLevel, " | grep " + filter, "-T", String.valueOf(MAX_BUFFER_LOG)};
				processLogcat = Runtime.getRuntime().exec(command);

				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(processLogcat.getInputStream()));

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
				LogcatActivity activity = logcatActivity.get();
				if (activity != null) {
					activity.onNewLogEntry(index, values[0]);
					index++;
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
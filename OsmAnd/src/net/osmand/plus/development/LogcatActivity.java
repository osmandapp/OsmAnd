package net.osmand.plus.development;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
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
import java.util.Collection;
import java.util.List;

public class LogcatActivity extends ActionBarProgressActivity {

	public static final String LOGCAT_PATH = "logcat.log";


	public static int MAX_BUFFER_LOG = 10000;

	private static final int SHARE_ID = 0;
	private static final int LEVEL_ID = 1;

	private static final Log log = PlatformUtil.getLog(LogcatActivity.class);

	private LogcatAsyncTask logcatAsyncTask;
	private List<String> logs = new ArrayList<>();
	private LogcatAdapter adapter;
	private String[] LEVELS = {"D", "I", "W", "E"};
	private int filterLevel = 1;
	private RecyclerView recyclerView;


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

		adapter = new LogcatAdapter();
		recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		recyclerView.setAdapter(adapter);

//		int colorResId = AndroidUtils.resolveAttribute(app, R.attr.divider_color_basic);
//		if (colorResId != 0) {
//			DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
//			dividerItemDecoration.setDrawable(new ColorDrawable(ContextCompat.getColor(app, colorResId)));
//
//			recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
//		}
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
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem share = menu.add(0, SHARE_ID, 0, R.string.shared_string_export);
		share.setIcon(R.drawable.ic_action_gshare_dark);
		share.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);


		MenuItem level = menu.add(0, LEVEL_ID, 0, "");
		level.setTitle(getFilterLevel());
		level.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);


		return super.onCreateOptionsMenu(menu);
	}

	@NonNull
	private String getFilterLevel() {
		return "*:" + LEVELS[this.filterLevel];
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
			case android.R.id.home:
				finish();
				return true;
			case LEVEL_ID:
				this.filterLevel++;
				if(this.filterLevel >= LEVELS.length) {
					this.filterLevel = 0;
				}
				item.setTitle(getFilterLevel());
				stopLogcatAsyncTask();
				logs.clear();
				adapter.notifyDataSetChanged();
				startLogcatAsyncTask();
				return true;
			case SHARE_ID:
				startSaveLogsAsyncTask();
				return true;

		}
		return false;
	}


	private void startSaveLogsAsyncTask() {
		SaveLogsAsyncTask saveLogsAsyncTask = new SaveLogsAsyncTask(this, logs);
		saveLogsAsyncTask.execute();
	}

	private void startLogcatAsyncTask() {
		logcatAsyncTask = new LogcatAsyncTask(this, getFilterLevel());
		logcatAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void stopLogcatAsyncTask() {
		if (logcatAsyncTask != null && logcatAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
			logcatAsyncTask.cancel(false);
			logcatAsyncTask.stopLogging();
		}
	}

	private class LogcatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

		@NonNull
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
			TextView itemView = (TextView) inflater.inflate(R.layout.bottom_sheet_item_description_long, viewGroup, false);
			itemView.setGravity(Gravity.CENTER_VERTICAL);

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


		private class LogViewHolder extends RecyclerView.ViewHolder {

			final TextView logTextView;

			public LogViewHolder(View itemView) {
				super(itemView);
				this.logTextView = itemView.findViewById(R.id.description);
			}
		}
	}

	public static class SaveLogsAsyncTask extends AsyncTask<Void, String, File> {

		private WeakReference<LogcatActivity> logcatActivity;
		private Collection<String> logs;

		SaveLogsAsyncTask(LogcatActivity logcatActivity, Collection<String> logs) {
			this.logcatActivity = new WeakReference<>(logcatActivity);
			this.logs = logs;
		}

		@Override
		protected void onPreExecute() {
			LogcatActivity activity = logcatActivity.get();
			if (activity != null) {
				activity.setSupportProgressBarIndeterminateVisibility(true);
			}
		}

		@Override
		protected File doInBackground(Void... voids) {
			File file = logcatActivity.get().getMyApplication().getAppPath(LOGCAT_PATH);
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
			LogcatActivity activity = logcatActivity.get();
			if (activity != null) {
				activity.setSupportProgressBarIndeterminateVisibility(false);
				activity.getMyApplication().sendCrashLog(file);
			}
		}
	}

	public static class LogcatAsyncTask extends AsyncTask<Void, String, Void> {

		private Process processLogcat;
		private WeakReference<LogcatActivity> logcatActivity;
		private String filterLevel;

		LogcatAsyncTask(LogcatActivity logcatActivity, String filterLevel) {
			this.logcatActivity = new WeakReference<>(logcatActivity);
			this.filterLevel = filterLevel;
		}

		@Override
		protected Void doInBackground(Void... voids) {
			try {
				String filter = String.valueOf(android.os.Process.myPid());
				String[] command = {"logcat", filterLevel, "--pid=" + filter, "-T", String.valueOf(MAX_BUFFER_LOG)};
				processLogcat = Runtime.getRuntime().exec(command);

				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(processLogcat.getInputStream()));

				String line;
				while ((line = bufferedReader.readLine()) != null && logcatActivity.get() != null) {
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
					boolean autoscroll = !activity.recyclerView.canScrollVertically(1);
					for(String s : values) {
						activity.logs.add(s);
					}
					activity.adapter.notifyDataSetChanged();
					if(autoscroll) {
						activity.recyclerView.scrollToPosition(activity.logs.size() - 1);
					}
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
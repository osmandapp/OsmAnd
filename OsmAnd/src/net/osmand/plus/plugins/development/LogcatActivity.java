package net.osmand.plus.plugins.development;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class LogcatActivity extends BaseLogcatActivity {

	private static final Log log = PlatformUtil.getLog(LogcatActivity.class);

	private static final int SHARE_ID = 0;
	private static final int LEVEL_ID = 1;

	private LogcatAdapter adapter;
	private final String[] LEVELS = {"D", "I", "W", "E"};
	private int filterLevel = 1;
	private RecyclerView recyclerView;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		app.applyTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.logcat_activity);
		ActionBar supportActionBar = getSupportActionBar();
		if (supportActionBar != null) {
			supportActionBar.setTitle(R.string.logcat_buffer);
			supportActionBar.setElevation(5.0f);
		}

		adapter = new LogcatAdapter();
		recyclerView = findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		recyclerView.setAdapter(adapter);
	}

	@Nullable
	@Override
	public List<Integer> getScrollableViewIds() {
		List<Integer> ids = new ArrayList<>();
		ids.add(R.id.recycler_view);
		return ids;
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
		Drawable shareIcon = app.getUIUtilities().getIcon(R.drawable.ic_action_gshare_dark);
		MenuItem share = menu.add(0, SHARE_ID, 0, R.string.shared_string_export);
		share.setIcon(AndroidUtils.getDrawableForDirection(app, shareIcon));
		share.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);


		MenuItem level = menu.add(0, LEVEL_ID, 0, "");
		level.setTitle(getFilterLevel());
		level.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);


		return super.onCreateOptionsMenu(menu);
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
				if (this.filterLevel >= LEVELS.length) {
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

	@NonNull
	@Override
	protected String getFilterLevel() {
		return "*:" + LEVELS[this.filterLevel];
	}

	@Override
	protected void onLogEntryAdded() {
		boolean autoscroll = !recyclerView.canScrollVertically(1);
		adapter.notifyDataSetChanged();
		if (autoscroll) {
			recyclerView.scrollToPosition(logs.size() - 1);
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
}
package net.osmand.plus.activities;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import net.osmand.plus.ProgressDialogImplementation;
import net.osmand.plus.R;
import net.osmand.plus.activities.LocalIndexHelper.LocalIndexInfo;
import android.app.Activity;
import android.app.ExpandableListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

public class LocalIndexesActivity extends ExpandableListActivity {

	private AsyncTask<Activity, LocalIndexInfo, List<LocalIndexInfo>> asyncLoader;
	private LocalIndexesAdapter listAdapter;
	private ProgressDialog progressDlg;
	private LoadLocalIndexDescriptionTask descriptionLoader;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.local_index);

		LoadLocalIndexTask task = new LoadLocalIndexTask();
		asyncLoader = task.execute(this);
		descriptionLoader = new LoadLocalIndexDescriptionTask();
		listAdapter = new LocalIndexesAdapter();
		findViewById(R.id.DownloadButton).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(LocalIndexesActivity.this, DownloadIndexActivity.class));
			}
		});
		findViewById(R.id.ReloadIndexes).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				reloadIndexes();
			}
		});
		setListAdapter(listAdapter);
	}

	public class LoadLocalIndexTask extends AsyncTask<Activity, LocalIndexInfo, List<LocalIndexInfo>> {
		List<LocalIndexInfo> progress = new ArrayList<LocalIndexInfo>();

		@Override
		protected List<LocalIndexInfo> doInBackground(Activity... params) {
			LocalIndexHelper helper = new LocalIndexHelper((OsmandApplication) params[0].getApplication());
			progress.clear();
			return helper.getAllLocalIndexData(this);
		}

		public void loadFile(LocalIndexInfo loaded) {
			publishProgress(loaded);
		}

		@Override
		protected void onProgressUpdate(LocalIndexInfo... values) {
			for (LocalIndexInfo v : values) {
				listAdapter.addLocalIndexInfo(v);
			}
			listAdapter.notifyDataSetChanged();
		}

		@Override
		protected void onPostExecute(List<LocalIndexInfo> result) {
			findViewById(R.id.ProgressBar).setVisibility(View.GONE);
			findViewById(R.id.ReloadIndexes).setVisibility(View.VISIBLE);
		}

	}
	
	public class LoadLocalIndexDescriptionTask extends AsyncTask<LocalIndexInfo, LocalIndexInfo, LocalIndexInfo[]> {

		@Override
		protected LocalIndexInfo[] doInBackground(LocalIndexInfo... params) {
			LocalIndexHelper helper = new LocalIndexHelper((OsmandApplication) LocalIndexesActivity.this.getApplication());
			for(LocalIndexInfo i : params){
				helper.updateDescription(i);
			}
			return params;
		}

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected void onProgressUpdate(LocalIndexInfo... values) {
			listAdapter.notifyDataSetChanged();
		}

		@Override
		protected void onPostExecute(LocalIndexInfo[] result) {
			listAdapter.notifyDataSetChanged();
		}

	}
	
	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		LocalIndexInfo item = listAdapter.getChild(groupPosition, childPosition);
		item.setExpanded(!item.isExpanded());
		if(item.isExpanded()){
			descriptionLoader = new LoadLocalIndexDescriptionTask();
			descriptionLoader.execute(item);
		}
		listAdapter.notifyDataSetInvalidated();
		return true;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (progressDlg != null) {
			progressDlg.dismiss();
			progressDlg = null;
		}
		asyncLoader.cancel(true);
		descriptionLoader.cancel(true);
	}

	protected class LocalIndexesAdapter extends BaseExpandableListAdapter {
		List<List<LocalIndexInfo>> data = new ArrayList<List<LocalIndexInfo>>();
		List<LocalIndexInfo> category = new ArrayList<LocalIndexInfo>();
		private MessageFormat formatMb;

		public LocalIndexesAdapter() {
			formatMb = new MessageFormat("{0, number,##.#} MB");
		}

		public void addLocalIndexInfo(LocalIndexInfo info) {
			int found = -1;
			// search from end
			for (int i = category.size() - 1; i >= 0; i--) {
				LocalIndexInfo cat = category.get(i);
				if (cat.getType() == info.getType() && info.isBackupedData() == cat.isBackupedData()) {
					found = i;
					break;
				}
			}
			if (found == -1) {
				found = category.size();
				category.add(new LocalIndexInfo(info.getType(), info.isBackupedData()));
			}
			if (found >= data.size()) {
				data.add(new ArrayList<LocalIndexInfo>());
			}
			data.get(found).add(info);
		}

		@Override
		public LocalIndexInfo getChild(int groupPosition, int childPosition) {
			return data.get(groupPosition).get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			// it would be unusable to have 10000 local indexes
			return groupPosition * 10000 + childPosition;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			View v = convertView;
			LocalIndexInfo child = (LocalIndexInfo) getChild(groupPosition, childPosition);
			if (v == null) {
				LayoutInflater inflater = getLayoutInflater();
				v = inflater.inflate(net.osmand.plus.R.layout.local_index_list_item, parent, false);
			}
			TextView viewName = ((TextView) v.findViewById(R.id.local_index_name));
			viewName.setText(child.getName());
			if (child.isNotSupported()) {
				viewName.setTextColor(Color.RED);
			} else if (child.isCorrupted()) {
				viewName.setTextColor(Color.MAGENTA);
			} else if (child.isLoaded()) {
				viewName.setTextColor(Color.GREEN);
			} else {
				viewName.setTextColor(Color.LTGRAY);
			}
			if (child.getSize() >= 0) {
				String size;
				if (child.getSize() > 100) {
					size = formatMb.format(new Object[] { (float) child.getSize() / (1 << 10) });
				} else {
					size = child.getSize() + " Kb";
				}
				((TextView) v.findViewById(R.id.local_index_size)).setText(size);
			} else {
				((TextView) v.findViewById(R.id.local_index_size)).setText("");
			}
			TextView descr = ((TextView) v.findViewById(R.id.local_index_descr));
			if(child.isExpanded()){
				descr.setVisibility(View.VISIBLE);
				descr.setText(child.getDescription());
			} else {
				descr.setVisibility(View.GONE);
			}

			return v;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			View v = convertView;
			LocalIndexInfo group = getGroup(groupPosition);
			if (v == null) {
				LayoutInflater inflater = getLayoutInflater();
				v = inflater.inflate(net.osmand.plus.R.layout.local_index_list_item_category, parent, false);
			}
			StringBuilder t = new StringBuilder(group.getType().getHumanString(LocalIndexesActivity.this));
			if (group.isBackupedData()) {
				t.append("* ");
			}
			TextView nameView = ((TextView) v.findViewById(R.id.local_index_category_name));
			t.append(" [").append(getChildrenCount(groupPosition)).append(" ").append(getString(R.string.local_index_items)).append("]");
			nameView.setText(t.toString());
			if (!group.isBackupedData()) {
				nameView.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
			} else {
				nameView.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
			}
			

			return v;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return data.get(groupPosition).size();
		}

		@Override
		public LocalIndexInfo getGroup(int groupPosition) {
			return category.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return category.size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}
		
		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

	}

	public void reloadIndexes() {
		progressDlg = ProgressDialog.show(this, getString(R.string.loading_data), getString(R.string.reading_indexes), true);
		final ProgressDialogImplementation impl = new ProgressDialogImplementation(progressDlg);
		impl.setRunnable("Initializing app", new Runnable() { //$NON-NLS-1$
				@Override
				public void run() {
					try {
						showWarnings(((OsmandApplication) getApplication()).getResourceManager().reloadIndexes(impl));
					} finally {
						if (progressDlg != null) {
							progressDlg.dismiss();
							progressDlg = null;
						}
					}
				}
		});
		impl.run();
	}

	protected void showWarnings(List<String> warnings) {
		if (!warnings.isEmpty()) {
			final StringBuilder b = new StringBuilder();
			boolean f = true;
			for (String w : warnings) {
				if (f) {
					f = false;
				} else {
					b.append('\n');
				}
				b.append(w);
			}
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(LocalIndexesActivity.this, b.toString(), Toast.LENGTH_LONG).show();
				}
			});
		}
	}

}

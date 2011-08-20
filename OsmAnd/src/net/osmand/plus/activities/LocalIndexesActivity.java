package net.osmand.plus.activities;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class LocalIndexesActivity extends ExpandableListActivity {

	private AsyncTask<Activity, LocalIndexInfo, List<LocalIndexInfo>> asyncLoader;
	private LocalIndexesAdapter listAdapter;
	private ProgressDialog progressDlg;
	private LoadLocalIndexDescriptionTask descriptionLoader;

	private boolean selectionMode = false;
	private Set<LocalIndexInfo> selectedItems = new LinkedHashSet<LocalIndexInfo>();

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
		
		((CheckBox)findViewById(R.id.SelectionMode)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				selectionMode = isChecked;
				listAdapter.notifyDataSetInvalidated();
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
			findViewById(R.id.SelectionMode).setVisibility(View.VISIBLE);
			
		}

	}

	public class LoadLocalIndexDescriptionTask extends AsyncTask<LocalIndexInfo, LocalIndexInfo, LocalIndexInfo[]> {

		@Override
		protected LocalIndexInfo[] doInBackground(LocalIndexInfo... params) {
			LocalIndexHelper helper = new LocalIndexHelper((OsmandApplication) LocalIndexesActivity.this.getApplication());
			for (LocalIndexInfo i : params) {
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
		if (item.isExpanded()) {
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
	
	private class SelectItemListener implements CompoundButton.OnCheckedChangeListener {
		

		private final LocalIndexInfo child;

		public SelectItemListener(LocalIndexInfo child) {
			this.child = child;
		}

		@Override
		public void onCheckedChanged(CompoundButton v, boolean isChecked) {
			if(isChecked){
				selectedItems.add(child);
			} else {
				selectedItems.remove(child);
			}
		}

		
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
			if (v == null || selectionMode) {
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
			if (child.isExpanded()) {
				descr.setVisibility(View.VISIBLE);
				descr.setText(child.getDescription());
			} else {
				descr.setVisibility(View.GONE);
			}
			CheckBox checkbox = (CheckBox) v.findViewById(R.id.check_local_index);
			checkbox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
			if (selectionMode) {
				checkbox.setSelected(selectedItems.contains(child));
				checkbox.setOnCheckedChangeListener(new SelectItemListener(child));
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
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, R.string.local_index_mi_reload, 0, R.string.local_index_mi_reload);
		menu.add(0, R.string.local_index_mi_delete, 0, R.string.local_index_mi_delete);
		menu.add(0, R.string.local_index_mi_restore, 0, R.string.local_index_mi_restore);
		menu.add(0, R.string.local_index_mi_backup, 0, R.string.local_index_mi_backup);
		
		return true;
	}
	
	private void openSelectionMode(String actionButton){
		selectionMode = true;
		selectedItems.clear();
		Button action = (Button) findViewById(R.id.ActionButton);
		action.setVisibility(View.VISIBLE);
		action.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
			}
		});
		Button cancel = (Button) findViewById(R.id.CancelButton);
		cancel.setVisibility(View.VISIBLE);
		cancel.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				closeSelectionMode();
			}
		});
		findViewById(R.id.DownloadButton).setVisibility(View.GONE);
		listAdapter.notifyDataSetChanged();
	}
	
	private void closeSelectionMode(){
		selectionMode = false;
		findViewById(R.id.DownloadButton).setVisibility(View.VISIBLE);
		findViewById(R.id.CancelButton).setVisibility(View.GONE);
		findViewById(R.id.ActionButton).setVisibility(View.GONE);
		listAdapter.notifyDataSetChanged();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == R.string.local_index_mi_reload){
			reloadIndexes();
		} else if(item.getItemId() == R.string.local_index_mi_delete){
			openSelectionMode(getString(R.string.local_index_mi_delete));
		} else if(item.getItemId() == R.string.local_index_mi_backup){
			openSelectionMode(getString(R.string.local_index_mi_backup));
		} else if(item.getItemId() == R.string.local_index_mi_restore){
			openSelectionMode(getString(R.string.local_index_mi_restore));
		} else {
			return super.onOptionsItemSelected(item);
		}
		return true;
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

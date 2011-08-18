package net.osmand.plus.activities;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import net.osmand.plus.R;
import net.osmand.plus.activities.LocalIndexHelper.LocalIndexInfo;
import android.app.Activity;
import android.app.ExpandableListActivity;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

public class LocalIndexesActivity extends ExpandableListActivity {

	private AsyncTask<Activity, LocalIndexInfo, List<LocalIndexInfo>> asyncLoader;
	private LocalIndexesAdapter listAdapter;
	private View loadingPanel;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.local_index);

		loadingPanel = findViewById(R.id.LoadingPanel);
		LoadLocalIndexTask task = new LoadLocalIndexTask();
		asyncLoader = task.execute(this);
		listAdapter = new LocalIndexesAdapter();
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
		
		public void loadFile(LocalIndexInfo loaded){
			publishProgress(loaded);
		}
		
		@Override
		protected void onProgressUpdate(LocalIndexInfo... values) {
			for(LocalIndexInfo v : values){
				listAdapter.addLocalIndexInfo(v);
			}
			listAdapter.notifyDataSetChanged();
		}
		
		@Override
		protected void onPostExecute(List<LocalIndexInfo> result) {
			loadingPanel.setVisibility(View.INVISIBLE);
		}
		
	}
	
	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		asyncLoader.cancel(true);
	}
	
	
	protected class LocalIndexesAdapter extends BaseExpandableListAdapter  {
		List<List<LocalIndexInfo>> data = new ArrayList<List<LocalIndexInfo>>();
		List<LocalIndexInfo> category = new ArrayList<LocalIndexInfo>();
		private MessageFormat format;

		public LocalIndexesAdapter() {
			format = new MessageFormat("{0, number,##.#} MB");
		}
		
		public void addLocalIndexInfo(LocalIndexInfo info){
			int found = -1;
			// search from end
			for (int i = category.size() - 1; i >= 0; i--) {
				LocalIndexInfo cat = category.get(i);
				if (cat.getType() == info.getType() && info.isBackupedData() == cat.isBackupedData()) {
					found = i;
					break;
				}
			}
			if(found == -1){
				found = category.size();
				category.add(new LocalIndexInfo(info.getType(), info.isBackupedData()));
			}
			if(found >= data.size()) {
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
			((TextView) v.findViewById(R.id.local_index_name)).setText(child.getName());
			if(child.getSize() != 0){
				String size = format.format(new Object[] { (float) child.getSize() / (1 << 10) });
				((TextView) v.findViewById(R.id.local_index_descr)).setText(size);
			} else {
				((TextView) v.findViewById(R.id.local_index_descr)).setText("");
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
			if(group.isBackupedData()){
				t.append(" ").append(getString(R.string.local_indexes_cat_backup));
			}
			TextView nameView = ((TextView) v.findViewById(R.id.local_index_category_name));
			t.append(" [").append(getChildrenCount(groupPosition)).append(" ").append(getString(R.string.local_index_items)).append("]");
			nameView.setText(t.toString());
			if(!group.isBackupedData()){
				nameView.setTypeface(Typeface.DEFAULT_BOLD);
			} else {
				nameView.setTypeface(Typeface.DEFAULT);
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
			return false;
		}
		
	}
	
	
	
	

}

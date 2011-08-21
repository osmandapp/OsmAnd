package net.osmand.plus.activities;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.osmand.Algoritms;
import net.osmand.FavouritePoint;
import net.osmand.IProgress;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.activities.LocalIndexHelper.LocalIndexInfo;
import net.osmand.plus.activities.LocalIndexHelper.LocalIndexType;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ExpandableListActivity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.AsyncTask.Status;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

public class LocalIndexesActivity extends ExpandableListActivity {

	private AsyncTask<Activity, LocalIndexInfo, List<LocalIndexInfo>> asyncLoader;
	private LocalIndexesAdapter listAdapter;
	private LoadLocalIndexDescriptionTask descriptionLoader;
	private LocalIndexOperationTask operationTask;

	private boolean selectionMode = false;
	private Set<LocalIndexInfo> selectedItems = new LinkedHashSet<LocalIndexInfo>();
	
	protected static int DELETE_OPERATION = 1;
	protected static int BACKUP_OPERATION = 2;
	protected static int RESTORE_OPERATION = 3;

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
		
		getExpandableListView().setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
				long packedPos = ((ExpandableListContextMenuInfo)menuInfo).packedPosition;
				
				final LocalIndexInfo point = (LocalIndexInfo) listAdapter.getChild(ExpandableListView.getPackedPositionGroup(packedPos),
						ExpandableListView.getPackedPositionChild(packedPos));
				if(point.getGpxFile() != null){
					Location loc = point.getGpxFile().findFistLocation();
					if(loc != null){
						OsmandSettings.getOsmandSettings(LocalIndexesActivity.this).setMapLocationToShow(loc.getLatitude(),loc.getLongitude());						
					}
					((OsmandApplication) getApplication()).setGpxFileToDisplay(point.getGpxFile());
					MapActivity.launchMapActivityMoveToTop(LocalIndexesActivity.this);
				}
			}	
		});
		
		setListAdapter(listAdapter);
	}

	public class LoadLocalIndexTask extends AsyncTask<Activity, LocalIndexInfo, List<LocalIndexInfo>> {
		List<LocalIndexInfo> progress = new ArrayList<LocalIndexInfo>();

		@Override
		protected List<LocalIndexInfo> doInBackground(Activity... params) {
			LocalIndexHelper helper = new LocalIndexHelper((OsmandApplication) getApplication());
			progress.clear();
			return helper.getAllLocalIndexData(this);
		}

		public void loadFile(LocalIndexInfo loaded) {
			publishProgress(loaded);
		}

		@Override
		protected void onPreExecute() {
			findViewById(R.id.ProgressBar).setVisibility(View.VISIBLE);
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
		}

	}
	
	public class LocalIndexOperationTask extends AsyncTask<LocalIndexInfo, LocalIndexInfo, String> {
		
		private final int operation;
		private OsmandSettings settings;

		public LocalIndexOperationTask(int operation){
			this.operation = operation;
			settings = ((OsmandApplication) getApplication()).getSettings();
		}
		
		public File getFileToRestore(LocalIndexInfo i){
			if(i.isBackupedData()){
				File parent = new File(i.getPathToData()).getParentFile();
				if(i.getType() == LocalIndexType.GPX_DATA){
					parent = settings.extendOsmandPath(ResourceManager.GPX_PATH);
				} else if(i.getType() == LocalIndexType.MAP_DATA){
					parent = settings.extendOsmandPath(ResourceManager.MAPS_PATH);
				} else if(i.getType() == LocalIndexType.POI_DATA){
					parent = settings.extendOsmandPath(ResourceManager.POI_PATH);
				} else if(i.getType() == LocalIndexType.TILES_DATA){
					parent = settings.extendOsmandPath(ResourceManager.TILES_PATH);
				} else if(i.getType() == LocalIndexType.VOICE_DATA){
					parent = settings.extendOsmandPath(ResourceManager.VOICE_PATH);
				} else if(i.getType() == LocalIndexType.TTS_VOICE_DATA){
					parent = settings.extendOsmandPath(ResourceManager.VOICE_PATH);
				}
				return new File(parent, i.getFileName());
			}
			return new File(i.getPathToData());
		}
		
		private File getFileToBackup(LocalIndexInfo i) {
			if(!i.isBackupedData()){
				return new File(settings.extendOsmandPath(ResourceManager.BACKUP_PATH), i.getFileName());
			}
			return new File(i.getPathToData());
		}
		
		private boolean move(File from, File to){
			if(!to.getParentFile().exists()){
				to.getParentFile().mkdirs();
			}
			return from.renameTo(to);
		}
		
		@Override
		protected String doInBackground(LocalIndexInfo... params) {
			int count = 0;
			int total = 0;
			for(LocalIndexInfo info : params) {
				if(!isCancelled()){
					boolean successfull = false;
					if(operation == DELETE_OPERATION){
						File f = new File(info.getPathToData());
						successfull = Algoritms.removeAllFiles(f);
					} else if(operation == RESTORE_OPERATION){
						successfull = move(new File(info.getPathToData()), getFileToRestore(info));
						if(successfull){
							info.setBackupedData(false);
						}
					} else if(operation == BACKUP_OPERATION){
						successfull = move(new File(info.getPathToData()), getFileToBackup(info));
						if(successfull){
							info.setBackupedData(true);
						}
					}
					total ++;
					if(successfull){
						count++;
						publishProgress(info);
					}
				}
			}
			if(operation == DELETE_OPERATION){
				return getString(R.string.local_index_items_deleted, count, total);
			} else if(operation == BACKUP_OPERATION){
				return getString(R.string.local_index_items_backuped, count, total);
			} else if(operation == RESTORE_OPERATION){
				return getString(R.string.local_index_items_restored, count, total);
			}  
			return "";
		}


		@Override
		protected void onProgressUpdate(LocalIndexInfo... values) {
			if(operation == DELETE_OPERATION){
				listAdapter.delete(values);
			} else if(operation == BACKUP_OPERATION){
				listAdapter.move(values, false);
			} else if(operation == RESTORE_OPERATION){
				listAdapter.move(values, true);
			}
			
		}
		
		@Override
		protected void onPreExecute() {
			findViewById(R.id.ProgressBar).setVisibility(View.VISIBLE);
		}

		@Override
		protected void onPostExecute(String result) {
			findViewById(R.id.ProgressBar).setVisibility(View.GONE);
			Toast.makeText(LocalIndexesActivity.this, result, Toast.LENGTH_LONG).show();
			reloadIndexes();
		}

	}

	public class LoadLocalIndexDescriptionTask extends AsyncTask<LocalIndexInfo, LocalIndexInfo, LocalIndexInfo[]> {

		@Override
		protected LocalIndexInfo[] doInBackground(LocalIndexInfo... params) {
			LocalIndexHelper helper = new LocalIndexHelper((OsmandApplication) getApplication());
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
		if(selectionMode){
			selectedItems.add(item);
		}
		listAdapter.notifyDataSetInvalidated();
		return true;
	}
	
	
	@Override
	protected void onPause() {
		super.onPause();
		if(operationTask != null){
			operationTask.cancel(true);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		asyncLoader.cancel(true);
		descriptionLoader.cancel(true);
	}
	
	

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, R.string.local_index_mi_backup, 0, R.string.local_index_mi_backup);
		menu.add(0, R.string.local_index_mi_reload, 1, R.string.local_index_mi_reload);
		menu.add(0, R.string.local_index_mi_delete, 2, R.string.local_index_mi_delete);
		menu.add(0, R.string.local_index_mi_restore, 3, R.string.local_index_mi_restore);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if(operationTask == null || operationTask.getStatus() == Status.FINISHED){
			menu.setGroupVisible(0, true);
		} else {
			menu.setGroupVisible(0, false);
		}
		return true;
	}
	
	public void doAction(int actionResId){
		if(actionResId == R.string.local_index_mi_backup){
			operationTask = new LocalIndexOperationTask(BACKUP_OPERATION);
		} else if(actionResId == R.string.local_index_mi_delete){
			operationTask = new LocalIndexOperationTask(DELETE_OPERATION);
		} else if(actionResId == R.string.local_index_mi_restore){
			operationTask = new LocalIndexOperationTask(RESTORE_OPERATION);
		} else {
			operationTask = null;
		}
		if(operationTask != null){
			operationTask.execute(selectedItems.toArray(new LocalIndexInfo[selectedItems.size()]));
		}
		closeSelectionMode();
	}
	
	private void openSelectionMode(final int actionResId){
		final String actionButton = getString(actionResId);
		if(listAdapter.getGroupCount() == 0){
			listAdapter.cancelFilter();
			Toast.makeText(LocalIndexesActivity.this, getString(R.string.local_index_no_items_to_do, actionButton.toLowerCase()), Toast.LENGTH_SHORT).show();
			return;
		}
		
		
		selectionMode = true;
		selectedItems.clear();
		Button action = (Button) findViewById(R.id.ActionButton);
		action.setVisibility(View.VISIBLE);
		action.setText(actionButton);
		action.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(selectedItems.isEmpty()){
					Toast.makeText(LocalIndexesActivity.this, getString(R.string.local_index_no_items_to_do, actionButton.toLowerCase()), Toast.LENGTH_SHORT).show();
					return;
				}
				
				Builder builder = new AlertDialog.Builder(LocalIndexesActivity.this);
				builder.setMessage(getString(R.string.local_index_action_do, actionButton.toLowerCase(), selectedItems.size()));
				builder.setPositiveButton(actionButton, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						doAction(actionResId);
					}
				});
				builder.setNegativeButton(R.string.default_buttons_cancel, null);
				builder.show();
				
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
		findViewById(R.id.DescriptionText).setVisibility(View.GONE);
		findViewById(R.id.FillLayoutStart).setVisibility(View.VISIBLE);
		findViewById(R.id.FillLayoutEnd).setVisibility(View.VISIBLE);
		listAdapter.notifyDataSetChanged();
	}
	
	private void closeSelectionMode(){
		selectionMode = false;
		findViewById(R.id.DownloadButton).setVisibility(View.VISIBLE);
		findViewById(R.id.DescriptionText).setVisibility(View.VISIBLE);
		findViewById(R.id.FillLayoutStart).setVisibility(View.GONE);
		findViewById(R.id.FillLayoutEnd).setVisibility(View.GONE);
		findViewById(R.id.CancelButton).setVisibility(View.GONE);
		findViewById(R.id.ActionButton).setVisibility(View.GONE);
		listAdapter.cancelFilter();
		listAdapter.notifyDataSetChanged();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == R.string.local_index_mi_reload){
			reloadIndexes();
		} else if(item.getItemId() == R.string.local_index_mi_delete){
			openSelectionMode(R.string.local_index_mi_delete);
		} else if(item.getItemId() == R.string.local_index_mi_backup){
			listAdapter.filterCategories(false);
			listAdapter.filterCategories(LocalIndexType.MAP_DATA, LocalIndexType.POI_DATA);
			openSelectionMode(R.string.local_index_mi_backup);
		} else if(item.getItemId() == R.string.local_index_mi_restore){
			listAdapter.filterCategories(true);
			openSelectionMode(R.string.local_index_mi_restore);
		} else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}
	

	public void reloadIndexes() {
		AsyncTask<Void, String, List<String>> task = new AsyncTask<Void, String, List<String>>(){

			@Override
			protected void onPostExecute(List<String> warnings) {
				findViewById(R.id.ProgressBar).setVisibility(View.GONE);
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
					Toast.makeText(LocalIndexesActivity.this, b.toString(), Toast.LENGTH_LONG).show();
				}
			}
			
			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				findViewById(R.id.ProgressBar).setVisibility(View.VISIBLE);
			}
			@Override
			protected List<String> doInBackground(Void... params) {
				return ((OsmandApplication) getApplication()).getResourceManager().reloadIndexes(IProgress.EMPTY_PROGRESS);
			}
			
		};
		task.execute();
		
	}

	

	protected class LocalIndexesAdapter extends BaseExpandableListAdapter {
		Map<LocalIndexInfo, List<LocalIndexInfo>> data = new LinkedHashMap<LocalIndexInfo, List<LocalIndexInfo>>();
		List<LocalIndexInfo> category = new ArrayList<LocalIndexInfo>();
		List<LocalIndexInfo> filterCategory = null;
		
		private MessageFormat formatMb;

		public LocalIndexesAdapter() {
			formatMb = new MessageFormat("{0, number,##.#} MB");
		}
		
		public LocalIndexInfo findCategory(LocalIndexInfo val, boolean backuped){
			for(LocalIndexInfo i : category){
				if(i.isBackupedData() == backuped && val.getType() == i.getType() ){
					return i;
				}
			}
			LocalIndexInfo newCat = new LocalIndexInfo(val.getType(), backuped);
			category.add(newCat);
			data.put(newCat, new ArrayList<LocalIndexInfo>());
			return newCat;
		}
		
		public void delete(LocalIndexInfo[] values) {
			for(LocalIndexInfo i : values){
				LocalIndexInfo c = findCategory(i, i.isBackupedData());
				if(c != null){
					data.get(c).remove(i);
				}
			}
			listAdapter.notifyDataSetChanged();
		}
		
		public void move(LocalIndexInfo[] values, boolean oldBackupState) {
			for(LocalIndexInfo i : values){
				LocalIndexInfo c = findCategory(i, oldBackupState);
				if(c != null){
					data.get(c).remove(i);
				}
				c = findCategory(i, !oldBackupState);
				if(c != null){
					data.get(c).add(i);
				}
			}
			listAdapter.notifyDataSetChanged();
		}

		public void cancelFilter(){
			filterCategory = null;
			notifyDataSetChanged();
		}
		
		public void filterCategories(LocalIndexType... types) {
			List<LocalIndexInfo> filter = new ArrayList<LocalIndexInfo>();
			List<LocalIndexInfo> source = filterCategory == null ? category : filterCategory;
			for (LocalIndexInfo info : source) {
				for (LocalIndexType ts : types) {
					if (info.getType() == ts) {
						filter.add(info);
					}
				}
			}
			filterCategory = filter;
			notifyDataSetChanged();
		}
		
		public void filterCategories(boolean backup) {
			List<LocalIndexInfo> filter = new ArrayList<LocalIndexInfo>();
			List<LocalIndexInfo> source = filterCategory == null ? category : filterCategory;
			for (LocalIndexInfo info : source) {
				if (info.isBackupedData() == backup) {
					filter.add(info);
				}
			}
			filterCategory = filter;
			notifyDataSetChanged();
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
			if (!data.containsKey(category.get(found))) {
				data.put(category.get(found), new ArrayList<LocalIndexInfo>());
			}
			data.get(category.get(found)).add(info);
		}

		@Override
		public LocalIndexInfo getChild(int groupPosition, int childPosition) {
			LocalIndexInfo cat = filterCategory != null ? filterCategory.get(groupPosition) : category.get(groupPosition);
			return data.get(cat).get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			// it would be unusable to have 10000 local indexes
			return groupPosition * 10000 + childPosition;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			View v = convertView;
			final LocalIndexInfo child = (LocalIndexInfo) getChild(groupPosition, childPosition);
			if (v == null ) {
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
			final CheckBox checkbox = (CheckBox) v.findViewById(R.id.check_local_index);
			checkbox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
			if (selectionMode) {
				checkbox.setChecked(selectedItems.contains(child));
				checkbox.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						if(checkbox.isChecked()){
							selectedItems.add(child);
						} else {
							selectedItems.remove(child);
						}
					}
				});
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
			LocalIndexInfo cat = filterCategory != null ? filterCategory.get(groupPosition) : category.get(groupPosition);
			return data.get(cat).size();
		}

		@Override
		public LocalIndexInfo getGroup(int groupPosition) {
			return filterCategory == null ?  category.get(groupPosition)  : filterCategory.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return filterCategory == null ?  category.size() : filterCategory.size();
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
	
}

package net.osmand.plus.activities;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.LogUtil;
import net.osmand.OpenstreetmapPoint;
import net.osmand.OpenstreetmapRemoteUtil;
import net.osmand.OpenstreetmapUtil;
import net.osmand.osm.EntityInfo;
import net.osmand.plus.AmenityIndexRepositoryOdb;
import net.osmand.plus.OpenstreetmapsDbHelper;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.ProgressDialogImplementation;
import net.osmand.plus.R;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.TextView;
import android.widget.Toast;

public class LocalOpenstreetmapActivity extends OsmandExpandableListActivity {

	/** dialogs **/
	protected static final int DIALOG_PROGRESS_UPLOAD = 0;

	private LocalOpenstreetmapAdapter listAdapter;

	private OpenstreetmapsDbHelper db;

	private OpenstreetmapRemoteUtil remote;

	private ProgressDialog progressPointDlg = null;

	protected OpenstreetmapPoint[] toUpload;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.local_openstreetmap);
		listAdapter = new LocalOpenstreetmapAdapter();

		getExpandableListView().setOnChildClickListener(new OnChildClickListener() {
			
			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				final OpenstreetmapPoint point = (OpenstreetmapPoint) listAdapter.getChild(groupPosition, childPosition);
				showContextMenu(point);
				return true;
			}
		});
		getExpandableListView().setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
				long packedPos = ((ExpandableListContextMenuInfo)menuInfo).packedPosition;
				int group = ExpandableListView.getPackedPositionGroup(packedPos);
				int child = ExpandableListView.getPackedPositionChild(packedPos);
				if (child >= 0 && group >= 0) {
					final OpenstreetmapPoint point = (OpenstreetmapPoint) listAdapter.getChild(group, child);
					showContextMenu(point);
				}
			}
		});
		
		setListAdapter(listAdapter);

		db = new OpenstreetmapsDbHelper(this);
		List<OpenstreetmapPoint> l = db.getOpenstreetmapPoints();
		android.util.Log.d(LogUtil.TAG, "List of POI " + l.size() + " length");
		for (OpenstreetmapPoint p : l) {
			listAdapter.addOpenstreetmapPoint(p);
		}
		listAdapter.notifyDataSetChanged();

		remote = new OpenstreetmapRemoteUtil(this, this.getWindow().getDecorView());

		findViewById(R.id.UploadAllButton).setOnClickListener(new View.OnClickListener() {


			@Override
			public void onClick(View v) {
				toUpload = listAdapter.values().toArray(new OpenstreetmapPoint[0]);
				showDialog(DIALOG_PROGRESS_UPLOAD);
			}
		});
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
			case DIALOG_PROGRESS_UPLOAD:
				progressPointDlg = ProgressDialogImplementation.createProgressDialog(
						LocalOpenstreetmapActivity.this,
						getString(R.string.uploading),
						getString(R.string.local_openstreetmap_uploading_poi),
						ProgressDialog.STYLE_HORIZONTAL).getDialog();
				return progressPointDlg;
		}
		return null;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
		switch (id) {
		case DIALOG_PROGRESS_UPLOAD:
			UploadOpenstreetmapPointAsyncTask uploadTask = new UploadOpenstreetmapPointAsyncTask(progressPointDlg, remote,
					toUpload.length);
			uploadTask.execute(toUpload);
			break;
		}
	}

	public class UploadOpenstreetmapPointAsyncTask extends AsyncTask<OpenstreetmapPoint, OpenstreetmapPoint, Integer> {

		private ProgressDialog progress;

		private OpenstreetmapRemoteUtil remote;

		private int listSize = 0;

		private boolean interruptUploading = false;

		public UploadOpenstreetmapPointAsyncTask(ProgressDialog progress,
												 OpenstreetmapRemoteUtil remote,
												 int listSize) {
			this.progress = progress;
			this.remote = remote;
			this.listSize = listSize;
		}

		@Override
		protected Integer doInBackground(OpenstreetmapPoint... points) {
			int uploaded = 0;

			for (OpenstreetmapPoint p : points) {
				if (interruptUploading) break;

				EntityInfo entityInfo = null;
				if (OpenstreetmapUtil.Action.CREATE != p.getAction()) {
					entityInfo = remote.loadNode(p.getEntity());
				}
				if (remote.commitNodeImpl(p.getAction(), p.getEntity(), entityInfo, p.getComment())) {
					publishProgress(p);
					uploaded++;
				}
			}

			return new Integer(uploaded);
		}

		@Override
		protected void onPreExecute() {
			interruptUploading = false;

			progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						UploadOpenstreetmapPointAsyncTask.this.setInterruptUploading(true);
					}
				});
			progress.setIndeterminate(false);
			progress.setMax(listSize);
			progress.setProgress(0);
		}

		@Override
		protected void onPostExecute(Integer result) {
			listAdapter.notifyDataSetChanged();
			if(result != null){
				Toast.makeText(LocalOpenstreetmapActivity.this, MessageFormat.format(getString(R.string.local_openstreetmap_poi_were_uploaded), result.intValue()), Toast.LENGTH_LONG).show();
			}
			removeDialog(DIALOG_PROGRESS_UPLOAD);
		}

		public void setInterruptUploading(boolean b) {
			interruptUploading = b;
		}

		@Override
		protected void onProgressUpdate(OpenstreetmapPoint... points) {
			listAdapter.delete(points[0]);
			progress.incrementProgressBy(1);
		}

	}
	
	private void showContextMenu(final OpenstreetmapPoint info) {
		Builder builder = new AlertDialog.Builder(this);
		final List<Integer> menu = new ArrayList<Integer>();

		menu.add(R.string.local_openstreetmap_show_poi);
		menu.add(R.string.local_openstreetmap_upload);
		menu.add(R.string.local_openstreetmap_delete);

		String[] values = new String[menu.size()];
		for (int i = 0; i < values.length; i++) {
			values[i] = getString(menu.get(i));
		}
		builder.setItems(values, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				int resId = menu.get(which);
				if (info != null) {
					if (resId == R.string.local_openstreetmap_show_poi) {
						OsmandSettings settings = OsmandSettings.getOsmandSettings(LocalOpenstreetmapActivity.this);
						settings.setMapLocationToShow(info.getLatitude(), info.getLongitude(), settings.getLastKnownMapZoom());
						MapActivity.launchMapActivityMoveToTop(LocalOpenstreetmapActivity.this);
					} else if (resId == R.string.local_openstreetmap_delete) {
						listAdapter.delete(info);
					} else if (resId == R.string.local_openstreetmap_upload) {
						toUpload = new OpenstreetmapPoint[]{info};
						showDialog(DIALOG_PROGRESS_UPLOAD);
					}
				}
			}
			});

		builder.show();
	}
	

	protected class LocalOpenstreetmapAdapter extends BaseExpandableListAdapter {
		Map<String, List<OpenstreetmapPoint>> data = new LinkedHashMap<String, List<OpenstreetmapPoint>>();
		List<String> category = new ArrayList<String>();
		List<String> filterCategory = null;
		

		public LocalOpenstreetmapAdapter() {
		}
		
		public void clear() {
			data.clear();
			category.clear();
			filterCategory = null;
			notifyDataSetChanged();
		}

		public List<OpenstreetmapPoint> values() {
			List<OpenstreetmapPoint> values = new ArrayList<OpenstreetmapPoint>();
			for (List<OpenstreetmapPoint> v : data.values()) {
				values.addAll(v);
			}
			return values;
		}

		public void delete(OpenstreetmapPoint i) {
			final AmenityIndexRepositoryOdb repo = ((OsmandApplication) getApplication()).getResourceManager().getUpdatablePoiDb();
			android.util.Log.d(LogUtil.TAG, "Delete " + i);
			db.deleteOpenstreetmap(i);
			String c = i.getType();
			if(c != null){
				data.get(c).remove(i);
				// We need to re-insert the POI if it is a delete or modify
				repo.deleteAmenities(i.getId() << 1);
				repo.clearCache();
			}
			listAdapter.notifyDataSetChanged();
		}
		
		public void cancelFilter(){
			filterCategory = null;
			notifyDataSetChanged();
		}
		
		public void filterCategories(String... types) {
			List<String> filter = new ArrayList<String>();
			List<String> source = filterCategory == null ? category : filterCategory;
			for (String info : source) {
				for (String ts : types) {
					if (info.compareTo(ts) == 0) {
						filter.add(info);
					}
				}
			}
			filterCategory = filter;
			notifyDataSetChanged();
		}
		
		public void addOpenstreetmapPoint(OpenstreetmapPoint info) {
			int found = -1;
			// search from end
			for (int i = category.size() - 1; i >= 0; i--) {
				String cat = category.get(i);
				if (cat.compareTo(info.getType()) == 0) {
					found = i;
					break;
				}
			}
			if (found == -1) {
				found = category.size();
				category.add(info.getType());
			}
			if (!data.containsKey(category.get(found))) {
				data.put(category.get(found), new ArrayList<OpenstreetmapPoint>());
			}
			data.get(category.get(found)).add(info);
		}

		@Override
		public OpenstreetmapPoint getChild(int groupPosition, int childPosition) {
			String cat = filterCategory != null ? filterCategory.get(groupPosition) : category.get(groupPosition);
			return data.get(cat).get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			// it would be unusable to have 10000 local categories
			return groupPosition * 10000 + childPosition;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			View v = convertView;
			final OpenstreetmapPoint child = (OpenstreetmapPoint) getChild(groupPosition, childPosition);
			if (v == null ) {
				LayoutInflater inflater = getLayoutInflater();
				v = inflater.inflate(net.osmand.plus.R.layout.local_openstreetmap_list_item, parent, false);
			}
			TextView viewName = ((TextView) v.findViewById(R.id.local_openstreetmap_name));
			viewName.setText("(" + child.getSubtype() + ") " + child.getName());
			if (child.getAction() == OpenstreetmapUtil.Action.CREATE) {
				viewName.setTextColor(getResources().getColor(R.color.osm_create));
			} else if (child.getAction() == OpenstreetmapUtil.Action.MODIFY) {
				viewName.setTextColor(getResources().getColor(R.color.osm_modify));
			} else if (child.getAction() == OpenstreetmapUtil.Action.DELETE) {
				viewName.setTextColor(getResources().getColor(R.color.osm_delete));
			}

			return v;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			View v = convertView;
			String group = getGroup(groupPosition);
			if (v == null) {
				LayoutInflater inflater = getLayoutInflater();
				v = inflater.inflate(net.osmand.plus.R.layout.local_openstreetmap_list_item_category, parent, false);
			}
			StringBuilder t = new StringBuilder(group);
			TextView nameView = ((TextView) v.findViewById(R.id.local_openstreetmap_category_name));
			t.append("  [").append(getChildrenCount(groupPosition));
			if(getString(R.string.local_openstreetmap_items).length() > 0){
				t.append(" ").append(getString(R.string.local_openstreetmap_items));
			}
			if(getString(R.string.local_openstreetmap_items).length() > 0){
				t.append(" ").append(getString(R.string.local_openstreetmap_items));
			}
			t.append("]");
			nameView.setText(t.toString());

			return v;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			String cat = filterCategory != null ? filterCategory.get(groupPosition) : category.get(groupPosition);
			return data.get(cat).size();
		}

		@Override
		public String getGroup(int groupPosition) {
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

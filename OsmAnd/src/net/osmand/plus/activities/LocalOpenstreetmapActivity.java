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
import net.osmand.access.AccessibleToast;
import net.osmand.osm.EntityInfo;
import net.osmand.osm.Node;
import net.osmand.plus.AmenityIndexRepositoryOdb;
import net.osmand.plus.OpenstreetmapsDbHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.ProgressDialogImplementation;
import net.osmand.plus.R;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.TextView;
import android.widget.Toast;

public class LocalOpenstreetmapActivity extends OsmandExpandableListActivity {

	/** dialogs **/
	protected static final int DIALOG_PROGRESS_UPLOAD = 0;
	protected static final int MENU_GROUP = 0;

	private LocalOpenstreetmapAdapter listAdapter;

	private OpenstreetmapsDbHelper db;

	private OpenstreetmapRemoteUtil remote;

	protected OpenstreetmapPoint[] toUpload;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.local_openstreetmap);
		listAdapter = new LocalOpenstreetmapAdapter();

		getExpandableListView().setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
				long packedPos = ((ExpandableListContextMenuInfo)menuInfo).packedPosition;
				int group = ExpandableListView.getPackedPositionGroup(packedPos);
				int child = ExpandableListView.getPackedPositionChild(packedPos);
				MenuInflater inflater = getMenuInflater();
				if (child >= 0 && group >= 0) {
				    inflater.inflate(R.menu.localosm_child, menu);
				} else if (group >= 0) { //group menu
				    inflater.inflate(R.menu.localosm_group, menu);
				}
			}
		});
		setListAdapter(listAdapter);

		db = new OpenstreetmapsDbHelper(this);

		remote = new OpenstreetmapRemoteUtil(this, this.getWindow().getDecorView());

		findViewById(R.id.UploadAllButton).setOnClickListener(new View.OnClickListener() {


			@Override
			public void onClick(View v) {
				//NOTE, the order of upload is important, there can be more edits per one POI!!
				toUpload = listAdapter.values().toArray(new OpenstreetmapPoint[0]);
				showDialog(DIALOG_PROGRESS_UPLOAD);
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		listAdapter.clear();
		List<OpenstreetmapPoint> l = db.getOpenstreetmapPoints();
		android.util.Log.d(LogUtil.TAG, "List of POI " + l.size() + " length");
		for (OpenstreetmapPoint p : l) {
			listAdapter.addOpenstreetmapPoint(p);
		}
		listAdapter.notifyDataSetChanged();
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		long packedPos = ((ExpandableListContextMenuInfo)item.getMenuInfo()).packedPosition;
		int group = ExpandableListView.getPackedPositionGroup(packedPos);
		int child = ExpandableListView.getPackedPositionChild(packedPos);
	    switch (item.getItemId()) {
	        case R.id.showpoi:
				OsmandSettings settings = OsmandApplication.getSettings();
				OpenstreetmapPoint info = (OpenstreetmapPoint) listAdapter.getChild(group, child);
				settings.setMapLocationToShow(info.getLatitude(), info.getLongitude(), settings.getLastKnownMapZoom());
				MapActivity.launchMapActivityMoveToTop(LocalOpenstreetmapActivity.this);
	            return true;
	        case R.id.deletepoimod:
				info = (OpenstreetmapPoint) listAdapter.getChild(group, child);
				listAdapter.delete(info);
	            return true;
	        case R.id.uploadpoimods:
				List<OpenstreetmapPoint> list = listAdapter.data.get(group);
				if (list != null) {
					toUpload = list.toArray(new OpenstreetmapPoint[] {});
					showDialog(DIALOG_PROGRESS_UPLOAD);
					return true;
				}
	        default:
	            return super.onContextItemSelected(item);
	    }
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (db != null) {
			db.close();
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
			case DIALOG_PROGRESS_UPLOAD:
				return ProgressDialogImplementation.createProgressDialog(
						LocalOpenstreetmapActivity.this,
						getString(R.string.uploading),
						getString(R.string.local_openstreetmap_uploading_poi),
						ProgressDialog.STYLE_HORIZONTAL).getDialog();
		}
		return null;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
		switch (id) {
		case DIALOG_PROGRESS_UPLOAD:
			UploadOpenstreetmapPointAsyncTask uploadTask = new UploadOpenstreetmapPointAsyncTask((ProgressDialog) dialog, remote,
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
				Node n;
				if ((n = remote.commitNodeImpl(p.getAction(), p.getEntity(), entityInfo, p.getComment())) != null) {
					remote.updateNodeInIndexes(LocalOpenstreetmapActivity.this, p.getAction(), n, p.getEntity());
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
				AccessibleToast.makeText(LocalOpenstreetmapActivity.this, MessageFormat.format(getString(R.string.local_openstreetmap_poi_were_uploaded), result.intValue()), Toast.LENGTH_LONG).show();
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
	
	protected class LocalOpenstreetmapAdapter extends BaseExpandableListAdapter {
		Map<Long, List<OpenstreetmapPoint>> data = new LinkedHashMap<Long, List<OpenstreetmapPoint>>();
		List<Long> category = new ArrayList<Long>();
		

		public LocalOpenstreetmapAdapter() {
		}
		
		public void clear() {
			data.clear();
			category.clear();
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
			final AmenityIndexRepositoryOdb repo = getMyApplication().getResourceManager().getUpdatablePoiDb();
			android.util.Log.d(LogUtil.TAG, "Delete " + i);
			db.deleteOpenstreetmap(i);
			Long c = i.getId();
			if(c != null){
				List<OpenstreetmapPoint> list = data.get(c);
				list.remove(i);
				if (list.isEmpty()) {
					data.remove(c);
					category.remove(c);
				}
				repo.deleteAmenities(i.getId() << 1);
				// We need to re-insert the POI if it is a delete or modify
				for (OpenstreetmapPoint p : list) {
					remote.updateNodeInIndexes(LocalOpenstreetmapActivity.this, p.getAction(), p.getEntity(), p.getEntity());
				}
				repo.clearCache();
			}
			listAdapter.notifyDataSetChanged();
		}
		
		public void cancelFilter(){
			notifyDataSetChanged();
		}
		
		public void addOpenstreetmapPoint(OpenstreetmapPoint info) {
			int found = -1;
			// search from end
			for (int i = category.size() - 1; i >= 0; i--) {
				Long cat = category.get(i);
				if (cat.compareTo(info.getId()) == 0) {
					found = i;
					break;
				}
			}
			if (found == -1) {
				found = category.size();
				category.add(info.getId());
			}
			if (!data.containsKey(category.get(found))) {
				data.put(category.get(found), new ArrayList<OpenstreetmapPoint>());
			}
			data.get(category.get(found)).add(info);
		}

		@Override
		public OpenstreetmapPoint getChild(int groupPosition, int childPosition) {
			Long cat = category.get(groupPosition);
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
			Long group = getGroup(groupPosition);
			if (v == null) {
				LayoutInflater inflater = getLayoutInflater();
				v = inflater.inflate(net.osmand.plus.R.layout.local_openstreetmap_list_item_category, parent, false);
			}
			StringBuilder t = new StringBuilder();
			t.append(" id:").append(group);
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
			Long cat = category.get(groupPosition);
			return data.get(cat).size();
		}

		@Override
		public Long getGroup(int groupPosition) {
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
}

package net.osmand.plus.osmedit;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.LogUtil;
import net.osmand.access.AccessibleToast;
import net.osmand.osm.EntityInfo;
import net.osmand.osm.Node;
import net.osmand.plus.AmenityIndexRepositoryOdb;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.ProgressDialogImplementation;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandExpandableListActivity;
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

	private OpenstreetmapsDbHelper dbpoi;
	private OsmBugsDbHelper dbbug;

	private OpenstreetmapRemoteUtil remotepoi;
	private OsmBugsRemoteUtil remotebug;

	protected OsmPoint[] toUpload;

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

		dbpoi = new OpenstreetmapsDbHelper(this);
		dbbug = new OsmBugsDbHelper(this);

		remotepoi = new OpenstreetmapRemoteUtil(this, this.getWindow().getDecorView());
		remotebug = new OsmBugsRemoteUtil();

		findViewById(R.id.UploadAllButton).setOnClickListener(new View.OnClickListener() {


			@Override
			public void onClick(View v) {
				//NOTE, the order of upload is important, there can be more edits per one POI!!
				toUpload = listAdapter.values().toArray(new OsmPoint[0]);
				showDialog(DIALOG_PROGRESS_UPLOAD);
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		listAdapter.clear();
		List<OpenstreetmapPoint> l1 = dbpoi.getOpenstreetmapPoints();
		List<OsmbugsPoint> l2 = dbbug.getOsmbugsPoints();
		android.util.Log.d(LogUtil.TAG, "List " + (l1.size() + l2.size()) + " length");
		for (OpenstreetmapPoint p : l1) {
			listAdapter.addOsmPoint(p);
		}
		for (OsmbugsPoint p : l2) {
			listAdapter.addOsmPoint(p);
		}
		listAdapter.notifyDataSetChanged();
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		long packedPos = ((ExpandableListContextMenuInfo)item.getMenuInfo()).packedPosition;
		int group = ExpandableListView.getPackedPositionGroup(packedPos);
		int child = ExpandableListView.getPackedPositionChild(packedPos);
		int itemId = item.getItemId();
		if(itemId == R.id.showmod) {
			OsmandSettings settings = getMyApplication().getSettings();
			OsmPoint info = (OsmPoint) listAdapter.getChild(group, child);
			settings.setMapLocationToShow(info.getLatitude(), info.getLongitude(), settings.getLastKnownMapZoom());
			MapActivity.launchMapActivityMoveToTop(LocalOpenstreetmapActivity.this);
			return true;
		} else if(itemId == R.id.deletemod) {
			OsmPoint info = (OsmPoint) listAdapter.getChild(group, child);
			if (info.getGroup() == OsmPoint.Group.POI) {
				dbpoi.deleteAllPOIModifications(info.getId());
			} else if (info.getGroup() == OsmPoint.Group.BUG) {
				dbbug.deleteAllBugModifications(info.getId());
			}
			listAdapter.delete(info);
			return true;
		} else if(itemId == R.id.uploadmods) {
			List<OsmPoint> list = listAdapter.data.get(group);
			if (list != null) {
				toUpload = list.toArray(new OsmPoint[] {});
				showDialog(DIALOG_PROGRESS_UPLOAD);
				return true;
			}
	    }
		
		return super.onContextItemSelected(item);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (dbpoi != null) {
			dbpoi.close();
		}
		if (dbbug != null) {
			dbbug.close();
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
			case DIALOG_PROGRESS_UPLOAD:
				return ProgressDialogImplementation.createProgressDialog(
						LocalOpenstreetmapActivity.this,
						getString(R.string.uploading),
						getString(R.string.local_openstreetmap_uploading),
						ProgressDialog.STYLE_HORIZONTAL).getDialog();
		}
		return null;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
		switch (id) {
		case DIALOG_PROGRESS_UPLOAD:
			UploadOpenstreetmapPointAsyncTask uploadTask = new UploadOpenstreetmapPointAsyncTask((ProgressDialog) dialog, remotepoi,
					 remotebug, toUpload.length);
			uploadTask.execute(toUpload);
			break;
		}
	}

	public class UploadOpenstreetmapPointAsyncTask extends AsyncTask<OsmPoint, OsmPoint, Integer> {

		private ProgressDialog progress;

		private OpenstreetmapRemoteUtil remotepoi;

		private OsmBugsRemoteUtil remotebug;

		private int listSize = 0;

		private boolean interruptUploading = false;

		public UploadOpenstreetmapPointAsyncTask(ProgressDialog progress,
												 OpenstreetmapRemoteUtil remotepoi,
												 OsmBugsRemoteUtil remotebug,
												 int listSize) {
			this.progress = progress;
			this.remotepoi = remotepoi;
			this.remotebug = remotebug;
			this.listSize = listSize;
		}

		@Override
		protected Integer doInBackground(OsmPoint... points) {
			int uploaded = 0;

			for (OsmPoint point : points) {
				if (interruptUploading) break;

				if (point.getGroup() == OsmPoint.Group.POI) {
					OpenstreetmapPoint p = (OpenstreetmapPoint) point;
					EntityInfo entityInfo = null;
					if (OsmPoint.Action.CREATE != p.getAction()) {
						entityInfo = remotepoi.loadNode(p.getEntity());
					}
					Node n;
					if ((n = remotepoi.commitNodeImpl(p.getAction(), p.getEntity(), entityInfo, p.getComment())) != null) {
						remotepoi.updateNodeInIndexes(LocalOpenstreetmapActivity.this, p.getAction(), n, p.getEntity());
						publishProgress(p);
						uploaded++;
					}
				} else if (point.getGroup() == OsmPoint.Group.BUG) {
					OsmbugsPoint p = (OsmbugsPoint) point;
					if (p.getAction() == OsmPoint.Action.CREATE) {
						remotebug.createNewBug(p.getLatitude(), p.getLongitude(), p.getText(), p.getAuthor());
					} else if (p.getAction() == OsmPoint.Action.MODIFY) {
						remotebug.addingComment(p.getId(), p.getText(), p.getAuthor());
					} else if (p.getAction() == OsmPoint.Action.DELETE) {
						remotebug.closingBug(p.getId());
					}
					publishProgress(p);
					uploaded++;
				}
			}

			return Integer.valueOf(uploaded);
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
				AccessibleToast.makeText(LocalOpenstreetmapActivity.this, MessageFormat.format(getString(R.string.local_openstreetmap_were_uploaded), result.intValue()), Toast.LENGTH_LONG).show();
			}
			removeDialog(DIALOG_PROGRESS_UPLOAD);
		}

		public void setInterruptUploading(boolean b) {
			interruptUploading = b;
		}

		@Override
		protected void onProgressUpdate(OsmPoint... points) {
			listAdapter.delete(points[0]);
			progress.incrementProgressBy(1);
		}

	}
	
	protected class LocalOpenstreetmapAdapter extends BaseExpandableListAdapter {
		Map<Long, List<OsmPoint>> data = new LinkedHashMap<Long, List<OsmPoint>>();
		List<Long> category = new ArrayList<Long>();
		

		public LocalOpenstreetmapAdapter() {
		}
		
		public void clear() {
			data.clear();
			category.clear();
			notifyDataSetChanged();
		}

		public List<OsmPoint> values() {
			List<OsmPoint> values = new ArrayList<OsmPoint>();
			for (List<OsmPoint> v : data.values()) {
				values.addAll(v);
			}
			return values;
		}

		public void delete(OsmPoint i) {
			final AmenityIndexRepositoryOdb repo = getMyApplication().getResourceManager().getUpdatablePoiDb();
			android.util.Log.d(LogUtil.TAG, "Delete " + i);

			if (i.getGroup() == OsmPoint.Group.POI) {
				dbpoi.deleteOpenstreetmap((OpenstreetmapPoint) i);
			} else if (i.getGroup() == OsmPoint.Group.BUG) {
				dbbug.deleteOsmbugs((OsmbugsPoint) i);
			}
			Long c = i.getId();
			if(c != null){
				List<OsmPoint> list = data.get(c);
				list.remove(i);
				if (list.isEmpty()) {
					data.remove(c);
					category.remove(c);
				}
				repo.deleteAmenities(i.getId() << 1);
				// We need to re-insert the POI if it is a delete or modify
				for (OsmPoint point : list) {
					if (point.getGroup() == OsmPoint.Group.POI) {
						OpenstreetmapPoint p = (OpenstreetmapPoint) point;
						remotepoi.updateNodeInIndexes(LocalOpenstreetmapActivity.this, p.getAction(), p.getEntity(), p.getEntity());
					}
				}
				repo.clearCache();
			}
			listAdapter.notifyDataSetChanged();
		}
		
		public void cancelFilter(){
			notifyDataSetChanged();
		}
		
		public void addOsmPoint(OsmPoint info) {
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
				data.put(category.get(found), new ArrayList<OsmPoint>());
			}
			data.get(category.get(found)).add(info);
		}

		@Override
		public OsmPoint getChild(int groupPosition, int childPosition) {
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
			final OsmPoint child = (OsmPoint) getChild(groupPosition, childPosition);
			if (v == null ) {
				LayoutInflater inflater = getLayoutInflater();
				v = inflater.inflate(net.osmand.plus.R.layout.local_openstreetmap_list_item, parent, false);
			}
			TextView viewName = ((TextView) v.findViewById(R.id.local_openstreetmap_name));
			if (child.getGroup() == OsmPoint.Group.POI)
				viewName.setText("(" + ((OpenstreetmapPoint) child).getSubtype() + ") " + ((OpenstreetmapPoint) child).getName());
			else if (child.getGroup() == OsmPoint.Group.BUG)
				viewName.setText("(" + ((OsmbugsPoint) child).getAuthor() + ") " + ((OsmbugsPoint) child).getText());
			if (child.getAction() == OsmPoint.Action.CREATE) {
				viewName.setTextColor(getResources().getColor(R.color.osm_create));
			} else if (child.getAction() == OsmPoint.Action.MODIFY) {
				viewName.setTextColor(getResources().getColor(R.color.osm_modify));
			} else if (child.getAction() == OsmPoint.Action.DELETE) {
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

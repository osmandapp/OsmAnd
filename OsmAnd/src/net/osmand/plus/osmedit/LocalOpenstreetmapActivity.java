package net.osmand.plus.osmedit;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import net.osmand.access.AccessibleToast;
import net.osmand.osm.EntityInfo;
import net.osmand.osm.Node;
import net.osmand.plus.AmenityIndexRepositoryOdb;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.ProgressDialogImplementation;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.osmedit.OsmPoint.Action;
import android.app.Dialog;
import android.app.ListActivity;
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
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class LocalOpenstreetmapActivity extends ListActivity {

	/** dialogs **/
	protected static final int DIALOG_PROGRESS_UPLOAD = 0;
	protected static final int MENU_GROUP = 0;

	private LocalOpenstreetmapAdapter listAdapter;

	private OpenstreetmapsDbHelper dbpoi;
	private OsmBugsDbHelper dbbug;

	private OpenstreetmapRemoteUtil remotepoi;
	private OsmBugsRemoteUtil remotebug;

	protected OsmPoint[] toUpload;
	private ArrayList<OsmPoint> dataPoints;
	
	
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.local_openstreetmap);
		listAdapter = new LocalOpenstreetmapAdapter();

		getListView().setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
				MenuInflater inflater = getMenuInflater();
				inflater.inflate(R.menu.localosm_child, menu);
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
				toUpload = dataPoints.toArray(new OsmPoint[0]);
				showDialog(DIALOG_PROGRESS_UPLOAD);
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		dataPoints = new ArrayList<OsmPoint>(); 
		List<OpenstreetmapPoint> l1 = dbpoi.getOpenstreetmapPoints();
		List<OsmbugsPoint> l2 = dbbug.getOsmbugsPoints();
		dataPoints.addAll(l1);
		dataPoints.addAll(l2);
		listAdapter.clear();
		for (OpenstreetmapPoint p : l1) {
			listAdapter.add(p);
		}
		for (OsmbugsPoint p : l2) {
			listAdapter.add(p);
		}
		listAdapter.notifyDataSetChanged();
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		int pos = ((AdapterContextMenuInfo)item.getMenuInfo()).position;
		int itemId = item.getItemId();
		if(itemId == R.id.showmod) {
			OsmandSettings settings = getMyApplication().getSettings();
			OsmPoint info = (OsmPoint) listAdapter.getItem(pos);
			settings.setMapLocationToShow(info.getLatitude(), info.getLongitude(), settings.getLastKnownMapZoom());
			MapActivity.launchMapActivityMoveToTop(LocalOpenstreetmapActivity.this);
			return true;
		} else if(itemId == R.id.deletemod) {
			OsmPoint info = (OsmPoint) listAdapter.getItem(pos);
			if (info.getGroup() == OsmPoint.Group.POI) {
				dbpoi.deletePOI((OpenstreetmapPoint) info);
				if (info.getAction() == Action.CREATE) {
					AmenityIndexRepositoryOdb repo = getMyApplication().getResourceManager().getUpdatablePoiDb();
					repo.deleteAmenities(info.getId() << 1);
					repo.clearCache();
				}
			} else if (info.getGroup() == OsmPoint.Group.BUG) {
				dbbug.deleteAllBugModifications((OsmbugsPoint) info);
			}
			listAdapter.delete(info);
			return true;
		} else if (itemId == R.id.uploadmods) {
			toUpload = new OsmPoint[]{ listAdapter.getItem(pos)};
			showDialog(DIALOG_PROGRESS_UPLOAD);
			return true;
		}
		
		return super.onContextItemSelected(item);
	}
	
	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getApplication();
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

		public UploadOpenstreetmapPointAsyncTask(ProgressDialog progress, OpenstreetmapRemoteUtil remotepoi, OsmBugsRemoteUtil remotebug,
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
				if (interruptUploading)
					break;

				if (point.getGroup() == OsmPoint.Group.POI) {
					OpenstreetmapPoint p = (OpenstreetmapPoint) point;
					EntityInfo entityInfo = null;
					if (OsmPoint.Action.CREATE != p.getAction()) {
						entityInfo = remotepoi.loadNode(p.getEntity());
					}
					Node n;
					if ((n = remotepoi.commitNodeImpl(p.getAction(), p.getEntity(), entityInfo, p.getComment())) != null) {
						remotepoi.updateNodeInIndexes(LocalOpenstreetmapActivity.this, p.getAction(), n, p.getEntity());
						dbpoi.deletePOI(p);
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
						remotebug.closingBug(p.getId(), p.getText(), p.getAuthor());
					}
					dbbug.deleteAllBugModifications(p);
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
			if (result != null) {
				AccessibleToast.makeText(LocalOpenstreetmapActivity.this,
						MessageFormat.format(getString(R.string.local_openstreetmap_were_uploaded), result.intValue()), Toast.LENGTH_LONG)
						.show();
			}
			removeDialog(DIALOG_PROGRESS_UPLOAD);
		}

		public void setInterruptUploading(boolean b) {
			interruptUploading = b;
		}

		@Override
		protected void onProgressUpdate(OsmPoint... points) {
			for(OsmPoint p : points) {
				listAdapter.delete(p);
				progress.incrementProgressBy(1);
			}
		}

	}
	
	protected class LocalOpenstreetmapAdapter extends ArrayAdapter<OsmPoint> {

		public LocalOpenstreetmapAdapter() {
			super(LocalOpenstreetmapActivity.this, net.osmand.plus.R.layout.local_openstreetmap_list_item);
		}
		


		public void delete(OsmPoint i) {
			dataPoints.remove(i);
			remove(i);
			listAdapter.notifyDataSetChanged();
		}
		
		public void cancelFilter(){
			notifyDataSetChanged();
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			final OsmPoint child = getItem(position);
			if (v == null ) {
				LayoutInflater inflater = getLayoutInflater();
				v = inflater.inflate(net.osmand.plus.R.layout.local_openstreetmap_list_item, parent, false);
			}
			TextView viewName = ((TextView) v.findViewById(R.id.local_openstreetmap_name));
			String idPrefix = (child.getGroup() == OsmPoint.Group.POI ? "POI " : "Bug ") + " id: " + child.getId();
			if (child.getGroup() == OsmPoint.Group.POI)
				viewName.setText(idPrefix + " (" + ((OpenstreetmapPoint) child).getSubtype() + ") " + ((OpenstreetmapPoint) child).getName());
			else if (child.getGroup() == OsmPoint.Group.BUG)
				viewName.setText(idPrefix +  " (" + ((OsmbugsPoint) child).getAuthor() + ") " + ((OsmbugsPoint) child).getText());
			if (child.getAction() == OsmPoint.Action.CREATE) {
				viewName.setTextColor(getResources().getColor(R.color.osm_create));
			} else if (child.getAction() == OsmPoint.Action.MODIFY) {
				viewName.setTextColor(getResources().getColor(R.color.osm_modify));
			} else if (child.getAction() == OsmPoint.Action.DELETE) {
				viewName.setTextColor(getResources().getColor(R.color.osm_delete));
			}

			return v;
		}

	}
}

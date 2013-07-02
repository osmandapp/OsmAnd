package net.osmand.plus.osmedit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import net.osmand.access.AccessibleToast;
import net.osmand.osm.edit.EntityInfo;
import net.osmand.osm.edit.Node;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.ProgressDialogImplementation;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandListActivity;
import net.osmand.plus.osmedit.OsmPoint.Action;

import org.xmlpull.v1.XmlSerializer;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Xml;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.Window;

public class LocalOpenstreetmapActivity extends OsmandListActivity {

	/** dialogs **/
	protected static final int DIALOG_PROGRESS_UPLOAD = 0;
	protected static final int MENU_GROUP = 0;
	private static final int UPLOAD_ID = 1;
	private static final int BACKUP_ID = 2;

	private LocalOpenstreetmapAdapter listAdapter;

	private OpenstreetmapsDbHelper dbpoi;
	private OsmBugsDbHelper dbbug;

	private OpenstreetmapRemoteUtil remotepoi;
	private OsmBugsRemoteUtil remotebug;

	protected OsmPoint[] toUpload = new OsmPoint[0];
	private ArrayList<OsmPoint> dataPoints;
	
	
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.local_openstreetmap);
		getSupportActionBar().setTitle(R.string.download_files);
		setSupportProgressBarIndeterminateVisibility(false);
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
		remotebug = new OsmBugsRemoteUtil(getMyApplication());

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		createMenuItem(menu, UPLOAD_ID, R.string.local_openstreetmap_uploadall, R.drawable.ic_action_gup_light, R.drawable.ic_action_gup_dark,
				MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		createMenuItem(menu, BACKUP_ID, R.string.local_osm_changes_backup, R.drawable.ic_action_gsave_light, R.drawable.ic_action_gsave_dark,
				MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		if (item.getItemId() == UPLOAD_ID) {
			toUpload = dataPoints.toArray(new OsmPoint[0]);
			showDialog(DIALOG_PROGRESS_UPLOAD);
			return true;
		} else if (item.getItemId() == BACKUP_ID) {
			new BackupOpenstreetmapPointAsyncTask().execute(dataPoints.toArray(new OsmPoint[0]));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	@Override
	protected void onResume() {
		super.onResume();
		
		dataPoints = new ArrayList<OsmPoint>(); 
		List<OpenstreetmapPoint> l1 = dbpoi.getOpenstreetmapPoints();
		List<OsmNotesPoint> l2 = dbbug.getOsmbugsPoints();
		dataPoints.addAll(l1);
		dataPoints.addAll(l2);
		listAdapter.clear();
		for (OpenstreetmapPoint p : l1) {
			listAdapter.add(p);
		}
		for (OsmNotesPoint p : l2) {
			listAdapter.add(p);
		}
		listAdapter.notifyDataSetChanged();
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		int pos = ((android.widget.AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).position;
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
			} else if (info.getGroup() == OsmPoint.Group.BUG) {
				dbbug.deleteAllBugModifications((OsmNotesPoint) info);
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
	
	public class BackupOpenstreetmapPointAsyncTask extends AsyncTask<OsmPoint, OsmPoint, String> {

		
		private File osmchange;

		public BackupOpenstreetmapPointAsyncTask() {
			OsmandApplication app = LocalOpenstreetmapActivity.this.getMyApplication();
			osmchange = app.getAppPath("poi_modification.osc");
		}
		

		@Override
		protected String doInBackground(OsmPoint... points) {
			FileOutputStream out = null;
			try {
				out = new FileOutputStream(osmchange);
				XmlSerializer sz = Xml.newSerializer();
			
				sz.setOutput(out, "UTF-8");
				sz.startDocument("UTF-8", true);
				sz.startTag("", "osmChange");
				sz.attribute("", "generator", "OsmAnd");
				sz.attribute("", "version", "0.6");
				sz.startTag("", "create");
				writeContent(sz, points, OsmPoint.Action.CREATE);
				sz.endTag("", "create");
				sz.startTag("", "modify");
				writeContent(sz, points, OsmPoint.Action.MODIFY);
				sz.endTag("", "modify");
				sz.startTag("", "delete");
				writeContent(sz, points, OsmPoint.Action.DELETE);
				sz.endTag("", "delete");
				sz.endTag("", "osmChange");
				sz.endDocument();
			} catch (Exception e) {
				return e.getMessage();
			} finally {
				try {
					if(out!= null) out.close();
				} catch (IOException e) {
				}
			}

			return null;
		}

		private void writeContent(XmlSerializer sz, OsmPoint[] points, Action a) throws IllegalArgumentException, IllegalStateException, IOException {
			for (OsmPoint point : points) {
				if (point.getGroup() == OsmPoint.Group.POI) {
					OpenstreetmapPoint p = (OpenstreetmapPoint) point;
					if (p.getAction() == a) {
						sz.startTag("", "node");
						sz.attribute("", "lat", p.getLatitude() + "");
						sz.attribute("", "lon", p.getLongitude() + "");
						sz.attribute("", "id", p.getId() + "");
						for (String tag : p.getEntity().getTagKeySet()) {
							String val = p.getEntity().getTag(tag);
							sz.startTag("", "tag");
							sz.attribute("", "k", tag);
							sz.attribute("", "v", val);
							sz.endTag("", "tag");
						}
						sz.endTag("", "node");
					}
				} else if (point.getGroup() == OsmPoint.Group.BUG) {
					OsmNotesPoint p = (OsmNotesPoint) point;
					if (p.getAction() == a) {
						sz.startTag("", "note");
						sz.attribute("", "lat", p.getLatitude() + "");
						sz.attribute("", "lon", p.getLongitude() + "");
						sz.attribute("", "id", p.getId() + "");
						sz.startTag("", "comment");
						sz.attribute("", "text", p.getText() +"");
						sz.endTag("", "comment");
						sz.endTag("", "note");
					}
				}
			}			
		}


		@Override
		protected void onPreExecute() {
			LocalOpenstreetmapActivity.this.setProgressBarIndeterminateVisibility(true);
		}

		@Override
		protected void onPostExecute(String result) {
			LocalOpenstreetmapActivity.this.setProgressBarIndeterminateVisibility(false);
			if (result != null) {
				AccessibleToast.makeText(LocalOpenstreetmapActivity.this, getString(R.string.local_osm_changes_backup_failed) + " " + result, Toast.LENGTH_LONG).show();
			} else {
				AccessibleToast.makeText(LocalOpenstreetmapActivity.this, getString(R.string.local_osm_changes_backup_successful, osmchange.getAbsolutePath()), Toast.LENGTH_LONG).show();
			}
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
					Node n = remotepoi.commitNodeImpl(p.getAction(), p.getEntity(), entityInfo, p.getComment(), false);
					if (n != null) {
						dbpoi.deletePOI(p);
						publishProgress(p);
						uploaded++;
					}
				} else if (point.getGroup() == OsmPoint.Group.BUG) {
					OsmNotesPoint p = (OsmNotesPoint) point;
					boolean success = false;
					if (p.getAction() == OsmPoint.Action.CREATE) {
						success = remotebug.createNewBug(p.getLatitude(), p.getLongitude(), p.getText()) == null;
					} else if (p.getAction() == OsmPoint.Action.MODIFY) {
						success = remotebug.addingComment(p.getId(), p.getText()) == null;
					} else if (p.getAction() == OsmPoint.Action.DELETE) {
						success = remotebug.closingBug(p.getId(), p.getText()) == null;
					}
					if (success) {
						dbbug.deleteAllBugModifications(p);
						uploaded++;
						publishProgress(p);
					}
					
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
				viewName.setText(idPrefix +  " (" + ((OsmNotesPoint) child).getAuthor() + ") " + ((OsmNotesPoint) child).getText());
			if (child.getAction() == OsmPoint.Action.CREATE) {
				viewName.setTextColor(getResources().getColor(R.color.color_ok));
			} else if (child.getAction() == OsmPoint.Action.MODIFY) {
				viewName.setTextColor(getResources().getColor(R.color.color_update));
			} else if (child.getAction() == OsmPoint.Action.DELETE) {
				viewName.setTextColor(getResources().getColor(R.color.color_warning));
			}

			return v;
		}

	}
}

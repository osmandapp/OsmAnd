package net.osmand.plus.osmedit;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;

import net.osmand.osm.edit.EntityInfo;
import net.osmand.osm.edit.Node;

import java.util.HashMap;
import java.util.Map;

/**
 */
public class UploadOpenstreetmapPointAsyncTask
		extends AsyncTask<OsmPoint, OsmPoint, Map<OsmPoint, String>> {
	private ProgressDialog progress;
	private OpenstreetmapRemoteUtil remotepoi;
	private OsmBugsRemoteUtil remotebug;
	private int listSize = 0;
	private boolean interruptUploading = false;
	private OsmEditsUploadListener listener;
	private OsmEditingPlugin plugin;
	private final boolean closeChangeSet;

	public UploadOpenstreetmapPointAsyncTask(ProgressDialog progress,
											 OsmEditsUploadListener listener,
											 OsmEditingPlugin plugin,
											 int listSize,
											 boolean closeChangeSet) {
		this.progress = progress;
		this.plugin = plugin;
		this.remotepoi = plugin.getPoiModificationRemoteUtil();
		this.remotebug = plugin.getOsmNotesRemoteUtil();
		this.listSize = listSize;
		this.listener = listener;
		this.closeChangeSet = closeChangeSet;
	}

	@Override
	protected Map<OsmPoint, String> doInBackground(OsmPoint... points) {
		Map<OsmPoint, String> loadErrorsMap = new HashMap<>();

		boolean uploaded = false;
		for (OsmPoint point : points) {
			if (interruptUploading)
				break;

			if (point.getGroup() == OsmPoint.Group.POI) {
				OpenstreetmapPoint p = (OpenstreetmapPoint) point;
				EntityInfo entityInfo = null;
				if (OsmPoint.Action.CREATE != p.getAction()) {
					entityInfo = remotepoi.loadNode(p.getEntity());
				}
				Node n = remotepoi.commitNodeImpl(p.getAction(), p.getEntity(), entityInfo,
						p.getComment(), false);
				if (n != null) {
					uploaded = true;
					plugin.getDBPOI().deletePOI(p);
					publishProgress(p);
				}
				loadErrorsMap.put(point, n != null ? null : "Unknown problem");
			} else if (point.getGroup() == OsmPoint.Group.BUG) {
				OsmNotesPoint p = (OsmNotesPoint) point;
				String errorMessage = remotebug.commit(p, p.getText(), p.getAction()).warning;
				if (errorMessage == null) {
					plugin.getDBBug().deleteAllBugModifications(p);
					publishProgress(p);
				}
				loadErrorsMap.put(point, errorMessage);
			}
		}
		if(uploaded && closeChangeSet) {
			remotepoi.closeChangeSet();
		}

		return loadErrorsMap;
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
	protected void onPostExecute(Map<OsmPoint, String> loadErrorsMap) {
		if (progress.isShowing()) {
			progress.dismiss();
		}
		listener.uploadEnded(loadErrorsMap);
	}

	public void setInterruptUploading(boolean b) {
		interruptUploading = b;
	}

	@Override
	protected void onProgressUpdate(OsmPoint... points) {
		for (OsmPoint p : points) {
			listener.uploadUpdated(p);
			progress.incrementProgressBy(1);
		}
	}

}


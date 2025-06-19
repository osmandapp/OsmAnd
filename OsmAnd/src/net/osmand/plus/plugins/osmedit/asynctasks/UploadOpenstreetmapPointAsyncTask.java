package net.osmand.plus.plugins.osmedit.asynctasks;

import android.content.DialogInterface;
import android.net.TrafficStats;
import android.os.AsyncTask;

import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.EntityInfo;
import net.osmand.plus.dialogs.ProgressDialogFragment;
import net.osmand.plus.plugins.osmedit.data.OpenstreetmapPoint;
import net.osmand.plus.plugins.osmedit.helpers.OpenstreetmapRemoteUtil;
import net.osmand.plus.plugins.osmedit.helpers.OsmBugsRemoteUtil;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.OsmEditsUploadListener;
import net.osmand.plus.plugins.osmedit.data.OsmNotesPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint;

import java.util.HashMap;
import java.util.Map;

/**
 */
public class UploadOpenstreetmapPointAsyncTask
		extends AsyncTask<OsmPoint, OsmPoint, Map<OsmPoint, String>> {
	private final ProgressDialogFragment progress;
	private final OpenstreetmapRemoteUtil remotepoi;
	private final OsmBugsRemoteUtil remotebug;
	private final int listSize;
	private boolean interruptUploading;
	private final OsmEditsUploadListener listener;
	private final OsmEditingPlugin plugin;
	private final boolean closeChangeSet;
	private final boolean loadAnonymous;
	private static final int THREAD_ID = 10102;

	public UploadOpenstreetmapPointAsyncTask(ProgressDialogFragment progress,
											 OsmEditsUploadListener listener,
											 OsmEditingPlugin plugin,
											 int listSize,
											 boolean closeChangeSet,
											 boolean loadAnonymous) {
		this.progress = progress;
		this.plugin = plugin;
		this.remotepoi = plugin.getPoiModificationRemoteUtil();
		this.remotebug = plugin.getOsmNotesRemoteUtil();
		this.listSize = listSize;
		this.listener = listener;
		this.closeChangeSet = closeChangeSet;
		this.loadAnonymous = loadAnonymous;
	}

	@Override
	protected Map<OsmPoint, String> doInBackground(OsmPoint... points) {
		TrafficStats.setThreadStatsTag(THREAD_ID);

		Map<OsmPoint, String> loadErrorsMap = new HashMap<>();

		boolean uploaded = false;
		for (OsmPoint point : points) {
			if (interruptUploading) {
				break;
			}

			if (point.getGroup() == OsmPoint.Group.POI) {
				OpenstreetmapPoint p = (OpenstreetmapPoint) point;
				EntityInfo entityInfo = null;

				p.trimChangedTagNamesValues();

				if (OsmPoint.Action.CREATE != p.getAction()) {
					entityInfo = remotepoi.loadEntity(p.getEntity());
				}
				Entity n = remotepoi.commitEntityImpl(p.getAction(), p.getEntity(), entityInfo,
						p.getComment(), false, null);
				if (n != null) {
					uploaded = true;
					plugin.getDBPOI().deletePOI(p);
					publishProgress(p);
				}
				loadErrorsMap.put(point, n != null ? null : "Unknown problem");
			} else if (point.getGroup() == OsmPoint.Group.BUG) {
				OsmNotesPoint p = (OsmNotesPoint) point;
				String errorMessage = remotebug.commit(p, p.getText(), p.getAction(), loadAnonymous).warning;
				if (errorMessage == null) {
					plugin.getDBBug().deleteAllBugModifications(p);
					publishProgress(p);
				}
				loadErrorsMap.put(point, errorMessage);
			}
		}
		if (uploaded && closeChangeSet) {
			remotepoi.closeChangeSet();
		}

		return loadErrorsMap;
	}

	@Override
	protected void onPreExecute() {
		interruptUploading = false;

		progress.setOnDismissListener(dialog -> UploadOpenstreetmapPointAsyncTask.this.setInterruptUploading(true));
		progress.setMax(listSize);
		progress.setRetainInstance(true);
	}

	@Override
	protected void onPostExecute(Map<OsmPoint, String> loadErrorsMap) {
		if (progress != null && progress.isAdded()) {
			progress.dismissAllowingStateLoss();
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
			if (progress != null) {
				progress.incrementProgressBy(1);
			}
		}
	}
}


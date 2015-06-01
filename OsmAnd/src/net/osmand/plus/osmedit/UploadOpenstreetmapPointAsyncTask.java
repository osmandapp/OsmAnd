package net.osmand.plus.osmedit;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;

import net.osmand.osm.edit.EntityInfo;
import net.osmand.osm.edit.Node;

/**
 * Created by Denis
 * on 11.03.2015.
 */
public class UploadOpenstreetmapPointAsyncTask extends AsyncTask<OsmPoint, OsmPoint, Integer> {

	private ProgressDialog progress;

	private OpenstreetmapRemoteUtil remotepoi;

	private OsmBugsRemoteUtil remotebug;


	private int listSize = 0;

	private boolean interruptUploading = false;

	private Fragment ctx;

	private OsmEditingPlugin plugin;

	public UploadOpenstreetmapPointAsyncTask(ProgressDialog progress,Fragment ctx,
			OsmEditingPlugin plugin, 
			OpenstreetmapRemoteUtil remotepoi, OsmBugsRemoteUtil remotebug,
											 int listSize) {
		this.progress = progress;
		this.plugin = plugin;
		this.remotepoi = remotepoi;
		this.remotebug = remotebug;
		this.listSize = listSize;
		this.ctx = ctx;
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
					
					plugin.getDBPOI().deletePOI(p);
					publishProgress(p);
					uploaded++;
				}
			} else if (point.getGroup() == OsmPoint.Group.BUG) {
				OsmNotesPoint p = (OsmNotesPoint) point;
				boolean success = false;
				if (p.getAction() == OsmPoint.Action.CREATE) {
					success = remotebug.createNewBug(p.getLatitude(), p.getLongitude(), p.getText(), p.getAuthor()) == null;
				} else if (p.getAction() == OsmPoint.Action.MODIFY) {
					success = remotebug.addingComment(p.getId(), p.getText(), p.getAuthor()) == null;
				} else if (p.getAction() == OsmPoint.Action.DELETE) {
					success = remotebug.closingBug(p.getId(), p.getText(), p.getAuthor()) == null;
				}
				if (success) {
					plugin.getDBBug().deleteAllBugModifications(p);
					uploaded++;
					publishProgress(p);
				}

			}
		}

		return uploaded;
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
		progress.dismiss();
		if (ctx instanceof OsmEditsUploadListener){
			((OsmEditsUploadListener)ctx).uploadEnded(result);
		}
	}

	public void setInterruptUploading(boolean b) {
		interruptUploading = b;
	}

	@Override
	protected void onProgressUpdate(OsmPoint... points) {
		for(OsmPoint p : points) {
			if (ctx instanceof OsmEditsUploadListener){
				((OsmEditsUploadListener)ctx).uploadUpdated(p);
			}
			progress.incrementProgressBy(1);
		}
	}

}


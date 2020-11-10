package net.osmand.plus.osmedit;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HandleOsmNoteAsyncTask extends AsyncTask<Void, Void, OsmBugsUtil.OsmBugResult> {
	private OsmBugsUtil osmbugsUtil;
	private final OsmBugsUtil local;
	private final OsmBugsLayer.OpenStreetNote bug;
	private final OsmNotesPoint point;
	private final String text;
	private final OsmPoint.Action action;
	private final HandleBugListener handleBugListener;

	public HandleOsmNoteAsyncTask(@NonNull OsmBugsUtil osmbugsUtil, @NonNull OsmBugsUtil local,
	                              @Nullable OsmBugsLayer.OpenStreetNote bug, @Nullable OsmNotesPoint point,
	                              String text, OsmPoint.Action action,
	                              @Nullable HandleBugListener handleBugListener) {
		this.osmbugsUtil = osmbugsUtil;
		this.local = local;
		this.bug = bug;
		this.point = point;
		this.text = text;
		this.action = action;
		this.handleBugListener = handleBugListener;
	}

	@Override
	protected OsmBugsUtil.OsmBugResult doInBackground(Void... params) {
		if (bug != null) {
			OsmNotesPoint pnt = new OsmNotesPoint();
			pnt.setId(bug.getId());
			pnt.setLatitude(bug.getLatitude());
			pnt.setLongitude(bug.getLongitude());
			return osmbugsUtil.commit(pnt, text, action);
		} else if (point != null) {
			osmbugsUtil = local;
			return osmbugsUtil.modify(point, text);
		}
		return null;
	}

	protected void onPostExecute(OsmBugsUtil.OsmBugResult obj) {
		handleBugListener.onOsmBugHandled(obj, action, bug, point, text);
	}

	public interface HandleBugListener {

		void onOsmBugHandled(OsmBugsUtil.OsmBugResult obj, OsmPoint.Action action, OsmBugsLayer.OpenStreetNote bug,
		                     OsmNotesPoint point, String text);
	}
}

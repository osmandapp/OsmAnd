package net.osmand.plus.plugins.osmedit.asynctasks;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.plugins.osmedit.helpers.OsmBugsUtil;
import net.osmand.plus.plugins.osmedit.data.OsmNotesPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint.Action;

import static net.osmand.plus.plugins.osmedit.OsmBugsLayer.*;
import static net.osmand.plus.plugins.osmedit.helpers.OsmBugsUtil.*;

public class HandleOsmNoteAsyncTask extends AsyncTask<Void, Void, OsmBugResult> {
	private OsmBugsUtil osmbugsUtil;
	private final OsmBugsUtil local;
	private final OpenStreetNote bug;
	private final OsmNotesPoint point;
	private final String text;
	private final Action action;
	private final HandleBugListener handleBugListener;

	public HandleOsmNoteAsyncTask(@NonNull OsmBugsUtil osmbugsUtil, @NonNull OsmBugsUtil local,
	                              @Nullable OpenStreetNote bug, @Nullable OsmNotesPoint point,
	                              String text, Action action,
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
	protected OsmBugResult doInBackground(Void... params) {
		if (bug != null) {
			OsmNotesPoint point = new OsmNotesPoint();
			point.setId(bug.getId());
			point.setLatitude(bug.getLatitude());
			point.setLongitude(bug.getLongitude());
			return osmbugsUtil.commit(point, text, action);
		} else if (point != null) {
			osmbugsUtil = local;
			return osmbugsUtil.modify(point, text);
		}
		return null;
	}

	protected void onPostExecute(OsmBugResult obj) {
		handleBugListener.onOsmBugHandled(obj, action, bug, point, text);
	}

	public interface HandleBugListener {

		void onOsmBugHandled(OsmBugResult obj, Action action, OpenStreetNote bug,
		                     OsmNotesPoint point, String text);
	}
}

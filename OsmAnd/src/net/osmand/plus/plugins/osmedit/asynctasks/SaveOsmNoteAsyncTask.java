package net.osmand.plus.plugins.osmedit.asynctasks;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.data.OsmNotesPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint.Action;
import net.osmand.plus.plugins.osmedit.helpers.OsmBugsUtil;
import net.osmand.plus.views.layers.ContextMenuLayer.ApplyMovedObjectCallback;

public class SaveOsmNoteAsyncTask extends AsyncTask<OsmNotesPoint, Void, OsmNotesPoint> {

	private final OsmandApplication app;
	private final OsmEditingPlugin plugin = PluginsHelper.requirePlugin(OsmEditingPlugin.class);
	private final OsmBugsUtil bugsUtil;

	private final String text;
	@Nullable
	private final ApplyMovedObjectCallback callback;

	public SaveOsmNoteAsyncTask(@NonNull OsmandApplication app, @NonNull OsmBugsUtil bugsUtil,
			@NonNull String text, @Nullable ApplyMovedObjectCallback callback) {
		this.app = app;
		this.text = text;
		this.callback = callback;
		this.bugsUtil = bugsUtil;
	}

	@Override
	protected OsmNotesPoint doInBackground(OsmNotesPoint... params) {
		OsmNotesPoint mOsmNotesPoint = params[0];
		Action action = mOsmNotesPoint.getAction();
		plugin.getDBBug().deleteAllBugModifications(mOsmNotesPoint);
		OsmBugsUtil.OsmBugResult result = bugsUtil.commit(mOsmNotesPoint, text, action);
		return result == null ? null : result.local;
	}

	@Override
	protected void onPostExecute(OsmNotesPoint point) {
		if (point != null) {
			app.showToastMessage(R.string.osm_changes_added_to_local_edits);
		}
		if (callback != null) {
			callback.onApplyMovedObject(point != null, point);
		}
	}
}

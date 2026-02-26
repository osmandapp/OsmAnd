package net.osmand.plus.plugins.osmedit.asynctasks;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.plugins.osmedit.helpers.OsmBugsUtil;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.data.OsmNotesPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint.Action;
import net.osmand.plus.views.layers.ContextMenuLayer.ApplyMovedObjectCallback;

public class SaveOsmNoteAsyncTask extends AsyncTask<OsmNotesPoint, Void, OsmNotesPoint> {
	private final String mText;
	private final Context mContext;
	@Nullable
	private final ApplyMovedObjectCallback mCallback;
	private final OsmEditingPlugin plugin;
	private final OsmBugsUtil mOsmbugsUtil;

	public SaveOsmNoteAsyncTask(String text,
								@NonNull Context context,
								@Nullable ApplyMovedObjectCallback callback,
								OsmEditingPlugin plugin, OsmBugsUtil osmbugsUtil) {
		mText = text;
		mContext = context;
		mCallback = callback;
		this.plugin = plugin;
		mOsmbugsUtil = osmbugsUtil;
	}

	@Override
	protected OsmNotesPoint doInBackground(OsmNotesPoint... params) {
		OsmNotesPoint mOsmNotesPoint = params[0];
		Action action = mOsmNotesPoint.getAction();
		plugin.getDBBug().deleteAllBugModifications(mOsmNotesPoint);
		OsmBugsUtil.OsmBugResult result = mOsmbugsUtil.commit(mOsmNotesPoint, mText, action);
		return result == null ? null : result.local;
	}

	@Override
	protected void onPostExecute(OsmNotesPoint point) {
		if (point != null) {
			Toast.makeText(mContext, R.string.osm_changes_added_to_local_edits, Toast.LENGTH_LONG).show();
		}
		if (mCallback != null) {
			mCallback.onApplyMovedObject(point != null, point);
		}
	}
}

package net.osmand.plus.osmedit;

import android.os.AsyncTask;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.osmedit.OsmBugsUtil.OsmBugResult;

public class ValidateOsmLoginDetailsTask extends AsyncTask<Void, Void, OsmBugResult> {

	private OsmandApplication app;

	public ValidateOsmLoginDetailsTask(OsmandApplication app) {
		this.app = app;
	}

	@Override
	protected OsmBugResult doInBackground(Void... params) {
		OsmEditingPlugin plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);
		assert plugin != null;
		OsmBugsRemoteUtil remoteUtil = plugin.getOsmNotesRemoteUtil();
		return remoteUtil.validateLoginDetails();
	}

	@Override
	protected void onPostExecute(OsmBugResult osmBugResult) {
		if (osmBugResult.warning != null) {
			app.showToastMessage(osmBugResult.warning);
		} else {
			app.showToastMessage(R.string.osm_authorization_success);
		}
	}
}
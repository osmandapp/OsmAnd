package net.osmand.plus.osmedit;

import android.os.AsyncTask;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.osmedit.OsmBugsUtil.OsmBugResult;

import java.lang.ref.WeakReference;

public class ValidateOsmLoginDetailsTask extends AsyncTask<Void, Void, OsmBugResult> {

	private OsmandApplication app;
	private final WeakReference<OsmEditingFragment> fragmentRef;

	public ValidateOsmLoginDetailsTask(OsmandApplication app, OsmEditingFragment targetfragment) {
		this.app = app;
		this.fragmentRef = new WeakReference<>(targetfragment);
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
			app.getSettings().USER_NAME.set("");
			app.getSettings().USER_PASSWORD.set("");
			app.showToastMessage(osmBugResult.warning);
		} else {
			app.showToastMessage(R.string.osm_authorization_success);
		}
		OsmEditingFragment targetfragment = fragmentRef.get();
		if (targetfragment != null) {
			targetfragment.updateAllSettings();
		}
	}
}
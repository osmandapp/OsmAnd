package net.osmand.plus.osmedit;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.osmedit.OsmBugsUtil.OsmBugResult;

public class ValidateOsmLoginDetailsTask extends AsyncTask<Void, Void, OsmBugResult> {

	private OsmandApplication app;
	private ValidateOsmLoginListener validateListener;

	public ValidateOsmLoginDetailsTask(@NonNull OsmandApplication app, ValidateOsmLoginListener validateListener) {
		this.app = app;
		this.validateListener = validateListener;
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
			app.getSettings().OSM_USER_NAME.resetToDefault();
			app.getSettings().OSM_USER_PASSWORD.resetToDefault();
			app.showToastMessage(osmBugResult.warning);
		} else {
			app.showToastMessage(R.string.osm_authorization_success);
		}
		if (validateListener != null) {
			validateListener.loginValidationFinished(osmBugResult.warning);
		}
	}

	public interface ValidateOsmLoginListener {

		void loginValidationFinished(String warning);

	}
}
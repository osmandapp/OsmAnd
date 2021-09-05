package net.osmand.plus.osmedit;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.osmedit.OsmBugsUtil.OsmBugResult;
import net.osmand.plus.settings.backend.OsmandSettings;

public class ValidateOsmLoginDetailsTask extends AsyncTask<Void, Void, OsmBugResult> {

	private final OsmandApplication app;
	private final ValidateOsmLoginListener validateListener;

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
		OsmandSettings settings = app.getSettings();
		if (osmBugResult.warning != null) {
			settings.OSM_USER_NAME_OR_EMAIL.resetToDefault();
			settings.OSM_USER_DISPLAY_NAME.resetToDefault();
			settings.OSM_USER_PASSWORD.resetToDefault();
			settings.MAPPER_LIVE_UPDATES_EXPIRE_TIME.resetToDefault();
			app.showToastMessage(osmBugResult.warning);
		} else {
			settings.OSM_USER_DISPLAY_NAME.set(osmBugResult.userName);
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
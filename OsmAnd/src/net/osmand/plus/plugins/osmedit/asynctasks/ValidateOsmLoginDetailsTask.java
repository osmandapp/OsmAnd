package net.osmand.plus.plugins.osmedit.asynctasks;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.helpers.OsmBugsRemoteUtil;
import net.osmand.plus.plugins.osmedit.helpers.OsmBugsUtil.OsmBugResult;

public class ValidateOsmLoginDetailsTask extends AsyncTask<Void, Void, OsmBugResult> {

	private final OsmandApplication app;
	private final ValidateOsmLoginListener listener;

	public ValidateOsmLoginDetailsTask(@NonNull OsmandApplication app, @Nullable ValidateOsmLoginListener listener) {
		this.app = app;
		this.listener = listener;
	}

	@Override
	protected OsmBugResult doInBackground(Void... params) {
		OsmEditingPlugin plugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
		assert plugin != null;
		OsmBugsRemoteUtil remoteUtil = plugin.getOsmNotesRemoteUtil();
		return remoteUtil.validateLoginDetails();
	}

	@Override
	protected void onPostExecute(OsmBugResult osmBugResult) {
		OsmEditingPlugin plugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
		if (plugin != null) {
			if (osmBugResult.warning != null) {
				plugin.OSM_USER_NAME_OR_EMAIL.resetToDefault();
				plugin.OSM_USER_DISPLAY_NAME.resetToDefault();
				plugin.OSM_USER_PASSWORD.resetToDefault();
				app.getSettings().MAPPER_LIVE_UPDATES_EXPIRE_TIME.resetToDefault();
				app.showToastMessage(osmBugResult.warning);
			} else {
				plugin.OSM_USER_DISPLAY_NAME.set(osmBugResult.userName);
				app.showToastMessage(R.string.osm_authorization_success);
			}
		}

		if (listener != null) {
			listener.loginValidationFinished(osmBugResult.warning);
		}
	}

	public interface ValidateOsmLoginListener {

		void loginValidationFinished(String warning);

	}
}
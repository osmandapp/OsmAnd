package net.osmand.plus.plugins.osmedit.asynctasks;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.helpers.OsmBugsRemoteUtil;
import net.osmand.plus.plugins.osmedit.helpers.OsmBugsUtil.OsmBugResult;
import net.osmand.plus.settings.backend.OsmandSettings;

public class ValidateOsmLoginDetailsTask extends AsyncTask<Void, Void, OsmBugResult> {

	private final OsmandApplication app;
	private final OsmEditingPlugin plugin;
	private final ValidateOsmLoginListener validateListener;

	private ValidateOsmLoginDetailsTask(@NonNull OsmandApplication app,
	                                    @NonNull OsmEditingPlugin plugin,
	                                    @Nullable ValidateOsmLoginListener validateListener) {
		this.app = app;
		this.plugin = plugin;
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
			plugin.OSM_USER_NAME_OR_EMAIL.resetToDefault();
			plugin.OSM_USER_DISPLAY_NAME.resetToDefault();
			plugin.OSM_USER_PASSWORD.resetToDefault();
			plugin.MAPPER_LIVE_UPDATES_EXPIRE_TIME.resetToDefault();
			app.showToastMessage(osmBugResult.warning);
		} else {
			plugin.OSM_USER_DISPLAY_NAME.set(osmBugResult.userName);
			app.showToastMessage(R.string.osm_authorization_success);
		}
		if (validateListener != null) {
			validateListener.loginValidationFinished(osmBugResult.warning);
		}
	}

	public static void execute(@NonNull OsmandApplication app,
	                           @NonNull OsmEditingPlugin plugin,
	                           @Nullable ValidateOsmLoginListener validateListener) {
		ValidateOsmLoginDetailsTask task = new ValidateOsmLoginDetailsTask(app, plugin, validateListener);
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public interface ValidateOsmLoginListener {

		void loginValidationFinished(String warning);

	}
}
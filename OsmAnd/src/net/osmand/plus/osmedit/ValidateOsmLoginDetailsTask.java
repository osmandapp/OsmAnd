package net.osmand.plus.osmedit;

import android.os.AsyncTask;

import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.osmedit.OsmBugsUtil.OsmBugResult;

import java.lang.ref.WeakReference;

public class ValidateOsmLoginDetailsTask extends AsyncTask<Void, Void, OsmBugResult> {

	private OsmandApplication app;
	private ValidateOsmLoginListener validateOsmLoginListener;

	public interface ValidateOsmLoginListener {

		void loginValidationFinished(String error);

	}

	public ValidateOsmLoginDetailsTask(OsmandApplication app, ValidateOsmLoginListener validateTargetOsmLoginDetailsTask ) {
		this.app = app;
		this.validateOsmLoginListener = validateTargetOsmLoginDetailsTask;
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
		if (validateOsmLoginListener != null) {
			validateOsmLoginListener.loginValidationFinished(osmBugResult.warning);
		}
	}
}
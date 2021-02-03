package net.osmand.plus.openplacereviews;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.osmedit.opr.OpenDBAPI;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import java.util.HashSet;
import java.util.Set;

public class OprAuthHelper {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final Set<OprAuthorizationListener> listeners = new HashSet<>();

	public OprAuthHelper(@NonNull OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();
	}

	public void addListener(OprAuthorizationListener listener) {
		listeners.add(listener);
	}

	public void removeListener(OprAuthorizationListener listener) {
		listeners.remove(listener);
	}

	public void resetAuthorization() {
		if (isLoginExists()) {
			settings.OPR_USERNAME.resetToDefault();
			settings.OPR_ACCESS_TOKEN.resetToDefault();
			settings.OPR_BLOCKCHAIN_NAME.resetToDefault();
		}
	}

	public boolean isLoginExists() {
		return !Algorithms.isEmpty(settings.OPR_USERNAME.get())
				&& !Algorithms.isEmpty(settings.OPR_BLOCKCHAIN_NAME.get())
				&& !Algorithms.isEmpty(settings.OPR_ACCESS_TOKEN.get());
	}

	private void notifyAndRemoveListeners() {
		for (OprAuthorizationListener listener : listeners) {
			listener.authorizationCompleted();
		}
		listeners.clear();
	}

	public void authorize(final String token, final String username) {
		CheckOprAuthTask checkOprAuthTask = new CheckOprAuthTask(app, token, username);
		checkOprAuthTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
	}

	private static class CheckOprAuthTask extends AsyncTask<Void, Void, Boolean> {

		private final OsmandApplication app;
		private final OpenDBAPI openDBAPI = new OpenDBAPI();

		private final String token;
		private final String username;

		private CheckOprAuthTask(@NonNull OsmandApplication app, String token, String username) {
			this.app = app;
			this.token = token;
			this.username = username;
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			String baseUrl = OPRConstants.getBaseUrl(app);
			return openDBAPI.checkPrivateKeyValid(app, baseUrl, username, token);
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result) {
				app.getSettings().OPR_ACCESS_TOKEN.set(token);
				app.getSettings().OPR_USERNAME.set(username);
			} else {
				app.getOprAuthHelper().resetAuthorization();
			}
			app.getOprAuthHelper().notifyAndRemoveListeners();
		}
	}

	public interface OprAuthorizationListener {
		void authorizationCompleted();
	}
}
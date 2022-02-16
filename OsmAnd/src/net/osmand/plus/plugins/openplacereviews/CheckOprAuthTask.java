package net.osmand.plus.plugins.openplacereviews;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;

public class CheckOprAuthTask extends AsyncTask<Void, Void, Boolean> {

	private final OsmandApplication app;
	private final OpenDBAPI openDBAPI = new OpenDBAPI();

	private final String token;
	private final String username;

	private final CheckOprAuthTaskListener listener;

	public interface CheckOprAuthTaskListener {
		void onOprAuthChecked(boolean authorized);
	}

	public CheckOprAuthTask(@NonNull OsmandApplication app, @NonNull String token,
	                        @NonNull String username, @Nullable CheckOprAuthTaskListener listener) {
		this.app = app;
		this.token = token;
		this.username = username;
		this.listener = listener;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		String baseUrl = OPRConstants.getBaseUrl(app);
		return openDBAPI.checkPrivateKeyValid(app, baseUrl, username, token);
	}

	@Override
	protected void onPostExecute(Boolean result) {
		if (listener != null) {
			listener.onOprAuthChecked(result);
		}
	}
}

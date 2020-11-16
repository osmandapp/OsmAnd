package net.osmand.plus.osmedit.oauth;

import android.view.ViewGroup;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;

public class OsmOAuthHelper {

	private final OsmOAuthAuthorizationAdapter authorizationAdapter;

	public OsmOAuthHelper(@NonNull OsmandApplication app) {
		authorizationAdapter = new OsmOAuthAuthorizationAdapter(app);
	}

	public void startOAuth(ViewGroup view) {
		authorizationAdapter.startOAuth(view);
	}

	public void authorize(String oauthVerifier, OsmAuthorizationListener listener) {
		authorizationAdapter.authorize(oauthVerifier, this, listener);
	}

	public OsmOAuthAuthorizationAdapter getAuthorizationAdapter() {
		return authorizationAdapter;
	}

	public interface OsmAuthorizationListener {
		void authorizationCompleted();
	}
}
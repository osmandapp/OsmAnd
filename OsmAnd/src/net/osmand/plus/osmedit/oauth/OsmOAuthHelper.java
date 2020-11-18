package net.osmand.plus.osmedit.oauth;

import android.view.ViewGroup;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;

import java.util.HashSet;
import java.util.Set;

public class OsmOAuthHelper {

	private final OsmandApplication app;
	private final OsmOAuthAuthorizationAdapter authorizationAdapter;
	private final Set<OsmAuthorizationListener> listeners = new HashSet<>();

	public OsmOAuthHelper(@NonNull OsmandApplication app) {
		this.app = app;
		authorizationAdapter = new OsmOAuthAuthorizationAdapter(app);
	}

	public void addListener(OsmAuthorizationListener listener) {
		listeners.add(listener);
	}

	public void removeListener(OsmAuthorizationListener listener) {
		listeners.remove(listener);
	}

	public OsmOAuthAuthorizationAdapter getAuthorizationAdapter() {
		return authorizationAdapter;
	}

	public void startOAuth(@NonNull ViewGroup view) {
		authorizationAdapter.startOAuth(view);
	}

	public void authorize(@NonNull String oauthVerifier) {
		authorizationAdapter.authorize(oauthVerifier, this);
	}

	public void notifyAndRemoveListeners() {
		for (OsmAuthorizationListener listener : listeners) {
			listener.authorizationCompleted();
		}
		listeners.clear();
	}

	public interface OsmAuthorizationListener {
		void authorizationCompleted();
	}
}
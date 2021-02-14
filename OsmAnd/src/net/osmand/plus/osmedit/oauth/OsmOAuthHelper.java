package net.osmand.plus.osmedit.oauth;

import android.view.ViewGroup;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import java.util.HashSet;
import java.util.Set;

public class OsmOAuthHelper {

	private final OsmandApplication app;
	private OsmOAuthAuthorizationAdapter authorizationAdapter;
	private final Set<OsmAuthorizationListener> listeners = new HashSet<>();
	private final OsmandSettings settings;

	public OsmOAuthHelper(@NonNull OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();
		authorizationAdapter = new OsmOAuthAuthorizationAdapter(app);
	}

	public void addListener(OsmAuthorizationListener listener) {
		listeners.add(listener);
	}

	public void updateAdapter() {
		authorizationAdapter = new OsmOAuthAuthorizationAdapter(app);
	}

	public void removeListener(OsmAuthorizationListener listener) {
		listeners.remove(listener);
	}

	public OsmOAuthAuthorizationAdapter getAuthorizationAdapter() {
		return authorizationAdapter;
	}

	public void startOAuth(@NonNull ViewGroup view, boolean nightMode) {
		authorizationAdapter.startOAuth(view, nightMode);
	}

	public void authorize(@NonNull String oauthVerifier) {
		if (oauthVerifier != null) {
			authorizationAdapter.authorize(oauthVerifier, this);
		} else {
			updateAdapter();
		}
	}

	public void resetAuthorization() {
		if (isValidToken()) {
			settings.OSM_USER_ACCESS_TOKEN.resetToDefault();
			settings.OSM_USER_ACCESS_TOKEN_SECRET.resetToDefault();
			authorizationAdapter.resetToken();
		} else if (isLoginExists()) {
			settings.OSM_USER_NAME.resetToDefault();
			settings.OSM_USER_PASSWORD.resetToDefault();
		}
		updateAdapter();
	}

	private boolean isLoginExists() {
		return !Algorithms.isEmpty(settings.OSM_USER_NAME.get()) && !Algorithms.isEmpty(settings.OSM_USER_PASSWORD.get());
	}

	public void notifyAndRemoveListeners() {
		for (OsmAuthorizationListener listener : listeners) {
			listener.authorizationCompleted();
		}
		listeners.clear();
	}

	public boolean isValidToken() {
		return authorizationAdapter.isValidToken();
	}

	public boolean isLogged() {
		return isValidToken() || isLoginExists();
	}

	public interface OsmAuthorizationListener {
		void authorizationCompleted();
	}
}
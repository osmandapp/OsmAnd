package net.osmand.plus.plugins.osmedit.oauth;

import android.view.ViewGroup;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.util.Algorithms;

import java.util.HashSet;
import java.util.Set;

public class OsmOAuthHelper {

	private final OsmandApplication app;
	private OsmOAuthAuthorizationAdapter authorizationAdapter;
	private final Set<OsmAuthorizationListener> listeners = new HashSet<>();

	public OsmOAuthHelper(@NonNull OsmandApplication app) {
		this.app = app;
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
		if (authorizationAdapter == null) {
			authorizationAdapter = new OsmOAuthAuthorizationAdapter(app);
		}
		return authorizationAdapter;
	}

	public void startOAuth(@NonNull ViewGroup view, boolean nightMode) {
		getAuthorizationAdapter().startOAuth(view, nightMode);
	}

	public void authorize(@NonNull String oauthVerifier) {
		getAuthorizationAdapter().authorize(oauthVerifier, this);
	}

	public void resetAuthorization() {
		OsmEditingPlugin plugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
		if (plugin == null) {
			return;
		}

		if (isValidToken()) {
			plugin.OSM_USER_ACCESS_TOKEN.resetToDefault();
			plugin.OSM_USER_ACCESS_TOKEN_SECRET.resetToDefault();
			getAuthorizationAdapter().resetToken();
		} else if (isLoginExists(plugin)) {
			plugin.OSM_USER_NAME_OR_EMAIL.resetToDefault();
			plugin.OSM_USER_PASSWORD.resetToDefault();
		}
		app.getSettings().MAPPER_LIVE_UPDATES_EXPIRE_TIME.resetToDefault();
		updateAdapter();
	}

	public void notifyAndRemoveListeners() {
		for (OsmAuthorizationListener listener : listeners) {
			listener.authorizationCompleted();
		}
		listeners.clear();
	}

	public boolean isValidToken() {
		return getAuthorizationAdapter().isValidToken();
	}

	public boolean isLogged(@NonNull OsmEditingPlugin plugin) {
		return isValidToken() || isLoginExists(plugin);
	}

	private boolean isLoginExists(@NonNull OsmEditingPlugin plugin) {
		return !Algorithms.isEmpty(plugin.OSM_USER_NAME_OR_EMAIL.get()) && !Algorithms.isEmpty(plugin.OSM_USER_PASSWORD.get());
	}

	public interface OsmAuthorizationListener {
		void authorizationCompleted();
	}
}
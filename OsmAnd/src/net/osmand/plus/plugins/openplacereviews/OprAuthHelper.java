package net.osmand.plus.plugins.openplacereviews;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.util.Algorithms;

import java.util.HashSet;
import java.util.Set;

public class OprAuthHelper {

	private final OsmandApplication app;
	private final Set<OprAuthorizationListener> listeners = new HashSet<>();

	public interface OprAuthorizationListener {
		void authorizationCompleted();
	}

	public OprAuthHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public void addListener(OprAuthorizationListener listener) {
		listeners.add(listener);
	}

	public void removeListener(OprAuthorizationListener listener) {
		listeners.remove(listener);
	}

	public void resetAuthorization() {
		OpenPlaceReviewsPlugin plugin = getPlugin();
		if (isLoginExists()) {
			plugin.OPR_USERNAME.resetToDefault();
			plugin.OPR_ACCESS_TOKEN.resetToDefault();
			plugin.OPR_BLOCKCHAIN_NAME.resetToDefault();
		}
	}

	public boolean isLoginExists() {
		OpenPlaceReviewsPlugin plugin = getPlugin();
		return !Algorithms.isEmpty(plugin.OPR_USERNAME.get())
				&& !Algorithms.isEmpty(plugin.OPR_BLOCKCHAIN_NAME.get())
				&& !Algorithms.isEmpty(plugin.OPR_ACCESS_TOKEN.get());
	}

	private void notifyAndRemoveListeners() {
		for (OprAuthorizationListener listener : listeners) {
			listener.authorizationCompleted();
		}
		listeners.clear();
	}

	public void authorize(String token, String username) {
		CheckOprAuthTask checkOprAuthTask = new CheckOprAuthTask(app, token, username, authorized -> {
			if (authorized) {
				OpenPlaceReviewsPlugin plugin = getPlugin();
				plugin.OPR_ACCESS_TOKEN.set(token);
				plugin.OPR_USERNAME.set(username);
			} else {
				resetAuthorization();
			}
			notifyAndRemoveListeners();
		});
		checkOprAuthTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
	}

	private OpenPlaceReviewsPlugin getPlugin() {
		return PluginsHelper.getPlugin(OpenPlaceReviewsPlugin.class);
	}
}
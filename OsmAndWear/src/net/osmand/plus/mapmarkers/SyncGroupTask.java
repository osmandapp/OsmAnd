package net.osmand.plus.mapmarkers;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.util.Algorithms;

import java.util.Set;

class SyncGroupTask extends AsyncTask<Void, Void, Void> {

	private final OsmandApplication app;
	private final MapMarkersGroup group;
	private final Set<OnGroupSyncedListener> listeners;

	SyncGroupTask(@NonNull OsmandApplication app, @NonNull MapMarkersGroup group, @Nullable Set<OnGroupSyncedListener> listeners) {
		this.app = app;
		this.group = group;
		this.listeners = listeners;
	}

	@Override
	protected void onPreExecute() {
		if (!Algorithms.isEmpty(listeners)) {
			app.runInUIThread(() -> {
				for (OnGroupSyncedListener listener : listeners) {
					listener.onSyncStarted();
				}
			});
		}
	}

	@Override
	protected Void doInBackground(Void... voids) {
		MapMarkersHelper helper = app.getMapMarkersHelper();
		helper.runGroupSynchronization(group);
		helper.saveGroups(false);
		helper.updateLastModifiedTime(group);
		return null;
	}

	@Override
	protected void onPostExecute(Void aVoid) {
		if (!Algorithms.isEmpty(listeners)) {
			app.runInUIThread(() -> {
				for (OnGroupSyncedListener listener : listeners) {
					listener.onSyncDone();
				}
			});
		}
	}

	public interface OnGroupSyncedListener {

		void onSyncStarted();

		void onSyncDone();
	}
}
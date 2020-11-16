package net.osmand.plus.settings.fragments;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.IProgress;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

class ReloadIndexesTack extends AsyncTask<Void, Void, Void> {

	private final WeakReference<MapActivity> mapActivityRef;
	private final OsmandApplication app;

	ReloadIndexesTack(@NonNull MapActivity mapActivity) {
		this.mapActivityRef = new WeakReference<>(mapActivity);
		this.app = mapActivity.getMyApplication();
	}

	@Override
	protected Void doInBackground(Void[] params) {
		app.getResourceManager().reloadIndexes(IProgress.EMPTY_PROGRESS, new ArrayList<String>());
		return null;
	}

	@Override
	protected void onPostExecute(Void aVoid) {
		MapActivity mapActivity = mapActivityRef.get();
		if (mapActivity != null) {
			mapActivity.refreshMap();
		}
	}
}

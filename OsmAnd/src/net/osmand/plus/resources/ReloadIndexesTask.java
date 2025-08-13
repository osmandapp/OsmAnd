package net.osmand.plus.resources;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IProgress;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;

import java.util.ArrayList;
import java.util.List;

public class ReloadIndexesTask extends AsyncTask<Void, String, List<String>> {

	private final OsmandApplication app;
	private final IProgress progress;
	private final ReloadIndexesListener listener;

	protected ReloadIndexesTask(@NonNull OsmandApplication app, @Nullable IProgress progress,
			@Nullable ReloadIndexesListener listener) {
		this.app = app;
		this.progress = progress;
		this.listener = listener;
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.reloadIndexesStarted();
		}
	}

	@Override
	protected List<String> doInBackground(Void... params) {
		ResourceManager resourceManager = app.getResourceManager();
		resourceManager.resetGeoidAltitudeCorrection();

		// do it lazy
		// indexingImageTiles(progress);
		List<String> warnings = new ArrayList<>();
		warnings.addAll(resourceManager.indexingMaps(progress));
		warnings.addAll(resourceManager.indexVoiceFiles(progress));
		warnings.addAll(resourceManager.indexFontFiles(progress));
		warnings.addAll(PluginsHelper.onIndexingFiles(progress));
		warnings.addAll(resourceManager.indexAdditionalMaps(progress));

		return warnings;
	}

	@Override
	protected void onPostExecute(List<String> warnings) {
		if (listener != null) {
			listener.reloadIndexesFinished(warnings);
		}
	}

	public interface ReloadIndexesListener {

		default void reloadIndexesStarted() {

		}

		void reloadIndexesFinished(@NonNull List<String> warnings);
	}
}

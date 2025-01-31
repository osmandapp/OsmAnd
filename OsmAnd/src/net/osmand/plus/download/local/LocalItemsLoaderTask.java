package net.osmand.plus.download.local;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.Map;

public class LocalItemsLoaderTask extends AsyncTask<Void, Void, Map<CategoryType, LocalCategory>> {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final LocalSizeController sizeController;
	@Nullable
	private final LoadItemsListener listener;

	public LocalItemsLoaderTask(@NonNull OsmandApplication app, @Nullable LoadItemsListener listener) {
		this.app = app;
		this.settings = app.getSettings();
		this.sizeController = LocalSizeController.requireInstance(app);
		this.listener = listener;
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.loadItemsStarted();
		}
	}

	@Override
	protected Map<CategoryType, LocalCategory> doInBackground(Void... params) {
		return loadAllFilesByCategories();
	}

	@NonNull
	public Map<CategoryType, LocalCategory> loadAllFilesByCategories() {
		File noBackupDir = settings.getNoBackupPath();
		File internalDir = getAppDir(settings.getInternalAppPath());
		File externalDir = getAppDir(settings.getExternalStorageDirectory());

		CollectLocalIndexesRules collectingRules = new CollectLocalIndexesRules.Builder(app)
				.addDirectoryIfNotPresent(internalDir, false)
				.addDirectoryIfNotPresent(externalDir, true)
				.addForcedAddUnknownDirectory(noBackupDir)
				.build();
		return CollectLocalIndexesAlgorithm.execute(collectingRules, this::onLocalItemAdded);
	}

	private void onLocalItemAdded(@NonNull LocalItem localItem) {
		sizeController.updateLocalItemSizeIfNeeded(localItem);
	}

	@NonNull
	private File getAppDir(@NonNull File dir) {
		File parentDir = dir.getParentFile();
		return parentDir != null && Algorithms.stringsEqual(parentDir.getName(), app.getPackageName()) ? parentDir : dir;
	}


	@Override
	protected void onPostExecute(@NonNull Map<CategoryType, LocalCategory> categories) {
		if (listener != null) {
			listener.loadItemsFinished(categories);
		}
	}

	public interface LoadItemsListener {

		default void loadItemsStarted() {
		}

		default void loadItemsFinished(@NonNull Map<CategoryType, LocalCategory> categories) {
		}
	}
}
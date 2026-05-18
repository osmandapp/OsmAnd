package net.osmand.plus.importfiles.tasks;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.plus.R;
import net.osmand.plus.importfiles.ImportHelper;

import java.io.File;
import java.util.ArrayList;

public class ObfImportTask extends BaseImportAsyncTask<Void, Void, String> {

	private final Uri uri;
	private final String name;

	public ObfImportTask(@NonNull FragmentActivity activity, @NonNull Uri uri, @NonNull String name) {
		super(activity);
		this.uri = uri;
		this.name = name;
	}

	@Override
	protected String doInBackground(Void... voids) {
		boolean unzip = name.endsWith(IndexConstants.ZIP_EXT);
		String fileName = unzip ? name.replace(IndexConstants.ZIP_EXT, "") : name;
		File dest = getObfDestFile(fileName);
		String error = ImportHelper.copyFile(app, dest, uri, false, unzip);
		if (error == null) {
			app.getResourceManager().reloadIndexes(IProgress.EMPTY_PROGRESS, new ArrayList<>());
			app.getDownloadThread().updateLoadedFiles();
			return app.getString(R.string.map_imported_successfully);
		}
		return app.getString(R.string.map_import_error) + ": " + error;
	}

	@Override
	protected void onPostExecute(String message) {
		hideProgress();
		notifyImportFinished();
		app.showShortToastMessage(message);
	}

	@NonNull
	private File getObfDestFile(@NonNull String name) {
		if (name.endsWith(IndexConstants.BINARY_ROAD_MAP_INDEX_EXT)) {
			return app.getAppPath(IndexConstants.ROADS_INDEX_DIR + name);
		} else if (name.endsWith(IndexConstants.BINARY_WIKI_MAP_INDEX_EXT)) {
			return app.getAppPath(IndexConstants.WIKI_INDEX_DIR + name);
		} else if (name.endsWith(IndexConstants.BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT)) {
			return app.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR + name);
		} else if (name.endsWith(IndexConstants.BINARY_DEPTH_MAP_INDEX_EXT)) {
			return app.getAppPath(IndexConstants.NAUTICAL_INDEX_DIR + name);
		}
		return app.getAppPath(name);
	}
}
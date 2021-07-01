package net.osmand.plus.importfiles;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseLoadAsyncTask;

import java.io.File;
import java.util.ArrayList;

class ObfImportTask extends BaseLoadAsyncTask<Void, Void, String> {

	private Uri uri;
	private String name;

	public ObfImportTask(@NonNull FragmentActivity activity, @NonNull Uri uri, @NonNull String name) {
		super(activity);
		this.uri = uri;
		this.name = name;
	}

	@Override
	protected String doInBackground(Void... voids) {
		String error = ImportHelper.copyFile(app, getObfDestFile(name), uri, false);
		if (error == null) {
			app.getResourceManager().reloadIndexes(IProgress.EMPTY_PROGRESS, new ArrayList<String>());
			app.getDownloadThread().updateLoadedFiles();
			return app.getString(R.string.map_imported_successfully);
		}
		return app.getString(R.string.map_import_error) + ": " + error;
	}

	@Override
	protected void onPostExecute(String message) {
		hideProgress();
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
		}
		return app.getAppPath(name);
	}
}
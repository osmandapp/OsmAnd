package net.osmand.plus.importfiles;

import static net.osmand.IndexConstants.HEIGHTMAP_INDEX_DIR;

import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseLoadAsyncTask;

import java.io.File;

class SqliteHeightmapImportTask extends BaseLoadAsyncTask<Void, Void, String> {

	private final Uri uri;
	private final String name;

	public SqliteHeightmapImportTask(@NonNull FragmentActivity activity, @NonNull Uri uri, @NonNull String name) {
		super(activity);
		this.uri = uri;
		this.name = name;
	}

	@Override
	protected String doInBackground(Void... voids) {
		File heightMapDir = app.getAppPath(HEIGHTMAP_INDEX_DIR);
		if (!heightMapDir.exists()) {
			heightMapDir.mkdir();
		}
//		File[] files = heightMapDir.listFiles();
//		if (files != null) {
//			for (File file : files) {
//				file.delete();
//			}
//		}
		return ImportHelper.copyFile(app, app.getAppPath(HEIGHTMAP_INDEX_DIR + name), uri, false, false);
	}

	@Override
	protected void onPostExecute(String error) {
		hideProgress();
		if (error == null) {
			Toast.makeText(app, app.getString(R.string.map_imported_successfully), Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(app, app.getString(R.string.map_import_error) + ": " + error, Toast.LENGTH_SHORT).show();
		}
	}
}
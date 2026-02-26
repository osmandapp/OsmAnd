package net.osmand.plus.importfiles.tasks;

import static net.osmand.IndexConstants.GEOTIFF_DIR;

import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.importfiles.ImportHelper;

import java.io.File;

public class GeoTiffImportTask extends BaseImportAsyncTask<Void, Void, String> {

	private final Uri uri;
	private final String targetFileName;

	public GeoTiffImportTask(@NonNull FragmentActivity activity, @NonNull Uri uri, @NonNull String name) {
		super(activity);
		this.uri = uri;
		this.targetFileName = name.replace('_', ' ');
	}

	@Override
	protected String doInBackground(Void... voids) {
		File geotiffDir = app.getAppPath(GEOTIFF_DIR);
		if (!geotiffDir.exists()) {
			geotiffDir.mkdir();
		}
		return ImportHelper.copyFile(app, new File(geotiffDir, targetFileName), uri, false, false);
	}

	@Override
	protected void onPostExecute(String error) {
		hideProgress();
		notifyImportFinished();
		if (error == null) {
			Toast.makeText(app, app.getString(R.string.map_imported_successfully), Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(app, app.getString(R.string.map_import_error) + ": " + error, Toast.LENGTH_SHORT).show();
		}
	}
}
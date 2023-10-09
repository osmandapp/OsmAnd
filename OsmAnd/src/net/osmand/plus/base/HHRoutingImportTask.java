package net.osmand.plus.base;

import static net.osmand.IndexConstants.HH_ROUTING_DIR;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.importfiles.ImportHelper;

public class HHRoutingImportTask extends BaseLoadAsyncTask<Void, Void, String> {

	private final Uri uri;
	private final String name;

	public HHRoutingImportTask(@NonNull FragmentActivity activity, @NonNull Uri uri, @NonNull String name) {
		super(activity);
		this.uri = uri;
		this.name = name;
	}

	@Override
	protected String doInBackground(Void... voids) {
		return ImportHelper.copyFile(app, app.getAppPath(HH_ROUTING_DIR + name), uri, false, false);
	}

	@Override
	protected void onPostExecute(String error) {
		hideProgress();

		if (error == null) {
			app.showToastMessage(R.string.file_imported_successfully, name);
		} else {
			app.showShortToastMessage(app.getString(R.string.file_import_error, name, error));
		}
	}
}

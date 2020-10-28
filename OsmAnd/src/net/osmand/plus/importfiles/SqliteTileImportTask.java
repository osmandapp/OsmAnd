package net.osmand.plus.importfiles;

import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;

import static net.osmand.IndexConstants.TILES_INDEX_DIR;

class SqliteTileImportTask extends BaseLoadAsyncTask<Void, Void, String> {

	private Uri uri;
	private String name;

	public SqliteTileImportTask(@NonNull FragmentActivity activity, @NonNull Uri uri, @NonNull String name) {
		super(activity);
		this.uri = uri;
		this.name = name;
	}

	@Override
	protected String doInBackground(Void... voids) {
		return ImportHelper.copyFile(app, app.getAppPath(TILES_INDEX_DIR + name), uri, false);
	}

	@Override
	protected void onPostExecute(String error) {
		hideProgress();
		if (error == null) {
			FragmentActivity activity = activityRef.get();
			OsmandRasterMapsPlugin plugin = OsmandPlugin.getPlugin(OsmandRasterMapsPlugin.class);
			if (plugin != null && !plugin.isActive() && !plugin.needsInstallation()) {
				OsmandPlugin.enablePlugin(activity, app, plugin, true);
			}
			if (activity instanceof MapActivity) {
				MapActivity mapActivity = (MapActivity) activity;
				mapActivity.getMapLayers().selectMapLayer(mapActivity.getMapView(), null, null);
			}
			Toast.makeText(app, app.getString(R.string.map_imported_successfully), Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(app, app.getString(R.string.map_import_error) + ": " + error, Toast.LENGTH_SHORT).show();
		}
	}
}
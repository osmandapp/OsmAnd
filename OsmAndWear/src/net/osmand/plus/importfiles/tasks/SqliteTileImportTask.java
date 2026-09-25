package net.osmand.plus.importfiles.tasks;

import static net.osmand.IndexConstants.TILES_INDEX_DIR;

import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;

public class SqliteTileImportTask extends BaseImportAsyncTask<Void, Void, String> {

	private final Uri uri;
	private final String name;

	public SqliteTileImportTask(@NonNull FragmentActivity activity, @NonNull Uri uri, @NonNull String name) {
		super(activity);
		this.uri = uri;
		this.name = name;
	}

	@Override
	protected String doInBackground(Void... voids) {
		return ImportHelper.copyFile(app, app.getAppPath(TILES_INDEX_DIR + name), uri, false, false);
	}

	@Override
	protected void onPostExecute(String error) {
		hideProgress();
		notifyImportFinished();
		if (error == null) {
			FragmentActivity activity = activityRef.get();
			OsmandRasterMapsPlugin plugin = PluginsHelper.getPlugin(OsmandRasterMapsPlugin.class);
			PluginsHelper.enablePluginIfNeeded(activity, app, plugin, true);
			if (activity instanceof MapActivity) {
				MapActivity mapActivity = (MapActivity) activity;
				mapActivity.getMapLayers().selectMapLayer(mapActivity, true, app.getSettings().MAP_TILE_SOURCES, null);
			}
			Toast.makeText(app, app.getString(R.string.map_imported_successfully), Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(app, app.getString(R.string.map_import_error) + ": " + error, Toast.LENGTH_SHORT).show();
		}
	}
}
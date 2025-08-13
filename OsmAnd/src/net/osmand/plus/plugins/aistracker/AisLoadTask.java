package net.osmand.plus.plugins.aistracker;

import android.net.Uri;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.utils.FileUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;

public class AisLoadTask extends AsyncTask<Void, Void, String> {

	private static final Log LOG = PlatformUtil.getLog(AisLoadTask.class.getName());

	private final AisTrackerPlugin plugin = PluginsHelper.requirePlugin(AisTrackerPlugin.class);
	private final OsmandApplication app;
	private final Uri uri;

	private File file;

	public AisLoadTask(@NonNull OsmandApplication app, @NonNull Uri uri) {
		this.app = app;
		this.uri = uri;
	}

	@Override
	protected String doInBackground(Void... voids) {
		String name = ImportHelper.getNameFromContentUri(app, uri);
		file = new File(FileUtils.getTempDir(app), "ais" + "_" + name);
		return ImportHelper.copyFile(app, file, uri, true, false);
	}

	@Override
	protected void onPostExecute(String error) {
		if (Algorithms.isEmpty(error)) {
			if (file.exists()) {
				plugin.getSimulationProvider().startSimulation(file);
			}
		} else {
			LOG.error(error);
			app.showToastMessage(error);
		}
	}
}
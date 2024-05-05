package net.osmand.test.common;

import android.content.res.AssetManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.importfiles.SaveImportedGpxListener;
import net.osmand.plus.importfiles.tasks.SaveGpxAsyncTask;
import net.osmand.plus.utils.FileUtils;
import net.osmand.util.Algorithms;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ResourcesImporter {

	public static void importGpxAssets(@NonNull OsmandApplication app, @NonNull List<String> assetFilePaths) throws IOException {
		File tmpDir = FileUtils.getTempDir(app);
		File gpxDestinationDir = ImportHelper.getGpxDestinationDir(app, true);
		for (String assetFilePath : assetFilePaths) {
			String fileName = new File(assetFilePath).getName();
			File file = new File(tmpDir, System.currentTimeMillis() + "_" + fileName);
			try (InputStream is = InstrumentationRegistry.getInstrumentation().getContext().getAssets()
					.open(assetFilePath, AssetManager.ACCESS_STREAMING)) {
				String error = ImportHelper.copyFile(app, file, is, true, false);
				if (error == null) {
					GPXFile gpxFile = GPXUtilities.loadGPXFile(file);
					String[] errors = {""};
					SaveImportedGpxListener listener = new SaveImportedGpxListener() {
						@Override
						public void onGpxSaved(@Nullable String error, @NonNull GPXFile gpxFile) {
							errors[0] = error;
						}
					};
					new SaveGpxAsyncTask(app, gpxFile, gpxDestinationDir, fileName, listener, true).execute().get();
					if (!Algorithms.isEmpty(errors[0])) {
						throw new IOException("Import gpx error: " + errors[0]);
					}
				}
			} catch (ExecutionException | InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static void importObfAssets(@NonNull OsmandApplication app, @NonNull List<String> assetFilePaths) throws IOException {
		String error = null;
		for (String assetFilePath : assetFilePaths) {
			String name = new File(assetFilePath).getName();
			InputStream is = InstrumentationRegistry.getInstrumentation().getContext().getAssets()
					.open(assetFilePath, AssetManager.ACCESS_STREAMING);
			boolean unzip = name.endsWith(IndexConstants.ZIP_EXT);
			String fileName = unzip ? name.replace(IndexConstants.ZIP_EXT, "") : name;
			File dest = getObfDestFile(app, fileName);
			error = ImportHelper.copyFile(app, dest, is, true, unzip);
			if (error != null) {
				break;
			}
		}
		if (error == null) {
			app.getResourceManager().reloadIndexes(IProgress.EMPTY_PROGRESS, new ArrayList<>());
			app.getDownloadThread().updateLoadedFiles();
		} else {
			throw new IOException("Map import error: " + error);
		}
	}

	@NonNull
	private static File getObfDestFile(@NonNull OsmandApplication app, @NonNull String name) {
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

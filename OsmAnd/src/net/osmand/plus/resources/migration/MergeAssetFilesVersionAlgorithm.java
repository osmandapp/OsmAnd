package net.osmand.plus.resources.migration;

import android.content.res.AssetManager;

import androidx.annotation.NonNull;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.download.DownloadOsmandIndexesHelper.AssetEntry;
import net.osmand.plus.resources.AssetsCollection;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.utils.FileUtils;

import org.apache.commons.logging.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

public class MergeAssetFilesVersionAlgorithm {

	private static final String AUXILIARY_DIR = IndexConstants.TEMP_DIR + "merge_assets_version/";

	private static final Log LOG = PlatformUtil.getLog(MergeAssetFilesVersionAlgorithm.class);

	private final OsmandApplication app;

	public static void execute(@NonNull OsmandApplication app) {
		new MergeAssetFilesVersionAlgorithm(app).executeImpl();
	}

	public MergeAssetFilesVersionAlgorithm(@NonNull OsmandApplication app) {
		this.app = app;
	}

	private void executeImpl() {
		AssetManager assetManager = app.getAssets();
		AssetsCollection assets;
		try {
			assets = app.getResourceManager().getAssets();
		} catch (Exception e) {
			LOG.error("Unable to read assets when try to upgrade app version: " + e.getMessage());
			return;
		}
		File appDir = app.getAppPath(null);
		File tempDir = FileUtils.getExistingDir(app, AUXILIARY_DIR);
		for (AssetEntry entry : assets.getFilteredEntries(entry -> entry.version != null)) {
			// Check if version not tracked
			Long versionTime = entry.getVersionTime();
			if (versionTime == null) {
				continue;
			}
			// Check if File is not created yet
			File destFile = new File(appDir, entry.destination);
			if (!destFile.exists()) {
				continue;
			}
			// Check if file and assets entry versions are not the same
			if (destFile.lastModified() != versionTime) {
				// Create a temporally file for assets entry
				// to check if existed file has been changed
				File tempFile = new File(tempDir, entry.destination);
				try {
					ResourceManager.copyAssets(assetManager, entry.source, tempFile);
				} catch (IOException e) {
					LOG.error("Error while copying assets: " + e.getMessage());
					continue;
				}
				try {
					// Check if file from assets and existed file have the same content
					if (haveSameContent(destFile, tempFile)) {
						// If content has not been changed, we can set version from assets
						boolean updated = destFile.setLastModified(versionTime);
					}
				} catch (IOException e) {
					LOG.error("Error while checking files content equality: " + e.getMessage());
				}
			}
		}
	}

	private boolean haveSameContent(@NonNull File f1, @NonNull File f2) throws IOException {
		try (BufferedReader reader1 = createBufferedReader(f1);
		     BufferedReader reader2 = createBufferedReader(f2)) {
			String line1;
			String line2;
			while (true) {
				line1 = reader1.readLine();
				line2 = reader2.readLine();
				if (line1 == null || line2 == null) {
					break;
				}
				if (!Objects.equals(line1.trim(), line2.trim())) {
					return false;
				}
			}
			return line1 == null && line2 == null;
		}
	}

	@NonNull
	private BufferedReader createBufferedReader(@NonNull File file) throws FileNotFoundException {
		return new BufferedReader(new InputStreamReader(new FileInputStream(file)));
	}
}

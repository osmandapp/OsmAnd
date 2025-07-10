package net.osmand.plus.resources.migration;

import android.content.res.AssetManager;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.download.DownloadOsmandIndexesHelper.AssetEntry;
import net.osmand.plus.resources.AssetsCollection;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class MergeAssetFilesVersionAlgorithm {

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
				// Check if file from assets and existed file have the same content
				try(InputStream fileIS = new FileInputStream(destFile)) {
					InputStream assetIS = assetManager.open(entry.source, AssetManager.ACCESS_STREAMING);
					if (isContentIdentical(fileIS, assetIS)) {
						// If content has not been changed, we can set version from assets
						boolean updated = destFile.setLastModified(versionTime);
					}
				} catch (Exception e) {
					LOG.error("Error while checking files content equality: " + e.getMessage());
				}
			}
		}
	}

	private boolean isContentIdentical(@NonNull InputStream is1, @NonNull InputStream is2) throws IOException {
		String md5Digest1 = new String(Hex.encodeHex(DigestUtils.md5(is1)));
		String md5Digest2 = new String(Hex.encodeHex(DigestUtils.md5(is2)));
		return Objects.equals(md5Digest1, md5Digest2);
	}
}

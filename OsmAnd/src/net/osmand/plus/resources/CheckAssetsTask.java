package net.osmand.plus.resources;

import static net.osmand.IndexConstants.MODEL_3D_DIR;
import static net.osmand.IndexConstants.TTSVOICE_INDEX_EXT_JS;
import static net.osmand.IndexConstants.VOICE_PROVIDER_SUFFIX;

import android.content.res.AssetManager;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.download.DownloadOsmandIndexesHelper.AssetEntry;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CheckAssetsTask extends AsyncTask<Void, String, List<String>> {

	private static final Log log = PlatformUtil.getLog(CheckAssetsTask.class);

	private static final String OVERWRITE_ONLY_IF_EXISTS = "overwriteOnlyIfExists";
	private static final String ALWAYS_OVERWRITE_OR_COPY = "alwaysOverwriteOrCopy";
	private static final String COPY_ONLY_IF_DOES_NOT_EXIST = "copyOnlyIfDoesNotExist";
	private static final String ALWAYS_COPY_ON_FIRST_INSTALL = "alwaysCopyOnFirstInstall";

	private final OsmandApplication app;
	private final IProgress progress;
	private final CheckAssetsListener listener;
	private final List<String> warnings = new ArrayList<>();

	private final boolean forceCheck;
	private final boolean forceUpdate;

	public CheckAssetsTask(@NonNull OsmandApplication app, @Nullable IProgress progress,
			boolean forceUpdate, boolean forceCheck, @Nullable CheckAssetsListener listener) {
		this.app = app;
		this.progress = progress;
		this.forceUpdate = forceUpdate;
		this.forceCheck = forceCheck;
		this.listener = listener;
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.checkAssetsStarted();
		}
	}

	@Override
	protected List<String> doInBackground(Void... params) {
		checkAssets();
		return warnings;
	}

	@NonNull
	private void checkAssets() {
		if (app.getAppInitializer().isAppVersionChanged()) {
			copyMissingJSAssets();
		}
		String fv = Version.getFullVersion(app);
		OsmandSettings settings = app.getSettings();
		boolean versionChanged = !fv.equalsIgnoreCase(settings.PREVIOUS_INSTALLED_VERSION.get());
		boolean overwrite = versionChanged || forceUpdate;
		if (overwrite || forceCheck) {
			File appDataDir = app.getAppPath(null);
			appDataDir.mkdirs();
			if (appDataDir.canWrite()) {
				try {
					progress.startTask(app.getString(R.string.installing_new_resources), -1);
					AssetManager manager = app.getAssets();
					boolean firstInstall = !settings.PREVIOUS_INSTALLED_VERSION.isSet();
					unpackBundledAssets(manager, appDataDir, firstInstall || forceUpdate, overwrite, forceCheck);
					settings.PREVIOUS_INSTALLED_VERSION.set(fv);
					copyRegionsBoundaries(overwrite);
					// see Issue #3381
					//copyPoiTypes();
					RendererRegistry registry = app.getRendererRegistry();
					for (String internalStyle : registry.getInternalRenderers().keySet()) {
						File file = registry.getFileForInternalStyle(internalStyle);
						if (file.exists() && overwrite) {
							registry.copyFileForInternalStyle(internalStyle);
						}
					}
				} catch (SQLiteException | IOException | XmlPullParserException e) {
					warnings.add(e.getMessage());
					log.error(e.getMessage(), e);
				}
			}
		}
	}

	public void copyMissingJSAssets() {
		try {
			File appPath = app.getAppPath(null);
			if (appPath.canWrite()) {
				AssetManager manager = app.getAssets();
				AssetsCollection assets = app.getResourceManager().getAssets();

				for (AssetEntry asset : assets.getEntries()) {
					File jsFile = new File(appPath, asset.destination);
					if (asset.destination.contains(VOICE_PROVIDER_SUFFIX)
							&& asset.destination.endsWith(TTSVOICE_INDEX_EXT_JS)) {
						String name = asset.destination.replace(VOICE_PROVIDER_SUFFIX, "");
						File oggFile = new File(appPath, name);
						if (oggFile.getParentFile().exists() && !oggFile.exists()) {
							ResourceManager.copyAssets(manager, asset.source, oggFile, asset.getVersionTime());
						}
					} else if (asset.destination.startsWith(MODEL_3D_DIR) && !jsFile.exists()) {
						ResourceManager.copyAssets(manager, asset.source, jsFile, asset.getVersionTime());
					}
					if (jsFile.getParentFile().exists() && !jsFile.exists()) {
						ResourceManager.copyAssets(manager, asset.source, jsFile, asset.getVersionTime());
					}
				}
			}
		} catch (IOException e) {
			warnings.add(e.getMessage());
			log.error("Error while loading tts files from assets", e);
		}
	}

	private void copyRegionsBoundaries(boolean overwrite) {
		try {
			File file = app.getAppPath("regions.ocbf");
			boolean exists = file.exists();
			if (!exists || overwrite) {
				FileOutputStream fout = new FileOutputStream(file);
				Algorithms.streamCopy(OsmandRegions.class.getResourceAsStream("regions.ocbf"), fout);
				fout.close();
			}
		} catch (Exception e) {
			warnings.add(e.getMessage());
			log.error(e.getMessage(), e);
		}
	}

	private void unpackBundledAssets(@NonNull AssetManager manager, @NonNull File appDataDir,
			boolean firstInstall, boolean overwrite,
			boolean forceCheck) throws IOException, XmlPullParserException {
		AssetsCollection assetsCollection = app.getResourceManager().getAssets();
		for (AssetEntry asset : assetsCollection.getEntries()) {
			String[] modes = asset.mode.split("\\|");
			if (modes.length == 0) {
				log.error("Mode '" + asset.mode + "' is not valid");
				continue;
			}
			String installMode = null;
			String copyMode = null;
			for (String mode : modes) {
				if (ALWAYS_COPY_ON_FIRST_INSTALL.equals(mode)) {
					installMode = mode;
				} else if (OVERWRITE_ONLY_IF_EXISTS.equals(mode) ||
						ALWAYS_OVERWRITE_OR_COPY.equals(mode) ||
						COPY_ONLY_IF_DOES_NOT_EXIST.equals(mode)) {
					copyMode = mode;
				} else {
					log.error("Mode '" + mode + "' is unknown");
				}
			}

			File destinationFile = new File(appDataDir, asset.destination);
			boolean exists = destinationFile.exists();
			boolean shouldCopy = false;
			if (ALWAYS_COPY_ON_FIRST_INSTALL.equals(installMode)) {
				if (firstInstall || (forceCheck && !exists)) {
					shouldCopy = true;
				}
			}
			if (copyMode == null) {
				log.error("No copy mode was defined for " + asset.source);
			}
			if (ALWAYS_OVERWRITE_OR_COPY.equals(copyMode)) {
				if (firstInstall || overwrite) {
					shouldCopy = true;
				} else if (forceCheck && !exists) {
					shouldCopy = true;
				}
			}
			if (OVERWRITE_ONLY_IF_EXISTS.equals(copyMode) && exists) {
				if (firstInstall || overwrite) {
					shouldCopy = true;
				}
			}
			if (COPY_ONLY_IF_DOES_NOT_EXIST.equals(copyMode)) {
				if (!exists) {
					shouldCopy = true;
				} else if (asset.dateVersion != null && destinationFile.lastModified() < asset.dateVersion.getTime()) {
					shouldCopy = true;
				}
			}
			if (shouldCopy) {
				ResourceManager.copyAssets(manager, asset.source, destinationFile, asset.getVersionTime());
			}
		}
	}

	@Override
	protected void onPostExecute(List<String> warnings) {
		if (listener != null) {
			listener.checkAssetsFinished(warnings);
		}
	}

	public interface CheckAssetsListener {

		void checkAssetsStarted();

		void checkAssetsFinished(List<String> warnings);
	}
}

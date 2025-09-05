package net.osmand.plus.importfiles.tasks;

import static net.osmand.IndexConstants.RENDERER_INDEX_EXT;
import static net.osmand.plus.AppInitializer.loadRoutingFiles;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.importfiles.ImportType;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.router.RoutingConfiguration.Builder;
import net.osmand.util.Algorithms;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class XmlImportTask extends BaseImportAsyncTask<Void, Void, String> {

	private final Uri uri;
	private String destFileName;
	private ImportType importType;
	private final boolean overwrite;

	public XmlImportTask(@NonNull FragmentActivity activity, @NonNull Uri uri,
	                     @NonNull String fileName, boolean overwrite) {
		super(activity);
		this.uri = uri;
		this.destFileName = fileName;
		this.overwrite = overwrite;
	}

	@Override
	protected String doInBackground(Void... voids) {
		importType = checkImportType(app, uri);
		if (importType != null) {
			File dest = getDestinationFile();
			if (dest != null) {
				return ImportHelper.copyFile(app, dest, uri, true, false);
			}
		}
		return app.getString(R.string.file_import_error, destFileName, app.getString(R.string.unsupported_type_error));
	}

	@Override
	protected void onPostExecute(String error) {
		File destDir = getDestinationDir(app, importType);
		File file = new File(destDir, destFileName);
		if (error == null && file.exists()) {
			if (importType == ImportType.RENDERING) {
				app.getRendererRegistry().updateExternalRenderers();
				onImportFinished(destFileName);
				hideProgress();
			} else if (importType == ImportType.ROUTING) {
				loadRoutingFiles(app, () -> {
					hideProgress();
					Builder builder = app.getCustomRoutingConfig(destFileName);
					if (builder != null) {
						onImportFinished(destFileName);
					} else {
						app.showToastMessage(R.string.file_does_not_contain_routing_rules, destFileName);
					}
				});
			}
		} else {
			hideProgress();
			notifyImportFinished();
			app.showShortToastMessage(R.string.file_import_error, destFileName, error);
		}
	}

	private void onImportFinished(@NonNull String fileName) {
		showSuccessSnackbar(fileName);
		notifyImportFinished();
	}

	private void showSuccessSnackbar(@NonNull String filename) {
		FragmentActivity activity = activityRef.get();
		if (AndroidUtils.isActivityNotDestroyed(activity)) {
			Snackbar snackbar = Snackbar.make(activity.findViewById(android.R.id.content),
					app.getString(R.string.is_imported, filename), Snackbar.LENGTH_LONG);
			boolean nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.APP);
			UiUtilities.setupSnackbar(snackbar, nightMode);
			snackbar.show();
		}
	}

	public static File getDestinationDir(OsmandApplication app, ImportType importType) {
		if (importType == ImportType.ROUTING) {
			return app.getAppPath(IndexConstants.ROUTING_PROFILES_DIR);
		} else if (importType == ImportType.RENDERING) {
			return app.getAppPath(IndexConstants.RENDERERS_DIR);
		}
		return null;
	}

	private File getDestinationFile() {
		File destDir = getDestinationDir(app, importType);
		if (destDir != null) {
			if (!destDir.exists()) {
				destDir.mkdirs();
			}
			if (importType == ImportType.RENDERING && !destFileName.endsWith(RENDERER_INDEX_EXT)) {
				String fileName = Algorithms.getFileNameWithoutExtension(destFileName);
				destFileName = fileName + RENDERER_INDEX_EXT;
			}
			File destFile = new File(destDir, destFileName);
			while (destFile.exists() && !overwrite) {
				destFileName = AndroidUtils.createNewFileName(destFileName);
				destFile = new File(destDir, destFileName);
			}
			return destFile;
		}
		return null;
	}

	public static ImportType checkImportType(OsmandApplication app, Uri uri) {
		ImportType importType = null;
		InputStream is = null;
		try {
			is = app.getContentResolver().openInputStream(uri);
			if (is != null) {
				XmlPullParser parser = PlatformUtil.newXMLPullParser();
				parser.setInput(is, "UTF-8");
				int tok;
				while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
					if (tok == XmlPullParser.START_TAG) {
						String name = parser.getName();
						if ("osmand_routing_config".equals(name)) {
							importType = ImportType.ROUTING;
						} else if ("renderingStyle".equals(name)) {
							importType = ImportType.RENDERING;
						} else if ("gpx".equals(name)) {
							importType = ImportType.GPX;
						} else if ("kml".equals(name)) {
							importType = ImportType.KML;
						}
						break;
					}
				}
				Algorithms.closeStream(is);
			}
		} catch (FileNotFoundException | XmlPullParserException e) {
			ImportHelper.LOG.error(e);
		} catch (IOException e) {
			ImportHelper.LOG.error(e);
		} catch (SecurityException e) {
			ImportHelper.LOG.error(e.getMessage(), e);
		} finally {
			Algorithms.closeStream(is);
		}
		return importType;
	}
}
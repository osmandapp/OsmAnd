package net.osmand.plus.importfiles;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.AppInitializer.LoadRoutingFilesCallback;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.plus.importfiles.ImportHelper.ImportType;
import net.osmand.router.RoutingConfiguration.Builder;
import net.osmand.util.Algorithms;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static net.osmand.IndexConstants.RENDERER_INDEX_EXT;
import static net.osmand.plus.AppInitializer.loadRoutingFiles;

class XmlImportTask extends BaseLoadAsyncTask<Void, Void, String> {

	private Uri uri;
	private String destFileName;
	private ImportType importType;
	private CallbackWithObject routingCallback;

	public XmlImportTask(@NonNull FragmentActivity activity, @NonNull Uri uri,
						 @NonNull String fileName, @Nullable CallbackWithObject routingCallback) {
		super(activity);
		this.uri = uri;
		this.destFileName = fileName;
		this.routingCallback = routingCallback;
	}

	@Override
	protected String doInBackground(Void... voids) {
		importType = checkImportType(app, uri);
		if (importType != null) {
			File dest = getDestinationFile();
			if (dest != null) {
				return ImportHelper.copyFile(app, dest, uri, true);
			}
		}
		return app.getString(R.string.file_import_error, destFileName, app.getString(R.string.unsupported_type_error));
	}

	@Override
	protected void onPostExecute(String error) {
		File destDir = getDestinationDir();
		File file = new File(destDir, destFileName);
		if (error == null && file.exists()) {
			if (importType == ImportType.RENDERING) {
				app.getRendererRegistry().updateExternalRenderers();
				app.showShortToastMessage(app.getString(R.string.file_imported_successfully, destFileName));
				hideProgress();
			} else if (importType == ImportType.ROUTING) {
				loadRoutingFiles(app, new LoadRoutingFilesCallback() {
					@Override
					public void onRoutingFilesLoaded() {
						hideProgress();
						Builder builder = app.getCustomRoutingConfig(destFileName);
						if (builder != null) {
							if (routingCallback != null) {
								routingCallback.processResult(builder);
							}
							app.showShortToastMessage(app.getString(R.string.file_imported_successfully, destFileName));
						} else {
							app.showToastMessage(app.getString(R.string.file_does_not_contain_routing_rules, destFileName));
						}
					}
				});
			}
		} else {
			hideProgress();
			app.showShortToastMessage(app.getString(R.string.file_import_error, destFileName, error));
		}
	}

	private File getDestinationDir() {
		if (importType == ImportType.ROUTING) {
			return app.getAppPath(IndexConstants.ROUTING_PROFILES_DIR);
		} else if (importType == ImportType.RENDERING) {
			return app.getAppPath(IndexConstants.RENDERERS_DIR);
		}
		return null;
	}

	private File getDestinationFile() {
		File destDir = getDestinationDir();
		if (destDir != null) {
			if (!destDir.exists()) {
				destDir.mkdirs();
			}
			if (importType == ImportType.RENDERING && !destFileName.endsWith(RENDERER_INDEX_EXT)) {
				String fileName = Algorithms.getFileNameWithoutExtension(destFileName);
				destFileName = fileName + RENDERER_INDEX_EXT;
			}
			File destFile = new File(destDir, destFileName);
			while (destFile.exists()) {
				destFileName = AndroidUtils.createNewFileName(destFileName);
				destFile = new File(destDir, destFileName);
			}
			return destFile;
		}
		return null;
	}

	protected static ImportType checkImportType(OsmandApplication app, Uri uri) {
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
			ImportHelper.log.error(e);
		} catch (IOException e) {
			ImportHelper.log.error(e);
		} catch (SecurityException e) {
			ImportHelper.log.error(e.getMessage(), e);
		} finally {
			Algorithms.closeStream(is);
		}
		return importType;
	}
}
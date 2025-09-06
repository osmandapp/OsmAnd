package net.osmand.plus.importfiles.tasks;

import static net.osmand.IndexConstants.BINARY_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.MAPS_PATH;
import static net.osmand.IndexConstants.RENDERER_INDEX_EXT;
import static net.osmand.IndexConstants.ROUTING_FILE_EXT;
import static net.osmand.IndexConstants.SQLITE_EXT;
import static net.osmand.IndexConstants.TEMP_DIR;
import static net.osmand.IndexConstants.TILES_INDEX_DIR;
import static net.osmand.IndexConstants.ZIP_EXT;
import static net.osmand.plus.utils.FileUtils.createUniqueFileName;
import static net.osmand.util.Algorithms.OBF_FILE_SIGNATURE;
import static net.osmand.util.Algorithms.SQLITE_FILE_SIGNATURE;
import static net.osmand.util.Algorithms.XML_FILE_SIGNATURE;
import static net.osmand.util.Algorithms.ZIP_FILE_SIGNATURE;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.importfiles.ImportType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FileUtils;
import net.osmand.util.Algorithms;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class UriImportTask extends BaseLoadAsyncTask<Void, Void, String> {

	private final ImportHelper importHelper;
	private final Uri uri;
	private String tempFileName;

	private int fileSignature;

	private final boolean save;
	private final boolean useImportDir;

	public UriImportTask(@NonNull ImportHelper importHelper, @NonNull FragmentActivity activity,
	                     @NonNull Uri uri, boolean save, boolean useImportDir) {
		super(activity);
		this.importHelper = importHelper;
		this.uri = uri;
		this.save = save;
		this.useImportDir = useImportDir;
	}

	@Override
	protected String doInBackground(Void... nothing) {
		String error = null;
		InputStream is = null;
		OutputStream out = null;
		try {
			is = app.getContentResolver().openInputStream(uri);
			if (is != null) {
				fileSignature = Algorithms.readInt(is);
				if (isSupportedFileSignature()) {
					File tempDir = FileUtils.getTempDir(app);
					tempFileName = getTempFileName();
					File dest = new File(tempDir, tempFileName);

					out = new FileOutputStream(dest);
					Algorithms.writeInt(out, Integer.reverseBytes(fileSignature));
					Algorithms.streamCopy(is, out);
				}
			}
		} catch (SecurityException | NullPointerException | IOException e) {
			ImportHelper.LOG.error(e);
			error = e.getMessage();
		} finally {
			Algorithms.closeStream(is);
			Algorithms.closeStream(out);
		}
		return error;
	}

	private boolean isSupportedFileSignature() {
		return fileSignature == XML_FILE_SIGNATURE || fileSignature == OBF_FILE_SIGNATURE
				|| fileSignature == ZIP_FILE_SIGNATURE || fileSignature == SQLITE_FILE_SIGNATURE;
	}

	@Override
	protected void onPostExecute(String error) {
		hideProgress();
		File file = app.getAppPath(TEMP_DIR + tempFileName);
		if (error == null && file.exists()) {
			Uri tempUri = AndroidUtils.getUriForFile(app, file);
			if (XML_FILE_SIGNATURE == fileSignature) {
				ImportType importType = XmlImportTask.checkImportType(app, tempUri);
				if (importType == ImportType.RENDERING || importType == ImportType.ROUTING) {
					String name = importType == ImportType.RENDERING ? "renderer" + RENDERER_INDEX_EXT : "router" + ROUTING_FILE_EXT;
					importHelper.handleXmlFileImport(tempUri, name);
				} else if (importType == ImportType.GPX || importType == ImportType.KML) {
					importHelper.handleGpxOrFavouritesImport(tempUri, tempFileName, save, useImportDir, false, false, false);
				}
			} else if (OBF_FILE_SIGNATURE == fileSignature) {
				String name = createUniqueFileName(app, "map", MAPS_PATH, BINARY_MAP_INDEX_EXT);
				importHelper.handleObfImport(tempUri, name + BINARY_MAP_INDEX_EXT);
			} else if (ZIP_FILE_SIGNATURE == fileSignature) {
				importHelper.handleZipImport(tempUri, save, useImportDir);
			} else if (SQLITE_FILE_SIGNATURE == fileSignature) {
				String name = createUniqueFileName(app, "online_map", TILES_INDEX_DIR, SQLITE_EXT);
				importHelper.handleSqliteTileImport(tempUri, name + SQLITE_EXT);
			}
		} else {
			app.showShortToastMessage(R.string.file_import_error, tempFileName, error);
		}
	}

	private String getTempFileName() {
		if (XML_FILE_SIGNATURE == fileSignature) {
			return createUniqueFileName(app, "xml_file", TEMP_DIR, ROUTING_FILE_EXT) + ROUTING_FILE_EXT;
		} else if (OBF_FILE_SIGNATURE == fileSignature) {
			return createUniqueFileName(app, "map", TEMP_DIR, BINARY_MAP_INDEX_EXT) + BINARY_MAP_INDEX_EXT;
		} else if (ZIP_FILE_SIGNATURE == fileSignature) {
			return createUniqueFileName(app, "zip_file", TEMP_DIR, ZIP_EXT) + ZIP_EXT;
		} else if (SQLITE_FILE_SIGNATURE == fileSignature) {
			return createUniqueFileName(app, "online_map", TEMP_DIR, SQLITE_EXT) + SQLITE_EXT;
		}
		return "";
	}
}
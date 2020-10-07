package net.osmand.plus.importfiles;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static net.osmand.FileUtils.createUniqueFileName;
import static net.osmand.IndexConstants.BINARY_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.MAPS_PATH;
import static net.osmand.IndexConstants.ROUTING_FILE_EXT;
import static net.osmand.IndexConstants.SQLITE_EXT;
import static net.osmand.IndexConstants.TEMP_DIR;
import static net.osmand.IndexConstants.TILES_INDEX_DIR;
import static net.osmand.IndexConstants.ZIP_EXT;
import static net.osmand.util.Algorithms.OBF_FILE_SIGNATURE;
import static net.osmand.util.Algorithms.SQLITE_FILE_SIGNATURE;
import static net.osmand.util.Algorithms.XML_FILE_SIGNATURE;
import static net.osmand.util.Algorithms.ZIP_FILE_SIGNATURE;

class UriImportTask extends BaseImportAsyncTask<Void, Void, String> {

	private ImportHelper importHelper;
	private Uri uri;
	private String tempFileName;

	private int fileSignature;

	private boolean save;
	private boolean useImportDir;

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
					File tempDir = app.getAppPath(TEMP_DIR);
					if (!tempDir.exists()) {
						tempDir.mkdirs();
					}
					tempFileName = getTempFileName();
					File dest = new File(tempDir, tempFileName);

					out = new FileOutputStream(dest);
					Algorithms.writeInt(out, Integer.reverseBytes(fileSignature));
					Algorithms.streamCopy(is, out);
				}
			}
		} catch (FileNotFoundException e) {
			ImportHelper.log.error(e);
			error = e.getMessage();
		} catch (SecurityException e) {
			ImportHelper.log.error(e);
			error = e.getMessage();
		} catch (IOException e) {
			ImportHelper.log.error(e);
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
				importHelper.handleXmlFileImport(tempUri, null, null);
			} else if (OBF_FILE_SIGNATURE == fileSignature) {
				String name = createUniqueFileName(app, "map", MAPS_PATH, BINARY_MAP_INDEX_EXT);
				importHelper.handleObfImport(tempUri, name);
			} else if (ZIP_FILE_SIGNATURE == fileSignature) {
//				importHelper.handleKmzImport(tempUri, null, save, useImportDir);
			} else if (SQLITE_FILE_SIGNATURE == fileSignature) {
				String name = createUniqueFileName(app, "online_map", TILES_INDEX_DIR, SQLITE_EXT);
				importHelper.handleSqliteTileImport(tempUri, name);
			}
		} else {
			app.showShortToastMessage(app.getString(R.string.file_import_error, tempFileName, error));
		}
	}

	private String getTempFileName() {
		if (XML_FILE_SIGNATURE == fileSignature) {
			return createUniqueFileName(app, "xml_file", TEMP_DIR, ROUTING_FILE_EXT);
		} else if (OBF_FILE_SIGNATURE == fileSignature) {
			return createUniqueFileName(app, "map", TEMP_DIR, BINARY_MAP_INDEX_EXT);
		} else if (ZIP_FILE_SIGNATURE == fileSignature) {
			return createUniqueFileName(app, "zip_file", TEMP_DIR, ZIP_EXT);
		} else if (SQLITE_FILE_SIGNATURE == fileSignature) {
			return createUniqueFileName(app, "online_map", TEMP_DIR, SQLITE_EXT);
		}
		return "";
	}
}
package net.osmand.plus.importfiles.tasks;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.IndexConstants.GPX_IMPORT_DIR;
import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.IndexConstants.OSMAND_SETTINGS_FILE_EXT;
import static net.osmand.IndexConstants.TEMP_DIR;
import static net.osmand.plus.importfiles.ImportHelper.KML_SUFFIX;
import static net.osmand.plus.importfiles.OnSuccessfulGpxImport.OPEN_GPX_CONTEXT_MENU;
import static net.osmand.plus.utils.FileUtils.createUniqueFileName;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.importfiles.ImportType;
import net.osmand.util.Algorithms;

import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipImportTask extends BaseLoadAsyncTask<Void, Void, ImportType> {

	private final ImportHelper importHelper;
	private final Uri uri;
	private final boolean save;
	private final boolean useImportDir;

	public ZipImportTask(@NonNull ImportHelper importHelper, @NonNull FragmentActivity activity,
	                     @NonNull Uri uri, boolean save, boolean useImportDir) {
		super(activity);
		this.importHelper = importHelper;
		this.uri = uri;
		this.save = save;
		this.useImportDir = useImportDir;
	}

	@Override
	protected ImportType doInBackground(Void... voids) {
		ImportType importType = null;
		InputStream is = null;
		ZipInputStream zis = null;
		try {
			is = app.getContentResolver().openInputStream(uri);
			if (is != null) {
				zis = new ZipInputStream(is);
				ZipEntry entry;
				while ((entry = zis.getNextEntry()) != null) {
					String fileName = checkEntryName(entry.getName());
					if (fileName.endsWith(KML_SUFFIX)) {
						importType = ImportType.KMZ;
						break;
					} else if (fileName.equals("items.json")) {
						importType = ImportType.SETTINGS;
						break;
					}
				}
			}
		} catch (Exception e) {
			ImportHelper.LOG.error(e.getMessage(), e);
		} finally {
			Algorithms.closeStream(is);
			Algorithms.closeStream(zis);
		}
		return importType;
	}

	private String checkEntryName(String entryName) {
		String fileExt = OSMAND_SETTINGS_FILE_EXT + "/";
		int index = entryName.indexOf(fileExt);
		if (index != -1) {
			entryName = entryName.substring(index + fileExt.length());
		}
		return entryName;
	}

	@Override
	protected void onPostExecute(ImportType importType) {
		hideProgress();
		if (importType == ImportType.KMZ) {
			String dir = useImportDir ? GPX_IMPORT_DIR : GPX_INDEX_DIR;
			String name = createUniqueFileName(app, "track", dir, GPX_FILE_EXT);
			importHelper.handleGpxImport(uri, name + GPX_FILE_EXT, OPEN_GPX_CONTEXT_MENU, useImportDir, save, false);
		} else if (importType == ImportType.SETTINGS) {
			String name = createUniqueFileName(app, "settings", TEMP_DIR, OSMAND_SETTINGS_FILE_EXT);
			importHelper.handleOsmAndSettingsImport(uri, name + OSMAND_SETTINGS_FILE_EXT,
					null, false, false, null, -1);
		}
	}
}
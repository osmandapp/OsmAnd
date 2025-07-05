package net.osmand.plus.backup;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.OperationLog;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.backup.BackupDbHelper.UploadedFileInfo;
import net.osmand.plus.backup.BackupListeners.OnCollectLocalFilesListener;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;
import net.osmand.plus.settings.backend.backup.items.GpxSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.settings.backend.backup.items.StreamSettingsItem;
import net.osmand.util.Algorithms;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class CollectLocalFilesTask extends AsyncTask<Void, LocalFile, List<LocalFile>> {

	private final OperationLog operationLog = new OperationLog("collectLocalFiles", BackupHelper.DEBUG);

	private final OsmandApplication app;
	private final BackupDbHelper dbHelper;
	private final BackupHelper backupHelper;

	private final OnCollectLocalFilesListener listener;

	private SQLiteConnection connection;
	private Map<String, UploadedFileInfo> infos;

	protected CollectLocalFilesTask(@NonNull OsmandApplication app,
			@Nullable OnCollectLocalFilesListener listener) {
		this.app = app;
		this.backupHelper = app.getBackupHelper();
		this.dbHelper = backupHelper.getDbHelper();
		this.listener = listener;
		operationLog.startOperation();
	}

	@Override
	protected void onPreExecute() {
		connection = dbHelper.openConnection(true);
	}

	@Override
	protected List<LocalFile> doInBackground(Void... voids) {
		List<LocalFile> result = new ArrayList<>();
		infos = dbHelper.getUploadedFileInfoMap();
		List<SettingsItem> localItems = getLocalItems();
		operationLog.log("getLocalItems");
		for (SettingsItem item : localItems) {
			String fileName = BackupUtils.getItemFileName(item);
			if (item instanceof FileSettingsItem fileItem) {
				File file = fileItem.getFile();
				if (file.isDirectory()) {
					if (item instanceof GpxSettingsItem) {
						continue;
					} else if (fileItem.getSubtype() == FileSubtype.VOICE) {
						File jsFile = new File(file, file.getName() + "_" + IndexConstants.TTSVOICE_INDEX_EXT_JS);
						if (jsFile.exists()) {
							fileName = jsFile.getPath().replace(app.getAppPath(null).getPath() + "/", "");
							createLocalFile(result, item, fileName, jsFile, jsFile.lastModified());
							continue;
						}
					} else if (fileItem.getSubtype() == FileSubtype.TTS_VOICE) {
						String langName = file.getName().replace(IndexConstants.VOICE_PROVIDER_SUFFIX, "");
						File jsFile = new File(file, langName + "_" + IndexConstants.TTSVOICE_INDEX_EXT_JS);
						if (jsFile.exists()) {
							fileName = jsFile.getPath().replace(app.getAppPath(null).getPath() + "/", "");
							createLocalFile(result, item, fileName, jsFile, jsFile.lastModified());
							continue;
						}
					} else if (fileItem.getSubtype() == FileSubtype.TILES_MAP) {
						continue;
					}
					List<File> dirs = new ArrayList<>();
					dirs.add(file);
					Algorithms.collectDirs(file, dirs);
					operationLog.log("collectDirs " + file.getName() + " BEGIN");
					for (File dir : dirs) {
						File[] files = dir.listFiles();
						if (files != null) {
							for (File f : files) {
								if (!f.isDirectory()) {
									fileName = f.getPath().replace(app.getAppPath(null).getPath() + "/", "");
									createLocalFile(result, item, fileName, f, f.lastModified());
								}
							}
						}
					}
					operationLog.log("collectDirs " + file.getName() + " END");
				} else if (fileItem.getSubtype() == FileSubtype.TILES_MAP) {
					if (file.getName().endsWith(SQLiteTileSource.EXT)) {
						createLocalFile(result, item, fileName, file, file.lastModified());
					}
				} else {
					createLocalFile(result, item, fileName, file, file.lastModified(), item.getInfoModifiedTime());
				}
			} else {
				createLocalFile(result, item, fileName, null, item.getLastModifiedTime());
			}
		}
		return result;
	}

	private void createLocalFile(@NonNull List<LocalFile> result, @NonNull SettingsItem item,
			@NonNull String fileName, @Nullable File file, long lastModifiedTime) {
		createLocalFile(result, item, fileName, file, lastModifiedTime, 0);
	}

	private void createLocalFile(@NonNull List<LocalFile> result, @NonNull SettingsItem item,
			@NonNull String fileName, @Nullable File file, long lastModifiedTime, long infoModifiedTime) {
		LocalFile localFile = new LocalFile();
		localFile.file = file;
		localFile.item = item;
		localFile.fileName = fileName;
		localFile.localModifiedTime = lastModifiedTime;

		if (infoModifiedTime > 0) {
			localFile.localModifiedTime = Math.max(lastModifiedTime, infoModifiedTime);
		} else {
			localFile.localModifiedTime = lastModifiedTime;
		}
		if (infos != null) {
			UploadedFileInfo fileInfo = infos.get(item.getType().name() + "___" + fileName);
			if (fileInfo != null) {
				localFile.uploadTime = fileInfo.getUploadTime();
				checkM5Digest(localFile, fileInfo, lastModifiedTime, infoModifiedTime);
			}
		}
		result.add(localFile);
		publishProgress(localFile);
	}

	private void checkM5Digest(@NonNull LocalFile localFile, @NonNull UploadedFileInfo fileInfo,
			long lastModifiedTime, long infoModifiedTime) {
		SettingsItem item = localFile.item;
		String lastMd5 = fileInfo.getMd5Digest();

		boolean needM5Digest = item instanceof StreamSettingsItem
				&& ((StreamSettingsItem) item).needMd5Digest()
				&& localFile.uploadTime < lastModifiedTime
				&& !Algorithms.isEmpty(lastMd5)
				&& infoModifiedTime <= lastModifiedTime;

		if (needM5Digest && localFile.file != null && localFile.file.exists()) {
			FileInputStream is = null;
			try {
				is = new FileInputStream(localFile.file);
				String md5 = new String(Hex.encodeHex(DigestUtils.md5(is)));
				if (md5.equals(lastMd5)) {
					item.setLocalModifiedTime(localFile.uploadTime);
					localFile.localModifiedTime = localFile.uploadTime;
				}
			} catch (IOException e) {
				BackupHelper.LOG.error(e.getMessage(), e);
			} finally {
				Algorithms.closeStream(is);
			}
		}
	}

	@NonNull
	private List<SettingsItem> getLocalItems() {
		List<ExportType> types = getEnabledExportTypes();
		return app.getFileSettingsHelper().getFilteredSettingsItems(types, true, true, false);
	}

	@NonNull
	private List<ExportType> getEnabledExportTypes() {
		List<ExportType> result = new ArrayList<>();
		for (ExportType exportType : ExportType.enabledValues()) {
			if (backupHelper.getBackupTypePref(exportType).get()) {
				result.add(exportType);
			}
		}
		return result;
	}

	@Override
	protected void onProgressUpdate(LocalFile... localFiles) {
		for (LocalFile file : localFiles) {
			if (listener != null) {
				listener.onFileCollected(file);
			}
		}
	}

	@Override
	protected void onPostExecute(List<LocalFile> localFiles) {
		if (connection != null) {
			connection.close();
		}
		operationLog.finishOperation(" Files=" + localFiles.size());
		if (listener != null) {
			listener.onFilesCollected(localFiles);
		}
	}
}

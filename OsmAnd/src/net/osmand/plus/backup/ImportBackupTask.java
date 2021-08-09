package net.osmand.plus.backup;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.backup.BackupImporter.CollectItemsResult;
import net.osmand.plus.backup.BackupImporter.NetworkImportProgressListener;
import net.osmand.plus.backup.ExportBackupTask.ItemProgressInfo;
import net.osmand.plus.backup.ImportBackupItemsTask.ImportItemsListener;
import net.osmand.plus.backup.NetworkSettingsHelper.BackupCollectListener;
import net.osmand.plus.settings.backend.backup.SettingsHelper.CheckDuplicatesListener;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ImportListener;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ImportType;
import net.osmand.plus.settings.backend.backup.items.CollectionSettingsItem;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.ProfileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportBackupTask extends AsyncTask<Void, ItemProgressInfo, List<SettingsItem>> {

	private final NetworkSettingsHelper helper;
	private final OsmandApplication app;

	private ImportListener importListener;
	private BackupCollectListener collectListener;
	private CheckDuplicatesListener duplicatesListener;
	private final BackupImporter importer;

	private List<SettingsItem> items = new ArrayList<>();
	private List<SettingsItem> selectedItems = new ArrayList<>();
	private List<Object> duplicates;

	private List<RemoteFile> remoteFiles;

	private final String key;
	private final Map<String, ItemProgressInfo> itemsProgress = new HashMap<>();
	private final ImportType importType;

	ImportBackupTask(@NonNull String key,
					 @NonNull NetworkSettingsHelper helper,
					 @Nullable BackupCollectListener collectListener,
					 boolean readData) {
		this.key = key;
		this.helper = helper;
		this.app = helper.getApp();
		this.collectListener = collectListener;
		importer = new BackupImporter(app.getBackupHelper(), getProgressListener());
		importType = readData ? ImportType.COLLECT_AND_READ : ImportType.COLLECT;
	}

	ImportBackupTask(@NonNull String key,
					 @NonNull NetworkSettingsHelper helper,
					 @NonNull List<SettingsItem> items,
					 @Nullable ImportListener importListener,
					 boolean forceReadData) {
		this.key = key;
		this.helper = helper;
		this.app = helper.getApp();
		this.importListener = importListener;
		this.items = items;
		importer = new BackupImporter(app.getBackupHelper(), getProgressListener());
		importType = forceReadData ? ImportType.IMPORT_FORCE_READ : ImportType.IMPORT;
	}

	ImportBackupTask(@NonNull String key,
					 @NonNull NetworkSettingsHelper helper,
					 @NonNull List<SettingsItem> items,
					 @NonNull List<SettingsItem> selectedItems,
					 @Nullable CheckDuplicatesListener duplicatesListener) {
		this.key = key;
		this.helper = helper;
		this.app = helper.getApp();
		this.items = items;
		this.duplicatesListener = duplicatesListener;
		this.selectedItems = selectedItems;
		importer = new BackupImporter(app.getBackupHelper(), getProgressListener());
		importType = ImportType.CHECK_DUPLICATES;
	}

	@Override
	protected List<SettingsItem> doInBackground(Void... voids) {
		switch (importType) {
			case COLLECT:
			case COLLECT_AND_READ:
				try {
					CollectItemsResult result = importer.collectItems(importType == ImportType.COLLECT_AND_READ);
					remoteFiles = result.remoteFiles;
					return result.items;
				} catch (IllegalArgumentException e) {
					NetworkSettingsHelper.LOG.error("Failed to collect items for backup", e);
				} catch (IOException e) {
					NetworkSettingsHelper.LOG.error("Failed to collect items for backup", e);
				}
				break;
			case CHECK_DUPLICATES:
				this.duplicates = getDuplicatesData(selectedItems);
				return selectedItems;
			case IMPORT:
			case IMPORT_FORCE_READ:
				if (items != null && items.size() > 0) {
					BackupHelper backupHelper = app.getBackupHelper();
					PrepareBackupResult backup = backupHelper.getBackup();
					for (SettingsItem item : items) {
						item.apply();
						String fileName = item.getFileName();
						if (fileName != null) {
							RemoteFile remoteFile = backup.getRemoteFile(item.getType().name(), fileName);
							if (remoteFile != null) {
								backupHelper.updateFileUploadTime(remoteFile.getType(), remoteFile.getName(), remoteFile.getClienttimems());
							}
						}
					}
				}
				return items;
		}
		return null;
	}

	@Override
	protected void onPostExecute(@Nullable List<SettingsItem> items) {
		if (items != null && importType != ImportType.CHECK_DUPLICATES) {
			this.items = items;
		} else {
			selectedItems = items;
		}
		switch (importType) {
			case COLLECT:
			case COLLECT_AND_READ:
				collectListener.onBackupCollectFinished(items != null, false, this.items, remoteFiles);
				helper.importAsyncTasks.remove(key);
				break;
			case CHECK_DUPLICATES:
				if (duplicatesListener != null) {
					duplicatesListener.onDuplicatesChecked(duplicates, selectedItems);
				}
				helper.importAsyncTasks.remove(key);
				break;
			case IMPORT:
			case IMPORT_FORCE_READ:
				if (items != null && items.size() > 0) {
					boolean forceReadData = importType == ImportType.IMPORT_FORCE_READ;
					ImportItemsListener itemsListener = (succeed, needRestart) -> {
						helper.importAsyncTasks.remove(key);
						helper.finishImport(importListener, succeed, items, needRestart);
					};
					new ImportBackupItemsTask(app, importer, items, itemsListener, forceReadData)
							.executeOnExecutor(app.getBackupHelper().getExecutor());
				}
				break;
		}
	}

	@Override
	protected void onProgressUpdate(ItemProgressInfo... progressInfos) {
		if (importListener != null) {
			for (ItemProgressInfo info : progressInfos) {
				ItemProgressInfo prevInfo = getItemProgressInfo(info.type, info.fileName);
				if (prevInfo != null) {
					info.setWork(prevInfo.getWork());
				}
				itemsProgress.put(info.type + info.fileName, info);

				if (info.isFinished()) {
					importListener.onImportItemFinished(info.type, info.fileName);
				} else if (info.getValue() == 0) {
					importListener.onImportItemStarted(info.type, info.fileName, info.getWork());
				} else {
					importListener.onImportItemProgress(info.type, info.fileName, info.getValue());
				}
			}
		}
	}

	@Nullable
	public ItemProgressInfo getItemProgressInfo(@NonNull String type, @NonNull String fileName) {
		return itemsProgress.get(type + fileName);
	}

	public List<SettingsItem> getItems() {
		return items;
	}

	public void setImportListener(ImportListener importListener) {
		this.importListener = importListener;
	}

	public void setDuplicatesListener(CheckDuplicatesListener duplicatesListener) {
		this.duplicatesListener = duplicatesListener;
	}

	ImportType getImportType() {
		return importType;
	}

	public List<Object> getDuplicates() {
		return duplicates;
	}

	public List<SettingsItem> getSelectedItems() {
		return selectedItems;
	}

	private List<Object> getDuplicatesData(List<SettingsItem> items) {
		List<Object> duplicateItems = new ArrayList<>();
		for (SettingsItem item : items) {
			if (item instanceof ProfileSettingsItem) {
				if (item.exists()) {
					duplicateItems.add(((ProfileSettingsItem) item).getModeBean());
				}
			} else if (item instanceof CollectionSettingsItem<?>) {
				CollectionSettingsItem<?> settingsItem = (CollectionSettingsItem<?>) item;
				List<?> duplicates = settingsItem.processDuplicateItems();
				if (!duplicates.isEmpty() && settingsItem.shouldShowDuplicates()) {
					duplicateItems.addAll(duplicates);
				}
			} else if (item instanceof FileSettingsItem) {
				if (item.exists()) {
					duplicateItems.add(((FileSettingsItem) item).getFile());
				}
			}
		}
		return duplicateItems;
	}

	private NetworkImportProgressListener getProgressListener() {
		return new NetworkImportProgressListener() {

			@Override
			public void itemExportStarted(@NonNull String type, @NonNull String fileName, int work) {
				publishProgress(new ItemProgressInfo(type, fileName, 0, work, false));
			}

			@Override
			public void updateItemProgress(@NonNull String type, @NonNull String fileName, int progress) {
				publishProgress(new ItemProgressInfo(type, fileName, progress, 0, false));
			}

			@Override
			public void itemExportDone(@NonNull String type, @NonNull String fileName) {
				publishProgress(new ItemProgressInfo(type, fileName, 0, 0, true));
				if (isCancelled()) {
					importer.cancel();
				}
			}
		};
	}
}

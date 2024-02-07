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
import net.osmand.plus.backup.PrepareBackupResult.RemoteFilesType;
import net.osmand.plus.settings.backend.backup.SettingsHelper.CheckDuplicatesListener;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ImportListener;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ImportType;
import net.osmand.plus.settings.backend.backup.items.CollectionSettingsItem;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.ProfileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.QuickActionsSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
	private final RemoteFilesType filesType;

	private List<SettingsItem> items = new ArrayList<>();
	private List<SettingsItem> selectedItems = new ArrayList<>();
	private List<Object> duplicates;

	private List<RemoteFile> remoteFiles;

	private final String key;
	private final Map<String, ItemProgressInfo> itemsProgress = new HashMap<>();
	private final ImportType importType;
	private final boolean shouldReplace;
	private final boolean restoreDeleted;

	private int maxProgress;
	private int generalProgress;

	ImportBackupTask(@NonNull String key,
	                 @NonNull NetworkSettingsHelper helper,
	                 @Nullable BackupCollectListener collectListener,
	                 boolean readData) {
		this.key = key;
		this.helper = helper;
		this.app = helper.getApp();
		this.filesType = RemoteFilesType.UNIQUE;
		this.collectListener = collectListener;
		this.shouldReplace = true;
		this.restoreDeleted = false;
		importer = new BackupImporter(app.getBackupHelper(), getProgressListener());
		importType = readData ? ImportType.COLLECT_AND_READ : ImportType.COLLECT;
		maxProgress = calculateMaxProgress(app);
	}

	ImportBackupTask(@NonNull String key,
	                 @NonNull NetworkSettingsHelper helper,
	                 @NonNull List<SettingsItem> items,
	                 @NonNull RemoteFilesType filesType,
	                 @Nullable ImportListener importListener,
	                 boolean forceReadData,
	                 boolean shouldReplace,
	                 boolean restoreDeleted) {
		this.key = key;
		this.helper = helper;
		this.app = helper.getApp();
		this.filesType = filesType;
		this.importListener = importListener;
		this.items = items;
		this.shouldReplace = shouldReplace;
		this.restoreDeleted = restoreDeleted;
		importer = new BackupImporter(app.getBackupHelper(), getProgressListener());
		importType = forceReadData ? ImportType.IMPORT_FORCE_READ : ImportType.IMPORT;
		maxProgress = calculateMaxProgress(app);
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
		this.filesType = RemoteFilesType.UNIQUE;
		this.duplicatesListener = duplicatesListener;
		this.selectedItems = selectedItems;
		this.shouldReplace = true;
		this.restoreDeleted = false;
		importer = new BackupImporter(app.getBackupHelper(), getProgressListener());
		importType = ImportType.CHECK_DUPLICATES;
		maxProgress = calculateMaxProgress(app);
	}

	@Override
	protected List<SettingsItem> doInBackground(Void... voids) {
		switch (importType) {
			case COLLECT:
			case COLLECT_AND_READ:
				try {
					CollectItemsResult result = importer.collectItems(null, importType == ImportType.COLLECT_AND_READ, restoreDeleted);
					remoteFiles = result.remoteFiles;
					return result.items;
				} catch (IllegalArgumentException | IOException e) {
					NetworkSettingsHelper.LOG.error("Failed to collect items for backup", e);
				}
				break;
			case CHECK_DUPLICATES:
				this.duplicates = getDuplicatesData(selectedItems);
				return selectedItems;
			case IMPORT:
			case IMPORT_FORCE_READ:
				if (items != null && items.size() > 0) {
					if (importType == ImportType.IMPORT_FORCE_READ) {
						try {
							CollectItemsResult result = importer.collectItems(items, true, restoreDeleted);
							for (SettingsItem item : result.items) {
								item.setShouldReplace(shouldReplace);
							}
							items = result.items;
						} catch (IllegalArgumentException | IOException e) {
							NetworkSettingsHelper.LOG.error("Failed to recollect items for backup import", e);
							return null;
						}
					} else {
						for (SettingsItem item : items) {
							item.apply();
						}
					}
				}
				return items;
		}
		return null;
	}

	@Override
	protected void onCancelled() {
		onPostExecute(null);
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
				helper.importAsyncTasks.remove(key);
				collectListener.onBackupCollectFinished(items != null, false, this.items, remoteFiles);
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
					new ImportBackupItemsTask(app, importer, items, filesType, itemsListener, forceReadData, restoreDeleted)
							.executeOnExecutor(app.getBackupHelper().getExecutor());
				} else {
					helper.importAsyncTasks.remove(key);
					helper.finishImport(importListener, false, Collections.emptyList(), false);
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

	public int getMaxProgress() {
		return maxProgress;
	}

	public int getGeneralProgress() {
		return generalProgress;
	}

	public static int calculateMaxProgress(@NonNull OsmandApplication app) {
		long maxProgress = 0;
		BackupHelper backupHelper = app.getBackupHelper();
		BackupInfo info = backupHelper.getBackup().getBackupInfo();
		if (info != null) {
			for (RemoteFile file : info.filesToDownload) {
				maxProgress += backupHelper.calculateFileSize(file);
			}
		}
		return (int) (maxProgress / 1024);
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
				FileSettingsItem settingsItem = (FileSettingsItem) item;
				if (item.exists() && !isDefaultObfMap(settingsItem)) {
					duplicateItems.add(settingsItem.getFile());
				}
			} else if (item instanceof QuickActionsSettingsItem) {
				if (item.exists()) {
					duplicateItems.add(((QuickActionsSettingsItem) item).getButtonState());
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
			}

			@Override
			public void updateGeneralProgress(int downloadedItems, int uploadedKb) {
				if (isCancelled()) {
					importer.cancel();
				}
				generalProgress = uploadedKb;
				if (importListener != null) {
					importListener.onImportProgressUpdate(generalProgress, uploadedKb);
				}
			}
		};
	}

	private boolean isDefaultObfMap(@NonNull FileSettingsItem settingsItem) {
		String fileName = BackupHelper.getItemFileName(settingsItem);
		return BackupHelper.isDefaultObfMap(app, settingsItem, fileName);
	}
}

package net.osmand.plus.backup;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.backup.BackupImporter.CollectItemsResult;
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
import java.util.List;

public class ImportBackupTask extends AsyncTask<Void, Void, List<SettingsItem>> {

	private final NetworkSettingsHelper helper;
	private final OsmandApplication app;
	private String latestChanges;
	private int version;

	private ImportListener importListener;
	private BackupCollectListener collectListener;
	private CheckDuplicatesListener duplicatesListener;
	private final BackupImporter importer;

	private List<SettingsItem> items = new ArrayList<>();
	private List<SettingsItem> selectedItems = new ArrayList<>();
	private List<Object> duplicates;

	private List<RemoteFile> remoteFiles;

	private final ImportType importType;
	private boolean importDone;

	ImportBackupTask(@NonNull NetworkSettingsHelper helper,
					 String latestChanges, int version, boolean readData,
					 @Nullable BackupCollectListener collectListener) {
		this.helper = helper;
		this.app = helper.getApp();
		this.collectListener = collectListener;
		this.latestChanges = latestChanges;
		this.version = version;
		importer = new BackupImporter(app.getBackupHelper());
		importType = readData ? ImportType.COLLECT_AND_READ : ImportType.COLLECT;
	}

	ImportBackupTask(@NonNull NetworkSettingsHelper helper, boolean forceReadData,
				   @NonNull List<SettingsItem> items, String latestChanges, int version,
				   @Nullable ImportListener importListener) {
		this.helper = helper;
		this.app = helper.getApp();
		this.importListener = importListener;
		this.items = items;
		this.latestChanges = latestChanges;
		this.version = version;
		importer = new BackupImporter(app.getBackupHelper());
		importType = forceReadData ? ImportType.IMPORT_FORCE_READ : ImportType.IMPORT;
	}

	ImportBackupTask(@NonNull NetworkSettingsHelper helper,
				   @NonNull List<SettingsItem> items,
				   @NonNull List<SettingsItem> selectedItems,
				   @Nullable CheckDuplicatesListener duplicatesListener) {
		this.helper = helper;
		this.app = helper.getApp();
		this.items = items;
		this.duplicatesListener = duplicatesListener;
		this.selectedItems = selectedItems;
		importer = new BackupImporter(app.getBackupHelper());
		importType = ImportType.CHECK_DUPLICATES;
	}

	@Override
	protected void onPreExecute() {
		ImportBackupTask importTask = helper.getImportTask();
		if (importTask != null && !importTask.importDone) {
			helper.finishImport(importListener, false, items, false);
		}
		helper.setImportTask(this);
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
				importDone = true;
				collectListener.onBackupCollectFinished(items != null, false, this.items, remoteFiles);
				break;
			case CHECK_DUPLICATES:
				importDone = true;
				if (duplicatesListener != null) {
					duplicatesListener.onDuplicatesChecked(duplicates, selectedItems);
				}
				break;
			case IMPORT:
			case IMPORT_FORCE_READ:
				if (items != null && items.size() > 0) {
					new ImportBackupItemsTask(helper, importType == ImportType.IMPORT_FORCE_READ, importListener, items)
							.executeOnExecutor(app.getBackupHelper().getExecutor());
				}
				break;
		}
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

	boolean isImportDone() {
		return importDone;
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
}

package net.osmand.plus.backup;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.backup.NetworkSettingsHelper.CollectType;
import net.osmand.plus.settings.backend.backup.SettingsHelper.CheckDuplicatesListener;
import net.osmand.plus.settings.backend.backup.SettingsHelper.CollectListener;
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
	private CollectListener collectListener;
	private CheckDuplicatesListener duplicatesListener;
	private final BackupImporter importer;

	private List<SettingsItem> items = new ArrayList<>();
	private List<SettingsItem> selectedItems = new ArrayList<>();
	private List<Object> duplicates;

	private final ImportType importType;
	private CollectType collectType;
	private boolean importDone;

	ImportBackupTask(@NonNull NetworkSettingsHelper helper,
					 String latestChanges, int version, CollectType collectType,
					 @Nullable CollectListener collectListener) {
		this.helper = helper;
		this.app = helper.getApp();
		this.collectListener = collectListener;
		this.latestChanges = latestChanges;
		this.version = version;
		this.collectType = collectType;
		importer = new BackupImporter(app.getBackupHelper());
		importType = ImportType.COLLECT;
	}

	ImportBackupTask(@NonNull NetworkSettingsHelper helper,
				   @NonNull List<SettingsItem> items, String latestChanges, int version,
				   @Nullable ImportListener importListener) {
		this.helper = helper;
		this.app = helper.getApp();
		this.importListener = importListener;
		this.items = items;
		this.latestChanges = latestChanges;
		this.version = version;
		importer = new BackupImporter(app.getBackupHelper());
		importType = ImportType.IMPORT;
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
					return importer.collectItems(collectType, importType == ImportType.COLLECT_AND_READ);
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
				importDone = true;
				collectListener.onCollectFinished(items != null, false, this.items);
				break;
			case CHECK_DUPLICATES:
				importDone = true;
				if (duplicatesListener != null) {
					duplicatesListener.onDuplicatesChecked(duplicates, selectedItems);
				}
				break;
			case IMPORT:
				if (items != null && items.size() > 0) {
					for (SettingsItem item : items) {
						item.apply();
					}
					new ImportBackupItemsTask(helper, importListener, items)
							.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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

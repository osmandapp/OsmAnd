package net.osmand.plus.settings.backend.backup;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.backup.SettingsHelper.CheckDuplicatesListener;
import net.osmand.plus.settings.backend.backup.SettingsHelper.CollectListener;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ImportListener;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ImportType;
import net.osmand.plus.settings.backend.backup.items.CollectionSettingsItem;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.ProfileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.QuickActionsSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImportFileTask extends AsyncTask<Void, Void, List<SettingsItem>> {

	private final FileSettingsHelper helper;
	private final OsmandApplication app;
	private final File file;
	private String latestChanges;
	private int version;

	private ImportListener importListener;
	private CollectListener collectListener;
	private CheckDuplicatesListener duplicatesListener;
	private final SettingsImporter importer;

	private List<SettingsItem> items = new ArrayList<>();
	private List<SettingsItem> selectedItems = new ArrayList<>();
	private List<Object> duplicates;

	private final ImportType importType;
	private boolean importDone;

	ImportFileTask(@NonNull FileSettingsHelper helper,
				   @NonNull File file,
				   String latestChanges, int version,
				   @Nullable CollectListener collectListener) {
		this.helper = helper;
		this.app = helper.getApp();
		this.file = file;
		this.collectListener = collectListener;
		this.latestChanges = latestChanges;
		this.version = version;
		importer = new SettingsImporter(app);
		importType = ImportType.COLLECT;
	}

	ImportFileTask(@NonNull FileSettingsHelper helper,
				   @NonNull File file,
				   @NonNull List<SettingsItem> items, String latestChanges, int version,
				   @Nullable ImportListener importListener) {
		this.helper = helper;
		this.app = helper.getApp();
		this.file = file;
		this.importListener = importListener;
		this.items = items;
		this.latestChanges = latestChanges;
		this.version = version;
		importer = new SettingsImporter(app);
		importType = ImportType.IMPORT;
	}

	ImportFileTask(@NonNull FileSettingsHelper helper,
				   @NonNull File file,
				   @NonNull List<SettingsItem> items,
				   @NonNull List<SettingsItem> selectedItems,
				   @Nullable CheckDuplicatesListener duplicatesListener) {
		this.helper = helper;
		this.app = helper.getApp();
		this.file = file;
		this.items = items;
		this.duplicatesListener = duplicatesListener;
		this.selectedItems = selectedItems;
		importer = new SettingsImporter(app);
		importType = ImportType.CHECK_DUPLICATES;
	}

	@Override
	protected void onPreExecute() {
		ImportFileTask importTask = helper.getImportTask();
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
					return importer.collectItems(file);
				} catch (IllegalArgumentException | IOException e) {
					FileSettingsHelper.LOG.error("Failed to collect items from: " + file.getName(), e);
				}
				break;
			case CHECK_DUPLICATES:
				this.duplicates = getDuplicatesData(selectedItems);
				return selectedItems;
			case IMPORT:
				if (items != null && items.size() > 0) {
					for (SettingsItem item : items) {
						item.apply();
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
				importDone = true;
				collectListener.onCollectFinished(true, false, this.items);
				break;
			case CHECK_DUPLICATES:
				importDone = true;
				if (duplicatesListener != null) {
					duplicatesListener.onDuplicatesChecked(duplicates, selectedItems);
				}
				break;
			case IMPORT:
				if (items != null && items.size() > 0) {
					new ImportFileItemsTask(helper, file, importListener, items)
							.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				}
				break;
		}
	}

	public List<SettingsItem> getItems() {
		return items;
	}

	public File getFile() {
		return file;
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
			} else if (item instanceof QuickActionsSettingsItem) {
				if (item.exists()) {
					duplicateItems.add(((QuickActionsSettingsItem) item).getButtonState());
				}
			}
		}
		return duplicateItems;
	}
}

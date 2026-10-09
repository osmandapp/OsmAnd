package net.osmand.plus.settings.backend.backup;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.osmand.IndexConstants.OSMAND_SETTINGS_FILE_EXT;

/*
	Usage:

	SettingsHelper helper = app.getSettingsHelper();
	File file = new File(app.getAppPath(null), "settings.zip");

	List<SettingsItem> items = new ArrayList<>();
	items.add(new GlobalSettingsItem(app.getSettings()));
	items.add(new ProfileSettingsItem(app.getSettings(), ApplicationMode.DEFAULT));
	items.add(new ProfileSettingsItem(app.getSettings(), ApplicationMode.CAR));
	items.add(new ProfileSettingsItem(app.getSettings(), ApplicationMode.PEDESTRIAN));
	items.add(new ProfileSettingsItem(app.getSettings(), ApplicationMode.BICYCLE));
	items.add(new FileSettingsItem(app, new File(app.getAppPath(GPX_INDEX_DIR), "Day 2.gpx")));
	items.add(new FileSettingsItem(app, new File(app.getAppPath(GPX_INDEX_DIR), "Day 3.gpx")));
	items.add(new FileSettingsItem(app, new File(app.getAppPath(RENDERERS_DIR), "default.render.xml")));
	items.add(new DataSettingsItem(new byte[] {'t', 'e', 's', 't', '1'}, "data1"));
	items.add(new DataSettingsItem(new byte[] {'t', 'e', 's', 't', '2'}, "data2"));

	helper.exportSettings(file, items);

	helper.importSettings(file);
 */

public class FileSettingsHelper extends SettingsHelper {

	ImportFileTask importTask;
	final Map<File, ExportFileTask> exportAsyncTasks = new HashMap<>();

	public interface SettingsExportListener {
		void onSettingsExportFinished(@NonNull File file, boolean succeed);
		default void onSettingsExportProgressUpdate(int value) {
		}
	}

	public FileSettingsHelper(@NonNull OsmandApplication app) {
		super(app);
	}

	@Nullable
	public ImportFileTask getImportTask() {
		return importTask;
	}

	public void setImportTask(ImportFileTask importTask) {
		this.importTask = importTask;
	}

	@Nullable
	public ImportType getImportTaskType() {
		ImportFileTask importTask = this.importTask;
		return importTask != null ? importTask.getImportType() : null;
	}

	public boolean isImportDone() {
		ImportFileTask importTask = this.importTask;
		return importTask == null || importTask.isImportDone();
	}

	public boolean cancelExportForFile(@NonNull File file) {
		ExportFileTask exportTask = exportAsyncTasks.get(file);
		if (exportTask != null && (exportTask.getStatus() == AsyncTask.Status.RUNNING)) {
			return exportTask.cancel(true);
		}
		return false;
	}

	public boolean isFileExporting(@NonNull File file) {
		return exportAsyncTasks.containsKey(file);
	}

	public void updateExportListener(@NonNull File file, @Nullable SettingsExportListener listener) {
		ExportFileTask exportAsyncTask = exportAsyncTasks.get(file);
		if (exportAsyncTask != null) {
			exportAsyncTask.setListener(listener);
		}
	}

	void finishImport(@Nullable ImportListener listener, boolean success,
					  @NonNull List<SettingsItem> items, boolean needRestart) {
		importTask = null;
		List<String> warnings = new ArrayList<>();
		for (SettingsItem item : items) {
			warnings.addAll(item.getWarnings());
		}
		if (!warnings.isEmpty()) {
			getApp().showToastMessage(AndroidUtils.formatWarnings(warnings).toString());
		}
		if (listener != null) {
			listener.onImportFinished(success, needRestart, items);
		}
	}

	public void collectSettings(@NonNull File settingsFile, String latestChanges, int version,
								@Nullable CollectListener listener) {
		new ImportFileTask(this, settingsFile, latestChanges, version, listener)
				.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void checkDuplicates(@NonNull File settingsFile, @NonNull List<SettingsItem> items,
								@NonNull List<SettingsItem> selectedItems, CheckDuplicatesListener listener) {
		new ImportFileTask(this, settingsFile, items, selectedItems, listener)
				.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void importSettings(@NonNull File settingsFile, @NonNull List<SettingsItem> items,
							   String latestChanges, int version, @Nullable ImportListener listener) {
		new ImportFileTask(this, settingsFile, items, latestChanges, version, listener)
				.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void exportSettings(@NonNull File fileDir, @NonNull String fileName,
							   @Nullable SettingsExportListener listener,
							   @NonNull List<SettingsItem> items, boolean exportItemsFiles) {
		File file = new File(fileDir, fileName + OSMAND_SETTINGS_FILE_EXT);
		ExportFileTask exportAsyncTask = new ExportFileTask(this, file, listener, items, exportItemsFiles);
		exportAsyncTasks.put(file, exportAsyncTask);
		exportAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void exportSettings(@NonNull File fileDir, @NonNull String fileName,
							   @Nullable SettingsExportListener listener,
							   boolean exportItemsFiles, @NonNull SettingsItem... items) {
		exportSettings(fileDir, fileName, listener, new ArrayList<>(Arrays.asList(items)), exportItemsFiles);
	}
}

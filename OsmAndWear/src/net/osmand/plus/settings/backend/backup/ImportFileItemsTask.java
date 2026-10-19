package net.osmand.plus.settings.backend.backup;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.StateChangedListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ImportListener;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ImportFileItemsTask extends AsyncTask<Void, Void, Boolean> {

	private final FileSettingsHelper helper;
	private final OsmandApplication app;
	private final SettingsImporter importer;
	private final File file;
	private final ImportListener listener;
	private final List<SettingsItem> items;
	private final StateChangedListener<String> localeListener;
	private boolean needRestart;

	ImportFileItemsTask(@NonNull FileSettingsHelper helper,
						@NonNull File file,
						@Nullable ImportListener listener,
						@NonNull List<SettingsItem> items) {
		this.helper = helper;
		this.app = helper.getApp();
		importer = new SettingsImporter(app);
		this.file = file;
		this.listener = listener;
		this.items = items;
		localeListener = change -> needRestart = true;
	}

	@Override
	protected void onPreExecute() {
		app.getSettings().PREFERRED_LOCALE.addListener(localeListener);
	}

	@Override
	protected Boolean doInBackground(Void... voids) {
		try {
			importer.importItems(file, items);
			return true;
		} catch (IllegalArgumentException | IOException e) {
			FileSettingsHelper.LOG.error("Failed to import items from: " + file.getName(), e);
		}
		return false;
	}

	@Override
	protected void onPostExecute(Boolean success) {
		app.getSettings().PREFERRED_LOCALE.removeListener(localeListener);
		helper.finishImport(listener, success, items, needRestart);
	}
}

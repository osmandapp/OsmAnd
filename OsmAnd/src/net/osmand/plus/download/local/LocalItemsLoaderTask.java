package net.osmand.plus.download.local;

import static net.osmand.plus.download.local.ItemType.OTHER;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.util.Algorithms;

import java.io.File;

public class LocalItemsLoaderTask extends AsyncTask<Void, Void, Void> {

	private final OsmandApplication app;
	private final LocalItemsHolder itemsHolder = new LocalItemsHolder();

	@Nullable
	private final LoadItemsListener listener;

	public LocalItemsLoaderTask(@NonNull OsmandApplication app, @Nullable LoadItemsListener listener) {
		this.app = app;
		this.listener = listener;
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.loadItemsStarted();
		}
	}

	@Override
	protected Void doInBackground(Void... params) {
		collectFiles(getInternalDir(), false);
		collectFiles(getExternalDir(), true);
		return null;
	}

	private void collectFiles(@NonNull File parentDir, boolean addUnknown) {
		File[] listFiles = parentDir.listFiles();
		if (!Algorithms.isEmpty(listFiles)) {
			for (File file : listFiles) {
				addFile(file, addUnknown);

				if (file.isDirectory()) {
					collectFiles(file, addUnknown);
				}
			}
		}
	}

	private void addFile(@NonNull File file, boolean addUnknown) {
		ItemType type = ItemType.getItemType(app, file);
		if (type != null && (type != OTHER || addUnknown)) {
			itemsHolder.addLocalItem(new LocalItem(app, file, type));
		}
	}

	@NonNull
	private File getInternalDir() {
		File filesDir = app.getFilesDir();
		File parentDir = filesDir.getParentFile();
		return parentDir != null ? parentDir : filesDir;
	}

	@NonNull
	private File getExternalDir() {
		File appDir = app.getAppPath(null);
		File parentDir = appDir.getParentFile();
		return parentDir != null ? parentDir : appDir;
	}

	@Override
	protected void onPostExecute(Void unused) {
		if (listener != null) {
			listener.loadItemsFinished(itemsHolder);
		}
	}

	public interface LoadItemsListener {

		void loadItemsStarted();

		void loadItemsFinished(@NonNull LocalItemsHolder holder);
	}
}
package net.osmand.plus.download.local;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;

import java.util.Map;

public class LocalItemsLoaderTask extends AsyncTask<Void, Void, Map<CategoryType, LocalCategory>> {

	private final OsmandApplication app;
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
	protected Map<CategoryType, LocalCategory> doInBackground(Void... params) {
		LocalIndexHelper helper = new LocalIndexHelper(app);
		return helper.loadAllFilesByCategories();
	}

	@Override
	protected void onPostExecute(@NonNull Map<CategoryType, LocalCategory> categories) {
		if (listener != null) {
			listener.loadItemsFinished(categories);
		}
	}

	public interface LoadItemsListener {

		default void loadItemsStarted() {
		}

		default void loadItemsFinished(@NonNull Map<CategoryType, LocalCategory> categories) {
		}
	}
}
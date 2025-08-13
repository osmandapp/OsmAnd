package net.osmand.plus.mapcontextmenu.gallery.tasks;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.CallbackWithObject;
import net.osmand.plus.mapcontextmenu.gallery.PhotoCacheManager;

public class CacheReadTask extends AsyncTask<Void, Void, String> {

	private final PhotoCacheManager manager;
	private final String keyRaw;
	private final CallbackWithObject<String> callback;

	public CacheReadTask(@NonNull PhotoCacheManager manager, @NonNull String keyRaw, @NonNull CallbackWithObject<String> callback) {
		this.manager = manager;
		this.keyRaw = keyRaw;
		this.callback = callback;
	}

	@Override
	protected String doInBackground(Void... voids) {
		return manager.load(keyRaw);
	}

	@Override
	protected void onPostExecute(String result) {
		callback.processResult(result);
	}
}
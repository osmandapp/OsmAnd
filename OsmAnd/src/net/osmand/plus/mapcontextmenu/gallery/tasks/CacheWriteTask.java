package net.osmand.plus.mapcontextmenu.gallery.tasks;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.plus.mapcontextmenu.gallery.PhotoCacheManager;

public class CacheWriteTask extends AsyncTask<Void, Void, Void> {

	private final PhotoCacheManager manager;
	private final String keyRaw;
	private final String data;

	public CacheWriteTask(@NonNull PhotoCacheManager manager, @NonNull String keyRaw, @NonNull String data) {
		this.manager = manager;
		this.keyRaw = keyRaw;
		this.data = data;
	}

	@Override
	protected Void doInBackground(Void... voids) {
		manager.save(keyRaw, data);
		return null;
	}
}
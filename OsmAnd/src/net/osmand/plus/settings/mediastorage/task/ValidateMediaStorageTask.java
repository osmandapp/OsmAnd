package net.osmand.plus.settings.mediastorage.task;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.CallbackWithObject;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.mediastorage.MediaStorageHelper;
import net.osmand.plus.settings.mediastorage.MediaStorageLocation;

public class ValidateMediaStorageTask extends AsyncTask<Void, Void, Boolean> {

	private final MediaStorageHelper mediaStorageHelper;
	private final MediaStorageLocation storage;
	private final CallbackWithObject<Boolean> callback;

	public ValidateMediaStorageTask(@NonNull OsmandApplication app, @NonNull MediaStorageLocation storage, @NonNull CallbackWithObject<Boolean> callback) {
		this.mediaStorageHelper = new MediaStorageHelper(app);
		this.storage = storage;
		this.callback = callback;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		return mediaStorageHelper.isStorageWritable(storage);
	}

	@Override
	protected void onPostExecute(Boolean writable) {
		callback.processResult(writable);
	}
}
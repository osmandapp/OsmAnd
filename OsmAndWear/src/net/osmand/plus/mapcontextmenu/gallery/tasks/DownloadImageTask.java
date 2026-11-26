package net.osmand.plus.mapcontextmenu.gallery.tasks;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.AndroidNetworkUtils;

public class DownloadImageTask extends AsyncTask<Void, Void, Bitmap> {
	private final String imageUrl;
	private final OsmandApplication app;
	private final DownloadImageListener listener;

	public DownloadImageTask(@NonNull OsmandApplication app, @NonNull String imageUrl, @Nullable DownloadImageListener listener) {
		this.imageUrl = imageUrl;
		this.app = app;
		this.listener = listener;
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.onStartDownloading();
		}
	}

	@Override
	protected Bitmap doInBackground(Void... params) {
		return AndroidNetworkUtils.downloadImage(app, imageUrl);
	}

	@Override
	protected void onPostExecute(Bitmap bitmap) {
		if (listener != null) {
			listener.onFinishDownloading(bitmap);
		}
	}

	public interface DownloadImageListener {
		void onStartDownloading();

		void onFinishDownloading(Bitmap bitmap);
	}
}
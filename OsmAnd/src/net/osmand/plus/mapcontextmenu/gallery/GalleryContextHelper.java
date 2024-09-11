package net.osmand.plus.mapcontextmenu.gallery;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.mapcontextmenu.gallery.GalleryGridAdapter.ImageCardListener;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.util.Algorithms;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class GalleryContextHelper {

	private final OsmandApplication app;

	private List<ImageCard> onlinePhotoCards;

	private final ConcurrentHashMap<String, Bitmap> cachedImages = new ConcurrentHashMap<>();

	public GalleryContextHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public void setOnlinePhotoCards(List<ImageCard> onlinePhotoCards) {
		this.onlinePhotoCards = onlinePhotoCards;
		cachedImages.clear();
	}

	@Nullable
	public Bitmap getBitmap(@NonNull String imageUrl, @NonNull ImageCardListener listener) {
		Bitmap bitmap = cachedImages.get(imageUrl);
		if (bitmap == null) {
			MenuBuilder.execute(new DownloadImageTask(app, imageUrl, result -> {
				String resultImageUrl = result.first;
				Bitmap resultBitmap = result.second;
				if (!Algorithms.isEmpty(resultImageUrl) && resultBitmap != null) {
					cachedImages.put(resultImageUrl, resultBitmap);
				}
				listener.onImageDownloaded(resultImageUrl, resultBitmap);
			}));

		}
		return cachedImages.get(imageUrl);
	}

	@Nullable
	public Bitmap getBitmap(@NonNull String imageUrl) {
		return cachedImages.get(imageUrl);
	}

	public static int getSettingsSpanCount(@NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		if (AndroidUiHelper.isOrientationPortrait(mapActivity)) {
			return app.getSettings().CONTEXT_GALLERY_SPAN_GRID_COUNT.get();
		} else {
			return app.getSettings().CONTEXT_GALLERY_SPAN_GRID_COUNT_LANDSCAPE.get();
		}
	}

	public static void setSpanSettings(@NonNull MapActivity mapActivity, int newSpanCount) {
		OsmandApplication app = mapActivity.getMyApplication();
		if (AndroidUiHelper.isOrientationPortrait(mapActivity)) {
			app.getSettings().CONTEXT_GALLERY_SPAN_GRID_COUNT.set(newSpanCount);
		} else {
			app.getSettings().CONTEXT_GALLERY_SPAN_GRID_COUNT_LANDSCAPE.set(newSpanCount);
		}
	}

	public List<ImageCard> getOnlinePhotoCards() {
		return onlinePhotoCards;
	}

	private static class DownloadImageTask extends AsyncTask<Void, Void, Pair<String, Bitmap>> {

		private final DownloadImageListener downloadListener;
		private final OsmandApplication app;
		private final String imageUrl;

		public DownloadImageTask(@NonNull OsmandApplication app, @NonNull String imageUrl, @NonNull DownloadImageListener downloadListener) {
			this.app = app;
			this.downloadListener = downloadListener;
			this.imageUrl = imageUrl;
		}

		@Override
		protected Pair<String, Bitmap> doInBackground(Void... params) {
			Bitmap bitmap = AndroidNetworkUtils.downloadImage(app, imageUrl);
			return new Pair<>(imageUrl, bitmap);
		}

		@Override
		protected void onPostExecute(Pair<String, Bitmap> result) {
			downloadListener.onDownloadFinished(result);
		}
	}

	private interface DownloadImageListener {
		void onDownloadFinished(Pair<String, Bitmap> result);
	}
}

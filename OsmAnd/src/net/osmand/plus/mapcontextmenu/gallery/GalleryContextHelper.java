package net.osmand.plus.mapcontextmenu.gallery;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.mapcontextmenu.gallery.tasks.GetImageWikiMetaDataTask;
import net.osmand.plus.wikipedia.WikiImageCard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GalleryContextHelper {
	private ImageCardsHolder currentCardsHolder;
	private final List<DownloadMetaDataListener> metaDataListeners = new ArrayList<>();

	public List<ImageCard> getOnlinePhotoCards() {
		List<ImageCard> imageCards = new ArrayList<>();
		if (currentCardsHolder != null) {
			imageCards.addAll(currentCardsHolder.getOrderedList());
		}
		return imageCards;
	}

	public ImageCardsHolder getCurrentCardsHolder() {
		return currentCardsHolder;
	}

	public void addMetaDataListener(DownloadMetaDataListener listener){
		metaDataListeners.add(listener);
	}

	public void removeMetaDataListener(DownloadMetaDataListener listener){
		metaDataListeners.remove(listener);
	}

	public void downloadWikiMetaData(@NonNull WikiImageCard wikiImageCard, DownloadMetaDataListener listener) {
		GetImageWikiMetaDataTask getMetadata = new GetImageWikiMetaDataTask(wikiImageCard.getMyApplication(), wikiImageCard, wikiImageCard1 -> {
			listener.onMetaDataDownloaded(wikiImageCard1);
			notifyMetaDataDownloaded(wikiImageCard1);
		});
		getMetadata.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void notifyMetaDataDownloaded(@NonNull WikiImageCard wikiImageCard) {
		for (DownloadMetaDataListener listener : metaDataListeners) {
			if (listener != null) {
				listener.onMetaDataDownloaded(wikiImageCard);
			}
		}
	}

	public void setCurrentCardsHolder(ImageCardsHolder currentCardsHolder) {
		this.currentCardsHolder = currentCardsHolder;
	}

	public void clearHolder() {
		this.currentCardsHolder = null;
	}

	public boolean isCurrentHolderEquals(@NonNull LatLon latLon, Map<String, String> params) {
		if (currentCardsHolder != null) {
			return currentCardsHolder.getLatLon().equals(latLon) && currentCardsHolder.getParams().equals(params);
		}
		return false;
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
	public int getImageCardFromUrl(@NonNull String imageUrl) {
		for (int i = 0; i < getOnlinePhotoCards().size(); i++) {
			ImageCard card = getOnlinePhotoCards().get(i);
			if (imageUrl.equals(card.getImageUrl())) {
				return i;
			}
		}
		return 0;
	}

	public interface DownloadMetaDataListener{
		void onMetaDataDownloaded(@NonNull WikiImageCard wikiImageCard);
	}
}

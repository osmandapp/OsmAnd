package net.osmand.plus.mapcontextmenu.gallery;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.mapcontextmenu.gallery.tasks.LoadImageMetadataTask;
import net.osmand.plus.wikipedia.WikiImageCard;
import net.osmand.util.Algorithms;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GalleryController implements IDialogController {

	public static final String PROCESS_ID = "gallery_context_controller";

	private final OsmandApplication app;

	private ImageCardsHolder currentCardsHolder;
	private final List<WeakReference<DownloadMetadataListener>> listeners = new LinkedList<>();


	public GalleryController(@NonNull OsmandApplication app) {
		this.app = app;
	}

	@NonNull
	public List<ImageCard> getOnlinePhotoCards() {
		List<ImageCard> imageCards = new ArrayList<>();
		if (currentCardsHolder != null) {
			imageCards.addAll(currentCardsHolder.getOrderedCards());
		}
		return imageCards;
	}

	@Nullable
	public ImageCardsHolder getCurrentCardsHolder() {
		return currentCardsHolder;
	}


	public void addMetaDataListener(@NonNull DownloadMetadataListener listener) {
		if (!listeners.contains(new WeakReference<>(listener))) {
			listeners.add(new WeakReference<>(listener));
		}
	}

	public void removeMetaDataListener(@NonNull DownloadMetadataListener listener) {
		Iterator<WeakReference<DownloadMetadataListener>> it = listeners.iterator();
		while (it.hasNext()) {
			DownloadMetadataListener metadataListener = it.next().get();
			if (metadataListener == listener) {
				it.remove();
			}
		}
	}

	public void downloadWikiMetaData(@NonNull WikiImageCard card) {
		LoadImageMetadataTask task = new LoadImageMetadataTask(app, card, this::notifyMetaDataDownloaded);
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void notifyMetaDataDownloaded(@NonNull WikiImageCard wikiImageCard) {
		Iterator<WeakReference<DownloadMetadataListener>> it = listeners.iterator();
		while (it.hasNext()) {
			DownloadMetadataListener listener = it.next().get();
			if (listener == null) {
				it.remove();
			} else {
				listener.onMetadataDownloaded(wikiImageCard);
			}
		}
	}

	public void setCurrentCardsHolder(@Nullable ImageCardsHolder cardsHolder) {
		this.currentCardsHolder = cardsHolder;
	}

	public void clearHolder() {
		this.currentCardsHolder = null;
	}

	public boolean isCurrentHolderEquals(@NonNull LatLon latLon, @NonNull Map<String, String> params) {
		return currentCardsHolder != null && Algorithms.objectEquals(currentCardsHolder.getLatLon(), latLon)
				&& Algorithms.objectEquals(currentCardsHolder.getParams(), params);
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

	public void clearListeners() {
		listeners.clear();
	}

	public interface DownloadMetadataListener {
		void onMetadataDownloaded(@NonNull WikiImageCard imageCard);
	}
}

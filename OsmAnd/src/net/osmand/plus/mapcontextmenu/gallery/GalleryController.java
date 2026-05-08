package net.osmand.plus.mapcontextmenu.gallery;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.gallery.GalleryItem;
import net.osmand.plus.gallery.MediaProvider;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.shared.media.domain.MediaItem;
import net.osmand.util.Algorithms;

import java.util.*;

public class GalleryController implements IDialogController {

	public static final String PROCESS_ID = "gallery_context_controller";

	private GalleryItemsHolder currentGalleryItemsHolder;

	private final MediaProvider mediaProvider;

	public GalleryController(@NonNull OsmandApplication app) {
		mediaProvider = new MediaProvider(app);
	}

	@NonNull
	public MediaProvider getMediaProvider() {
		return mediaProvider;
	}

	@Nullable
	public GalleryItemsHolder getCurrentGalleryItemsHolder() {
		return currentGalleryItemsHolder;
	}

	public void setCurrentGalleryItemsHolder(@Nullable GalleryItemsHolder galleryItemsHolder) {
		this.currentGalleryItemsHolder = galleryItemsHolder;
	}

	public void clearHolder() {
		this.currentGalleryItemsHolder = null;
	}

	public boolean isCurrentHolderEquals(@NonNull LatLon latLon, @NonNull Map<String, String> params) {
		return currentGalleryItemsHolder != null
				&& Algorithms.objectEquals(currentGalleryItemsHolder.getLatLon(), latLon)
				&& Algorithms.objectEquals(currentGalleryItemsHolder.getParams(), params);
	}

	public int getMediaItemIndexById(@NonNull String id) {
		List<GalleryItem.Media> mediaItems = getOnlinePhotoItems();
		for (int i = 0; i < mediaItems.size(); i++) {
			MediaItem mediaItem = mediaItems.get(i).getMediaItem();
			if (Algorithms.stringsEqual(id, mediaItem.getId())) {
				return i;
			}
		}
		return 0;
	}

	@NonNull
	public List<GalleryItem.Media> getOnlinePhotoItems() {
		List<GalleryItem.Media> galleryItems = new ArrayList<>();
		if (currentGalleryItemsHolder != null) {
			for (GalleryItem item : currentGalleryItemsHolder.getOrderedGalleryItems()) {
				if (item instanceof GalleryItem.Media media) {
					galleryItems.add(media);
				}
			}
		}
		return galleryItems;
	}

	public static int getSettingsSpanCount(@NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getApp();
		if (AndroidUiHelper.isOrientationPortrait(mapActivity)) {
			return app.getSettings().CONTEXT_GALLERY_SPAN_GRID_COUNT.get();
		} else {
			return app.getSettings().CONTEXT_GALLERY_SPAN_GRID_COUNT_LANDSCAPE.get();
		}
	}

	public static void setSpanSettings(@NonNull MapActivity mapActivity, int newSpanCount) {
		OsmandApplication app = mapActivity.getApp();
		if (AndroidUiHelper.isOrientationPortrait(mapActivity)) {
			app.getSettings().CONTEXT_GALLERY_SPAN_GRID_COUNT.set(newSpanCount);
		} else {
			app.getSettings().CONTEXT_GALLERY_SPAN_GRID_COUNT_LANDSCAPE.set(newSpanCount);
		}
	}
}

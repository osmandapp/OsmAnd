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
import net.osmand.shared.util.NetworkImageLoader;
import net.osmand.util.Algorithms;

import java.util.*;

public class GalleryController implements IDialogController {

	public static final String PROCESS_ID = "gallery_context_controller";

	private GalleryItemsHolder currentMediaHolder;

	private final MediaProvider mediaProvider;
	private final NetworkImageLoader imageLoader;

	public GalleryController(@NonNull OsmandApplication app) {
		mediaProvider = new MediaProvider(app);
		imageLoader = new NetworkImageLoader(app, true);
	}

	public NetworkImageLoader getImageLoader() {
		return imageLoader;
	}

	@NonNull
	public MediaProvider getMediaProvider() {
		return mediaProvider;
	}

	@NonNull
	public List<GalleryItem> getOnlinePhotoItems() {
		List<GalleryItem> galleryItems = new ArrayList<>();
		if (currentMediaHolder != null) {
			galleryItems.addAll(currentMediaHolder.getOrderedGalleryItems());
		}
		return galleryItems;
	}

	@Nullable
	public GalleryItemsHolder getCurrentMediaHolder() {
		return currentMediaHolder;
	}

	public void setCurrentMediaHolder(@Nullable GalleryItemsHolder mediaHolder) {
		this.currentMediaHolder = mediaHolder;
	}

	public void clearHolder() {
		this.currentMediaHolder = null;
	}

	public boolean isCurrentHolderEquals(@NonNull LatLon latLon, @NonNull Map<String, String> params) {
		return currentMediaHolder != null && Algorithms.objectEquals(currentMediaHolder.getLatLon(), latLon)
				&& Algorithms.objectEquals(currentMediaHolder.getParams(), params);
	}

	public int getItemIndexBySourceUri(@NonNull String sourceUri) {
		List<GalleryItem> items = getOnlinePhotoItems();
		for (int i = 0; i < items.size(); i++) {
			GalleryItem item = items.get(i);
			if (item instanceof GalleryItem.Media media) {
				MediaItem mediaItem = media.getMediaItem();
				if (Algorithms.stringsEqual(sourceUri, mediaItem.getSourceUri())) {
					return i;
				}
			}
		}
		return 0;
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

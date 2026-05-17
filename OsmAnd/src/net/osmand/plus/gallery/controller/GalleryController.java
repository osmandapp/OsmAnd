package net.osmand.plus.gallery.controller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.gallery.model.GalleryItem;
import net.osmand.plus.gallery.online.OnlinePhotosHolder;
import net.osmand.plus.gallery.provider.MediaProvider;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.shared.media.domain.MediaItem;
import net.osmand.shared.media.domain.MediaType;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GalleryController implements IDialogController, GalleryMediaLoadStateProvider {

	public static final String PROCESS_ID = "gallery_context_controller";

	private OnlinePhotosHolder itemsHolder;

	private final MediaProvider mediaProvider;
	private final Set<String> failedMediaIds = new HashSet<>();

	public GalleryController(@NonNull OsmandApplication app) {
		mediaProvider = new MediaProvider(app);
	}

	@NonNull
	public MediaProvider getMediaProvider() {
		return mediaProvider;
	}

	@Nullable
	public OnlinePhotosHolder getItemsHolder() {
		return itemsHolder;
	}

	public void setItemsHolder(@Nullable OnlinePhotosHolder itemsHolder) {
		this.itemsHolder = itemsHolder;
	}

	public void clearHolder() {
		itemsHolder = null;
		failedMediaIds.clear();
	}

	public boolean isCurrentHolderEquals(@NonNull LatLon latLon, @NonNull Map<String, String> params) {
		return itemsHolder != null
				&& Algorithms.objectEquals(itemsHolder.getLatLon(), latLon)
				&& Algorithms.objectEquals(itemsHolder.getParams(), params);
	}

	@Override
	public void markMediaLoadFailed(@NonNull MediaItem mediaItem) {
		failedMediaIds.add(mediaItem.getId());
	}

	@Override
	public boolean isMediaLoadFailed(@NonNull MediaItem mediaItem) {
		return failedMediaIds.contains(mediaItem.getId());
	}

	public int getPhotoItemIndexById(@NonNull String id) {
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
		if (itemsHolder != null) {
			for (GalleryItem item : itemsHolder.getOrderedGalleryItems()) {
				if (item instanceof GalleryItem.Media media && isPhoto(media.getMediaItem())) {
					galleryItems.add(media);
				}
			}
		}
		return galleryItems;
	}

	private boolean isPhoto(@NonNull MediaItem mediaItem) {
		return mediaItem.getType() == MediaType.PHOTO;
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
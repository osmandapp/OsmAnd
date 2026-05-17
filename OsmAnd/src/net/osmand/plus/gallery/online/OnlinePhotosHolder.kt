package net.osmand.plus.gallery.online;

import static net.osmand.plus.gallery.online.OnlinePhotosGroup.ASTRONOMY;
import static net.osmand.plus.gallery.online.OnlinePhotosGroup.MAPILLARY;
import static net.osmand.plus.gallery.online.OnlinePhotosGroup.MAPILLARY_AMENITY;
import static net.osmand.plus.gallery.online.OnlinePhotosGroup.OTHER;
import static net.osmand.plus.gallery.online.OnlinePhotosGroup.WIKIDATA;
import static net.osmand.plus.gallery.online.OnlinePhotosGroup.WIKIMEDIA;

import androidx.annotation.NonNull;

import net.osmand.data.LatLon;
import net.osmand.plus.gallery.model.GalleryItem;
import net.osmand.shared.media.domain.MediaItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OnlinePhotosHolder {

	private final LatLon latLon;
	private final Map<String, String> params;
	private final Map<OnlinePhotosGroup, LinkedHashMap<String, GalleryItem>> itemsByGroup = new LinkedHashMap<>();

	public OnlinePhotosHolder(@NonNull LatLon latLon, @NonNull Map<String, String> params) {
		this.latLon = latLon;
		this.params = params;
	}

	@NonNull
	public LatLon getLatLon() {
		return latLon;
	}

	@NonNull
	public Map<String, String> getParams() {
		return params;
	}

	@NonNull
	public List<GalleryItem> getOrderedGalleryItems() {
		return getGalleryItemsWithGroups(MAPILLARY_AMENITY, WIKIDATA, WIKIMEDIA, OTHER, ASTRONOMY);
	}

	@NonNull
	public List<GalleryItem> getMapillaryGalleryItems() {
		return getGalleryItemsWithGroups(MAPILLARY);
	}

	@NonNull
	public List<GalleryItem> getAstronomyGalleryItems() {
		return getGalleryItemsWithGroups(ASTRONOMY);
	}

	@NonNull
	private List<GalleryItem> getGalleryItemsWithGroups(@NonNull OnlinePhotosGroup... groups) {
		List<GalleryItem> list = new ArrayList<>();
		for (OnlinePhotosGroup group : groups) {
			LinkedHashMap<String, GalleryItem> items = itemsByGroup.get(group);
			if (items != null && !items.isEmpty()) {
				list.addAll(items.values());
			}
		}
		return list;
	}

	public void addMediaItem(@NonNull OnlinePhotosGroup group, @NonNull MediaItem mediaItem) {
		addMediaItem(group, mediaItem, false);
	}

	public void addMediaItem(@NonNull OnlinePhotosGroup group, @NonNull MediaItem mediaItem, boolean showLoadingProgress) {
		addGalleryItem(group, mediaItem.getId(), new GalleryItem.Media(mediaItem, showLoadingProgress));
	}

	public void addGalleryItem(@NonNull OnlinePhotosGroup group, @NonNull String key, @NonNull GalleryItem item) {
		LinkedHashMap<String, GalleryItem> items = itemsByGroup.computeIfAbsent(group, ignored -> new LinkedHashMap<>());
		items.put(key, item);
	}
}
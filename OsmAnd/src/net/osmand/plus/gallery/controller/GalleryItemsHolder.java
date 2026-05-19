package net.osmand.plus.gallery.controller;

import static net.osmand.plus.gallery.model.GalleryMediaGroup.ASTRONOMY;
import static net.osmand.plus.gallery.model.GalleryMediaGroup.MAPILLARY;
import static net.osmand.plus.gallery.model.GalleryMediaGroup.MAPILLARY_AMENITY;
import static net.osmand.plus.gallery.model.GalleryMediaGroup.OTHER;
import static net.osmand.plus.gallery.model.GalleryMediaGroup.WIKIDATA;
import static net.osmand.plus.gallery.model.GalleryMediaGroup.WIKIMEDIA;

import androidx.annotation.NonNull;

import net.osmand.data.LatLon;
import net.osmand.plus.gallery.model.GalleryItem;
import net.osmand.plus.gallery.model.GalleryMediaGroup;
import net.osmand.shared.media.domain.MediaItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GalleryItemsHolder {

	private final LatLon latLon;
	private final Map<String, String> params;
	private final Map<GalleryMediaGroup, LinkedHashMap<String, GalleryItem>> itemsByGroup = new LinkedHashMap<>();

	public GalleryItemsHolder(@NonNull LatLon latLon, @NonNull Map<String, String> params) {
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
	private List<GalleryItem> getGalleryItemsWithGroups(@NonNull GalleryMediaGroup... groups) {
		List<GalleryItem> list = new ArrayList<>();
		for (GalleryMediaGroup group : groups) {
			LinkedHashMap<String, GalleryItem> items = itemsByGroup.get(group);
			if (items != null && !items.isEmpty()) {
				list.addAll(items.values());
			}
		}
		return list;
	}

	public void addMediaItem(@NonNull GalleryMediaGroup group, @NonNull MediaItem mediaItem) {
		addMediaItem(group, mediaItem, false);
	}

	public void addMediaItem(@NonNull GalleryMediaGroup group, @NonNull MediaItem mediaItem, boolean showLoadingProgress) {
		addGalleryItem(group, mediaItem.getId(), new GalleryItem.Media(mediaItem, showLoadingProgress));
	}

	public void addGalleryItem(@NonNull GalleryMediaGroup group, @NonNull String key, @NonNull GalleryItem item) {
		LinkedHashMap<String, GalleryItem> items = itemsByGroup.computeIfAbsent(group, ignored -> new LinkedHashMap<>());
		items.put(key, item);
	}
}
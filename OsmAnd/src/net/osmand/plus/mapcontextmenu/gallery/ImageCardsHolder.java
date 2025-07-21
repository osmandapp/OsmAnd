package net.osmand.plus.mapcontextmenu.gallery;

import static net.osmand.plus.mapcontextmenu.gallery.ImageCardType.MAPILLARY;
import static net.osmand.plus.mapcontextmenu.gallery.ImageCardType.MAPILLARY_AMENITY;
import static net.osmand.plus.mapcontextmenu.gallery.ImageCardType.OTHER;
import static net.osmand.plus.mapcontextmenu.gallery.ImageCardType.WIKIDATA;
import static net.osmand.plus.mapcontextmenu.gallery.ImageCardType.WIKIMEDIA;

import androidx.annotation.NonNull;

import net.osmand.data.LatLon;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.wikipedia.WikiImageCard;
import net.osmand.shared.wiki.WikiMetadata;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ImageCardsHolder {

	private final LatLon latLon;
	private final Map<String, String> params;
	private final Map<ImageCardType, LinkedHashMap<String, ImageCard>> cardsByType = new LinkedHashMap<>();

	public ImageCardsHolder(@NonNull LatLon latLon, @NonNull Map<String, String> params) {
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
	public List<ImageCard> getOrderedCards() {
		return getCardsWithTypes(MAPILLARY_AMENITY, WIKIDATA, WIKIMEDIA, OTHER);
	}

	@NonNull
	public List<ImageCard> getMapillaryCards() {
		return getCardsWithTypes(MAPILLARY);
	}

	@NonNull
	private List<ImageCard> getCardsWithTypes(@NonNull ImageCardType... types) {
		List<ImageCard> list = new ArrayList<>();
		for (ImageCardType type : types) {
			LinkedHashMap<String, ImageCard> images = cardsByType.get(type);
			if (images != null) {
				List<ImageCard> cards = new ArrayList<>(images.values());
				if (!Algorithms.isEmpty(cards)) {
					list.addAll(cards);
				}
			}
		}
		return list;
	}

	public void addCard(@NonNull ImageCardType type, @NonNull ImageCard card) {
		LinkedHashMap<String, ImageCard> cards = cardsByType.get(type);
		String key;
		if (card instanceof WikiImageCard wikiImageCard) {
			key = wikiImageCard.getWikiImage().getWikiMediaTag();
		} else {
			key = card.getImageUrl();
		}
		if (cards != null) {
			cards.put(key, card);
		} else {
			cards = new LinkedHashMap<>();
			cards.put(key, card);
			cardsByType.put(type, cards);
		}
	}

	public void updateWikiMetadata(@NonNull Map<String, Map<String, String>> metadataMap) {
		LinkedHashMap<String, ImageCard> wikiImages = cardsByType.get(WIKIMEDIA);
		if (wikiImages == null) {
			return;
		}
		for (String key : metadataMap.keySet()) {
			ImageCard card = wikiImages.get(key);
			if (card instanceof WikiImageCard wikiImageCard) {
				WikiMetadata.Metadata metadata = wikiImageCard.getWikiImage().getMetadata();
				WikiMetadata.INSTANCE.updateMetadata(metadataMap, metadata);
				wikiImageCard.setMetaDataDownloaded(true);
			}
		}
	}
}
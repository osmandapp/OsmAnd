package net.osmand.plus.mapcontextmenu.gallery;

import static net.osmand.plus.mapcontextmenu.gallery.ImageCardType.MAPILLARY;
import static net.osmand.plus.mapcontextmenu.gallery.ImageCardType.MAPILLARY_AMENITY;
import static net.osmand.plus.mapcontextmenu.gallery.ImageCardType.OTHER;
import static net.osmand.plus.mapcontextmenu.gallery.ImageCardType.WIKIDATA;
import static net.osmand.plus.mapcontextmenu.gallery.ImageCardType.WIKIMEDIA;

import androidx.annotation.NonNull;

import net.osmand.data.LatLon;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ImageCardsHolder {

	private final LatLon latLon;
	private final Map<String, String> params;
	private final Map<ImageCardType, List<ImageCard>> cardsByType = new LinkedHashMap<>();

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
		return getCardsWithTypes(OTHER, MAPILLARY_AMENITY, WIKIDATA, WIKIMEDIA);
	}

	@NonNull
	public List<ImageCard> getMapillaryCards() {
		return getCardsWithTypes(MAPILLARY);
	}

	@NonNull
	private List<ImageCard> getCardsWithTypes(@NonNull ImageCardType... types) {
		List<ImageCard> list = new ArrayList<>();
		for (ImageCardType type : types) {
			List<ImageCard> cards = cardsByType.get(type);
			if (!Algorithms.isEmpty(cards)) {
				list.addAll(cards);
			}
		}
		return list;
	}

	public void addCard(@NonNull ImageCardType type, @NonNull ImageCard card) {
		List<ImageCard> cards = cardsByType.get(type);
		if (cards != null) {
			cards.add(card);
		} else {
			cards = new ArrayList<>();
			cards.add(card);
			cardsByType.put(type, cards);
		}
	}
}
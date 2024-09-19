package net.osmand.plus.mapcontextmenu.gallery;

import static net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.ImageCardType.MAPILLARY;
import static net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.ImageCardType.MAPILLARY_AMENITY;
import static net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.ImageCardType.OPR;
import static net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.ImageCardType.OTHER;
import static net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.ImageCardType.WIKIDATA;
import static net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.ImageCardType.WIKIMEDIA;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageCardsHolder {

	private final Map<ImageCard.ImageCardType, List<ImageCard>> cardsByType = new HashMap<>();

	public boolean add(@NonNull ImageCard.ImageCardType type, @Nullable ImageCard image) {
		if (image != null) {
			List<ImageCard> list = cardsByType.get(type);
			if (list != null) {
				list.add(image);
			} else {
				list = new ArrayList<>();
				list.add(image);
				cardsByType.put(type, list);
			}
			return true;
		}
		return false;
	}

	@NonNull
	public List<ImageCard> getOrderedList() {
		return getCardList(OTHER, MAPILLARY_AMENITY, WIKIDATA, WIKIMEDIA, OPR);
	}

	@NonNull
	public List<ImageCard> getMapillaryList() {
		return getCardList(MAPILLARY);
	}

	private List<ImageCard> getCardList(ImageCard.ImageCardType... types) {
		List<ImageCard> result = new ArrayList<>();
		for (ImageCard.ImageCardType cardType : types) {
			List<ImageCard> typeCards = cardsByType.get(cardType);
			if (!Algorithms.isEmpty(typeCards)) {
				result.addAll(cardsByType.get(cardType));
			}
		}
		return result;
	}

	@Nullable
	public ImageCard getFirstItem() {
		List<ImageCard> result = getOrderedList();
		if (!Algorithms.isEmpty(result)) {
			return result.get(0);
		}
		return null;
	}

}
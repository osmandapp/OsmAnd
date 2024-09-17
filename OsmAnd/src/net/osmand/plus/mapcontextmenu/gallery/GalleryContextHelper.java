package net.osmand.plus.mapcontextmenu.gallery;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;

import java.util.List;

public class GalleryContextHelper {

	private List<ImageCard> onlinePhotoCards;


	public GalleryContextHelper() {
	}

	public void setOnlinePhotoCards(List<ImageCard> onlinePhotoCards) {
		this.onlinePhotoCards = onlinePhotoCards;
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

	public List<ImageCard> getOnlinePhotoCards() {
		return onlinePhotoCards;
	}

	public int getImageCardFromUrl(@NonNull String imageUrl) {
		for(int i = 0; i < getOnlinePhotoCards().size(); i++){
			ImageCard card = getOnlinePhotoCards().get(i);
			if(imageUrl.equals(card.getImageUrl())){
				return i;
			}
		}
		return 0;
	}
}

package net.osmand.plus.wikipedia;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.shared.wiki.WikiImage;

public class WikiImageCard extends ImageCard {

	private final WikiImage wikiImage;

	public WikiImageCard(@NonNull MapActivity mapActivity, @NonNull WikiImage wikiImage) {
		super(mapActivity, null);
		this.wikiImage = wikiImage;
		if (topIconId == 0) {
			topIconId = R.drawable.ic_logo_wikimedia;
		}

		this.imageUrl = wikiImage.getImageStubUrl();
		this.title = wikiImage.getImageName();
		this.url = this.imageUrl;
		this.imageHiresUrl = wikiImage.getImageHiResUrl();
	}

	@NonNull
	public WikiImage getWikiImage() {
		return wikiImage;
	}
}

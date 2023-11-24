package net.osmand.plus.wikimedia;

import android.view.View;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.util.Algorithms;
import net.osmand.wiki.WikiImage;

public class WikiImageCard extends ImageCard {

	public WikiImageCard(MapActivity mapActivity,
	                     WikiImage wikiImage) {
		super(mapActivity, null);

		if (topIconId == 0) {
			topIconId = R.drawable.ic_logo_wikimedia;
		}

		this.imageUrl = wikiImage.getImageStubUrl();
		this.title = wikiImage.getImageName();
		this.url = this.imageUrl;

		View.OnClickListener onClickListener = v -> openUrl(getMapActivity(), getMyApplication(),
				getTitle(), wikiImage.getUrlWithCommonAttributions(), false, false);

		if (!Algorithms.isEmpty(buttonText)) {
			this.onButtonClickListener = onClickListener;
		} else {
			this.onClickListener = onClickListener;
		}
	}
}

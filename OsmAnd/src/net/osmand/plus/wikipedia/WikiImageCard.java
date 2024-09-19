package net.osmand.plus.wikipedia;

import android.view.View;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.util.Algorithms;
import net.osmand.wiki.WikiImage;

public class WikiImageCard extends ImageCard {
	public WikiImage wikiImage;

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

		View.OnClickListener onClickListener = v -> openUrl(getMapActivity(), getMyApplication(),
				getTitle(), wikiImage.getUrlWithCommonAttributions(), false, false);

		if (!Algorithms.isEmpty(buttonText)) {
			this.onButtonClickListener = onClickListener;
		} else {
			this.onClickListener = onClickListener;
		}
	}
}

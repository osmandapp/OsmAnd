package net.osmand.plus.wikimedia;

import android.view.View;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.util.Algorithms;

import org.json.JSONObject;

public class WikiImageCard extends ImageCard {

	public WikiImageCard(final MapActivity mapActivity, final JSONObject imageObject,
	                     final WikiImage wikiImage) {
		super(mapActivity, imageObject);

		if (topIconId == 0) {
			topIconId = R.drawable.ic_logo_wikimedia;
		}

		this.imageUrl = wikiImage.getImageStubUrl();
		this.url = this.imageUrl;

		View.OnClickListener onClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openUrl(getMapActivity(), getMyApplication(), getTitle(), wikiImage.getImageHiResUrl(),
						isExternalLink() || Algorithms.isEmpty(getImageHiresUrl()),
						!Algorithms.isEmpty(getImageHiresUrl()));
			}
		};

		if (!Algorithms.isEmpty(buttonText)) {
			this.onButtonClickListener = onClickListener;
		} else {
			this.onClickListener = onClickListener;
		}
	}

}

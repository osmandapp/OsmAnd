package net.osmand.plus.mapcontextmenu.builders.cards;

import android.view.View;
import android.view.View.OnClickListener;

import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;

import org.json.JSONObject;

public class UrlImageCard extends ImageCard {

	public UrlImageCard(MapActivity mapActivity, JSONObject imageObject) {
		super(mapActivity, imageObject);

		final String url;
		final boolean hasImageUrl;
		if (Algorithms.isEmpty(getImageHiresUrl())) {
			url = getUrl();
			hasImageUrl = false;
		} else {
			url = getImageHiresUrl();
			hasImageUrl = true;
		}

		if (!Algorithms.isEmpty(url)) {
			OnClickListener onClickListener = new OnClickListener() {
				@Override
				public void onClick(View v) {
					openUrl(getMapActivity(), getMyApplication(), getTitle(), url, isExternalLink(), hasImageUrl);
				}
			};
			if (!Algorithms.isEmpty(buttonText)) {
				this.onButtonClickListener = onClickListener;
			} else {
				this.onClickListener = onClickListener;
			}
		}
	}
}

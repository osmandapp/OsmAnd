package net.osmand.plus.mapcontextmenu.builders.cards;

import android.view.View;
import android.view.View.OnClickListener;

import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;

import org.json.JSONObject;

public class UrlImageCard extends ImageCard {

	public UrlImageCard(MapActivity mapActivity, JSONObject imageObject) {
		super(mapActivity, imageObject);

		if (!Algorithms.isEmpty(getSuitableUrl())) {
			OnClickListener onClickListener = new OnClickListener() {
				@Override
				public void onClick(View v) {
					openUrl(getMapActivity(), getMyApplication(), getTitle(), getSuitableUrl(),
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

	private String getSuitableUrl() {
		String url;
		if (Algorithms.isEmpty(getImageHiresUrl())) {
			url = getUrl();
		} else {
			url = getImageHiresUrl();
		}
		return url;
	}
}

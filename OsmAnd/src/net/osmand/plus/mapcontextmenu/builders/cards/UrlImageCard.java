package net.osmand.plus.mapcontextmenu.builders.cards;

import android.view.View;
import android.view.View.OnClickListener;

import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;

import org.json.JSONObject;

public class UrlImageCard extends ImageCard {

	public UrlImageCard(MapActivity mapActivity, JSONObject imageObject) {
		super(mapActivity, imageObject);
		if (!Algorithms.isEmpty(getUrl())) {
			OnClickListener onClickListener = new OnClickListener() {
				@Override
				public void onClick(View v) {
					openUrl(getMapActivity(), getMyApplication(), "", getUrl(), isExternalLink());
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

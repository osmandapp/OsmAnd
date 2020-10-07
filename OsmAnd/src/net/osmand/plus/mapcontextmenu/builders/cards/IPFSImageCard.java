package net.osmand.plus.mapcontextmenu.builders.cards;

import android.view.View;
import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;
import org.json.JSONException;
import org.json.JSONObject;

public class IPFSImageCard extends ImageCard {
	private static final String BASE_URL = "https://test.openplacereviews.org/api/ipfs/image-ipfs?cid=";

	public IPFSImageCard(MapActivity mapActivity, JSONObject imageObject) {
		super(mapActivity, imageObject);
		try {
			this.url = BASE_URL + imageObject.get("cid");
			this.imageHiresUrl = BASE_URL + imageObject.get("cid");
			this.imageUrl = BASE_URL + imageObject.get("cid");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		if (!Algorithms.isEmpty(getSuitableUrl())) {
			View.OnClickListener onClickListener = new View.OnClickListener() {
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
		final String url;
		if (Algorithms.isEmpty(getImageHiresUrl())) {
			url = getUrl();
		} else {
			url = getImageHiresUrl();
		}
		return url;
	}
}
package net.osmand.plus.mapcontextmenu.builders.cards;

import android.view.View;
import net.osmand.AndroidNetworkUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;
import org.apache.commons.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;

public class IPFSImageCard extends ImageCard {
	private static final String BASE_URL = "https://test.openplacereviews.org/api/ipfs/image-ipfs?cid=";
	private static final Log LOG = PlatformUtil.getLog(IPFSImageCard.class);

	public IPFSImageCard(MapActivity mapActivity, JSONObject imageObject) {
		super(mapActivity, imageObject);
		String cid = "";
		try {
			cid = (String) imageObject.get("cid");
		} catch (JSONException e) {
			LOG.error(e);
		}
		this.url = BASE_URL + cid;
		this.imageHiresUrl = BASE_URL + cid;
		this.imageUrl = BASE_URL + cid;
		if (!Algorithms.isEmpty(getUrl())) {
			View.OnClickListener onClickListener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openUrl(getMapActivity(), getMyApplication(), getTitle(), getUrl(),
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
}

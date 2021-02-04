package net.osmand.plus.openplacereviews;


import android.view.View;

import androidx.core.content.ContextCompat;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;

public class IPFSImageCard extends ImageCard {
	private static final Log LOG = PlatformUtil.getLog(IPFSImageCard.class);

	public IPFSImageCard(MapActivity mapActivity, JSONObject imageObject) {
		super(mapActivity, imageObject);
		try {
			String calcImageUrl = mapActivity.getString(R.string.opr_base_url) + "api/ipfs/image?";
			calcImageUrl += "cid=" + (String) imageObject.getString("cid");
			calcImageUrl += "&hash=" + (String) imageObject.getString("hash");
			calcImageUrl += "&ext=" + (String) imageObject.getString("extension");
			url = calcImageUrl;
			imageHiresUrl = url;
			imageUrl = url;
		} catch (JSONException e) {
			LOG.error(e);
		}
		icon = ContextCompat.getDrawable(getMyApplication(), R.drawable.ic_logo_openplacereview);
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
				onButtonClickListener = onClickListener;
			} else {
				this.onClickListener = onClickListener;
			}
		}
	}
}

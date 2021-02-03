package net.osmand.plus.mapcontextmenu.builders.cards;


import android.view.View;
import androidx.core.content.ContextCompat;
import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;
import org.apache.commons.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;

public class IPFSImageCard extends ImageCard {
	private static final Log LOG = PlatformUtil.getLog(IPFSImageCard.class);

	public IPFSImageCard(MapActivity mapActivity, JSONObject imageObject) {
		super(mapActivity, imageObject);
		url = mapActivity.getString(R.string.opr_base_url) + "api/ipfs/image?";
		try {
			url += "cid=" + (String) imageObject.getString("cid");
			url += "&hash=" + (String) imageObject.getString("hash");
			url += "&ext=" + (String) imageObject.getString("extension");
		} catch (JSONException e) {
			LOG.error(e);
		}
		imageHiresUrl = url;
		imageUrl = url;
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

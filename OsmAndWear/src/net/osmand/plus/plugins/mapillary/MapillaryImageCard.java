package net.osmand.plus.plugins.mapillary;

import android.view.View.OnClickListener;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.util.Algorithms;

import org.json.JSONObject;

public class MapillaryImageCard extends ImageCard {

	public MapillaryImageCard(MapActivity mapActivity, JSONObject imageObject) {
		super(mapActivity, imageObject);
		if (topIconId == 0) {
			topIconId = R.drawable.ic_logo_mapillary;
		}
		OnClickListener onClickListener = v -> {
			MapActivity activity = getMapActivity();
			activity.getContextMenu().close();
			MapillaryImageDialog.show(activity, getKey(), getImageHiresUrl(), getUrl(), getLocation(),
					getCa(), activity.getString(R.string.mapillary), null, true);
		};
		if (!Algorithms.isEmpty(buttonText)) {
			this.onButtonClickListener = onClickListener;
		} else {
			this.onClickListener = onClickListener;
		}
	}
}

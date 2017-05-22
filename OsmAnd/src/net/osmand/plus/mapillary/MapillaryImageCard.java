package net.osmand.plus.mapillary;

import android.view.View;
import android.view.View.OnClickListener;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

public class MapillaryImageCard extends ImageCard {

	public MapillaryImageCard(final MapActivity mapActivity, final JSONObject imageObject) {
		super(mapActivity, imageObject);
		if (topIconId == 0) {
			topIconId = R.drawable.ic_logo_mapillary;
		}
		OnClickListener onClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				getMapActivity().getContextMenu().hideMenues();
				MapillaryImageDialog.show(getMapActivity(), getKey(), getImageHiresUrl(), getUrl(), getLocation(),
						getCa(), getMyApplication().getString(R.string.mapillary), null);
			}
		};
		if (!Algorithms.isEmpty(buttonText)) {
			this.onButtonClickListener = onClickListener;
		} else {
			this.onClickListener = onClickListener;
		}
	}
}

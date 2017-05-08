package net.osmand.plus.mapillary;

import android.view.View;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;

import org.json.JSONObject;

public class MapillaryImageCard extends ImageCard {

	public MapillaryImageCard(final MapActivity mapActivity, final JSONObject imageObject) {
		super(mapActivity, imageObject);
		this.icon = getMyApplication().getIconsCache().getIcon(R.drawable.ic_logo_mapillary);
		this.onClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getMapActivity().getContextMenu().hideMenues();
				MapillaryImageDialog.show(getMapActivity(), getKey(), getImageHiresUrl(), getUrl(), getLocation(),
						getCa(), getMyApplication().getString(R.string.mapillary), null);
			}
		};
	}
}

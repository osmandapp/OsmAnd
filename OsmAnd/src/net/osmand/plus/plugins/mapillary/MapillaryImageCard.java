package net.osmand.plus.plugins.mapillary;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;

import org.json.JSONObject;

public class MapillaryImageCard extends ImageCard {

	public MapillaryImageCard(MapActivity mapActivity, JSONObject imageObject) {
		super(mapActivity, imageObject);
		if (topIconId == 0) {
			topIconId = R.drawable.ic_logo_mapillary;
		}
	}
}

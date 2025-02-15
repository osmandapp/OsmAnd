package net.osmand.plus.plugins.mapillary;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;

import org.json.JSONObject;

public class MapillaryContributeCard extends ImageCard {

	public MapillaryContributeCard(MapActivity mapActivity, JSONObject imageObject) {
		super(mapActivity, imageObject);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.context_menu_card_add_mapillary_images;
	}
}
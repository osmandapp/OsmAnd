package net.osmand.plus.mapillary;

import android.view.View;

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

	@Override
	public void update() {
		if (view != null) {
			view.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					MapillaryPlugin.openMapillary(getMapActivity(), null);
				}
			});
		}
	}
}
package net.osmand.plus.plugins.mapillary;

import android.view.View;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
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
			boolean night = getMyApplication().getDaynightHelper().isNightModeForMapControls();
			MapActivity ctx = getMapActivity();
			AndroidUtils.setBackgroundColor(ctx, view, ColorUtilities.getListBgColorId(night));
			AndroidUtils.setTextPrimaryColor(ctx, view.findViewById(R.id.title), night);
			AndroidUtils.setBackground(ctx, view.findViewById(R.id.card_background), night,
					R.drawable.context_menu_card_light, R.drawable.context_menu_card_dark);
			view.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					MapillaryPlugin.openMapillary(getMapActivity(), null);
				}
			});
		}
	}
}
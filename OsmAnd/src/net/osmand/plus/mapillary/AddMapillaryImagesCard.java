package net.osmand.plus.mapillary;

import android.view.View;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.mapcontextmenu.builders.cards.AbstractCard;

public class AddMapillaryImagesCard extends AbstractCard {

	public AddMapillaryImagesCard(OsmandApplication app) {
		super(app);
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
					MapillaryPlugin.openMapillary(getMyApplication());
				}
			});
		}
	}
}
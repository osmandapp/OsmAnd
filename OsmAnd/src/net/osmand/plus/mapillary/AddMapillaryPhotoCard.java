package net.osmand.plus.mapillary;

import android.view.View;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.mapcontextmenu.builders.cards.AbstractCard;

class AddMapillaryPhotoCard extends AbstractCard {

	AddMapillaryPhotoCard(OsmandApplication app) {
		super(app);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.mapillary_context_menu_action;
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

package net.osmand.plus.mapcontextmenu.builders.cards;

import android.view.View;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.mapillary.MapillaryPlugin;

public class NoImagesCard extends AbstractCard {

	public NoImagesCard(OsmandApplication app) {
		super(app);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.context_menu_card_no_images;
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

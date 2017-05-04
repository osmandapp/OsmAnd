package net.osmand.plus.mapillary;

import android.view.View;

import net.osmand.plus.R;
import net.osmand.plus.mapcontextmenu.builders.cards.AbstractCard;

public class AddMapillaryPhotoCard extends AbstractCard {

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
					// todo open mapillary
				}
			});
		}
	}
}

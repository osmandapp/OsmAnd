package net.osmand.plus.mapcontextmenu.builders.cards;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class ProgressCard extends AbstractCard {

	public ProgressCard(MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.context_menu_card_progress;
	}

	@Override
	public void update() {
		OsmandApplication app = getMyApplication();
		boolean night = app.getDaynightHelper().isNightModeForMapControls();
		AndroidUtils.setBackgroundColor(app, view, ColorUtilities.getListBgColorId(night));
	}
}

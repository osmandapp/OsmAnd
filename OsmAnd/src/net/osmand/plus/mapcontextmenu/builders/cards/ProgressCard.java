package net.osmand.plus.mapcontextmenu.builders.cards;

import net.osmand.AndroidUtils;
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
		boolean night = getMyApplication().getDaynightHelper().isNightModeForMapControls();
		AndroidUtils.setBackgroundColor(getMapActivity(), view, night, R.color.bg_color_light, R.color.bg_color_dark);
	}
}

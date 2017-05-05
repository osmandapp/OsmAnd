package net.osmand.plus.mapcontextmenu.builders.cards;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public class ProgressCard extends AbstractCard {

	public ProgressCard(OsmandApplication app) {
		super(app);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.context_menu_card_progress;
	}

	@Override
	public void update() {
	}
}

package net.osmand.plus.search.listitems;

import net.osmand.plus.OsmandApplication;

public class QuickSearchCardDividerListItem extends QuickSearchListItem {

	public QuickSearchCardDividerListItem(OsmandApplication app) {
		super(app, null);
	}

	public QuickSearchListItemType getType() {
		return QuickSearchListItemType.CARD_DIVIDER;
	}
}
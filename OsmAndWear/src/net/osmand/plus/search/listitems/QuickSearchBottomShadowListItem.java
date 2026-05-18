package net.osmand.plus.search.listitems;

import net.osmand.plus.OsmandApplication;

public class QuickSearchBottomShadowListItem extends QuickSearchListItem {

	public QuickSearchBottomShadowListItem(OsmandApplication app) {
		super(app, null);
	}

	public QuickSearchListItemType getType() {
		return QuickSearchListItemType.BOTTOM_SHADOW;
	}
}

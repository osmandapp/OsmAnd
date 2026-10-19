package net.osmand.plus.search.listitems;

import net.osmand.plus.OsmandApplication;

public class QuickSearchTopShadowListItem extends QuickSearchListItem {

	public QuickSearchTopShadowListItem(OsmandApplication app) {
		super(app, null);
	}

	public QuickSearchListItemType getType() {
		return QuickSearchListItemType.TOP_SHADOW;
	}
}

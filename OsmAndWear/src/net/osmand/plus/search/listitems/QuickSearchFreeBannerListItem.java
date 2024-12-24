package net.osmand.plus.search.listitems;

import net.osmand.plus.OsmandApplication;

public class QuickSearchFreeBannerListItem extends QuickSearchListItem {

	public QuickSearchFreeBannerListItem(OsmandApplication app) {
		super(app, null);
	}

	public QuickSearchListItemType getType() {
		return QuickSearchListItemType.FREE_VERSION_BANNER;
	}
}

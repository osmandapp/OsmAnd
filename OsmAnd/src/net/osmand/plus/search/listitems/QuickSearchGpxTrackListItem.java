package net.osmand.plus.search.listitems;

import net.osmand.plus.OsmandApplication;
import net.osmand.search.core.SearchResult;

public class QuickSearchGpxTrackListItem extends QuickSearchListItem {

	public QuickSearchGpxTrackListItem(OsmandApplication app, SearchResult searchResult) {
		super(app, searchResult);
	}

	@Override
	public QuickSearchListItemType getType() {
		return QuickSearchListItemType.GPX_TRACK;
	}
}

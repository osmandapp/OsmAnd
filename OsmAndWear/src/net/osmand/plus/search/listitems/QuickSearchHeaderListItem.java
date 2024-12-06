package net.osmand.plus.search.listitems;

import net.osmand.plus.OsmandApplication;

public class QuickSearchHeaderListItem extends QuickSearchListItem {

	private final String title;
	private final boolean showTopDivider;

	public QuickSearchHeaderListItem(OsmandApplication app, String title, boolean showTopDivider) {
		super(app, null);
		this.title = title;
		this.showTopDivider = showTopDivider;
	}

	public QuickSearchListItemType getType() {
		return QuickSearchListItemType.HEADER;
	}

	public String getTitle() {
		return title;
	}

	public boolean isShowTopDivider() {
		return showTopDivider;
	}

	@Override
	public String getName() {
		return title;
	}
}

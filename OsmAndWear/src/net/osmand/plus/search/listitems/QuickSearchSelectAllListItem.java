package net.osmand.plus.search.listitems;

import android.view.View;

import net.osmand.plus.OsmandApplication;

public class QuickSearchSelectAllListItem extends QuickSearchListItem {

	private final String name;
	private final View.OnClickListener onClickListener;

	public QuickSearchSelectAllListItem(OsmandApplication app, String name, View.OnClickListener onClickListener) {
		super(app, null);
		this.name = name;
		this.onClickListener = onClickListener;
	}

	public QuickSearchListItemType getType() {
		return QuickSearchListItemType.SELECT_ALL;
	}

	@Override
	public String getName() {
		return name;
	}

	public View.OnClickListener getOnClickListener() {
		return onClickListener;
	}
}

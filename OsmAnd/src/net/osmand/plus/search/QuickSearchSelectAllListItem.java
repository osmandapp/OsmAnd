package net.osmand.plus.search;

import android.view.View;

import net.osmand.plus.OsmandApplication;

public class QuickSearchSelectAllListItem extends QuickSearchListItem {

	private String name;
	private View.OnClickListener onClickListener;

	public QuickSearchSelectAllListItem(OsmandApplication app, String name, View.OnClickListener onClickListener) {
		super(app, null);
		this.name = name;
		this.onClickListener = onClickListener;
	}

	@Override
	public String getName() {
		return name;
	}

	public View.OnClickListener getOnClickListener() {
		return onClickListener;
	}
}

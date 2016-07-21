package net.osmand.plus.search;

import android.view.View.OnClickListener;

import net.osmand.plus.OsmandApplication;

public class QuickSearchMoreListItem extends QuickSearchListItem {

	private String name;
	private OnClickListener onClickListener;

	public QuickSearchMoreListItem(OsmandApplication app, String name, OnClickListener onClickListener) {
		super(app, null);
		this.name = name;
		this.onClickListener = onClickListener;
	}

	@Override
	public String getName() {
		return name;
	}

	public OnClickListener getOnClickListener() {
		return onClickListener;
	}
}

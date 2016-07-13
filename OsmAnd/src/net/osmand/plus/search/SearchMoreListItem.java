package net.osmand.plus.search;

import android.view.View.OnClickListener;

import net.osmand.plus.OsmandApplication;

public class SearchMoreListItem extends SearchListItem {

	private String name;
	private OnClickListener onClickListener;

	public SearchMoreListItem(OsmandApplication app, String name, OnClickListener onClickListener) {
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

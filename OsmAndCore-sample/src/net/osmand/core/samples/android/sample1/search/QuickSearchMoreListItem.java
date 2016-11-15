package net.osmand.core.samples.android.sample1.search;

import android.view.View.OnClickListener;

import net.osmand.core.samples.android.sample1.SampleApplication;

public class QuickSearchMoreListItem extends QuickSearchListItem {

	private String name;
	private OnClickListener onClickListener;

	public QuickSearchMoreListItem(SampleApplication app, String name, OnClickListener onClickListener) {
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

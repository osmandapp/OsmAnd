package net.osmand.plus.base.bottomsheetmenu.simpleitems;

import android.content.Context;

import androidx.annotation.ColorRes;

public class SimpleDividerItem extends DividerItem {

	public SimpleDividerItem(Context context) {
		super(context);
	}

	public SimpleDividerItem(Context context, @ColorRes int colorId) {
		super(context, colorId);
	}

	public SimpleDividerItem(Context context, @ColorRes int colorId, int position) {
		super(context, colorId, position);
	}

	@Override
	protected int getTopMargin(Context context) {
		return 0;
	}

	@Override
	protected int getBottomMargin(Context context) {
		return 0;
	}
}
package net.osmand.plus.base.bottomsheetmenu.simpleitems;

import android.content.Context;

import androidx.annotation.ColorRes;

public class TitleDividerItem extends DividerItem {

	public TitleDividerItem(Context context) {
		super(context);
	}

	public TitleDividerItem(Context context, @ColorRes int colorId) {
		super(context, colorId);
	}

	public TitleDividerItem(Context context, @ColorRes int colorId, int position) {
		super(context, colorId, position);
	}

	@Override
	protected int getTopMargin(Context context) {
		return 0;
	}
}

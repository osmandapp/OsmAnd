package net.osmand.plus.base.bottomsheetmenu.simpleitems;

import android.content.Context;

import androidx.annotation.ColorRes;

public class SubtitleDividerItem extends DividerItem {

	public SubtitleDividerItem(Context context) {
		super(context);
	}

	public SubtitleDividerItem(Context context, @ColorRes int colorId) {
		super(context, colorId);
	}

	public SubtitleDividerItem(Context context, @ColorRes int colorId, int position) {
		super(context, colorId, position);
	}

	@Override
	protected int getBottomMargin(Context context) {
		return 0;
	}
}

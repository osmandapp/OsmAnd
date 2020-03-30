package net.osmand.plus.base.bottomsheetmenu.simpleitems;

import android.content.Context;

import androidx.annotation.ColorRes;

import net.osmand.plus.R;

public class DividerHalfItem extends DividerItem {

	public DividerHalfItem(Context context) {
		super(context);
	}

	public DividerHalfItem(Context context, @ColorRes int colorId) {
		super(context, colorId);
	}

	public DividerHalfItem(Context context, @ColorRes int colorId, int position) {
		super(context, colorId, position);
	}

	@Override
	protected int getTopMargin(Context context) {
		return context.getResources().getDimensionPixelSize(R.dimen.bottom_sheet_divider_margin_top);
	}

	@Override
	protected int getStartMargin(Context context) {
		return context.getResources().getDimensionPixelSize(R.dimen.bottom_sheet_divider_margin_start);
	}
}

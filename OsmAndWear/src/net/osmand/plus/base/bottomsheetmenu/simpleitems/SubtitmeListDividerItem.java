package net.osmand.plus.base.bottomsheetmenu.simpleitems;

import android.content.Context;

import androidx.annotation.ColorRes;

import net.osmand.plus.R;

public class SubtitmeListDividerItem extends DividerItem {

	public SubtitmeListDividerItem(Context context) {
		super(context);
	}

	public SubtitmeListDividerItem(Context context, @ColorRes int colorId) {
		super(context, colorId);
	}

	public SubtitmeListDividerItem(Context context, @ColorRes int colorId, int position) {
		super(context, colorId, position);
	}

	@Override
	protected int getBottomMargin(Context context) {
		return 0;
	}

	@Override
	protected int getStartMargin(Context context) {
		return context.getResources().getDimensionPixelSize(R.dimen.list_content_padding);
	}
}

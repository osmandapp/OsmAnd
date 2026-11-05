package net.osmand.plus.base.bottomsheetmenu.simpleitems;

import android.content.Context;

public class DividerSpaceItem extends DividerItem {

	private final int verticalSpacePx;

	public DividerSpaceItem(Context context, int verticalSpacePx) {
		super(context);
		this.verticalSpacePx = verticalSpacePx;
	}

	@Override
	protected int getTopMargin(Context context) {
		return 0;
	}

	@Override
	protected int getBottomMargin(Context context) {
		return 0;
	}

	@Override
	protected int getHeight(Context ctx) {
		return verticalSpacePx;
	}

	@Override
	protected int getBgColorId(boolean nightMode) {
		return android.R.color.transparent;
	}
}

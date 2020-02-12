package net.osmand.plus.base.bottomsheetmenu.simpleitems;

import android.content.Context;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;

public class DividerItem extends BaseBottomSheetItem {

	@ColorRes
	private int colorId;

	public DividerItem(Context context) {
		setupView(context, INVALID_ID, INVALID_POSITION);
	}

	public DividerItem(Context context, @ColorRes int colorId) {
		setupView(context, colorId, INVALID_POSITION);
	}

	public DividerItem(Context context, @ColorRes int colorId, int position) {
		setupView(context, colorId, position);
	}

	private void setupView(Context context, @ColorRes int colorId, int position) {
		view = new View(context);
		this.colorId = colorId;
		this.position = position;
	}

	@Override
	public void inflate(Context context, ViewGroup container, boolean nightMode) {
		super.inflate(context, container, nightMode);

		int height = AndroidUtils.dpToPx(context, 1);

		LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
		params.setMargins(getLeftMargin(context), getTopMargin(context), 0, getBottomMargin(context));
		params.height = height;

		view.setMinimumHeight(height);
		view.setBackgroundColor(ContextCompat.getColor(context, getBgColorId(nightMode)));
	}

	protected int getTopMargin(Context context) {
		return context.getResources().getDimensionPixelSize(R.dimen.bottom_sheet_content_padding_small);
	}

	protected int getLeftMargin(Context context) {
		return 0;
	}

	protected int getBottomMargin(Context context) {
		return context.getResources().getDimensionPixelSize(R.dimen.bottom_sheet_content_padding_small);
	}

	@ColorRes
	private int getBgColorId(boolean nightMode) {
		if (colorId != INVALID_ID) {
			return colorId;
		}
		return nightMode ? R.color.divider_color_dark : R.color.divider_color_light;
	}
}

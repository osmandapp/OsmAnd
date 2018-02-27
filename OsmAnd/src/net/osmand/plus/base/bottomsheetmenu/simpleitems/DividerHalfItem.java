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

public class DividerHalfItem extends BaseBottomSheetItem {

	@ColorRes
	private int colorId;

	public DividerHalfItem(Context context) {
		setupView(context, INVALID_ID, INVALID_POSITION);
	}

	public DividerHalfItem(Context context, @ColorRes int colorId) {
		setupView(context, colorId, INVALID_POSITION);
	}

	public DividerHalfItem(Context context, @ColorRes int colorId, int position) {
		setupView(context, colorId, position);
	}

	private void setupView(Context context, @ColorRes int colorId, int position) {
		view = new View(context);
		this.colorId = colorId;
		this.position = position;
	}

	@Override
	public void inflate(OsmandApplication app, ViewGroup container, boolean nightMode) {
		super.inflate(app, container, nightMode);

		int marginTopBottom = app.getResources().getDimensionPixelSize(R.dimen.bottom_sheet_content_padding_small);
		int marginLeft = app.getResources().getDimensionPixelSize(R.dimen.bottom_sheet_divider_margin_start);
		int height = AndroidUtils.dpToPx(app, 1);

		LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
		params.setMargins(marginLeft, marginTopBottom, 0, marginTopBottom);
		params.height = height;

		view.setMinimumHeight(height);
		view.setBackgroundColor(ContextCompat.getColor(app, getBgColorId(nightMode)));
	}

	@ColorRes
	private int getBgColorId(boolean nightMode) {
		if (colorId != INVALID_ID) {
			return colorId;
		}
		return nightMode ? R.color.dashboard_divider_dark : R.color.dashboard_divider_light;
	}
}

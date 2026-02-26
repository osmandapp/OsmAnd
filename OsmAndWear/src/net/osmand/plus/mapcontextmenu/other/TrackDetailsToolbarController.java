package net.osmand.plus.mapcontextmenu.other;

import android.content.Context;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.views.mapwidgets.TopToolbarController;
import net.osmand.plus.views.mapwidgets.TopToolbarView;

class TrackDetailsToolbarController extends TopToolbarController {

	TrackDetailsToolbarController() {
		super(TopToolbarControllerType.TRACK_DETAILS);
		setBackBtnIconClrIds(0, 0);
		setRefreshBtnIconClrIds(0, 0);
		setCloseBtnIconClrIds(0, 0);
		setTitleTextClrIds(R.color.text_color_tab_active_light, R.color.text_color_tab_active_dark);
		setDescrTextClrIds(R.color.text_color_tab_active_light, R.color.text_color_tab_active_dark);
		setBgIds(R.drawable.gradient_toolbar, R.drawable.gradient_toolbar,
				R.drawable.gradient_toolbar, R.drawable.gradient_toolbar);
	}

	@Override
	public void updateToolbar(@NonNull TopToolbarView toolbarView) {
		super.updateToolbar(toolbarView);
		AndroidUiHelper.updateVisibility(toolbarView.getShadowView(), false);
	}

	@Override
	public int getStatusBarColor(Context context, boolean nightMode) {
		return NO_COLOR;
	}
}

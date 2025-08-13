package net.osmand.plus.mapmarkers;

import static net.osmand.plus.views.mapwidgets.TopToolbarController.TopToolbarControllerType.MEASUREMENT_TOOL;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.views.mapwidgets.TopToolbarController;
import net.osmand.plus.views.mapwidgets.TopToolbarView;

class PlanRouteToolbarController extends TopToolbarController {

	PlanRouteToolbarController() {
		super(MEASUREMENT_TOOL);
		setBackBtnIconClrIds(0, 0);
		setTitleTextClrIds(R.color.text_color_tab_active_light, R.color.text_color_tab_active_dark);
		setDescrTextClrIds(R.color.text_color_tab_active_light, R.color.text_color_tab_active_dark);
		setBgIds(R.drawable.gradient_toolbar, R.drawable.gradient_toolbar,
				R.drawable.gradient_toolbar, R.drawable.gradient_toolbar);
		setCloseButtonVisible(false);
		setSaveViewVisible(true);
	}

	@Override
	public void updateToolbar(@NonNull TopToolbarView toolbarView) {
		super.updateToolbar(toolbarView);
		View shadow = toolbarView.getShadowView();
		if (shadow != null) {
			shadow.setVisibility(View.GONE);
		}
	}

	@Override
	public int getStatusBarColor(Context context, boolean nightMode) {
		return NO_COLOR;
	}
}

package net.osmand.plus.measurementtool;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.mapwidgets.TopToolbarController;
import net.osmand.plus.views.mapwidgets.TopToolbarView;

class MeasurementToolBarController extends TopToolbarController {

	private MeasurementToolFragment fragment;

	MeasurementToolBarController(@NonNull MeasurementToolFragment fragment) {
		super(TopToolbarControllerType.MEASUREMENT_TOOL);
		this.fragment = fragment;

		setBackBtnIconClrIds(0, 0);
		setTitleTextClrIds(R.color.text_color_tab_active_light, R.color.text_color_tab_active_dark);
		setDescrTextClrIds(R.color.text_color_tab_active_light, R.color.text_color_tab_active_dark);
		setBgIds(R.drawable.gradient_toolbar, R.drawable.gradient_toolbar,
				R.drawable.gradient_toolbar, R.drawable.gradient_toolbar);
		setCloseBtnVisible(false);
		setSaveViewVisible(true);
		setSingleLineTitle(true);
		setSaveViewTextId(R.string.shared_string_done);
	}

	@Override
	public void updateToolbar(@NonNull TopToolbarView toolbarView) {
		super.updateToolbar(toolbarView);
		setupDoneButton(toolbarView);
		View shadow = toolbarView.getShadowView();
		if (shadow != null) {
			AndroidUiHelper.updateVisibility(shadow, false);
		}
	}

	private void setupDoneButton(@NonNull TopToolbarView toolbarView) {
		TextView done = toolbarView.getSaveView();
		done.setAllCaps(false);

		Context ctx = done.getContext();
		Resources resources = ctx.getResources();
		int margin = resources.getDimensionPixelSize(R.dimen.context_menu_padding_margin_large);

		ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) done.getLayoutParams();
		layoutParams.height = resources.getDimensionPixelSize(R.dimen.measurement_tool_button_height);
		layoutParams.leftMargin = margin;
		layoutParams.rightMargin = margin;
		done.setPadding(margin, done.getPaddingTop(), margin, done.getPaddingBottom());
		AndroidUtils.setBackground(ctx, done, toolbarView.isNightMode(), R.drawable.purchase_dialog_outline_btn_bg_light,
				R.drawable.purchase_dialog_outline_btn_bg_dark);

		AndroidUiHelper.updateVisibility(done, fragment.isVisible());
	}

	@Override
	public int getStatusBarColor(@NonNull Context context, boolean nightMode) {
		return NO_COLOR;
	}
}
package net.osmand.plus.base;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;

public abstract class MenuBottomSheetDialogFragment extends BottomSheetDialogFragment {

	@Override
	public void onStart() {
		super.onStart();
		if (!AndroidUiHelper.isOrientationPortrait(getActivity())) {
			final Window window = getDialog().getWindow();
			WindowManager.LayoutParams params = window.getAttributes();
			params.width = getActivity().getResources().getDimensionPixelSize(R.dimen.landscape_bottom_sheet_dialog_fragment_width);
			window.setAttributes(params);
		}
	}

	@Override
	protected Drawable getContentIcon(@DrawableRes int id) {
		return getIcon(id, isNightMode() ? R.color.ctx_menu_info_text_dark : R.color.on_map_icon_color);
	}

	protected Drawable getActiveIcon(@DrawableRes int id) {
		return getIcon(id, isNightMode() ? R.color.osmand_orange : R.color.color_myloc_distance);
	}

	protected void setupHeightAndBackground(final View mainView, final int scrollViewId) {
		final Activity activity = getActivity();
		final boolean night = isNightMode();
		final int screenHeight = AndroidUtils.getScreenHeight(activity);
		final int statusBarHeight = AndroidUtils.getStatusBarHeight(activity);
		final int navBarHeight = AndroidUtils.getNavBarHeight(activity);

		mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				final View scrollView = mainView.findViewById(scrollViewId);
				int scrollViewHeight = scrollView.getHeight();
				int dividerHeight = AndroidUtils.dpToPx(getContext(), 1);
				int cancelButtonHeight = getContext().getResources().getDimensionPixelSize(R.dimen.bottom_sheet_cancel_button_height);
				int spaceForScrollView = screenHeight - statusBarHeight - navBarHeight - dividerHeight - cancelButtonHeight;
				if (scrollViewHeight > spaceForScrollView) {
					scrollView.getLayoutParams().height = spaceForScrollView;
					scrollView.requestLayout();
				}

				if (AndroidUiHelper.isOrientationPortrait(activity)) {
					AndroidUtils.setBackground(activity, mainView, night, R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);
				} else {
					if (screenHeight - statusBarHeight - mainView.getHeight() >= getResources().getDimension(R.dimen.bottom_sheet_content_padding_small)) {
						AndroidUtils.setBackground(activity, mainView, night,
								R.drawable.bg_bottom_sheet_topsides_landscape_light, R.drawable.bg_bottom_sheet_topsides_landscape_dark);
					} else {
						AndroidUtils.setBackground(activity, mainView, night,
								R.drawable.bg_bottom_sheet_sides_landscape_light, R.drawable.bg_bottom_sheet_sides_landscape_dark);
					}
				}

				ViewTreeObserver obs = mainView.getViewTreeObserver();
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					obs.removeOnGlobalLayoutListener(this);
				} else {
					obs.removeGlobalOnLayoutListener(this);
				}
			}
		});
	}

	protected abstract boolean isNightMode();
}

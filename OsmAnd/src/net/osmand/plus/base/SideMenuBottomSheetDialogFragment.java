package net.osmand.plus.base;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

public abstract class SideMenuBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	@Override
	public void onStart() {
		super.onStart();
		Activity activity = requireActivity();
		if (AndroidUiHelper.isOrientationPortrait(activity)) {
			return;
		}

		Dialog dialog = getDialog();
		if (dialog != null) {
			int width = getDimen(R.dimen.dashboard_land_width);
			CoordinatorLayout.LayoutParams layoutParams = new CoordinatorLayout.LayoutParams(
					width, CoordinatorLayout.LayoutParams.MATCH_PARENT);
			dialog.findViewById(R.id.content_container).setLayoutParams(layoutParams);

			Window window = dialog.getWindow();
			if (window != null) {
				WindowManager.LayoutParams params = window.getAttributes();
				params.width = AndroidUtils.getScreenWidth(activity);
				params.gravity = Gravity.START;
				window.setAttributes(params);
			}
		}
	}

	@Override
	protected Drawable getLandscapeSidesBg(@NonNull Context ctx) {
		int backgroundResAttr = AndroidUtils.isLayoutRtl(ctx) ?
				 R.attr.right_menu_view_bg : R.attr.left_menu_view_bg;
		return createBackgroundDrawable(ctx, AndroidUtils.resolveAttribute(ctx, backgroundResAttr));
	}

	@Override
	protected Drawable getLandscapeTopsidesBg(@NonNull Context ctx) {
		int backgroundResAttr = AndroidUtils.isLayoutRtl(ctx) ?
				R.attr.right_menu_view_bg : R.attr.left_menu_view_bg;
		return createBackgroundDrawable(ctx, AndroidUtils.resolveAttribute(ctx, backgroundResAttr));
	}

	@Override
	protected int getWindowAnimations(@NonNull Activity context) {
		if (AndroidUiHelper.isOrientationPortrait(context)) {
			return super.getWindowAnimations(context);
		}
		return AndroidUtils.isLayoutRtl(context) ?
				R.style.Animations_PopUpMenu_MiddleHeightRight : R.style.Animations_PopUpMenu_MiddleHeightLeft;
	}

	@Nullable
	protected MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		}
		return null;
	}
}
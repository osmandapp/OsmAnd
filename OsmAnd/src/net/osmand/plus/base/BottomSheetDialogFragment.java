package net.osmand.plus.base;

import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;

public abstract class BottomSheetDialogFragment extends DialogFragment {

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		boolean isLightTheme = getMyApplication()
				.getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;
		int themeId = isLightTheme ? R.style.OsmandLightTheme_BottomSheet
				: R.style.OsmandDarkTheme_BottomSheet;
		final Dialog dialog = new Dialog(getActivity(), themeId);
		dialog.setCanceledOnTouchOutside(true);
		dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		if (!getMyApplication().getSettings().DO_NOT_USE_ANIMATIONS.get()) {
			dialog.getWindow().getAttributes().windowAnimations = R.style.Animations_PopUpMenu_Bottom;
		}
		dialog.setCanceledOnTouchOutside(true);
		return dialog;
	}

	@Nullable
	@Override
	public abstract View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);

	@Override
	public void onStart() {
		super.onStart();

		if (getDialog() != null) {
			final Window window = getDialog().getWindow();
			WindowManager.LayoutParams params = window.getAttributes();
			params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
			params.gravity = Gravity.BOTTOM;
			params.width = ViewGroup.LayoutParams.MATCH_PARENT;
			window.setAttributes(params);
		}
	}


	protected OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	protected Drawable getIcon(@DrawableRes int drawableRes, @ColorRes int color) {
		return getMyApplication().getIconsCache().getIcon(drawableRes, color);
	}

	protected Drawable getContentIcon(@DrawableRes int drawableRes) {
		return getMyApplication().getIconsCache().getThemedIcon(drawableRes);
	}

}

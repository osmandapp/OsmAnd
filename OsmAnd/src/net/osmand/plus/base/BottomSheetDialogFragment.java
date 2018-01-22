package net.osmand.plus.base;

import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;

public abstract class BottomSheetDialogFragment extends DialogFragment {

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		OsmandSettings settings = getMyApplication().getSettings();
		int themeId = settings.isLightContent() ? R.style.OsmandLightTheme_BottomSheet : R.style.OsmandDarkTheme_BottomSheet;

		BottomSheetDialog dialog = new BottomSheetDialog(getContext(), themeId);
		dialog.setCanceledOnTouchOutside(true);
		Window window = dialog.getWindow();
		if (!settings.DO_NOT_USE_ANIMATIONS.get() && window != null) {
			window.getAttributes().windowAnimations = R.style.Animations_PopUpMenu_Bottom;
		}

		return dialog;
	}

	@Nullable
	@Override
	public abstract View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);

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

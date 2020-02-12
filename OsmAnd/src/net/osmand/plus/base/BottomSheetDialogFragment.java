package net.osmand.plus.base;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
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

import net.osmand.plus.OnDialogFragmentResultListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;

public abstract class BottomSheetDialogFragment extends DialogFragment {

	private OnDialogFragmentResultListener dialogFragmentResultListener;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Context context = requireContext();
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		OsmandSettings settings = app.getSettings();
		int themeId = settings.isLightContent() ? R.style.OsmandLightTheme_BottomSheet : R.style.OsmandDarkTheme_BottomSheet;

		BottomSheetDialog dialog = new BottomSheetDialog(context, themeId);
		dialog.setCanceledOnTouchOutside(true);
		Window window = dialog.getWindow();
		if (!settings.DO_NOT_USE_ANIMATIONS.get() && window != null) {
			window.getAttributes().windowAnimations = R.style.Animations_PopUpMenu_Bottom;
		}

		return dialog;
	}

	@Nullable
	@Override
	public abstract View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		if (context instanceof OnDialogFragmentResultListener) {
			dialogFragmentResultListener = (OnDialogFragmentResultListener) context;
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		dialogFragmentResultListener = null;
	}

	@Nullable
	public OnDialogFragmentResultListener getResultListener() {
		return dialogFragmentResultListener;
	}

	@Nullable
	protected OsmandApplication getMyApplication() {
		Activity activity = getActivity();
		if (activity != null) {
			return (OsmandApplication) activity.getApplication();
		} else {
			return null;
		}
	}

	@NonNull
	protected OsmandApplication requiredMyApplication() {
		return (OsmandApplication) requireActivity().getApplication();
	}

	@Nullable
	protected Drawable getIcon(@DrawableRes int drawableRes, @ColorRes int color) {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			return app.getUIUtilities().getIcon(drawableRes, color);
		} else {
			return null;
		}
	}

	@Nullable
	protected Drawable getContentIcon(@DrawableRes int drawableRes) {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			return app.getUIUtilities().getThemedIcon(drawableRes);
		} else {
			return null;
		}
	}
}

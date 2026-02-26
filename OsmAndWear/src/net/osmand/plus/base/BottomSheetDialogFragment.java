package net.osmand.plus.base;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import net.osmand.plus.OnDialogFragmentResultListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
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
			window.getAttributes().windowAnimations = getWindowAnimations(requireActivity());
		}

		return dialog;
	}

	@Nullable
	@Override
	public abstract View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);

	@Override
	public void onAttach(@NonNull Context context) {
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
	protected Drawable getIcon(@DrawableRes int drawableRes) {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			return app.getUIUtilities().getIcon(drawableRes);
		} else {
			return null;
		}
	}

	@Nullable
	protected Drawable getIcon(@DrawableRes int drawableRes, @ColorRes int colorRes) {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			return app.getUIUtilities().getIcon(drawableRes, colorRes);
		} else {
			return null;
		}
	}

	@Nullable
	protected Drawable getPaintedIcon(@DrawableRes int drawableRes, @ColorInt int color) {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			return app.getUIUtilities().getPaintedIcon(drawableRes, color);
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

	protected int getWindowAnimations(@NonNull Activity context) {
		return R.style.Animation_MaterialComponents_BottomSheetDialog;
	}
}
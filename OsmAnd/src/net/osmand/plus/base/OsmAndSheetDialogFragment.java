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
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import net.osmand.plus.OnDialogFragmentResultListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;

public abstract class OsmAndSheetDialogFragment extends DialogFragment {

	private OnDialogFragmentResultListener dialogFragmentResultListener;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Context context = requireContext();
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		OsmandSettings settings = app.getSettings();
		int themeId = settings.isLightContent() ? R.style.OsmandLightTheme_BottomSheet : R.style.OsmandDarkTheme_BottomSheet;

		OsmandSheetDialog dialog = new OsmandSheetDialog(context, themeId, getSheetDialogType());
		dialog.setCanceledOnTouchOutside(getCancelOnTouchOutside());
		dialog.setInteractWithOutside(getInteractWithOutside());
		Window window = dialog.getWindow();
		if (window != null && !settings.DO_NOT_USE_ANIMATIONS.get()) {
			window.getAttributes().windowAnimations = getSheetDialogType().getAnimationStyleResId();
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

	@Override
	public void onStart() {
		super.onStart();
		final Window window = getDialog().getWindow();
		FragmentActivity activity = requireActivity();
		if (window != null) {
			window.setDimAmount(getBackgroundDimAmount());
		}
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
	
	protected SheetDialogType getSheetDialogType() {
		return SheetDialogType.BOTTOM;
	}
	
	protected boolean getCancelOnTouchOutside() {
		return true;
	}
	
	protected boolean getInteractWithOutside() {
		return false;
	}

	protected float getBackgroundDimAmount() {
		return 0.3f;
	}
}

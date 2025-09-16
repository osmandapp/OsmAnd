package net.osmand.plus.base;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OnDialogFragmentResultListener;
import net.osmand.plus.R;

import java.util.ArrayList;
import java.util.List;

public abstract class BottomSheetDialogFragment extends BaseOsmAndDialogFragment {

	private OnDialogFragmentResultListener dialogFragmentResultListener;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		updateNightMode();
		FragmentActivity activity = requireActivity();
		int themeId = nightMode ? R.style.OsmandDarkTheme_BottomSheet : R.style.OsmandLightTheme_BottomSheet;

		BottomSheetDialog dialog = new BottomSheetDialog(activity, themeId);
		dialog.setCanceledOnTouchOutside(true);
		Window window = dialog.getWindow();
		if (!settings.DO_NOT_USE_ANIMATIONS.get() && window != null) {
			window.getAttributes().windowAnimations = getWindowAnimations(activity);
		}
		return dialog;
	}

	@Nullable
	@Override
	public abstract View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);

	@Override
	@Nullable
	public List<Integer> getBottomContainersIds() {
		List<Integer> ids = new ArrayList<>();
		ids.add(R.id.bottom_buttons_container);
		return ids;
	}

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

	protected int getWindowAnimations(@NonNull Activity context) {
		return R.style.Animation_MaterialComponents_BottomSheetDialog;
	}
}
package net.osmand.plus.base;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Window;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.ColorUtilities;

public abstract class BaseFullScreenDialogFragment extends BaseOsmAndDialogFragment {

	@StyleRes
	protected int getThemeId() {
		return nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
	}

	protected int getDialogStyle() {
		return STYLE_NO_FRAME;
	}

	@ColorRes
	protected int getStatusBarColorId() {
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setStyle(getDialogStyle(), getThemeId());
		requireActivity().getWindow().setSoftInputMode(SOFT_INPUT_ADJUST_RESIZE);
	}

	@NonNull
	@Override
	public final Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		updateNightMode();

		Dialog dialog = createDialog(savedInstanceState);
		Window window = dialog.getWindow();
		if (window != null) {
			if (!settings.DO_NOT_USE_ANIMATIONS.get()) {
				window.getAttributes().windowAnimations = R.style.Animations_Alpha;
			}
			int colorId = getStatusBarColorId();
			if (colorId != -1) {
				AndroidUiHelper.setStatusBarColor(window, getColor(getStatusBarColorId()));
			}
		}
		return dialog;
	}

	@NonNull
	public Dialog createDialog(@Nullable Bundle savedInstanceState) {
		return new Dialog(requireContext(), getThemeId());
	}
}
package net.osmand.plus.base;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.fragment.app.DialogFragment;

import net.osmand.plus.R;

public abstract class BaseFullScreenDialogFragment extends BaseOsmAndDialogFragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(getDialogStyle(), nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme);
		requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
	}

	protected int getDialogStyle() {
		return DialogFragment.STYLE_NO_FRAME;
	}
}

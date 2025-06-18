package net.osmand.plus.base;

import android.os.Bundle;
import android.view.WindowManager;

import net.osmand.plus.R;

public abstract class BaseFullScreenDialogFragment extends BaseOsmAndDialogFragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(STYLE_NO_FRAME, nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme);
		requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
	}
}

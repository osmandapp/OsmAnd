package net.osmand.plus.track.fragments.controller;

import androidx.annotation.ColorInt;

public interface IColorPickerDialogController {

	@ColorInt
	int getSelectedColor();

	boolean onSelectColor(@ColorInt int color);

	void onApplyColorSelection();

}

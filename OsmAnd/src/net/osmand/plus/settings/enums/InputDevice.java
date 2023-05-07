package net.osmand.plus.settings.enums;

import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum InputDevice {

	KEYBOARD(R.string.sett_generic_ext_input, 1),
	WUNDER_LINQ(R.string.sett_wunderlinq_ext_input, 2),
	PARROT(R.string.sett_parrot_ext_input, 3);

	private int titleId;
	private int value;

	InputDevice(@StringRes int titleId, int value) {
		this.titleId = titleId;
		this.value = value;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	public int getValue() {
		return value;
	}
}

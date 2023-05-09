package net.osmand.plus.settings.enums;

import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum InputDevice {

	NONE(R.string.shared_string_none, 0),
	KEYBOARD(R.string.sett_generic_ext_input, 1),
	WUNDER_LINQ(R.string.sett_wunderlinq_ext_input, 2),
	PARROT(R.string.sett_parrot_ext_input, 3);

	private final int titleId;
	private final int value;

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

	public static InputDevice getByValue(int value) {
		for (InputDevice device : values()) {
			if (device.getValue() == value) {
				return device;
			}
		}
		return NONE;
	}
}

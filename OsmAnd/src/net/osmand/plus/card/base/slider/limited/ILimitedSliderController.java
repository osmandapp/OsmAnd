package net.osmand.plus.card.base.slider.limited;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.utils.ColorUtilities;

public interface ILimitedSliderController {

	void bindComponent(@NonNull ILimitedSliderCard card);

	@NonNull
	default Limits getSliderLimits() {
		return new Limits(0, 100);
	}

	@ColorInt
	default int getControlsColor(@NonNull Context context, boolean nightMode) {
		return ColorUtilities.getActiveColor(context, nightMode);
	}

	default int getSelectedSliderValue() {
		return 50;
	}

	void onChangeSliderValue(float newValue);

}

package net.osmand.plus.card.base.slider;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.utils.ColorUtilities;

public interface ISliderCardController {

	void bindComponent(@NonNull ISliderCard cardInstance);

	@NonNull
	default Limits getSliderLimits() {
		return new Limits(0, 100);
	}

	@ColorInt
	default int getSliderColor(@NonNull Context context, boolean nightMode) {
		return ColorUtilities.getActiveColor(context, nightMode);
	}

	default int getSelectedSliderValue() {
		return 50;
	}

	void onChangeSliderValue(float newValue);

}

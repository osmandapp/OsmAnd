package net.osmand.plus.card.base.slider.moded;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.card.base.slider.ISliderCardController;
import net.osmand.plus.card.base.slider.moded.data.SliderMode;

import java.util.List;

public interface IModedSliderController extends ISliderCardController {

	boolean isSliderVisible();

	@NonNull
	List<SliderMode> getSliderModes();

	void askSelectSliderMode(@NonNull SliderMode sliderMode);

	@Nullable
	SliderMode getSelectedSliderMode();

	boolean isSelectedSliderMode(@NonNull SliderMode sliderMode);

}

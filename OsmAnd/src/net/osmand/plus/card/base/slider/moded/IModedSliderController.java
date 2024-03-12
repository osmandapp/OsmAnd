package net.osmand.plus.card.base.slider.moded;

import androidx.annotation.NonNull;

import net.osmand.plus.card.base.slider.limited.ILimitedSliderController;
import net.osmand.plus.card.base.slider.moded.data.SliderMode;

import java.util.List;

public interface IModedSliderController extends ILimitedSliderController {

	boolean isSliderVisible();

	List<SliderMode> getSliderModes();

	boolean askSelectSliderMode(@NonNull SliderMode sliderMode);

	boolean isSelectedSliderMode(@NonNull SliderMode sliderMode);

}

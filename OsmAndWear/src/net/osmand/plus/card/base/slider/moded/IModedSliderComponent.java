package net.osmand.plus.card.base.slider.moded;

import android.view.View;

import androidx.annotation.NonNull;

import net.osmand.plus.card.base.slider.ISliderCard;

public interface IModedSliderComponent extends ISliderCard {
	void updateSegmentedButtonSelection();
	void updateSliderVisibility();

	@NonNull
	View getSliderContainer();
}

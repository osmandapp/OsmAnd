package net.osmand.plus.card.base.slider;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.slider.Slider;

import net.osmand.plus.R;
import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.utils.UiUtilities;

public class SliderCard extends BaseCard implements ISliderCard {

	private final ISliderCardController controller;

	private Slider slider;

	public SliderCard(@NonNull FragmentActivity activity,
	                  @NonNull ISliderCardController controller, boolean usedOnMap) {
		super(activity, usedOnMap);
		this.controller = controller;
		controller.bindComponent(this);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.card_limited_slider;
	}

	@NonNull
	public Slider getSlider() {
		return slider;
	}

	@Override
	protected void updateContent() {
		slider = view.findViewById(R.id.slider);
		setupSlider();
	}

	private void setupSlider() {
		Limits sliderLimits = controller.getSliderLimits();
		int min = (int) sliderLimits.getMin();
		int max = (int) sliderLimits.getMax();
		int selected = controller.getSelectedSliderValue();

		slider.setValueFrom(min);
		slider.setValueTo(max);
		slider.setValue(selected);

		setText(R.id.value_min, String.valueOf(min));
		setText(R.id.value_max, String.valueOf(max));

		slider.addOnChangeListener((slider, newValue, fromUser) -> {
			if (fromUser) {
				controller.onChangeSliderValue(newValue);
			}
		});
		updateSliderColor();
	}

	@Override
	public void updateControlsColor() {
		updateSliderColor();
	}

	protected void updateSliderColor() {
		int accentColor = controller.getSliderColor(app, nightMode);
		UiUtilities.setupSlider(slider, nightMode, accentColor, true);
	}
}

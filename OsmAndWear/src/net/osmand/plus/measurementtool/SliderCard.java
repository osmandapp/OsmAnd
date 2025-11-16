package net.osmand.plus.measurementtool;

import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.slider.Slider;

import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;

public class SliderCard extends MapBaseCard {

	private final int initValue;
	private SliderCardListener listener;

	public SliderCard(MapActivity mapActivity, int initValue) {
		super(mapActivity);
		this.initValue = initValue;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.plan_route_threshold_slider;
	}

	@Override
	protected void updateContent() {
		TextView thresholdDistanceValue = view.findViewById(R.id.value);
		thresholdDistanceValue.setText(getStringValueWithMetric(initValue));
		TextView thresholdDistance = view.findViewById(R.id.title);
		thresholdDistance.setText(R.string.threshold_distance);
		Slider slider = view.findViewById(R.id.slider);
		slider.setValueFrom(0);
		slider.setValue(initValue);
		slider.setValueTo(100);
		slider.setStepSize(5);
		UiUtilities.setupSlider(slider, nightMode, ContextCompat.getColor(view.getContext(),
				R.color.profile_icon_color_blue_light));
		slider.addOnChangeListener(new Slider.OnChangeListener() {
			@Override
			public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
				if (fromUser) {
					String valueStr = getStringValueWithMetric((int) value);
					thresholdDistanceValue.setText(valueStr);
				}
			}
		});
		slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
			@Override
			public void onStartTrackingTouch(@NonNull Slider slider) {

			}

			@Override
			public void onStopTrackingTouch(@NonNull Slider slider) {
				if (listener != null) {
					listener.onSliderChange((int) slider.getValue());
				}
			}
		});
	}

	public void setListener(SliderCardListener listener) {
		this.listener = listener;
	}

	private String getStringValueWithMetric(int value) {
		return String.format(view.getContext().getString(R.string.ltr_or_rtl_combine_via_space),
				value, view.getContext().getString(R.string.m));
	}

	interface SliderCardListener {

		void onSliderChange(int sliderValue);

	}
}

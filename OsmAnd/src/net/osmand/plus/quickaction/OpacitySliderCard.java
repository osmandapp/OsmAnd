package net.osmand.plus.quickaction;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.slider.Slider;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class OpacitySliderCard extends MapBaseCard {

	private final ButtonAppearanceParams appearanceParams;

	protected Slider slider;

	@Override
	public int getCardLayoutId() {
		return R.layout.map_button_opacity_card;
	}

	public OpacitySliderCard(@NonNull MapActivity activity, @NonNull ButtonAppearanceParams appearanceParams) {
		super(activity, false);
		this.appearanceParams = appearanceParams;
	}

	@Override
	protected void updateContent() {
		setupSlider(view);
		setupHeader(view);
	}

	protected void setupHeader(@NonNull View view) {
		TextView valueMin = view.findViewById(R.id.value_min);
		TextView valueMax = view.findViewById(R.id.value_max);

		valueMin.setText(getFormattedValue(0));
		valueMax.setText(getFormattedValue(100));
	}

	protected void setupSlider(@NonNull View view) {
		slider = view.findViewById(R.id.slider);
		slider.setValue(appearanceParams.getOpacity());
		slider.addOnChangeListener((s, value, fromUser) -> {
			if (fromUser) {
				onValueSelected(value);
			}
		});
		UiUtilities.setupSlider(slider, nightMode, ColorUtilities.getActiveColor(app, nightMode), false);
	}

	protected void onValueSelected(float value) {
		appearanceParams.setOpacity(value);
		notifyCardPressed();
	}

	@NonNull
	protected String getFormattedValue(float value) {
		return (int) value + "%";
	}
}
package net.osmand.plus.quickaction;


import static com.google.android.material.slider.LabelFormatter.LABEL_FLOATING;

import android.view.View;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class CornerRadiusCard extends SliderButtonsCard {

	public static final int[] CORNER_RADIUS_VALUES = {3, 6, 9, 12, 36};

	private final ButtonAppearanceParams appearanceParams;

	public CornerRadiusCard(@NonNull MapActivity activity, @NonNull ButtonAppearanceParams appearanceParams) {
		super(activity);
		this.appearanceParams = appearanceParams;
	}

	protected void setupHeader(@NonNull View view) {
		super.setupHeader(view);
		title.setText(R.string.corner_radius);
		description.setText(getFormattedValue(appearanceParams.getCornerRadius()));
	}

	protected void setupSlider(@NonNull View view) {
		super.setupSlider(view);
		UiUtilities.setupSlider(slider, nightMode, ColorUtilities.getActiveColor(app, nightMode), true);

		slider.setValueTo(CORNER_RADIUS_VALUES.length - 1);
		slider.setValueFrom(0);
		slider.setStepSize(1);
		slider.setValue(getSelectedIndex());
		slider.setLabelBehavior(LABEL_FLOATING);
		slider.setLabelFormatter(value -> CornerRadiusCard.this.getFormattedValue(CORNER_RADIUS_VALUES[(int) value]));
	}

	protected void onValueSelected(float value) {
		super.onValueSelected(value);

		int index = (int) value;
		appearanceParams.setCornerRadius(CORNER_RADIUS_VALUES[index]);
		description.setText(getFormattedValue(appearanceParams.getCornerRadius()));

		notifyCardPressed();
	}

	private int getSelectedIndex() {
		int value = appearanceParams.getCornerRadius();
		for (int i = 0; i < CORNER_RADIUS_VALUES.length; i++) {
			if (CORNER_RADIUS_VALUES[i] == value) {
				return i;
			}
		}
		return 0;
	}

	@NonNull
	protected String getFormattedValue(float value) {
		return getString(R.string.ltr_or_rtl_combine_via_space, (int) value, getString(R.string.shared_string_dp));
	}
}

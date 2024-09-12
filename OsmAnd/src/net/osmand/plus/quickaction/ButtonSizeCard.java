package net.osmand.plus.quickaction;

import android.view.View;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class ButtonSizeCard extends SliderButtonsCard {

	public static final int MIN_BUTTON_SIZE = 40;
	public static final int MAX_BUTTON_SIZE = 72;
	public static final int BUTTON_SIZE_STEP = 8;

	private final ButtonAppearanceParams appearanceParams;

	public ButtonSizeCard(@NonNull MapActivity activity, @NonNull ButtonAppearanceParams appearanceParams) {
		super(activity);
		this.appearanceParams = appearanceParams;
	}

	protected void setupHeader(@NonNull View view) {
		super.setupHeader(view);
		title.setText(R.string.shared_string_size);
		description.setText(getFormattedValue(appearanceParams.getSize()));
	}

	protected void setupSlider(@NonNull View view) {
		super.setupSlider(view);

		slider.setValueTo(MAX_BUTTON_SIZE);
		slider.setValueFrom(MIN_BUTTON_SIZE);
		slider.setStepSize(BUTTON_SIZE_STEP);
		slider.setValue(appearanceParams.getSize());
		UiUtilities.setupSlider(slider, nightMode, ColorUtilities.getActiveColor(app, nightMode), true);
	}

	protected void onValueSelected(float value) {
		super.onValueSelected(value);

		appearanceParams.setSize((int) value);
		description.setText(getFormattedValue(appearanceParams.getSize()));

		notifyCardPressed();
	}

	@NonNull
	protected String getFormattedValue(float value) {
		return getString(R.string.ltr_or_rtl_combine_via_space, (int) value, getString(R.string.shared_string_dp));
	}
}
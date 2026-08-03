package net.osmand.plus.quickaction;

import static com.google.android.material.slider.LabelFormatter.LABEL_FLOATING;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.ProgressHelper;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class OpacitySliderCard extends SliderButtonsCard {

	public static final int MIN_OPACITY = 0;
	public static final int MAX_OPACITY = 1;

	private final ButtonAppearanceParams appearanceParams;

	@Override
	public int getCardLayoutId() {
		return R.layout.map_button_opacity_card;
	}

	public OpacitySliderCard(@NonNull MapActivity activity, @NonNull ButtonAppearanceParams appearanceParams) {
		super(activity);
		this.appearanceParams = appearanceParams;
	}

	@Override
	protected void updateContent() {
		setupHeader(view);
		setupSlider(view);
	}

	@Override
	protected void setupHeader(@NonNull View view) {
		super.setupHeader(view);

		TextView valueMin = view.findViewById(R.id.value_min);
		TextView valueMax = view.findViewById(R.id.value_max);

		valueMin.setText(getFormattedValue(MIN_OPACITY));
		valueMax.setText(getFormattedValue(MAX_OPACITY));

		updateDescription();
	}

	@Override
	protected void setupSlider(@NonNull View view) {
		super.setupSlider(view);
		UiUtilities.setupSlider(slider, nightMode, ColorUtilities.getActiveColor(app, nightMode), false);

		slider.setValueTo(MAX_OPACITY);
		slider.setValueFrom(MIN_OPACITY);
		slider.setValue(appearanceParams.getOpacity());
		slider.setLabelBehavior(LABEL_FLOATING);
		slider.setLabelFormatter(OpacitySliderCard.this::getFormattedValue);
	}

	protected void onValueSelected(float value) {
		appearanceParams.setOpacity(value);
		updateDescription();
		notifyCardPressed();
	}

	private void updateDescription() {
		description.setText(getFormattedValue(appearanceParams.getOpacity()));
	}

	@NonNull
	protected String getFormattedValue(float value) {
		return ProgressHelper.normalizeProgressPercent((int) (value * 100)) + "%";
	}
}
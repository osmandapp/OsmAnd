package net.osmand.plus.quickaction;


import static com.google.android.material.slider.LabelFormatter.LABEL_FLOATING;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.ORIGINAL_VALUE;

import android.view.View;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.card.base.multistate.CardState;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CornerRadiusCard extends SliderButtonsCard {

	public static final int[] CORNER_RADIUS_VALUES = {3, 6, 9, 12, 36};

	private final ButtonAppearanceParams appearanceParams;

	public CornerRadiusCard(@NonNull MapActivity activity,
			@NonNull ButtonAppearanceParams appearanceParams, boolean showOriginal) {
		super(activity, showOriginal);
		this.appearanceParams = appearanceParams;
	}

	protected void setupHeader(@NonNull View view) {
		super.setupHeader(view);
		title.setText(R.string.corner_radius);
		valueTv.setText(getFormattedValue(appearanceParams.getCornerRadius()));
	}

	@Override
	protected void setupDescription(@NonNull @NotNull View view) {
		super.setupDescription(view);
		description.setText(R.string.default_buttons_corners_original_description);
	}

	protected void setupSlider(@NonNull View view) {
		super.setupSlider(view);
		UiUtilities.setupSlider(slider, nightMode, ColorUtilities.getActiveColor(app, nightMode), true);

		slider.setValueTo(CORNER_RADIUS_VALUES.length - 1);
		slider.setValueFrom(0);
		slider.setStepSize(1);
		slider.setLabelBehavior(LABEL_FLOATING);
		slider.setLabelFormatter(value -> CornerRadiusCard.this.getFormattedValue(CORNER_RADIUS_VALUES[(int) value]));

		if (!isOriginalValue()) {
			slider.setValue(getSelectedIndex());
		}
	}

	protected void onValueSelected(float value) {
		super.onValueSelected(value);

		int index = (int) value;
		appearanceParams.setCornerRadius(CORNER_RADIUS_VALUES[index]);
		valueTv.setText(getFormattedValue(appearanceParams.getCornerRadius()));

		notifyCardPressed();
	}

	@Override
	protected boolean isOriginalValue() {
		return appearanceParams.getCornerRadius() == ORIGINAL_VALUE;
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
		if (value == ORIGINAL_VALUE) {
			return getString(R.string.shared_string_original);
		}
		return getString(R.string.ltr_or_rtl_combine_via_space, (int) value, getString(R.string.shared_string_dp));
	}

	@NonNull
	@Override
	protected List<CardState> getCardStates() {
		List<CardState> list = new ArrayList<>();

		list.add(new CardState(R.string.shared_string_original).setTag(ORIGINAL_VALUE));

		for (int i = 0; i < CORNER_RADIUS_VALUES.length; i++) {
			int value = CORNER_RADIUS_VALUES[i];
			list.add(new CardState(getFormattedValue(value))
					.setShowTopDivider(i == 0)
					.setTag(value));
		}
		return list;
	}

	@Override
	protected void setSelectedState(@NonNull CardState cardState) {
		if (cardState.getTag() instanceof Integer value) {
			appearanceParams.setCornerRadius(value);
		}
		updateContent();
		notifyCardPressed();
	}
}

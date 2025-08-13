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

public class ButtonSizeCard extends SliderButtonsCard {

	public static final int MIN_BUTTON_SIZE = 40;
	public static final int MAX_BUTTON_SIZE = 72;
	public static final int BUTTON_SIZE_STEP = 8;

	private final ButtonAppearanceParams appearanceParams;

	public ButtonSizeCard(@NonNull MapActivity activity,
			@NonNull ButtonAppearanceParams appearanceParams, boolean showOriginal) {
		super(activity, showOriginal);
		this.appearanceParams = appearanceParams;
	}

	protected void setupHeader(@NonNull View view) {
		super.setupHeader(view);
		title.setText(R.string.shared_string_size);
		valueTv.setText(getFormattedValue(appearanceParams.getSize()));
	}

	@Override
	protected void setupDescription(@NonNull @NotNull View view) {
		super.setupDescription(view);
		description.setText(R.string.default_buttons_corners_original_description);
	}

	protected void setupSlider(@NonNull View view) {
		super.setupSlider(view);
		UiUtilities.setupSlider(slider, nightMode, ColorUtilities.getActiveColor(app, nightMode), true);

		slider.setValueTo(MAX_BUTTON_SIZE);
		slider.setValueFrom(MIN_BUTTON_SIZE);
		slider.setStepSize(BUTTON_SIZE_STEP);
		slider.setLabelBehavior(LABEL_FLOATING);
		slider.setLabelFormatter(ButtonSizeCard.this::getFormattedValue);

		if (!isOriginalValue()) {
			slider.setValue(appearanceParams.getSize());
		}
	}

	protected void onValueSelected(float value) {
		super.onValueSelected(value);

		appearanceParams.setSize((int) value);
		valueTv.setText(getFormattedValue(appearanceParams.getSize()));

		notifyCardPressed();
	}

	@Override
	protected boolean isOriginalValue() {
		return appearanceParams.getSize() == ORIGINAL_VALUE;
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

		for (int i = MIN_BUTTON_SIZE; i <= MAX_BUTTON_SIZE; i += BUTTON_SIZE_STEP) {
			list.add(new CardState(getFormattedValue(i))
					.setShowTopDivider(i == MIN_BUTTON_SIZE)
					.setTag(i));
		}
		return list;
	}

	@Override
	protected void setSelectedState(@NonNull CardState cardState) {
		if (cardState.getTag() instanceof Integer value) {
			appearanceParams.setSize(value);
		}
		updateContent();
		notifyCardPressed();
	}
}
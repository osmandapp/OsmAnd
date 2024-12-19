package net.osmand.plus.quickaction;

import static com.google.android.material.slider.LabelFormatter.LABEL_FLOATING;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.ORIGINAL_VALUE;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.TRANSPARENT_ALPHA;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.ProgressHelper;
import net.osmand.plus.card.base.multistate.CardState;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class OpacitySliderCard extends SliderButtonsCard {

	public static final int MIN_OPACITY = 0;
	public static final int MAX_OPACITY = 1;

	private final ButtonAppearanceParams appearanceParams;

	private float selectedOpacity;

	public OpacitySliderCard(@NonNull MapActivity activity,
			@NonNull ButtonAppearanceParams appearanceParams, boolean showOriginal) {
		super(activity, showOriginal);
		this.selectedOpacity = appearanceParams.getOpacity();
		this.appearanceParams = appearanceParams;
	}

	@NonNull
	@Override
	public View inflate(@NonNull Context ctx) {
		View view = super.inflate(ctx);
		addMinMaxRow(view.findViewById(R.id.slider_container));
		return view;
	}

	private void addMinMaxRow(@NonNull ViewGroup container) {
		View view = themedInflater.inflate(R.layout.min_max_container, container, false);

		TextView valueMin = view.findViewById(R.id.value_min);
		TextView valueMax = view.findViewById(R.id.value_max);

		valueMin.setText(getFormattedValue(MIN_OPACITY));
		valueMax.setText(getFormattedValue(MAX_OPACITY));

		container.addView(view);
	}

	@Override
	protected void setupHeader(@NonNull View view) {
		super.setupHeader(view);

		title.setText(R.string.background_opacity);
		updateDescription();
	}

	@Override
	protected void setupDescription(@NonNull @NotNull View view) {
		super.setupDescription(view);
		description.setText(R.string.default_buttons_corners_original_description);
	}

	@Override
	protected void setupSlider(@NonNull View view) {
		super.setupSlider(view);
		UiUtilities.setupSlider(slider, nightMode, ColorUtilities.getActiveColor(app, nightMode), false);

		slider.setValueTo(MAX_OPACITY);
		slider.setValueFrom(MIN_OPACITY);
		slider.setLabelBehavior(LABEL_FLOATING);
		slider.setLabelFormatter(OpacitySliderCard.this::getFormattedValue);

		if (!isOriginalValue()) {
			slider.setValue(appearanceParams.getOpacity());
		}
	}

	@Override
	protected void setupButtons(@NonNull @NotNull View view) {
		super.setupButtons(view);

		AndroidUiHelper.updateVisibility(increaseButton, false);
		AndroidUiHelper.updateVisibility(decreaseButton, false);
	}

	protected void onValueSelected(float value) {
		selectedOpacity = value;
		appearanceParams.setOpacity(value);
		updateDescription();
		notifyCardPressed();
	}

	@Override
	protected boolean isOriginalValue() {
		return appearanceParams.getOpacity() == ORIGINAL_VALUE;
	}

	@NonNull
	@Override
	protected List<CardState> getCardStates() {
		List<CardState> list = new ArrayList<>();
		float value = selectedOpacity != ORIGINAL_VALUE ? selectedOpacity : TRANSPARENT_ALPHA;
		list.add(new CardState(R.string.shared_string_original).setTag(ORIGINAL_VALUE));
		list.add(new CardState(R.string.shared_string_custom).setTag(value).setShowTopDivider(true));

		return list;
	}

	private void updateDescription() {
		valueTv.setText(getFormattedValue(appearanceParams.getOpacity()));
	}

	@NonNull
	protected String getFormattedValue(float value) {
		if (value == ORIGINAL_VALUE) {
			return getString(R.string.shared_string_original);
		}
		return ProgressHelper.normalizeProgressPercent((int) (value * 100)) + "%";
	}

	@Override
	protected void setSelectedState(@NonNull CardState cardState) {
		if (cardState.getTag() instanceof Number value) {
			appearanceParams.setOpacity(value.floatValue());
		}
		updateContent();
		notifyCardPressed();
	}
}
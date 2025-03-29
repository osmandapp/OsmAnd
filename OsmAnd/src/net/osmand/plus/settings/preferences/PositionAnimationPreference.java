package net.osmand.plus.settings.preferences;

import static net.osmand.plus.utils.OsmAndFormatter.getFormattedPredictionTime;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceViewHolder;

import com.google.android.material.slider.Slider;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class PositionAnimationPreference extends BaseCustomPreference {

	private final static int MIN_VALUE = 0;
	private final static int MAX_VALUE = 100;
	private final static int VALUE_STEP = 10;

	private OsmandApplication app;
	private final boolean nightMode;
	private final ApplicationMode selectedAppMode;

	private int currentValue;
	private CommonPreference<Integer> preference;
	private boolean isSliderVisible;

	private TextView title;
	private TextView summary;
	private TextView from;
	private TextView to;
	private View advancedItem;
	private ImageView advancedItemIcon;
	private View sliderContainer;
	private Slider slider;

	public PositionAnimationPreference(Context context, AttributeSet attrs, @NonNull ApplicationMode selectedAppMode, boolean nightMode) {
		super(context, attrs);
		this.nightMode = nightMode;
		this.selectedAppMode = selectedAppMode;
	}

	@Override
	protected int getLayoutId() {
		return R.layout.position_animation_settings_card;
	}

	@Override
	public void onBindViewHolder(PreferenceViewHolder holder) {
		super.onBindViewHolder(holder);
		this.app = AndroidUtils.getApp(holder.itemView.getContext());
		this.preference = app.getSettings().LOCATION_INTERPOLATION_PERCENT;

		View view = holder.itemView;
		title = view.findViewById(R.id.title);
		summary = view.findViewById(R.id.summary);
		from = view.findViewById(R.id.from_value);
		to = view.findViewById(R.id.to_value);
		slider = view.findViewById(R.id.slider);
		advancedItem = view.findViewById(R.id.selectable_list_item);
		sliderContainer = view.findViewById(R.id.slider_container);
		advancedItemIcon = view.findViewById(R.id.advanced_item_icon);

		isSliderVisible = preference.getModeValue(selectedAppMode) > 0;
	}

	public void updateView() {
		setupSliderView();
		setupAdvancedButton();
		updateContent();
	}

	private void updateContent() {
		AndroidUiHelper.updateVisibility(sliderContainer, isSliderVisible);
		advancedItemIcon.setImageDrawable(app.getUIUtilities().getIcon(isSliderVisible
				? R.drawable.ic_action_arrow_down
				: R.drawable.ic_action_arrow_up, ColorUtilities.getDefaultIconColorId(nightMode)));
	}

	private void setupAdvancedButton() {
		advancedItem.setOnClickListener(v -> {
			isSliderVisible = !isSliderVisible;
			updateContent();
		});
	}

	private void setupSliderView() {
		currentValue = preference.getModeValue(selectedAppMode);

		title.setText(app.getString(R.string.prediction_time));
		summary.setText(getFormattedPredictionTime(app, currentValue));
		from.setText(getFormattedPredictionTime(app, MIN_VALUE));
		to.setText(getFormattedPredictionTime(app, MAX_VALUE));

		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		UiUtilities.setupSlider(slider, nightMode, activeColor, true);
		slider.setValueFrom(MIN_VALUE);
		slider.setValueTo(MAX_VALUE);
		slider.setStepSize(VALUE_STEP);
		slider.setValue(currentValue);

		slider.addOnChangeListener((sl, value, fromUser) -> {
			if (fromUser) {
				currentValue = (int) value;
				preference.setModeValue(selectedAppMode, currentValue);
				summary.setText(getFormattedPredictionTime(app, currentValue));
			}
		});
	}
}

package net.osmand.plus.settings.fragments.configureitems.viewholders;

import static net.osmand.plus.utils.OsmAndFormatter.getFormattedPredictionTime;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.slider.Slider;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class PositionAnimationSettingCard {

	private final OsmandApplication app;

	private Integer[] range;
	private int minValue;
	private int maxValue;
	private int currentValue;
	private final CommonPreference<Integer> preference;

	private final TextView title;
	private final TextView summary;
	private final TextView from;
	private final TextView to;
	private final View advancedItem;
	private final ImageView advancedItemIcon;
	private final View sliderContainer;
	private final Slider slider;
	private final boolean nightMode;

	private boolean isSliderVisible;

	public PositionAnimationSettingCard(@NonNull OsmandApplication app, @NonNull View view, @NonNull CommonPreference<Integer> preference, boolean nightMode) {
		this.app = app;
		this.preference = preference;
		this.nightMode = nightMode;

		title = view.findViewById(R.id.title);
		summary = view.findViewById(R.id.summary);
		from = view.findViewById(R.id.from_value);
		to = view.findViewById(R.id.to_value);
		slider = view.findViewById(R.id.slider);
		advancedItem = view.findViewById(R.id.selectable_list_item);
		sliderContainer = view.findViewById(R.id.slider_container);
		advancedItemIcon = view.findViewById(R.id.advanced_item_icon);

		isSliderVisible = preference.get() > 0;
	}

	public void bind() {
		initData();
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
		advancedItem.setOnClickListener(v-> {
			isSliderVisible = !isSliderVisible;
			updateContent();
		});
	}

	private void setupSliderView() {
		title.setText(app.getString(R.string.prediction_time));
		summary.setText(getFormattedPredictionTime(app, currentValue));
		from.setText(getFormattedPredictionTime(app, minValue));
		to.setText(getFormattedPredictionTime(app, maxValue));

		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		UiUtilities.setupSlider(slider, nightMode, activeColor, true);
		slider.setValueFrom(0);
		slider.setValueTo(range.length - 1);
		slider.setStepSize(1);
		slider.setValue(getRangeIndex(currentValue));

		slider.addOnChangeListener((sl, value, fromUser) -> {
			if (fromUser) {
				currentValue = range[(int) sl.getValue()];
				preference.set(currentValue);
				summary.setText(getFormattedPredictionTime(app, currentValue));
			}
		});
	}

	private void initData() {
		List<Integer> rangeList = getAvailableRange();
		range = new Integer[rangeList.size()];
		rangeList.toArray(range);
		if (range.length > 0) {
			minValue = range[0];
			maxValue = range[range.length - 1];
		}
		currentValue = preference.get();
	}

	private List<Integer> getAvailableRange() {
		List<Integer> powRange = new ArrayList<>();
		for (int i = 0; i <= 100; i += 10) {
			powRange.add(i);
		}
		return powRange;
	}

	private int getRangeIndex(float value) {
		for (int i = 0; i < range.length; i++) {
			if (value == range[i]) {
				return i;
			}
		}
		return range.length - 1;
	}
}

package net.osmand.plus.settings.bottomsheets;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.slider.Slider;

import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.enums.MetricsConstants;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;

public class SpeedLimitBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = SpeedLimitBottomSheet.class.getSimpleName();

	private static final String CURRENT_VALUE = "current_value";
	public static final String KEY_APP_MODE = "app_mode";

	private static final float MILES_IN_METER = 1.609f;
	private static final int MIN_VALUE_KM_H = -10;
	private static final int MAX_VALUE_KM_H = 20;

	private OsmandSettings settings;
	private ApplicationMode appMode;
	private OsmandPreference<Float> speedLimitPreference;
	private int currentValue;
	private int fromValue;
	private int toValue;
	private String unit;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		OsmandApplication app = requiredMyApplication();
		settings = app.getSettings();
		speedLimitPreference = settings.SPEED_LIMIT_EXCEED_KMH;
		setupValues();

		if (savedInstanceState != null) {
			currentValue = (int) savedInstanceState.getFloat(CURRENT_VALUE);
			appMode = ApplicationMode.valueOfStringKey(savedInstanceState.getString(KEY_APP_MODE), settings.getApplicationMode());
		} else {
			currentValue = (int) (isMetric()
					? speedLimitPreference.get()
					: speedLimitPreference.get() * MILES_IN_METER);
		}

		if(appMode == null){
			appMode = settings.getApplicationMode();
		}

		int contentPadding = app.getResources().getDimensionPixelSize(R.dimen.content_padding);
		String title = getString(R.string.speed_limit_exceed);

		items.add(new TitleItem(title));
		items.add(new LongDescriptionItem(getString(R.string.speed_limit_exceed_message)));
		items.add(new DividerSpaceItem(app, contentPadding));
		items.add(createSliderView());
		items.add(new DividerSpaceItem(app, contentPadding));
	}

	private BaseBottomSheetItem createSliderView() {
		View sliderView = UiUtilities.getInflater(getContext(), nightMode)
				.inflate(R.layout.bottom_sheet_item_slider_with_four_text, null);

		TextView sliderTitle = sliderView.findViewById(R.id.title);
		sliderTitle.setText(R.string.selected_value);

		TextView summary = sliderView.findViewById(R.id.summary);
		summary.setText(getFormattedCurrentValue());

		Slider slider = sliderView.findViewById(R.id.slider);
		slider.setValue(currentValue);
		slider.setValueFrom(fromValue);
		slider.setValueTo(toValue);
		slider.addOnChangeListener((slider1, value, fromUser) -> {
			currentValue = (int) value;
			summary.setText(getFormattedCurrentValue());
		});

		int appModeColor = appMode.getProfileColor(nightMode);
		UiUtilities.setupSlider(slider, nightMode, appModeColor, false);

		TextView fromTv = sliderView.findViewById(R.id.from_value);
		fromTv.setText(getFormattedFromValue());

		TextView toTv = sliderView.findViewById(R.id.to_value);
		toTv.setText(getFormattedToValue());

		return new BaseBottomSheetItem.Builder()
				.setCustomView(sliderView)
				.create();
	}

	@Override
	protected void onRightBottomButtonClick() {
		speedLimitPreference.set(isMetric()
				? currentValue
				: currentValue / MILES_IN_METER);
		dismiss();
	}

	private boolean isMetric() {
		return settings.METRIC_SYSTEM.getModeValue(appMode) == MetricsConstants.KILOMETERS_AND_METERS;
	}

	private String getFormattedCurrentValue() {
		return currentValue + " " + unit;
	}

	private String getFormattedFromValue() {
		return fromValue + " " + unit;
	}

	private String getFormattedToValue() {
		return toValue + " " + unit;
	}

	private void setupValues() {
		if (isMetric()) {
			fromValue = MIN_VALUE_KM_H;
			toValue = MAX_VALUE_KM_H;
			unit = getString(R.string.km_h);
		} else {
			fromValue = (int) (MIN_VALUE_KM_H * MILES_IN_METER);
			toValue = (int) (MAX_VALUE_KM_H * MILES_IN_METER);
			unit = getString(R.string.mile_per_hour);
		}
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putFloat(CURRENT_VALUE, currentValue);
		outState.putString(KEY_APP_MODE, appMode.getStringKey());
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, ApplicationMode selectedAppMode) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			SpeedLimitBottomSheet fragment = new SpeedLimitBottomSheet();
			fragment.appMode = selectedAppMode;
			fragment.show(fragmentManager, TAG);
		}
	}
}
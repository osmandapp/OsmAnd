package net.osmand.plus.settings.bottomsheets;

import static net.osmand.plus.utils.OsmAndFormatter.getFormattedSpeed;
import static net.osmand.plus.utils.OsmAndFormatter.getFormattedSpeedValue;
import static net.osmand.plus.utils.OsmAndFormatter.getMpSFromFormattedValue;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;

import com.google.android.material.slider.Slider;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.SpeedConstants;
import net.osmand.plus.settings.fragments.OnPreferenceChanged;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;

public class SpeedLimitBottomSheet extends BasePreferenceBottomSheet {

	private static final String TAG = SpeedLimitBottomSheet.class.getSimpleName();

	private static final String SELECTED_VALUE = "selected_value";

	private OsmandApplication app;
	private float selectedValue;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();

		if (savedInstanceState != null) {
			selectedValue = savedInstanceState.getFloat(SELECTED_VALUE);
		} else {
			float value = app.getSettings().SPEED_LIMIT_EXCEED_KMH.getModeValue(getAppMode());
			selectedValue = value;
		}

		int padding = getResources().getDimensionPixelSize(R.dimen.content_padding);

		items.add(new TitleItem(getString(R.string.speed_limit_exceed)));
		items.add(new LongDescriptionItem(getString(R.string.speed_limit_exceed_message)));
		items.add(new DividerSpaceItem(app, padding));
		items.add(createSliderItem());
		items.add(new DividerSpaceItem(app, padding));
	}

	@NonNull
	private BaseBottomSheetItem createSliderItem() {
		ApplicationMode appMode = getAppMode();
		LayoutInflater inflater = UiUtilities.getInflater(getContext(), nightMode);
		View view = inflater.inflate(R.layout.bottom_sheet_item_slider_with_four_text, null);

		TextView title = view.findViewById(R.id.title);
		title.setText(R.string.selected_value);

		Slider slider = view.findViewById(R.id.slider);
		SpeedConstants speedFormat = OsmAndFormatter.getSpeedModeForPaceMode(app.getSettings().SPEED_SYSTEM.getModeValue(appMode));
		boolean isSpeedToleranceBigRange = appMode.isSpeedToleranceBigRange();
		float convertedLimitFrom = getFormattedSpeedValue(appMode.getMinSpeedToleranceLimit(), app, isSpeedToleranceBigRange, speedFormat).valueSrc;
		float convertedLimitTo = getFormattedSpeedValue(appMode.getMaxSpeedToleranceLimit(), app, isSpeedToleranceBigRange, speedFormat).valueSrc;
		float convertedSelectedValue = getFormattedSpeedValue(selectedValue / 3.6f, app, isSpeedToleranceBigRange, speedFormat).valueSrc;
		if (convertedSelectedValue > convertedLimitTo) {
			convertedSelectedValue = convertedLimitTo;
		} else if (convertedSelectedValue < convertedLimitFrom) {
			convertedSelectedValue = convertedLimitFrom;
		}
		float step = 0.1f;
		if (isSpeedToleranceBigRange) {
			convertedSelectedValue = getIntegerSpeed(convertedSelectedValue);
			convertedLimitFrom = getIntegerSpeed(convertedLimitFrom);
			convertedLimitTo = getIntegerSpeed(convertedLimitTo);
			step = 1f;
		}
		slider.setValue(convertedSelectedValue);
		slider.setValueFrom(convertedLimitFrom);
		slider.setValueTo(convertedLimitTo);
		slider.setStepSize(step);

		TextView summary = view.findViewById(R.id.summary);
		summary.setText(getFormattedSpeed(getMpSFromFormattedValue(app, convertedSelectedValue, speedFormat), app, isSpeedToleranceBigRange, speedFormat));

		TextView fromTv = view.findViewById(R.id.from_value);
		fromTv.setText(getFormattedSpeed(getMpSFromFormattedValue(app, convertedLimitFrom, speedFormat), app, isSpeedToleranceBigRange, speedFormat));

		TextView toTv = view.findViewById(R.id.to_value);
		toTv.setText(getFormattedSpeed(getMpSFromFormattedValue(app, convertedLimitTo, speedFormat), app, isSpeedToleranceBigRange, speedFormat));

		slider.addOnChangeListener((s, value, fromUser) -> {
			float selectedSpeedInMS = getMpSFromFormattedValue(app, value, speedFormat);
			selectedValue = selectedSpeedInMS * 3.6f;
			summary.setText(getFormattedSpeed(selectedSpeedInMS, app, isSpeedToleranceBigRange, speedFormat));
		});

		int color = appMode.getProfileColor(nightMode);
		UiUtilities.setupSlider(slider, nightMode, color, false);

		return new BaseBottomSheetItem.Builder()
				.setCustomView(view)
				.create();
	}

	private int getIntegerSpeed(float floatSpeed) {
		if (floatSpeed < 0) {
			return (int) Math.floor(floatSpeed);
		} else {
			return (int) Math.ceil(floatSpeed);
		}
	}

	@Override
	protected void onRightBottomButtonClick() {
		Preference preference = getPreference();
		if (preference != null) {
			float value = selectedValue;
			if (preference.callChangeListener(value)) {
				app.getSettings().SPEED_LIMIT_EXCEED_KMH.setModeValue(getAppMode(), value);
			}
			Fragment target = getTargetFragment();
			if (target instanceof OnPreferenceChanged) {
				((OnPreferenceChanged) target).onPreferenceChanged(preference.getKey());
			}
		}
		dismiss();
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putFloat(SELECTED_VALUE, selectedValue);
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull Fragment target,
	                                @NonNull String key, @NonNull ApplicationMode appMode) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle args = new Bundle();
			args.putString(PREFERENCE_ID, key);

			SpeedLimitBottomSheet fragment = new SpeedLimitBottomSheet();
			fragment.setArguments(args);
			fragment.setAppMode(appMode);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}
}
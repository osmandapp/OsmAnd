package net.osmand.plus.settings.bottomsheets;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.MeasurementUnits;
import net.osmand.plus.settings.vehiclespecs.SpecificationType;
import net.osmand.plus.settings.vehiclespecs.profiles.VehicleSpecs;
import net.osmand.plus.settings.fragments.ApplyQueryType;
import net.osmand.plus.settings.fragments.OnConfirmPreferenceChange;
import net.osmand.plus.settings.preferences.VehicleSpecificationPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.widgets.chips.ChipItem;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VehicleParametersBottomSheet extends BaseTextFieldBottomSheet {
	private static final Log LOG = PlatformUtil.getLog(VehicleParametersBottomSheet.class);
	public static final String TAG = VehicleParametersBottomSheet.class.getSimpleName();

	private VehicleSpecificationPreference preference;

	@NonNull
	@SuppressLint("ClickableViewAccessibility")
	protected BaseBottomSheetItem createBottomSheetItem(@NonNull OsmandApplication app, @NonNull View mainView) {
		preference = (VehicleSpecificationPreference) getPreference();
		boolean useMetricSystem = preference.isUseMetricSystem();
		VehicleSpecs vehicleSpecs = preference.getSpecifications();
		SpecificationType type = preference.getSpecificationType();
		List<ChipItem> chips = collectChipItems(vehicleSpecs, type, useMetricSystem);

		title.setText(preference.getTitle().toString());

		Drawable icon = getIcon(vehicleSpecs.getIconId(type, nightMode));
		ivImage.setImageDrawable(icon);

		String description = getString(vehicleSpecs.getDescriptionId(type));
		tvDescription.setText(description);

		MeasurementUnits units = vehicleSpecs.getMeasurementUnits(type, useMetricSystem);
		tvMetric.setText(units.getNameResId());

		currentValue = vehicleSpecs.readSavedValue(preference);
		etText.setText(formatInputValue(currentValue));
		etText.clearFocus();
		etText.setOnTouchListener((v, event) -> {
			etText.onTouchEvent(event);
			etText.setSelection(etText.getText().length());
			return true;
		});

		etText.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				currentValue = (float) Algorithms.parseDoubleSilently(s.toString(), 0.0f);
				String error;
				if (currentValue == 0.0f || (error = vehicleSpecs.checkValue(app, type, useMetricSystem, currentValue)).isEmpty()) {
					onCorrectInput();
					updateChips();
				} else {
					onWrongInput(error);
				}
			}
		});

		chipsView.setItems(chips);
		ChipItem selected = chipsView.findChipByTag(currentValue);
		chipsView.setSelected(selected);
		return new BaseBottomSheetItem.Builder().setCustomView(mainView).create();
	}

	@NonNull
	public List<ChipItem> collectChipItems(@NonNull VehicleSpecs vehicleSpecs,
	                                       @NonNull SpecificationType type,
	                                       boolean useMetricSystem) {
		// Add "None"
		List<ChipItem> chips = new ArrayList<>();
		String none = app.getString(R.string.shared_string_none);
		ChipItem chip = new ChipItem(none);
		chip.title = none;
		chip.contentDescription = none;
		chip.tag = 0.0f;
		chips.add(chip);

		// Add predefined values
		MeasurementUnits units = vehicleSpecs.getMeasurementUnits(type, useMetricSystem);
		String symbol = getString(units.getSymbolResId());
		for (Float value : vehicleSpecs.getPredefinedValues(type, units.isMetricSystem())) {
			String pattern = getString(R.string.ltr_or_rtl_combine_via_space);
			String valueStr = units.formatValue(value);
			String title = String.format(pattern, valueStr, symbol);
			chip = new ChipItem(title);
			chip.title = title;
			chip.contentDescription = title;
			chip.tag = value;
			chips.add(chip);
		}
		return chips;
	}

	private void onCorrectInput() {
		tilCaption.setErrorEnabled(false);
		updateApplyButton(true);
	}

	private void onWrongInput(@NonNull String message) {
		tilCaption.setErrorEnabled(true);
		tilCaption.setError(message);
		updateApplyButton(false);
	}

	private void updateApplyButton(boolean enable) {
		rightButton.setEnabled(enable);
		rightButton.setButtonType(enable ? DialogButtonType.PRIMARY : DialogButtonType.STROKED);
		rightButton.setTitleId(getRightBottomButtonTextId());
	}

	@Override
	protected void onRightBottomButtonClick() {
		if (getTargetFragment() instanceof OnConfirmPreferenceChange callback) {
			String preferenceId = getPreference().getKey();
			VehicleSpecs vehicleSpecs = preference.getSpecifications();
			String value = String.valueOf(vehicleSpecs.prepareValueToSave(preference, currentValue));
			callback.onConfirmPreferenceChange(preferenceId, value, ApplyQueryType.SNACK_BAR);
		}
		dismiss();
	}

	@Override
	protected String formatInputValue(float input) {
		if (input == 0.0f) {
			return "";
		}
		DecimalFormat formatter = new DecimalFormat("#.####", new DecimalFormatSymbols(Locale.US));
		return formatter.format(input);
	}

	public static void showInstance(@NonNull FragmentManager fm, String key, Fragment target,
	                                boolean usedOnMap, @Nullable ApplicationMode appMode) {
		if (AndroidUtils.isFragmentCanBeAdded(fm, TAG)) {
			Bundle args = new Bundle();
			args.putString(PREFERENCE_ID, key);
			VehicleParametersBottomSheet fragment = new VehicleParametersBottomSheet();
			fragment.setArguments(args);
			fragment.setUsedOnMap(usedOnMap);
			fragment.setAppMode(appMode);
			fragment.setTargetFragment(target, 0);
			fragment.show(fm, TAG);
		}
	}
}

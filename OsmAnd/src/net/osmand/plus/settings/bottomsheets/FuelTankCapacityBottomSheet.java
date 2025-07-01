package net.osmand.plus.settings.bottomsheets;

import android.annotation.SuppressLint;
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
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.VolumeUnit;
import net.osmand.plus.settings.fragments.ApplyQueryType;
import net.osmand.plus.settings.fragments.OnConfirmPreferenceChange;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.widgets.chips.ChipItem;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FuelTankCapacityBottomSheet extends BaseTextFieldBottomSheet {
	private static final Log LOG = PlatformUtil.getLog(VehicleParametersBottomSheet.class);
	public static final String TAG = FuelTankCapacityBottomSheet.class.getSimpleName();

	private VolumeUnit volumeUnit;

	@NonNull
	@SuppressLint("ClickableViewAccessibility")
	protected BaseBottomSheetItem createBottomSheetItem(@NonNull OsmandApplication app, @NonNull View mainView) {
		volumeUnit = app.getSettings().UNIT_OF_VOLUME.getModeValue(getAppMode());
		List<ChipItem> chips = collectChipItems(app, volumeUnit);

		title.setText(R.string.fuel_tank_capacity);
		AndroidUiHelper.updateVisibility(ivImage, false);
		tvDescription.setText(R.string.fuel_tank_capacity_description);
		tvMetric.setText(volumeUnit.toHumanString(app));

		currentValue = OsmAndFormatter.readSavedFuelTankCapacity(app.getSettings(), volumeUnit, getAppMode());
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
				onCorrectInput();
				updateChips();
			}
		});

		chipsView.setItems(chips);
		ChipItem selected = chipsView.findChipByTag(currentValue);
		chipsView.setSelected(selected);
		return new BaseBottomSheetItem.Builder()
				.setCustomView(mainView)
				.create();
	}

	@NonNull
	public List<ChipItem> collectChipItems(@NonNull OsmandApplication app,
	                                       @NonNull VolumeUnit volumeUnit) {
		List<ChipItem> chips = new ArrayList<>();
		String none = getString(R.string.shared_string_none);
		ChipItem chip = new ChipItem(none);
		chip.title = none;
		chip.contentDescription = none;
		chip.tag = 0.0f;
		chips.add(chip);

		DecimalFormat formatter = new DecimalFormat("0.#", new DecimalFormatSymbols(Locale.US));
		for (int i = 1; i <= 11; i++) {
			float value = 10 * i;
			String pattern = getString(R.string.ltr_or_rtl_combine_via_space);
			String valueStr = formatter.format(value);
			String title = String.format(pattern, valueStr, volumeUnit.getUnitSymbol(app));
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
		rightButton.setEnabled(true);
		rightButton.setButtonType(DialogButtonType.PRIMARY);
		rightButton.setTitleId(getRightBottomButtonTextId());
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment target = getTargetFragment();
		if (target instanceof OnConfirmPreferenceChange callback) {
			String preferenceId = getPreference().getKey();
			Float value = OsmAndFormatter.prepareFuelTankCapacityToSave(volumeUnit, currentValue);
			callback.onConfirmPreferenceChange(preferenceId, value, ApplyQueryType.SNACK_BAR);
		}
		dismiss();
	}

	@Override
	protected String formatInputValue(float input) {
		if (input == 0.0f) {
			return "";
		}
		DecimalFormat formatter = new DecimalFormat("0.#", new DecimalFormatSymbols(Locale.US));
		return formatter.format(input);
	}

	public static void showInstance(@NonNull FragmentManager fm, String key, Fragment target,
	                                boolean usedOnMap, @Nullable ApplicationMode appMode) {
		if (AndroidUtils.isFragmentCanBeAdded(fm, TAG)) {
			Bundle args = new Bundle();
			args.putString(PREFERENCE_ID, key);
			FuelTankCapacityBottomSheet fragment = new FuelTankCapacityBottomSheet();
			fragment.setArguments(args);
			fragment.setUsedOnMap(usedOnMap);
			fragment.setAppMode(appMode);
			fragment.setTargetFragment(target, 0);
			fragment.show(fm, TAG);
		}
	}
}

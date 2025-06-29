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
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.vehiclesize.SizeData;
import net.osmand.plus.settings.vehiclesize.SizeType;
import net.osmand.plus.settings.vehiclesize.VehicleSizes;
import net.osmand.plus.settings.fragments.ApplyQueryType;
import net.osmand.plus.settings.fragments.OnConfirmPreferenceChange;
import net.osmand.plus.settings.preferences.SizePreference;
import net.osmand.plus.settings.vehiclesize.containers.Metric;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.widgets.chips.ChipItem;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class VehicleParametersBottomSheet extends BaseTextFieldBottomSheet {
	private static final Log LOG = PlatformUtil.getLog(VehicleParametersBottomSheet.class);
	public static final String TAG = VehicleParametersBottomSheet.class.getSimpleName();

	private SizePreference sizePreference;

	@NonNull
	@SuppressLint("ClickableViewAccessibility")
	protected BaseBottomSheetItem createBottomSheetItem(@NonNull OsmandApplication app, @NonNull View mainView) {
		sizePreference = (SizePreference) getPreference();
		VehicleSizes vehicleSizes = sizePreference.getVehicleSizes();
		Metric metric = sizePreference.getMetric();
		SizeType sizeType = sizePreference.getSizeType();
		SizeData data = vehicleSizes.getSizeData(sizeType);
		List<ChipItem> chips = vehicleSizes.collectChipItems(app, sizeType, metric);

		title.setText(sizePreference.getTitle().toString());

		Drawable icon = getIcon(data.assets().getIconId(nightMode));
		ivImage.setImageDrawable(icon);

		String description = getString(data.assets().getDescriptionId());
		tvDescription.setText(description);

		int metricStringId = vehicleSizes.getMetricStringId(sizeType, metric);
		tvMetric.setText(metricStringId);

		currentValue = vehicleSizes.readSavedValue(sizePreference);
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
				StringBuilder error = new StringBuilder();
				if (currentValue == 0.0f || vehicleSizes.verifyValue(app, sizeType, metric, currentValue, error)) {
					onCorrectInput();
					updateChips();
				} else {
					onWrongInput(error.toString());
				}
			}
		});

		chipsView.setItems(chips);
		ChipItem selected = chipsView.findChipByTag(currentValue);
		chipsView.setSelected(selected);
		return new BaseBottomSheetItem.Builder().setCustomView(mainView).create();
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
			VehicleSizes vehicleSizes = sizePreference.getVehicleSizes();
			String value = String.valueOf(vehicleSizes.prepareValueToSave(sizePreference, currentValue));
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

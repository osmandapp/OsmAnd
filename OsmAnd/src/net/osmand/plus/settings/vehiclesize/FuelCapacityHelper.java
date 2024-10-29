package net.osmand.plus.settings.vehiclesize;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.enums.VolumeUnit;
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue;
import net.osmand.plus.widgets.chips.ChipItem;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FuelCapacityHelper {

	public static final float IMPERIAL_GALLONS_IN_LITER = 4.546091879f;
	public static final float US_GALLONS_IN_LITER = 3.785411784f;

	public static FormattedValue getFormattedValue(@NonNull OsmandApplication app, @NonNull VolumeUnit volumeUnit, @NonNull ApplicationMode mode) {
		OsmandSettings settings = app.getSettings();
		boolean separateWithSpace = false;
		String textValue;
		String textUnit;
		float value = readSavedValue(settings, volumeUnit, mode);
		if (value == 0.0f) {
			textValue = app.getString(R.string.shared_string_none);
			textUnit = "";
		} else {
			DecimalFormat formatter = new DecimalFormat("0.#", new DecimalFormatSymbols(Locale.US));
			textValue = formatter.format(value);
			textUnit = volumeUnit.getUnitSymbol(app);
			separateWithSpace = true;
		}
		return new FormattedValue(value, textValue, textUnit, separateWithSpace);
	}

	public static float readSavedValue(@NonNull OsmandSettings settings, @NonNull VolumeUnit volumeUnit, @NonNull ApplicationMode mode) {
		OsmandPreference<Float> fuelTankCapacity = settings.FUEL_TANK_CAPACITY;
		float value = fuelTankCapacity.getModeValue(mode);
		if (value == 0.0f) {
			return value;
		}

		if (volumeUnit == VolumeUnit.US_GALLONS) {
			return value / US_GALLONS_IN_LITER;
		} else if (volumeUnit == VolumeUnit.IMPERIAL_GALLONS) {
			return value / IMPERIAL_GALLONS_IN_LITER;
		} else {
			return value;
		}
	}

	public static float prepareValueToSave(@NonNull VolumeUnit volumeUnit, float value) {
		float preparedValueToSave = 0;
		if (value != 0.0f) {
			if (volumeUnit == VolumeUnit.US_GALLONS) {
				preparedValueToSave = value * US_GALLONS_IN_LITER;
			} else if (volumeUnit == VolumeUnit.IMPERIAL_GALLONS) {
				preparedValueToSave = value * IMPERIAL_GALLONS_IN_LITER;
			} else {
				preparedValueToSave = value;
			}
		}
		return preparedValueToSave;
	}

	@NonNull
	public static List<ChipItem> collectChipItems(@NonNull OsmandApplication app,
	                                              @NonNull VolumeUnit volumeUnit) {
		List<ChipItem> chips = new ArrayList<>();
		String none = app.getString(R.string.shared_string_none);
		ChipItem chip = new ChipItem(none);
		chip.title = none;
		chip.contentDescription = none;
		chip.tag = 0.0f;
		chips.add(chip);

		DecimalFormat formatter = new DecimalFormat("0.#", new DecimalFormatSymbols(Locale.US));
		for (int i = 1; i <= 11; i++) {
			float value = 10 * i;
			String pattern = app.getString(R.string.ltr_or_rtl_combine_via_space);
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
}

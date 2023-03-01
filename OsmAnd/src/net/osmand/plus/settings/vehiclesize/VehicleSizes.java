package net.osmand.plus.settings.vehiclesize;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.settings.enums.MetricsConstants;
import net.osmand.plus.settings.preferences.SizePreference;
import net.osmand.plus.settings.vehiclesize.containers.Assets;
import net.osmand.plus.widgets.chips.ChipItem;
import net.osmand.router.GeneralRouter.GeneralRouterProfile;
import net.osmand.util.Algorithms;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class VehicleSizes {

	public static final int DEFAULT_PROPOSED_VALUES_COUNT = 7;

	private static final String DERIVED_PROFILE_TRUCK = "Truck";
	private static final String DERIVED_PROFILE_MOTORCYCLE = "Motorcycle";

	private Map<SizeType, SizeData> sizes = new HashMap<>();

	protected VehicleSizes() {
		collectSizesData();
	}

	protected abstract void collectSizesData();

	protected void add(@NonNull SizeType type, @NonNull Assets assets, @NonNull Limits limits) {
		sizes.put(type, new SizeData(assets, limits));
	}

	public SizeData getSizeData(@NonNull SizeType type) {
		return sizes.get(type);
	}

	public boolean verifyValue(@NonNull SizeType type, @NonNull Context ctx,
	                           float value, @NonNull StringBuilder error) {
		return true;
	}

	public int getMetricStringId(@NonNull SizeType type, @NonNull MetricsConstants mc) {
		if (type == SizeType.WEIGHT) {
			return R.string.shared_string_tones;
		} else {
			if (mc == MetricsConstants.MILES_AND_FEET || mc == MetricsConstants.NAUTICAL_MILES_AND_FEET) {
				return useInchesInsteadOfFeet() ? R.string.shared_string_inches : R.string.shared_string_feet;
			}
			if (mc == MetricsConstants.MILES_AND_YARDS) {
				return useInchesInsteadOfYards() ? R.string.shared_string_inches : R.string.shared_string_yards;
			}
			return R.string.shared_string_meters;
		}
	}

	public int getMetricShortStringId(@NonNull SizeType type, @NonNull MetricsConstants mc) {
		if (type == SizeType.WEIGHT) {
			return R.string.metric_ton;
		} else {
			if (mc == MetricsConstants.MILES_AND_FEET || mc == MetricsConstants.NAUTICAL_MILES_AND_FEET) {
				return useInchesInsteadOfFeet() ? R.string.inch : R.string.foot;
			}
			if (mc == MetricsConstants.MILES_AND_YARDS) {
				return useInchesInsteadOfYards() ? R.string.inch : R.string.yard;
			}
			return R.string.m;
		}
	}

	public float readSavedValue(@NonNull SizePreference preference) {
		float value = (float) Algorithms.parseDoubleSilently(preference.getValue(), 0.0f);
		if (value != 0.0f) {
			value += 0.01f;
		}
		if (preference.getSizeType() != SizeType.WEIGHT) {
			// Convert display value to selected metric system
			value = VehicleAlgorithms.convertLengthFromMeters(
					preference.getLengthMetricSystem(), value, useInchesInsteadOfFeet(), useInchesInsteadOfYards());
		}
		return value;
	}

	public float prepareValueToSave(@NonNull SizePreference preference, float value) {
		if (preference.getSizeType() != SizeType.WEIGHT) {
			// Convert length to meters before save
			value = VehicleAlgorithms.convertLengthToMeters(
					preference.getLengthMetricSystem(), value, useInchesInsteadOfFeet(), useInchesInsteadOfYards());
		}
		if (value != 0.0f) {
			value -= 0.01f;
		}
		return value;
	}

	@NonNull
	public List<ChipItem> collectChipItems(@NonNull OsmandApplication app, @NonNull SizeType type,
	                                       @NonNull MetricsConstants lengthMetricSystem) {
		List<ChipItem> chips = new ArrayList<>();

		// Add "None"
		String none = app.getString(R.string.shared_string_none);
		ChipItem chip = new ChipItem(none);
		chip.title = none;
		chip.tag = 0.0f;
		chips.add(chip);

		// Add other variants
		String metricShort = app.getString(getMetricShortStringId(type, lengthMetricSystem));
		for (Float value : collectProposedValues(type, lengthMetricSystem)) {
			String pattern = app.getString(R.string.ltr_or_rtl_combine_via_space);
			DecimalFormat formatter = new DecimalFormat("#.#");
			String valueStr = formatter.format(value);
			String title = String.format(pattern, valueStr, metricShort);
			chip = new ChipItem(title);
			chip.title = title;
			chip.tag = value;
			chips.add(chip);
		}
		return chips;
	}

	@NonNull
	private List<Float> collectProposedValues(@NonNull SizeType type,
	                                          @NonNull MetricsConstants lengthMetricSystem) {
		SizeData data = getSizeData(type);
		Limits limits = data.getLimits();
		if (type != SizeType.WEIGHT) {
			limits = VehicleAlgorithms.convertLimitsByMetricSystem(limits, lengthMetricSystem, useInchesInsteadOfFeet(), useInchesInsteadOfYards());
		}
		return VehicleAlgorithms.collectProposedValues(limits, 1, getMinProposedValuesCount());
	}

	protected int getMinProposedValuesCount() {
		return DEFAULT_PROPOSED_VALUES_COUNT;
	}

	protected boolean useInchesInsteadOfFeet() {
		return true;
	}

	protected boolean useInchesInsteadOfYards() {
		return true;
	}

	@Nullable
	public static VehicleSizes newInstance(@NonNull GeneralRouterProfile routerProfile,
	                                       @Nullable String derivedProfile) {
		if (routerProfile == GeneralRouterProfile.BOAT) {
			return new BoatSizes();
		}
		if (routerProfile == GeneralRouterProfile.CAR) {
			if (DERIVED_PROFILE_TRUCK.equalsIgnoreCase(derivedProfile)) {
				return new TruckSizes();
			} else if (DERIVED_PROFILE_MOTORCYCLE.equalsIgnoreCase(derivedProfile)) {
				return new MotorcycleSizes();
			} else {
				return new CarSizes();
			}
		}
		return null;
	}

}

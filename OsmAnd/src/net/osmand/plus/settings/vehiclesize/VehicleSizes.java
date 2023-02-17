package net.osmand.plus.settings.vehiclesize;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.wrapper.Assets;
import net.osmand.plus.base.wrapper.Limits;
import net.osmand.plus.settings.enums.MetricsConstants;
import net.osmand.plus.settings.preferences.SizePreference;
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

	private Map<DimensionType, DimensionData> dimensions = new HashMap<>();

	protected VehicleSizes() {
		collectDimensionsData();
	}

	protected abstract void collectDimensionsData();

	protected void add(@NonNull DimensionType type,
	                   @NonNull Assets assets, @NonNull Limits limits) {
		dimensions.put(type, new DimensionData(assets, limits));
	}

	public DimensionData getDimensionData(@NonNull DimensionType type) {
		return dimensions.get(type);
	}

	public boolean verifyValue(@NonNull DimensionType type, @NonNull Context ctx,
	                           float value, @NonNull StringBuilder error) {
		return true;
	}

	public int getMetricStringId(@NonNull DimensionType type, @NonNull MetricsConstants mc) {
		if (type == DimensionType.WEIGHT) {
			return R.string.shared_string_tones;
		} else {
			if (mc == MetricsConstants.MILES_AND_FEET || mc == MetricsConstants.NAUTICAL_MILES_AND_FEET) {
				return R.string.shared_string_feet;
			}
			if (mc == MetricsConstants.MILES_AND_YARDS) {
				return R.string.shared_string_yards;
			}
			return R.string.shared_string_meters;
		}
	}

	public int getMetricShortStringId(@NonNull DimensionType type, @NonNull MetricsConstants mc) {
		if (type == DimensionType.WEIGHT) {
			return R.string.metric_ton;
		} else {
			if (mc == MetricsConstants.MILES_AND_FEET || mc == MetricsConstants.NAUTICAL_MILES_AND_FEET) {
				return R.string.foot;
			}
			if (mc == MetricsConstants.MILES_AND_YARDS) {
				return R.string.yard;
			}
			return R.string.m;
		}
	}

	public float readSavedValue(@NonNull SizePreference preference) {
		float value = (float) Algorithms.parseDoubleSilently(preference.getValue(), 0.0f);
		if (value != 0.0f) {
			value += 0.01f;
		}
		if (preference.getDimensionType() != DimensionType.WEIGHT) {
			// Convert display value to selected metric system
			value = VehicleAlgorithms.convertLengthFromMeters(preference.getLengthMetricSystem(), value);
		}
		return value;
	}

	public float prepareValueToSave(@NonNull SizePreference preference, float value) {
		if (preference.getDimensionType() != DimensionType.WEIGHT) {
			// Convert length to meters before save
			value = VehicleAlgorithms.convertLengthToMeters(preference.getLengthMetricSystem(), value);
		}
		if (value != 0.0f) {
			value -= 0.01f;
		}
		return value;
	}

	public List<ChipItem> collectChipItems(@NonNull OsmandApplication app, @NonNull DimensionType type,
	                                       @NonNull MetricsConstants lengthMetric) {
		List<ChipItem> chips = new ArrayList<>();

		// Add "None"
		String none = app.getString(R.string.shared_string_none);
		ChipItem chip = new ChipItem(none);
		chip.title = none;
		chip.tag = 0.0f;
		chips.add(chip);

		// Add other variants
		for (Float value : collectProposedValues(type, lengthMetric)) {
			String pattern = app.getString(R.string.ltr_or_rtl_combine_via_space);
			DecimalFormat formatter = new DecimalFormat("#.#");
			String valueStr = formatter.format(value);
			String metricShort = app.getString(getMetricShortStringId(type, lengthMetric));
			String title = String.format(pattern, valueStr, metricShort);
			chip = new ChipItem(title);
			chip.title = title;
			chip.tag = value;
			chips.add(chip);
		}
		return chips;
	}

	private List<Float> collectProposedValues(@NonNull DimensionType type,
	                                          @NonNull MetricsConstants lengthMetric) {
		DimensionData data = getDimensionData(type);
		Limits limits = data.getLimits();
		if (type != DimensionType.WEIGHT) {
			limits = VehicleAlgorithms.convertLimitsByMetricSystem(limits, lengthMetric);
		}
		return VehicleAlgorithms.collectProposedValues(limits, 1, getMinProposedValuesCount());
	}

	protected int getMinProposedValuesCount() {
		return DEFAULT_PROPOSED_VALUES_COUNT;
	}

	@Nullable
	public static VehicleSizes newInstance(@NonNull GeneralRouterProfile routerProfile,
	                                       @Nullable String derivedProfile) {
		if (routerProfile == GeneralRouterProfile.BOAT) {
			return new BoatSizes();
		}
		if (routerProfile == GeneralRouterProfile.CAR) {
			if ("Truck".equalsIgnoreCase(derivedProfile)) {
				return new TruckSizes();
			} else if ("Motorcycle".equalsIgnoreCase(derivedProfile)) {
				return new MotorcycleSizes();
			} else {
				return new CarSizes();
			}
		}
		return null;
	}

}

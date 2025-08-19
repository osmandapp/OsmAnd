package net.osmand.plus.settings.vehiclesize;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.containers.Limits;
import net.osmand.shared.settings.enums.MetricsConstants;
import net.osmand.plus.settings.preferences.SizePreference;
import net.osmand.plus.settings.vehiclesize.containers.Assets;
import net.osmand.plus.settings.vehiclesize.containers.Metric;
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

	private final Map<SizeType, SizeData> sizes = new HashMap<>();

	protected VehicleSizes() {
		collectSizesData();
	}

	protected abstract void collectSizesData();

	protected void add(@NonNull SizeType type, @NonNull Assets assets, @NonNull Limits<Float> limits) {
		sizes.put(type, new SizeData(assets, limits));
	}

	public SizeData getSizeData(@NonNull SizeType type) {
		return sizes.get(type);
	}

	public boolean verifyValue(@NonNull Context ctx, @NonNull SizeType type,
	                           @NonNull Metric metric, float value, @NonNull StringBuilder error) {
		return true;
	}

	public int getMetricStringId(@NonNull SizeType type, @NonNull Metric metric) {
		if (type.isWeightType()) {
			WeightMetric wm = metric.getWeightMetric();
			if (wm == WeightMetric.TONES) {
				return useKilogramsInsteadOfTons() ? R.string.shared_string_kilograms : R.string.shared_string_tones;
			}
			return R.string.shared_string_pounds;
		} else {
			MetricsConstants lm = metric.getLengthMetric();
			if (lm == MetricsConstants.MILES_AND_FEET || lm == MetricsConstants.NAUTICAL_MILES_AND_FEET) {
				return useInchesInsteadOfFeet() ? R.string.shared_string_inches : R.string.shared_string_feet;
			}
			if (lm == MetricsConstants.MILES_AND_YARDS) {
				return useInchesInsteadOfYards() ? R.string.shared_string_inches : R.string.shared_string_yards;
			}
			return R.string.shared_string_meters;
		}
	}

	public int getMetricShortStringId(@NonNull SizeType type, @NonNull Metric metric) {
		if (type.isWeightType()) {
			WeightMetric wm = metric.getWeightMetric();
			if (wm == WeightMetric.TONES) {
				return useKilogramsInsteadOfTons() ? R.string.kg : R.string.metric_ton;
			}
			return R.string.metric_lbs;
		} else {
			MetricsConstants lm = metric.getLengthMetric();
			if (lm == MetricsConstants.MILES_AND_FEET || lm == MetricsConstants.NAUTICAL_MILES_AND_FEET) {
				return useInchesInsteadOfFeet() ? R.string.inch : R.string.foot;
			}
			if (lm == MetricsConstants.MILES_AND_YARDS) {
				return useInchesInsteadOfYards() ? R.string.inch : R.string.yard;
			}
			return R.string.m;
		}
	}

	public float readSavedValue(@NonNull SizePreference preference) {
		Metric metric = preference.getMetric();
		float value = (float) Algorithms.parseDoubleSilently(preference.getValue(), 0.0f);
		if (value != 0.0f) {
			value += 0.0001f;
			if (preference.getSizeType().isWeightType()) {
				// Convert weight from tons to selected weight metric system
				value = VehicleAlgorithms.convertWeightFromTons(
						metric.getWeightMetric(), value, useKilogramsInsteadOfTons());
			} else {
				// Convert length from meters to selected length metric system
				value = VehicleAlgorithms.convertLengthFromMeters(
						metric.getLengthMetric(), value, useInchesInsteadOfFeet(), useInchesInsteadOfYards());
			}
		}
		return value;
	}

	public float prepareValueToSave(@NonNull SizePreference preference, float value) {
		if (value != 0.0f) {
			Metric metric = preference.getMetric();
			if (preference.getSizeType().isWeightType()) {
				// Convert weight to tons before save
				value = VehicleAlgorithms.convertWeightToTons(
						metric.getWeightMetric(), value, useKilogramsInsteadOfTons());
			} else {
				// Convert length to meters before save
				value = VehicleAlgorithms.convertLengthToMeters(
						metric.getLengthMetric(), value, useInchesInsteadOfFeet(), useInchesInsteadOfYards());
			}
			value -= 0.0001f;
		}
		return value;
	}

	@NonNull
	public List<ChipItem> collectChipItems(@NonNull OsmandApplication app,
	                                       @NonNull SizeType type, @NonNull Metric metric) {
		// Add "None"
		List<ChipItem> chips = new ArrayList<>();
		String none = app.getString(R.string.shared_string_none);
		ChipItem chip = new ChipItem(none);
		chip.title = none;
		chip.contentDescription = none;
		chip.tag = 0.0f;
		chips.add(chip);

		// Add other variants
		String metricShort = app.getString(getMetricShortStringId(type, metric));
		for (Float value : collectProposedValues(type, metric)) {
			String pattern = app.getString(R.string.ltr_or_rtl_combine_via_space);
			String valueStr = formatValue(value);
			String title = String.format(pattern, valueStr, metricShort);
			chip = new ChipItem(title);
			chip.title = title;
			chip.contentDescription = title;
			chip.tag = value;
			chips.add(chip);
		}
		return chips;
	}

	@NonNull
	private List<Float> collectProposedValues(@NonNull SizeType type, @NonNull Metric metric) {
		SizeData data = getSizeData(type);
		Limits<Float> limits = data.limits();
		if (type.isWeightType()) {
			limits = VehicleAlgorithms.convertWeightLimitsByMetricSystem(
					limits, metric.getWeightMetric(), useKilogramsInsteadOfTons());
		} else {
			limits = VehicleAlgorithms.convertLengthLimitsByMetricSystem(
					limits, metric.getLengthMetric(), useInchesInsteadOfFeet(), useInchesInsteadOfYards());
		}
		return VehicleAlgorithms.collectProposedValues(limits, 1, getMinProposedValuesCount());
	}

	protected int getMinProposedValuesCount() {
		return DEFAULT_PROPOSED_VALUES_COUNT;
	}

	public boolean useKilogramsInsteadOfTons() {
		return false;
	}

	protected boolean useInchesInsteadOfFeet() {
		return true;
	}

	protected boolean useInchesInsteadOfYards() {
		return true;
	}

	protected String formatValue(float value) {
		DecimalFormat formatter = new DecimalFormat("#.#");
		return formatter.format(value);
	}

	@Nullable
	public static VehicleSizes newInstance(@NonNull GeneralRouterProfile routerProfile,
	                                       @Nullable String derivedProfile) {
		if (routerProfile == GeneralRouterProfile.BOAT) {
			return new BoatSizes();
		} else if (routerProfile == GeneralRouterProfile.CAR) {
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

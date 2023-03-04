package net.osmand.plus.settings.vehiclesize;

import static net.osmand.plus.utils.OsmAndFormatter.FEET_IN_ONE_METER;
import static net.osmand.plus.utils.OsmAndFormatter.INCHES_IN_ONE_METER;
import static net.osmand.plus.utils.OsmAndFormatter.YARDS_IN_ONE_METER;

import androidx.annotation.NonNull;

import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.settings.enums.MetricsConstants;

import java.util.ArrayList;
import java.util.List;

public class VehicleAlgorithms {

	public static Limits convertLimitsByMetricSystem(@NonNull Limits limits,
	                                                 @NonNull MetricsConstants lengthMetricSystem,
	                                                 boolean useInchesInsteadOfFeet, boolean useInchesInsteadOfYards) {
		float min = limits.getMin();
		float max = limits.getMax();
		// Convert to appropriate length metric system
		min = convertLengthFromMeters(lengthMetricSystem, min, useInchesInsteadOfFeet, useInchesInsteadOfYards);
		max = convertLengthFromMeters(lengthMetricSystem, max, useInchesInsteadOfFeet, useInchesInsteadOfYards);
		if (lengthMetricSystem != MetricsConstants.KILOMETERS_AND_METERS) {
			// Round min / max
			int multiplier = 10;
			float scaledMin = min * multiplier;
			float scaledMax = max * multiplier;
			min = (float) (Math.floor(scaledMin)) / multiplier;
			max = (float) (Math.ceil(scaledMax)) / multiplier;
		}
		return new Limits(min, max);
	}

	public static List<Float> collectProposedValues(@NonNull Limits limits, int upscale,
	                                                int minValuesCount) {
		int multiplier = (int) Math.pow(10, upscale);
		int scaledMin = (int) (limits.getMin() * multiplier);
		int scaledMax = (int) (limits.getMax() * multiplier);

		// Find appropriate step size
		int maxScale = (int) Math.log10(scaledMax);
		int[] possibleSteps = collectPossibleSteps(maxScale);
		int step = (int) Math.pow(10, maxScale);
		for (int i = 0; i < possibleSteps.length; i++) {
			step = possibleSteps[i];
			int roundedMin = roundToMultipleOf(scaledMin, step, false);
			int roundedMax = roundToMultipleOf(scaledMax, step, true);
			int range = roundedMax - roundedMin;
			int count = (range / step) + 1;
			count += scaledMin % step != 0 ? 1 : 0;
			count += scaledMax % step != 0 ? 1 : 0;
			if (count >= minValuesCount) {
				break;
			}
		}

		// Collect proposed values
		int value = scaledMin;
		List<Float> result = new ArrayList<>();
		while (value <= scaledMax) {
			result.add((float) value / multiplier);
			if (value % step != 0) {
				value = roundToMultipleOf(scaledMin, step, false);
			} else {
				value += step;
			}
		}
		if (scaledMax % step != 0) {
			result.add((float) scaledMax / multiplier);
		}
		return result;
	}

	private static int roundToMultipleOf(int number, int multipleOf, boolean roundDown) {
		int result = (int) (Math.floor((number + (float) multipleOf / 2) / multipleOf) * multipleOf);
		if (roundDown && result > number) {
			result -= multipleOf;
		} else if (!roundDown && result < number) {
			result += multipleOf;
		}
		return result;
	}

	private static int[] collectPossibleSteps(int scale) {
		int[] coefficients = new int[]{5, 2, 1};
		int[] result = new int[scale * coefficients.length + 1];
		result[0] = (int) Math.pow(10, scale);
		scale--;
		int index = 0;
		while (scale >= 0) {
			for (int coefficient : coefficients) {
				result[++index] = (int) (Math.pow(10, scale) * coefficient);
			}
			scale--;
		}
		return result;
	}

	public static float convertLengthToMeters(@NonNull MetricsConstants mc, float value,
	                                          boolean useInchesInsteadOfFeet, boolean useInchesInsteadOfYards) {
		float resultValue;
		if (mc == MetricsConstants.MILES_AND_FEET || mc == MetricsConstants.NAUTICAL_MILES_AND_FEET) {
			resultValue = value / (useInchesInsteadOfFeet ? INCHES_IN_ONE_METER : FEET_IN_ONE_METER);
		} else if (mc == MetricsConstants.MILES_AND_YARDS) {
			resultValue = value / (useInchesInsteadOfYards ? INCHES_IN_ONE_METER : YARDS_IN_ONE_METER);
		} else {
			resultValue = value;
		}
		return resultValue;
	}

	public static float convertLengthFromMeters(@NonNull MetricsConstants mc, float valueInMeters,
	                                            boolean useInchesInsteadOfFeet, boolean useInchesInsteadOfYards) {
		float result;
		if (mc == MetricsConstants.MILES_AND_FEET || mc == MetricsConstants.NAUTICAL_MILES_AND_FEET) {
			result = valueInMeters * (useInchesInsteadOfFeet ? INCHES_IN_ONE_METER : FEET_IN_ONE_METER);
		} else if (mc == MetricsConstants.MILES_AND_YARDS) {
			result = valueInMeters * (useInchesInsteadOfYards ? INCHES_IN_ONE_METER : YARDS_IN_ONE_METER);
		} else {
			result = valueInMeters;
		}
		return result;
	}

}

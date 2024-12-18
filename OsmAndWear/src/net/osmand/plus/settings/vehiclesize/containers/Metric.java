package net.osmand.plus.settings.vehiclesize.containers;

import androidx.annotation.NonNull;

import net.osmand.shared.settings.enums.MetricsConstants;
import net.osmand.plus.settings.vehiclesize.WeightMetric;

public class Metric {

	private final WeightMetric weightMetric;
	private final MetricsConstants lengthMetric;

	public Metric(@NonNull WeightMetric weightMetric,
	              @NonNull MetricsConstants lengthMetric) {
		this.weightMetric = weightMetric;
		this.lengthMetric = lengthMetric;
	}

	@NonNull
	public WeightMetric getWeightMetric() {
		return weightMetric;
	}

	@NonNull
	public MetricsConstants getLengthMetric() {
		return lengthMetric;
	}

}

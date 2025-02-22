package net.osmand.plus.plugins.externalsensors.devices.sensors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.shared.settings.enums.MetricsConstants;
import net.osmand.plus.utils.OsmAndFormatter;

public class SensorDistanceWidgetDataField extends SensorWidgetDataField {

	public SensorDistanceWidgetDataField(int nameId, int unitNameId, @NonNull Number distanceValue) {
		super(SensorWidgetDataFieldType.BIKE_DISTANCE, nameId, unitNameId, distanceValue);
	}

	@Nullable
	@Override
	public OsmAndFormatter.FormattedValue getFormattedValue(@NonNull OsmandApplication app) {
		float distance = getNumberValue().floatValue();
		if (distance > 0) {
			return OsmAndFormatter.getFormattedDistanceValue(distance,
					app, OsmAndFormatter.OsmAndFormatterParams.NO_TRAILING_ZEROS);
		}
		return null;
	}
}
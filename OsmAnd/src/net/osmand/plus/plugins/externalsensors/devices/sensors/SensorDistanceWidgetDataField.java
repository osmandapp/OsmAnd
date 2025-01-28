package net.osmand.plus.plugins.externalsensors.devices.sensors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.FormattedValue;
import net.osmand.plus.utils.OsmAndFormatterParams;
import net.osmand.plus.utils.OsmAndFormatter;

public class SensorDistanceWidgetDataField extends SensorWidgetDataField {

	public SensorDistanceWidgetDataField(int nameId, int unitNameId, @NonNull Number distanceValue) {
		super(SensorWidgetDataFieldType.BIKE_DISTANCE, nameId, unitNameId, distanceValue);
	}

	@Nullable
	@Override
	public FormattedValue getFormattedValue(@NonNull OsmandApplication app) {
		float distance = getNumberValue().floatValue();
		if (distance > 0) {
			return OsmAndFormatter.getFormattedDistanceValue(distance,
					app, OsmAndFormatterParams.NO_TRAILING_ZEROS);
		}
		return null;
	}
}
package net.osmand.plus.plugins.externalsensors.devices.sensors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.OsmAndFormatter;

public class SensorSpeedWidgetDataField extends SensorWidgetDataField {

	public SensorSpeedWidgetDataField(int nameId, int unitNameId, @NonNull Number speedValue) {
		super(SensorWidgetDataFieldType.BIKE_SPEED, nameId, unitNameId, speedValue);
	}

	@Nullable
	@Override
	public OsmAndFormatter.FormattedValue getFormattedValue(@NonNull OsmandApplication app) {
		float speed = getNumberValue().floatValue();
		return speed > 0
				? OsmAndFormatter.getFormattedSpeedValue(speed, app)
				: null;
	}
}
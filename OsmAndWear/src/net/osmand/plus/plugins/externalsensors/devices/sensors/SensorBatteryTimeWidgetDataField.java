package net.osmand.plus.plugins.externalsensors.devices.sensors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.OsmAndFormatter;

public class SensorBatteryTimeWidgetDataField extends SensorWidgetDataField {

	public SensorBatteryTimeWidgetDataField(int nameId, int unitNameId, @NonNull Number distanceValue) {
		super(SensorWidgetDataFieldType.BATTERY, nameId, unitNameId, distanceValue);
	}

	@Nullable
	@Override
	public OsmAndFormatter.FormattedValue getFormattedValue(@NonNull OsmandApplication app) {
		long time = getNumberValue().intValue();
		if (time > 0) {
			return new OsmAndFormatter.FormattedValue(time, String.valueOf(time), app.getString(R.string.shared_string_sec));
		}
		return null;
	}
}
package net.osmand.plus.plugins.externalsensors.devices.sensors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public interface SensorData {
	@NonNull
	default List<SensorDataField> getDataFields() {
		return new ArrayList<>();
	}

	@NonNull
	default List<SensorDataField> getExtraDataFields() {
		return new ArrayList<>();
	}

	@Nullable
	default List<SensorWidgetDataField> getWidgetFields() {
		return null;
	}

	@Nullable
	default SensorWidgetDataField getWidgetField(@NonNull SensorWidgetDataFieldType fieldType) {
		List<SensorWidgetDataField> widgetFields = getWidgetFields();
		if (widgetFields != null) {
			for (SensorWidgetDataField widgetField : widgetFields) {
				if (widgetField.getFieldType() == fieldType) {
					return widgetField;
				}
			}
		}
		return null;
	}
}

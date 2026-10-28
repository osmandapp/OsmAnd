package net.osmand.plus.plugins.externalsensors.devices.sensors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public class SensorWidgetDataField extends SensorDataField {

	private final SensorWidgetDataFieldType fieldType;

	public SensorWidgetDataField(@NonNull SensorWidgetDataFieldType fieldType,
	                             @StringRes int nameId, @StringRes int unitNameId,
	                             @Nullable Number numberValue, @Nullable String stringValue) {
		super(nameId, unitNameId, numberValue, stringValue);
		this.fieldType = fieldType;
	}

	public SensorWidgetDataField(@NonNull SensorWidgetDataFieldType fieldType,
	                             @StringRes int nameId, @StringRes int unitNameId,
	                             @Nullable Number numberValue) {
		super(nameId, unitNameId, numberValue);
		this.fieldType = fieldType;
	}

	public SensorWidgetDataField(@NonNull SensorWidgetDataFieldType fieldType,
	                             @StringRes int nameId, @StringRes int unitNameId,
	                             @Nullable String stringValue) {
		super(nameId, unitNameId, stringValue);
		this.fieldType = fieldType;
	}

	@NonNull
	public SensorWidgetDataFieldType getFieldType() {
		return fieldType;
	}
}

package net.osmand.plus.plugins.externalsensors.devices.sensors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue;

public class SensorDataField {
	private final int nameId;
	private final int unitNameId;
	private final Number numberValue;
	private final String stringValue;

	public SensorDataField(@StringRes int nameId, @StringRes int unitNameId,
	                       @Nullable Number numberValue, @Nullable String stringValue) {
		this.nameId = nameId;
		this.numberValue = numberValue;
		this.stringValue = stringValue;
		this.unitNameId = unitNameId;
	}

	public SensorDataField(@StringRes int nameId, @StringRes int unitNameId,
	                       @Nullable Number numberValue) {
		this.nameId = nameId;
		this.numberValue = numberValue;
		this.stringValue = null;
		this.unitNameId = unitNameId;
	}

	public SensorDataField(@StringRes int nameId, @StringRes int unitNameId,
	                       @Nullable String stringValue) {
		this.nameId = nameId;
		this.numberValue = null;
		this.stringValue = stringValue;
		this.unitNameId = unitNameId;
	}

	@StringRes
	public int getNameId() {
		return nameId;
	}

	public Number getNumberValue() {
		return numberValue;
	}

	public String getStringValue() {
		return stringValue;
	}

	@StringRes
	public int getUnitNameId() {
		return unitNameId;
	}

	@Nullable
	public FormattedValue getFormattedValue(@NonNull OsmandApplication app) {
		if (numberValue == null && stringValue == null) {
			return null;
		}
		float number = numberValue != null ? numberValue.floatValue() : 0;
		String value = stringValue;
		if (value == null) {
			value = numberValue.toString();
		}
		String unitName = unitNameId != -1 ? app.getString(unitNameId) : null;
		return new FormattedValue(number, value, unitName);
	}
}

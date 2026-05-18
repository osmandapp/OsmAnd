package net.osmand.plus.plugins.externalsensors.devices.sensors;

import androidx.annotation.NonNull;

public enum SensorPressureUnit {
	UNKNOWN(-1),
	MMHG(0),
	KPA(1);

	private final int unitId;

	SensorPressureUnit(int unitId) {
		this.unitId = unitId;
	}

	@NonNull
	public static SensorPressureUnit getUnitById(int unitId) {
		for (SensorPressureUnit unit : values()) {
			if (unit.unitId == unitId) {
				return unit;
			}
		}
		return UNKNOWN;
	}
}

package net.osmand.plus.plugins.externalsensors.devices.sensors.ant;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dsi.ant.plugins.antplus.pccbase.AntPlusCommonPcc;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusLegacyCommonPcc;

import net.osmand.plus.R;
import net.osmand.plus.plugins.externalsensors.devices.ant.AntCommonDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorBatteryTimeWidgetDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;

public class AntBatterySensor<T extends AntPlusCommonPcc> extends AntAbstractSensor<T> {

	private BatteryData batteryData;

	public static class BatteryData implements SensorData {

		// The estimated timestamp of when this event was triggered.
		// Useful for correlating multiple events and determining when data was sent for more accurate data records.
		private final long timestamp;

		private final long cumulativeOperatingTime;

		public BatteryData(long timestamp, long cumulativeOperatingTime) {
			this.timestamp = timestamp;
			this.cumulativeOperatingTime = cumulativeOperatingTime;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public double getBatteryTime() {
			return cumulativeOperatingTime;
		}

		@NonNull
		@Override
		public List<SensorDataField> getDataFields() {
			return Collections.singletonList(
					new SensorBatteryTimeWidgetDataField(R.string.map_widget_battery, -1, cumulativeOperatingTime));
		}

		@NonNull
		@Override
		public List<SensorDataField> getExtraDataFields() {
			return Collections.singletonList(
					new SensorDataField(R.string.shared_string_time, -1, timestamp));
		}

		@Nullable
		@Override
		public List<SensorWidgetDataField> getWidgetFields() {
			return Collections.singletonList(
					new SensorBatteryTimeWidgetDataField(R.string.shared_string_time, -1, cumulativeOperatingTime));
		}

		@NonNull
		@Override
		public String toString() {
			return "BatteryData {" +
					"timestamp=" + timestamp +
					", batteryTime=" + cumulativeOperatingTime +
					'}';
		}
	}

	public AntBatterySensor(@NonNull AntCommonDevice<T> device) {
		super(device, device.getDeviceId() + "_battery_time");
	}

	public AntBatterySensor(@NonNull AntCommonDevice<T> device, @NonNull String sensorId) {
		super(device, sensorId);
	}

	@NonNull
	@Override
	public String getName() {
		return "Battery time";
	}

	@NonNull
	@Override
	public List<SensorWidgetDataFieldType> getSupportedWidgetDataFieldTypes() {
		return Collections.emptyList();
	}

	@Nullable
	@Override
	public List<SensorData> getLastSensorDataList() {
		return Collections.singletonList(batteryData);
	}

	@Override
	public void subscribeToEvents() {
		AntPlusCommonPcc pcc = getAntDevice().getPcc();
		if(pcc != null) {
			pcc.subscribeBatteryStatusEvent((estTimestamp,
			                                 eventFlags, cumulativeOperatingTime,
			                                 batteryVoltage, batteryStatus,
			                                 cumulativeOperatingTimeResolution, numberOfBatteries,
			                                 batteryIdentifier) -> {
				batteryData = new BatteryData(estTimestamp, cumulativeOperatingTime);
			});
		}
	}

	@Override
	public void writeSensorDataToJson(@NonNull JSONObject json, @NonNull SensorWidgetDataFieldType widgetDataFieldType) throws JSONException {
	}

}
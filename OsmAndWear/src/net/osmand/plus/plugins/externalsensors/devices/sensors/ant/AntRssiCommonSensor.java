package net.osmand.plus.plugins.externalsensors.devices.sensors.ant;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dsi.ant.plugins.antplus.pccbase.AntPlusCommonPcc;

import net.osmand.plus.R;
import net.osmand.plus.plugins.externalsensors.devices.ant.AntCommonDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;

public class AntRssiCommonSensor<T extends AntPlusCommonPcc> extends AntAbstractSensor<T> {

	private RssiData rssiData;

	public static class RssiData implements SensorData {

		// The estimated timestamp of when this event was triggered.
		// Useful for correlating multiple events and determining when data was sent for more accurate data records.
		private final long timestamp;

		private final double rssi;

		public RssiData(long timestamp, double rssi) {
			this.timestamp = timestamp;
			this.rssi = rssi;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public double getRssi() {
			return rssi;
		}

		@NonNull
		@Override
		public List<SensorDataField> getDataFields() {
			return Collections.singletonList(
					new SensorDataField(R.string.map_widget_rssi, -1, rssi));
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
			return Collections.emptyList();
		}

		@NonNull
		@Override
		public String toString() {
			return "RssiData {" +
					"timestamp=" + timestamp +
					", rssi=" + rssi +
					'}';
		}
	}

	@Nullable
	public RssiData getRssiData() {
		return rssiData;
	}

	public AntRssiCommonSensor(@NonNull AntCommonDevice<T> device) {
		super(device, device.getDeviceId() + "_rssi");
	}

	public AntRssiCommonSensor(@NonNull AntCommonDevice<T> device, @NonNull String sensorId) {
		super(device, sensorId);
	}

	@NonNull
	@Override
	public String getName() {
		return "Rssi level";
	}

	@NonNull
	@Override
	public List<SensorWidgetDataFieldType> getSupportedWidgetDataFieldTypes() {
		return Collections.emptyList();
	}

	@Nullable
	@Override
	public List<SensorData> getLastSensorDataList() {
		return Collections.singletonList(rssiData);
	}

	@Override
	public void subscribeToEvents() {
		AntPlusCommonPcc pcc = getAntDevice().getPcc();
		if (pcc != null) {
			pcc.subscribeRssiEvent(null);
			pcc.subscribeRssiEvent((estTimestamp, enumSet, rssi) -> {
				rssiData = new RssiData(estTimestamp, rssi);
				getDevice().fireSensorDataEvent(AntRssiCommonSensor.this, rssiData);
			});
		}
	}

	@Override
	public void writeSensorDataToJson(@NonNull JSONObject json, @NonNull SensorWidgetDataFieldType widgetDataFieldType) throws JSONException {
	}
}
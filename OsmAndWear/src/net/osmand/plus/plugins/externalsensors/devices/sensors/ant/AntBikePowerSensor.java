package net.osmand.plus.plugins.externalsensors.devices.sensors.ant;

import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_BIKE_POWER;
import static net.osmand.util.Algorithms.DECIMAL_FORMAT;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.externalsensors.devices.ant.AntBikePowerDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;

public class AntBikePowerSensor extends AntAbstractSensor<AntPlusBikePowerPcc> {

	private BikePowerData lastBikePowerData;

	public static class BikePowerData implements SensorData {

		// The estimated timestamp of when this event was triggered.
		private final long timestamp;

		// The average power calculated from sensor data. Units: W.
		private final double calculatedPower;

		private final boolean powerOnlyData;
		private final boolean initialPowerOnlyData;

		private static class BikePowerDataField extends SensorWidgetDataField {

			public BikePowerDataField(int nameId, int unitNameId, @NonNull Number powerValue) {
				super(SensorWidgetDataFieldType.BIKE_POWER, nameId, unitNameId, powerValue);
			}

			@Nullable
			@Override
			public FormattedValue getFormattedValue(@NonNull OsmandApplication app) {
				float power = getNumberValue().floatValue();
				return power > 0
						? new FormattedValue(power, String.valueOf(power), "W")
						: null;
			}
		}

		BikePowerData(long timestamp,
		              double calculatedPower,
		              boolean powerOnlyData,
		              boolean initialPowerOnlyData) {
			this.timestamp = timestamp;
			this.calculatedPower = calculatedPower;
			this.powerOnlyData = powerOnlyData;
			this.initialPowerOnlyData = initialPowerOnlyData;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public double getCalculatedPower() {
			return calculatedPower;
		}

		public boolean isPowerOnlyData() {
			return powerOnlyData;
		}

		public boolean isInitialPowerOnlyData() {
			return initialPowerOnlyData;
		}

		@NonNull
		@Override
		public List<SensorDataField> getDataFields() {
			return Collections.singletonList(new BikePowerDataField(R.string.map_widget_ant_bicycle_power, -1, calculatedPower));
		}

		@NonNull
		@Override
		public List<SensorDataField> getExtraDataFields() {
			return Collections.singletonList(new SensorDataField(R.string.shared_string_time, -1, timestamp));
		}

		@Nullable
		@Override
		public List<SensorWidgetDataField> getWidgetFields() {
			return Collections.singletonList(new BikePowerDataField(R.string.map_widget_ant_bicycle_power, -1, calculatedPower));
		}

		@NonNull
		@Override
		public String toString() {
			return "BikePowerData {" +
					"timestamp=" + timestamp +
					", calculatedPower=" + calculatedPower +
					", powerOnlyData=" + powerOnlyData +
					", initialPowerOnlyData=" + initialPowerOnlyData +
					'}';
		}
	}

	public AntBikePowerSensor(@NonNull AntBikePowerDevice device) {
		super(device, device.getDeviceId() + "_power");
	}

	public AntBikePowerSensor(@NonNull AntBikePowerDevice device, @NonNull String sensorId) {
		super(device, sensorId);
	}

	@NonNull
	@Override
	public String getName() {
		return "Bicycle Power";
	}

	@NonNull
	@Override
	public List<SensorWidgetDataFieldType> getSupportedWidgetDataFieldTypes() {
		return Collections.singletonList(SensorWidgetDataFieldType.BIKE_POWER);
	}

	@Nullable
	@Override
	public List<SensorData> getLastSensorDataList() {
		return Collections.singletonList(lastBikePowerData);
	}

	@Override
	public void subscribeToEvents() {
		AntPlusBikePowerPcc pcc = getAntDevice().getPcc();
		if (pcc != null) {
			pcc.subscribeRssiEvent(null);
			pcc.subscribeCalculatedPowerEvent((estTimestamp, eventFlags, dataSource, calculatedPower) -> {
				boolean powerOnlyData = AntPlusBikePowerPcc.DataSource.POWER_ONLY_DATA.equals(dataSource);
				boolean initialPowerOnlyData = AntPlusBikePowerPcc.DataSource.INITIAL_VALUE_POWER_ONLY_DATA.equals(dataSource);
				lastBikePowerData = new BikePowerData(estTimestamp, calculatedPower.doubleValue(),
						powerOnlyData, initialPowerOnlyData);
				getDevice().fireSensorDataEvent(this, lastBikePowerData);
			});
		}
	}

	@Override
	public void writeSensorDataToJson(@NonNull JSONObject json, @NonNull SensorWidgetDataFieldType widgetDataFieldType) throws JSONException {
		BikePowerData data = lastBikePowerData;
		double calculatedPower = data != null ? data.getCalculatedPower() : 0;
		if (calculatedPower > 0) {
			json.put(SENSOR_TAG_BIKE_POWER, DECIMAL_FORMAT.format(calculatedPower));
		}
	}
}
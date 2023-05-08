package net.osmand.plus.plugins.externalsensors.devices.sensors.ant;

import static net.osmand.gpx.GPXUtilities.DECIMAL_FORMAT;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeSpeedDistancePcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeSpeedDistancePcc.CalculatedSpeedReceiver;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;

import net.osmand.plus.R;
import net.osmand.plus.plugins.externalsensors.devices.ant.AntBikeSpeedDistanceDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorSpeedWidgetDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public class AntBikeSpeedSensor extends AntAbstractSensor<AntPlusBikeSpeedDistancePcc> {

	private static final double WHEEL_CIRCUMFERENCE = 2.1; //The wheel circumference in meters, used to calculate speed

	private BikeSpeedData lastBikeSpeedData;

	public static class BikeSpeedData implements SensorData {

		// The estimated timestamp of when this event was triggered.
		// Useful for correlating multiple events and determining when data was sent for more accurate data records.
		private final long timestamp;

		// The speed calculated from the raw values in the sensor broadcast, based on this classes' set wheel circumference passed to the constructor. Units: m/s.
		private final double calculatedSpeed;

		public BikeSpeedData(long timestamp, double calculatedSpeed) {
			this.timestamp = timestamp;
			this.calculatedSpeed = calculatedSpeed;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public double getCalculatedSpeed() {
			return calculatedSpeed;
		}

		@NonNull
		@Override
		public List<SensorDataField> getDataFields() {
			return Collections.singletonList(
					new SensorDataField(R.string.map_widget_ant_bicycle_speed, -1, calculatedSpeed));
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
					new SensorSpeedWidgetDataField(R.string.map_widget_ant_bicycle_speed, -1, calculatedSpeed));
		}

		@NonNull
		@Override
		public String toString() {
			return "BikeSpeedData {" +
					"timestamp=" + timestamp +
					", calculatedSpeed=" + calculatedSpeed +
					'}';
		}
	}

	public AntBikeSpeedSensor(@NonNull AntBikeSpeedDistanceDevice device) {
		super(device, device.getDeviceId() + "_speed");
	}

	public AntBikeSpeedSensor(@NonNull AntBikeSpeedDistanceDevice device, @NonNull String sensorId) {
		super(device, sensorId);
	}

	@NonNull
	@Override
	public String getName() {
		return "Bicycle Speed";
	}

	@NonNull
	@Override
	public List<SensorWidgetDataFieldType> getSupportedWidgetDataFieldTypes() {
		return Collections.singletonList(SensorWidgetDataFieldType.BIKE_SPEED);
	}

	@Nullable
	@Override
	public List<SensorData> getLastSensorDataList() {
		return Collections.singletonList(lastBikeSpeedData);
	}

	@Override
	public void subscribeToEvents() {
		getAntDevice().getPcc().subscribeCalculatedSpeedEvent(new CalculatedSpeedReceiver(new BigDecimal(WHEEL_CIRCUMFERENCE)) {
			@Override
			public void onNewCalculatedSpeed(long estTimestamp, EnumSet<EventFlag> enumSet, BigDecimal calculatedSpeed) {
				lastBikeSpeedData = new BikeSpeedData(estTimestamp, calculatedSpeed.doubleValue());
				getDevice().fireSensorDataEvent(AntBikeSpeedSensor.this, lastBikeSpeedData);
			}
		});
	}

	@Override
	public void writeSensorDataToJson(@NonNull JSONObject json) throws JSONException {
		BikeSpeedData data = lastBikeSpeedData;
		double calculatedSpeed = data != null ? data.getCalculatedSpeed() : 0;
		if (calculatedSpeed > 0) {
			json.put(getSensorId(), DECIMAL_FORMAT.format(calculatedSpeed));
		}
	}
}

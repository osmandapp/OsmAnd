package net.osmand.plus.plugins.externalsensors.devices.sensors.ant;

import static net.osmand.util.Algorithms.DECIMAL_FORMAT;
import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_DISTANCE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeSpeedDistancePcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeSpeedDistancePcc.CalculatedAccumulatedDistanceReceiver;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;

import net.osmand.plus.R;
import net.osmand.plus.plugins.externalsensors.devices.ant.AntBikeSpeedDistanceDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorDistanceWidgetDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public class AntBikeDistanceSensor extends AntAbstractSensor<AntPlusBikeSpeedDistancePcc> {

	private static final double WHEEL_CIRCUMFERENCE = 2.1; //The wheel circumference in meters, used to calculate distance

	private BikeDistanceData lastBikeDistanceData;

	public static class BikeDistanceData implements SensorData {

		// The estimated timestamp of when this event was triggered.
		// Useful for correlating multiple events and determining when data was sent for more accurate data records.
		private final long timestamp;

		// The accumulated distance calculated from the raw values in the sensor broadcast since the sensor was first connected, based on this classes' set wheel circumference passed to the constructor.
		private final double accumulatedDistance;

		public BikeDistanceData(long timestamp, double accumulatedDistance) {
			this.timestamp = timestamp;
			this.accumulatedDistance = accumulatedDistance;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public double getAccumulatedDistance() {
			return accumulatedDistance;
		}

		@NonNull
		@Override
		public List<SensorDataField> getDataFields() {
			return Collections.singletonList(
					new SensorDistanceWidgetDataField(R.string.map_widget_ant_bicycle_dist, -1, accumulatedDistance));
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
					new SensorDistanceWidgetDataField(R.string.map_widget_ant_bicycle_dist, -1, accumulatedDistance));
		}

		@NonNull
		@Override
		public String toString() {
			return "BikeDistanceData {" +
					"timestamp=" + timestamp +
					", calculatedAccumulatedDistance=" + accumulatedDistance +
					'}';
		}
	}

	public AntBikeDistanceSensor(@NonNull AntBikeSpeedDistanceDevice device) {
		super(device, device.getDeviceId() + "_distance");
	}

	public AntBikeDistanceSensor(@NonNull AntBikeSpeedDistanceDevice device, @NonNull String sensorId) {
		super(device, sensorId);
	}

	@NonNull
	@Override
	public String getName() {
		return "Bicycle Distance";
	}

	private AntBikeSpeedDistanceDevice getBikeSpeedDistanceDevice() {
		return (AntBikeSpeedDistanceDevice) device;
	}

	@NonNull
	@Override
	public List<SensorWidgetDataFieldType> getSupportedWidgetDataFieldTypes() {
		return Collections.singletonList(SensorWidgetDataFieldType.BIKE_DISTANCE);
	}

	@Nullable
	@Override
	public List<SensorData> getLastSensorDataList() {
		return Collections.singletonList(lastBikeDistanceData);
	}

	@Override
	public void subscribeToEvents() {
		AntPlusBikeSpeedDistancePcc pcc = getAntDevice().getPcc();
		if (pcc != null) {
			pcc.subscribeCalculatedAccumulatedDistanceEvent(null);
			pcc.subscribeCalculatedAccumulatedDistanceEvent(new CalculatedAccumulatedDistanceReceiver(BigDecimal.valueOf(getBikeSpeedDistanceDevice().getWheelCircumference())) {
				@Override
				public void onNewCalculatedAccumulatedDistance(long estTimestamp, EnumSet<EventFlag> eventFlags, BigDecimal accumulatedDistance) {
					lastBikeDistanceData = new BikeDistanceData(estTimestamp, accumulatedDistance.doubleValue());
					getDevice().fireSensorDataEvent(AntBikeDistanceSensor.this, lastBikeDistanceData);
				}
			});
		}
	}

	@Override
	public void writeSensorDataToJson(@NonNull JSONObject json, @NonNull SensorWidgetDataFieldType widgetDataFieldType) throws JSONException {
		BikeDistanceData data = lastBikeDistanceData;
		double accumulatedDistance = data != null ? data.getAccumulatedDistance() : 0;
		if (accumulatedDistance > 0) {
			json.put(SENSOR_TAG_DISTANCE, DECIMAL_FORMAT.format(accumulatedDistance));
		}
	}
}

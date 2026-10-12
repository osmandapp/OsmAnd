package net.osmand.plus.plugins.externalsensors.devices.sensors.ant;

import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_HEART_RATE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;

import net.osmand.plus.R;
import net.osmand.plus.plugins.externalsensors.devices.ant.AntHeartRateDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class AntHeartRateSensor extends AntAbstractSensor<AntPlusHeartRatePcc> {

	private HeartRateData lastHeartRateData;

	public static class HeartRateData implements SensorData {

		private final boolean sensorConnected;

		// The estimated timestamp of when this event was triggered.
		private final long timestamp;

		// Current heart rate valid for display, computed by sensor. Units: BPM
		private final int computedHeartRate;
		private final boolean computedHeartRateInitial;

		// Heart beat count. Units: beats
		private final long heartBeatCount;
		private final boolean heartBeatCountInitial;

		// Sensor reported time counter value of last distance or speed computation (up to 1/1024s accuracy). Units: s
		private final BigDecimal heartBeatEventTime;
		private final boolean heartBeatEventTimeInitial;

		HeartRateData(boolean sensorConnected, long timestamp,
		              int computedHeartRate, boolean computedHeartRateInitial,
		              long heartBeatCount, boolean heartBeatCountInitial,
		              BigDecimal heartBeatEventTime, boolean heartBeatEventTimeInitial) {
			this.sensorConnected = sensorConnected;
			this.timestamp = timestamp;
			this.computedHeartRate = computedHeartRate;
			this.computedHeartRateInitial = computedHeartRateInitial;
			this.heartBeatCount = heartBeatCount;
			this.heartBeatCountInitial = heartBeatCountInitial;
			this.heartBeatEventTime = heartBeatEventTime;
			this.heartBeatEventTimeInitial = heartBeatEventTimeInitial;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public int getComputedHeartRate() {
			return computedHeartRate;
		}

		public boolean isComputedHeartRateInitial() {
			return computedHeartRateInitial;
		}

		public long getHeartBeatCount() {
			return heartBeatCount;
		}

		public boolean isHeartBeatCountInitial() {
			return heartBeatCountInitial;
		}

		public BigDecimal getHeartBeatEventTime() {
			return heartBeatEventTime;
		}

		public boolean isHeartBeatEventTimeInitial() {
			return heartBeatEventTimeInitial;
		}

		@NonNull
		@Override
		public List<SensorDataField> getDataFields() {
			return Collections.singletonList(new SensorDataField(R.string.map_widget_ant_heart_rate, R.string.beats_per_minute_short,
					computedHeartRate, computedHeartRate + (computedHeartRateInitial ? "*" : "")));
		}

		@NonNull
		@Override
		public List<SensorDataField> getExtraDataFields() {
			return Collections.singletonList(new SensorDataField(R.string.shared_string_time, -1, timestamp));
		}

		@Nullable
		@Override
		public List<SensorWidgetDataField> getWidgetFields() {
			int computedHeartRate = sensorConnected ? this.computedHeartRate : 0;
			if (computedHeartRate > 0) {
				return Collections.singletonList(new SensorWidgetDataField(
						SensorWidgetDataFieldType.HEART_RATE, R.string.map_widget_ant_heart_rate, R.string.beats_per_minute_short,
						computedHeartRate, computedHeartRate + (computedHeartRateInitial ? "*" : "")));
			} else {
				return Collections.singletonList(new SensorWidgetDataField(
						SensorWidgetDataFieldType.HEART_RATE, R.string.map_widget_ant_heart_rate, R.string.beats_per_minute_short, computedHeartRate));
			}
		}

		@NonNull
		@Override
		public String toString() {
			return "HeartRateData {" +
					"timestamp=" + timestamp +
					", computedHeartRate=" + computedHeartRate +
					", computedHeartRateInitial=" + computedHeartRateInitial +
					", heartBeatCount=" + heartBeatCount +
					", heartBeatCountInitial=" + heartBeatCountInitial +
					", heartBeatEventTime=" + heartBeatEventTime +
					", heartBeatEventTimeInitial=" + heartBeatEventTimeInitial +
					'}';
		}
	}

	public AntHeartRateSensor(@NonNull AntHeartRateDevice device) {
		super(device, device.getDeviceId() + "_heart_rate");
	}

	public AntHeartRateSensor(@NonNull AntHeartRateDevice device, @NonNull String sensorId) {
		super(device, sensorId);
	}

	@NonNull
	@Override
	public String getName() {
		return "Heart Rate";
	}

	@NonNull
	@Override
	public List<SensorWidgetDataFieldType> getSupportedWidgetDataFieldTypes() {
		return Collections.singletonList(SensorWidgetDataFieldType.HEART_RATE);
	}

	@Nullable
	@Override
	public List<SensorData> getLastSensorDataList() {
		return Collections.singletonList(lastHeartRateData);
	}

	@Override
	public void subscribeToEvents() {
		AntPlusHeartRatePcc pcc = getAntDevice().getPcc();
		if (pcc != null) {
			pcc.subscribeHeartRateDataEvent(null);
			pcc.subscribeHeartRateDataEvent((estTimestamp, eventFlags, computedHeartRate, heartBeatCount, heartBeatEventTime, dataState) -> {
				boolean zeroState = AntPlusHeartRatePcc.DataState.ZERO_DETECTED.equals(dataState);
				boolean initialState = AntPlusHeartRatePcc.DataState.INITIAL_VALUE.equals(dataState);

				lastHeartRateData = new HeartRateData(getDevice().isConnected(), estTimestamp, computedHeartRate, zeroState,
						heartBeatCount, initialState, heartBeatEventTime, initialState);
				getDevice().fireSensorDataEvent(AntHeartRateSensor.this, lastHeartRateData);
			});
		}
	}

	@Override
	public void writeSensorDataToJson(@NonNull JSONObject json, @NonNull SensorWidgetDataFieldType widgetDataFieldType) throws JSONException {
		HeartRateData data = lastHeartRateData;
		int computedHeartRate = data != null ? data.getComputedHeartRate() : 0;
		if (computedHeartRate > 0) {
			json.put(SENSOR_TAG_HEART_RATE, computedHeartRate);
		}
	}
}

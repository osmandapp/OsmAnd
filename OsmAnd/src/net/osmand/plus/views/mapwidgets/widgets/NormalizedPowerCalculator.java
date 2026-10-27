package net.osmand.plus.views.mapwidgets.widgets;

import androidx.annotation.Nullable;

import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.externalsensors.ExternalSensorsPlugin;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.AbstractSensor;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Singleton class for calculating Normalized Power (NP) using Andrew Coggan's method.
 * 
 * Normalized Power calculation:
 *  1. Compute a rolling 30-second average of power.
 *  2. Raise each 30-second average to the 4th power.
 *  3. Average all of those values over the ENTIRE ride.
 *  4. Take the 4th root: NP = avg(rollingAvg30s^4)^(1/4).
 *
 * This singleton allows multiple widgets (NPWidget, IFWidget, etc.) to access
 * the same NP calculation without duplicating the logic.
 *
 * Thread-safe through synchronized access to shared state.
 */
public class NormalizedPowerCalculator {

	private static final int NP_WINDOW_SECONDS = 30;
	private static final long NP_WINDOW_MS = NP_WINDOW_SECONDS * 1000L;

	/**
	 * A new 30-second rolling average is contributed at most once per second.
	 * Processing faster than this would add the same sensor value multiple
	 * times and bias the aggregates.
	 */
	private static final long MIN_SAMPLE_INTERVAL_MS = 1000L;

	/**
	 * Tolerance applied to the full-window check. Widget updates are driven by
	 * map refresh ticks and carry jitter, so samples are never spaced at
	 * exactly 1 s. Without a tolerance, the pruning rule (evict when older
	 * than the window) and the full-window check (contribute when the window
	 * span reaches exactly NP_WINDOW_MS) can never both be satisfied, and no
	 * average is ever contributed.
	 */
	private static final long WINDOW_FILL_TOLERANCE_MS = 2000L;

	/**
	 * Minimum number of 30-second averages that must be accumulated before
	 * a value is displayed. Prevents showing a meaningless spike during the
	 * first seconds of a ride.
	 */
	private static final int MIN_AVERAGES_FOR_DISPLAY = 5;

	private static volatile NormalizedPowerCalculator instance;
	private static final Object lock = new Object();

	private final ExternalSensorsPlugin externalSensorsPlugin;

	/** Power samples within the current 30-second window, oldest first. */
	private final Deque<PowerSample> windowSamples = new ArrayDeque<>();

	/** Running sum of power within the window; avoids an O(n) sum per update. */
	private double windowPowerSum = 0;

	/** Ride-wide running sum of (30-second rolling average)^4. */
	private double sumRollingAvgPow4 = 0;

	/** Ride-wide count of contributed 30-second rolling averages. */
	private long rollingAvgCount = 0;

	/** Timestamp of the last processed sample, in milliseconds. */
	private long lastSampleTime = 0;

	private static class PowerSample {
		final long timestamp;
		final float power;

		PowerSample(long timestamp, float power) {
			this.timestamp = timestamp;
			this.power = power;
		}
	}

	private NormalizedPowerCalculator() {
		this.externalSensorsPlugin = PluginsHelper.getPlugin(ExternalSensorsPlugin.class);
	}

	/**
	 * Returns the singleton instance of NormalizedPowerCalculator.
	 * Uses double-checked locking for thread-safe lazy initialization.
	 */
	@Nullable
	public static NormalizedPowerCalculator getInstance() {
		if (instance == null) {
			synchronized (lock) {
				if (instance == null) {
					instance = new NormalizedPowerCalculator();
				}
			}
		}
		return instance;
	}

	/**
	 * Updates the NP calculation with the latest power sample.
	 * Should be called periodically (approximately once per second).
	 * 
	 * @return the current normalized power value, or 0 if not enough data
	 */
	public float updateAndGetNormalizedPower() {
		Float currentPower = getCurrentPower();

		if (currentPower != null && currentPower > 0) {
			long now = System.currentTimeMillis();

			// One sample per second is sufficient; also guards against
			// processing the same sensor value more than once per update cycle.
			if (now - lastSampleTime >= MIN_SAMPLE_INTERVAL_MS) {
				lastSampleTime = now;
				addSample(now, currentPower);
				updateRollingAverage(now);
			}

			return calculateNormalizedPower();
		}
		return 0f;
	}

	/**
	 * Returns the current normalized power value without updating.
	 * 
	 * @return the current normalized power value, or 0 if not enough data
	 */
	public float getNormalizedPower() {
		return calculateNormalizedPower();
	}

	/**
	 * Resets the NP calculation. Call this when starting a new ride.
	 */
	public void reset() {
		synchronized (this) {
			windowSamples.clear();
			windowPowerSum = 0;
			sumRollingAvgPow4 = 0;
			rollingAvgCount = 0;
			lastSampleTime = 0;
		}
	}

	/**
	 * Returns the current 30-second rolling average power.
	 * 
	 * @return the rolling average, or 0 if not enough data
	 */
	public float getRollingAverage() {
		synchronized (this) {
			if (windowSamples.isEmpty()) {
				return 0f;
			}
			return (float) (windowPowerSum / windowSamples.size());
		}
	}

	/**
	 * Returns the current power reading from the sensor.
	 * 
	 * @return the latest power reading in watts, or null if no sensor available
	 */
	@Nullable
	public Float getCurrentPower() {
		if (externalSensorsPlugin == null) {
			return null;
		}

		List<AbstractDevice<?>> powerDevices = externalSensorsPlugin.getPairedDevicesByWidgetType(
				SensorWidgetDataFieldType.BIKE_POWER);
		if (powerDevices == null || powerDevices.isEmpty()) {
			return null;
		}

		// Pick the first connected paired device.
		AbstractDevice<?> device = null;
		for (AbstractDevice<?> d : powerDevices) {
			if (d.isConnected()) {
				device = d;
				break;
			}
		}
		if (device == null) {
			return null;
		}

		// Pick the sensor on that device that provides bike power.
		AbstractSensor powerSensor = null;
		for (AbstractSensor sensor : device.getSensors()) {
			if (sensor.getSupportedWidgetDataFieldTypes().contains(SensorWidgetDataFieldType.BIKE_POWER)) {
				powerSensor = sensor;
				break;
			}
		}
		if (powerSensor == null) {
			return null;
		}

		List<SensorData> sensorDataList = powerSensor.getLastSensorDataList();
		if (sensorDataList == null || sensorDataList.isEmpty()) {
			return null;
		}

		// Extract the most recent power value from the sensor's data fields.
		for (SensorData sensorData : sensorDataList) {
			if (sensorData != null) {
				List<SensorWidgetDataField> widgetFields = sensorData.getWidgetFields();
				if (widgetFields != null) {
					for (SensorWidgetDataField field : widgetFields) {
						if (field != null && field.getFieldType() == SensorWidgetDataFieldType.BIKE_POWER) {
							Number powerValue = field.getNumberValue();
							if (powerValue != null) {
								return powerValue.floatValue();
							}
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Appends a sample to the window and evicts samples that have fallen out
	 * of the 30-second window.
	 */
	private void addSample(long now, float power) {
		synchronized (this) {
			windowSamples.addLast(new PowerSample(now, power));
			windowPowerSum += power;

			long cutoff = now - NP_WINDOW_MS;
			while (!windowSamples.isEmpty() && windowSamples.peekFirst().timestamp < cutoff) {
				PowerSample old = windowSamples.removeFirst();
				windowPowerSum -= old.power;
			}
		}
	}

	/**
	 * Computes the current 30-second rolling average and adds its 4th power
	 * to the ride-wide aggregates. In Coggan's method this step runs once per
	 * second for the whole ride, so the displayed value converges gradually.
	 */
	private void updateRollingAverage(long now) {
		synchronized (this) {
			if (windowSamples.isEmpty()) {
				return;
			}
			// Actual time span covered by the window (shorter than 30 s at ride start).
			double windowSpanMs = now - windowSamples.peekFirst().timestamp + 1;
			double rollingAverage = windowPowerSum / windowSamples.size();

			// Contribute once the window is (nearly) full. The tolerance absorbs
			// jitter in the update cadence; partial windows would overweight the
			// very first seconds of the ride.
			if (windowSpanMs >= NP_WINDOW_MS - WINDOW_FILL_TOLERANCE_MS) {
				sumRollingAvgPow4 += Math.pow(rollingAverage, 4);
				rollingAvgCount++;
			}
		}
	}

	/** @return Normalized Power, or 0 if not enough data has accumulated yet. */
	private float calculateNormalizedPower() {
		synchronized (this) {
			if (rollingAvgCount < MIN_AVERAGES_FOR_DISPLAY) {
				return 0f;
			}
			double avgPow4 = sumRollingAvgPow4 / rollingAvgCount;
			return (float) Math.pow(avgPow4, 0.25);
		}
	}
}

package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.NP;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.externalsensors.ExternalSensorsPlugin;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.AbstractSensor;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Live Normalized Power (NP) widget, implementing Andrew Coggan's method:
 *
 *  1. Compute a rolling 30-second average of power.
 *  2. Raise each 30-second average to the 4th power.
 *  3. Average all of those values over the ENTIRE ride.
 *  4. Take the 4th root: NP = avg(rollingAvg30s^4)^(1/4).
 *
 * This differs from a plain power average: the 4th power makes variable,
 * high-intensity efforts weigh more than steady riding at the same average
 * power. Because the aggregates accumulate over the whole ride, the value
 * rises during hard efforts and only decays slowly during recovery — unlike
 * a short-window statistic, it reflects accumulated load.
 *
 * Implementation notes: a sliding deque holds the power samples of the current
 * 30-second window, with a running sum so the rolling average is O(1) per
 * update. Ride-wide aggregates ({@link #sumRollingAvgPow4} and
 * {@link #rollingAvgCount}) are updated once per second. Memory usage is
 * therefore constant (at most ~30 s of samples), regardless of ride duration.
 */
public class NPWidget extends SimpleWidget {

	private static final int NP_WINDOW_SECONDS = 30;
	private static final long NP_WINDOW_MS = NP_WINDOW_SECONDS * 1000L;

	/**
	 * A new 30-second rolling average is contributed at most once per second.
	 * Processing faster than this would add the same sensor value multiple
	 * times and bias the aggregates.
	 */
	private static final long MIN_SAMPLE_INTERVAL_MS = 1000L;

	/**
	 * Minimum number of 30-second averages that must be accumulated before
	 * a value is displayed. Prevents showing a meaningless spike during the
	 * first seconds of a ride.
	 */
	private static final int MIN_AVERAGES_FOR_DISPLAY = 5;

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

	public NPWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, NP, customId, widgetsPanel);
		this.externalSensorsPlugin = PluginsHelper.getPlugin(ExternalSensorsPlugin.class);
	}

	@Override
	protected void setupView(@NonNull View view) {
		super.setupView(view);
		setIcons(NP);
		setText(null, null);
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
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

			float normalizedPower = calculateNormalizedPower();
			if (normalizedPower > 0) {
				setText(String.valueOf(Math.round(normalizedPower)), "W");
				return;
			}
		}
		// A null currentPower means the sensor is temporarily unavailable;
		// keep the last shown value instead of clearing the widget.
	}

	/**
	 * Appends a sample to the window and evicts samples that have fallen out
	 * of the 30-second window.
	 */
	private void addSample(long now, float power) {
		windowSamples.addLast(new PowerSample(now, power));
		windowPowerSum += power;

		long cutoff = now - NP_WINDOW_MS;
		while (!windowSamples.isEmpty() && windowSamples.peekFirst().timestamp < cutoff) {
			PowerSample old = windowSamples.removeFirst();
			windowPowerSum -= old.power;
		}
	}

	/**
	 * Computes the current 30-second rolling average and adds its 4th power
	 * to the ride-wide aggregates. In Coggan's method this step runs once per
	 * second for the whole ride, so the displayed value converges gradually.
	 */
	private void updateRollingAverage(long now) {
		if (windowSamples.isEmpty()) {
			return;
		}
		// Actual time span covered by the window (shorter than 30 s at ride start).
		double windowSpanMs = now - windowSamples.peekFirst().timestamp + 1;
		double rollingAverage = windowPowerSum / windowSamples.size();

		// Only contribute once the window is full; partial windows would
		// overweight the very first seconds of the ride.
		if (windowSpanMs >= NP_WINDOW_MS) {
			sumRollingAvgPow4 += Math.pow(rollingAverage, 4);
			rollingAvgCount++;
		}
	}

	/** @return Normalized Power, or 0 if not enough data has accumulated yet. */
	private float calculateNormalizedPower() {
		if (rollingAvgCount < MIN_AVERAGES_FOR_DISPLAY) {
			return 0f;
		}
		double avgPow4 = sumRollingAvgPow4 / rollingAvgCount;
		return (float) Math.pow(avgPow4, 0.25);
	}

	/**
	 * @return the latest power reading in watts from the first connected
	 * paired bike power sensor, or null if no sensor or reading is available.
	 */
	@Nullable
	private Float getCurrentPower() {
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

	@Override
	public boolean isMetricSystemDepended() {
		return false;
	}
}

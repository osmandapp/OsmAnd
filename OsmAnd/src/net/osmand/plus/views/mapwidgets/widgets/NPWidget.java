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

import java.util.ArrayList;
import java.util.List;

public class NPWidget extends SimpleWidget {

	private static final int NP_WINDOW_SECONDS = 30;
	private static final int MAX_SAMPLES = NP_WINDOW_SECONDS * 2; // Store samples for 60 seconds

	private final ExternalSensorsPlugin externalSensorsPlugin;
	private final List<PowerSample> powerSamples = new ArrayList<>();

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
		// Get current power from external sensors
		Float currentPower = getCurrentPower();

		// If we have valid power data
		if (currentPower != null && currentPower > 0) {
			// Add new power sample
			long currentTime = System.currentTimeMillis();
			powerSamples.add(new PowerSample(currentTime, currentPower));

			// Remove old samples (older than 60 seconds)
			long cutoffTime = currentTime - (MAX_SAMPLES * 500L);
			while (!powerSamples.isEmpty() && powerSamples.get(0).timestamp < cutoffTime) {
				powerSamples.remove(0);
			}

			// Calculate Normalized Power
			float normalizedPower = calculateNormalizedPower();

			if (normalizedPower > 0) {
				setText(String.valueOf(Math.round(normalizedPower)), "W");
				return;
			}
		}
	}

	@Nullable
	private Float getCurrentPower() {
		if (externalSensorsPlugin == null) {
			return null;
		}

		// Try to get a connected power sensor
		List<AbstractDevice<?>> powerDevices = externalSensorsPlugin.getPairedDevicesByWidgetType(
				SensorWidgetDataFieldType.BIKE_POWER);

		if (powerDevices == null || powerDevices.isEmpty()) {
			return null;
		}

		// Get the first connected device
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

		// Get the power sensor from the device
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

		// Get the latest power data
		List<SensorData> sensorDataList = powerSensor.getLastSensorDataList();
		if (sensorDataList == null || sensorDataList.isEmpty()) {
			return null;
		}

		// Find the power data in the list
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

	private float calculateNormalizedPower() {
		if (powerSamples.isEmpty()) {
			return 0f;
		}

		long currentTime = System.currentTimeMillis();
		long windowStartTime = currentTime - (NP_WINDOW_SECONDS * 1000L);

		// Collect samples within the 30-second window
		List<Float> windowSamples = new ArrayList<>();
		for (PowerSample sample : powerSamples) {
			if (sample.timestamp >= windowStartTime) {
				windowSamples.add(sample.power);
			}
		}

		if (windowSamples.isEmpty()) {
			return 0f;
		}

		// Calculate average of power^4
		double sumPower4 = 0;
		for (Float power : windowSamples) {
			sumPower4 += Math.pow(power, 4);
		}

		double avgPower4 = sumPower4 / windowSamples.size();

		// Take the 4th root to get normalized power
		return (float) Math.pow(avgPower4, 0.25);
	}

	@Override
	public boolean isMetricSystemDepended() {
		return false;
	}
}

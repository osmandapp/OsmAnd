package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.POWER_ZONE;

import android.graphics.Color;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
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

import java.util.List;

public class PowerZoneWidget extends SimpleWidget {

	private static final String NO_VALUE = "—";

	public PowerZoneWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, POWER_ZONE, customId, widgetsPanel);
	}

	@Override
	protected void setupView(@NonNull View view) {
		super.setupView(view);
		setIcons(POWER_ZONE);
		setText(null, null);
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		int ftpValue = settings.TRAINING_FTP.get();
		Float currentPower = getCurrentPower();

		if (ftpValue > 0 && currentPower != null && currentPower > 0) {
			String zone = calculatePowerZone(currentPower, ftpValue);
			int zoneColor = getZoneColor(zone);
			setText(zone, null);
			updateBackgroundColor(zoneColor);
		} else {
			setText(isVerticalWidget() ? NO_VALUE : null, null);
			updateBackgroundColor(Color.TRANSPARENT);
		}
	}

	@Nullable
	private Float getCurrentPower() {
		ExternalSensorsPlugin externalSensorsPlugin = PluginsHelper.getPlugin(ExternalSensorsPlugin.class);
		if (externalSensorsPlugin == null) {
			return null;
		}

		List<AbstractDevice<?>> powerDevices = externalSensorsPlugin.getPairedDevicesByWidgetType(
				SensorWidgetDataFieldType.BIKE_POWER);
		if (powerDevices == null || powerDevices.isEmpty()) {
			return null;
		}

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

	private String calculatePowerZone(float power, int ftp) {
		if (ftp <= 0) {
			return NO_VALUE;
		}

		float powerPercentage = (power / ftp) * 100;

		if (powerPercentage > 150) {
			return "Z7";
		} else if (powerPercentage > 120) {
			return "Z6";
		} else if (powerPercentage > 105) {
			return "Z5";
		} else if (powerPercentage > 90) {
			return "Z4";
		} else if (powerPercentage > 75) {
			return "Z3";
		} else if (powerPercentage > 55) {
			return "Z2";
		} else {
			return "Z1";
		}
	}

	private int getZoneColor(String zone) {
		switch (zone) {
			case "Z1":
				return Color.parseColor("#808080");
			case "Z2":
				return Color.parseColor("#0000FF");
			case "Z3":
				return Color.parseColor("#00FF00");
			case "Z4":
				return Color.parseColor("#FFFF00");
			case "Z5":
				return Color.parseColor("#FF8800");
			case "Z6":
				return Color.parseColor("#FF0000");
			case "Z7":
				return Color.parseColor("#800080");
			default:
				return Color.TRANSPARENT;
		}
	}

	private void updateBackgroundColor(int color) {
		View view = getView();
		if (view != null) {
			View widgetBg = view.findViewById(R.id.widget_bg);
			if (widgetBg != null) {
				widgetBg.setBackgroundColor(color);
			}
		}
	}

	@Override
	public boolean isMetricSystemDepended() {
		return false;
	}
}

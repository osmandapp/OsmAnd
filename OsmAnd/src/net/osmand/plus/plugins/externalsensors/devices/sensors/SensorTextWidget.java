package net.osmand.plus.plugins.externalsensors.devices.sensors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;
import net.osmand.util.Algorithms;

import java.util.List;

public class SensorTextWidget extends TextInfoWidget {

	private final AbstractSensor sensor;
	private final SensorWidgetDataFieldType fieldType;
	private Number cachedNumber;

	public SensorTextWidget(@NonNull MapActivity mapActivity, @NonNull AbstractSensor sensor,
	                        @NonNull SensorWidgetDataFieldType fieldType) {
		super(mapActivity, fieldType.getWidgetType());
		this.sensor = sensor;
		this.fieldType = fieldType;
		updateInfo(null);
		setIcons(fieldType.getWidgetType());
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		List<SensorData> dataList = sensor.getLastSensorDataList();
		if (!sensor.getDevice().isConnected() || Algorithms.isEmpty(dataList)) {
			setText(NO_VALUE, null);
			return;
		}
		SensorWidgetDataField field = null;
		for (SensorData data : dataList) {
			if (data != null) {
				field = data.getWidgetField(fieldType);
				if (field != null) {
					break;
				}
			}
		}
		if (field != null) {
			if (isUpdateNeeded() || !Algorithms.objectEquals(cachedNumber, field.getNumberValue())) {
				cachedNumber = field.getNumberValue();
				FormattedValue formattedValue = field.getFormattedValue(app);
				if (formattedValue != null) {
					setText(formattedValue.value, formattedValue.unit);
				} else {
					setText(NO_VALUE, null);
				}
			}
		} else {
			setText(NO_VALUE, null);
		}
	}

	@Override
	public boolean isMetricSystemDepended() {
		return true;
	}
}
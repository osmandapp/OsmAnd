package net.osmand.plus.views.mapwidgets.widgets;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.AverageSpeedComputer;
import net.osmand.plus.views.mapwidgets.WidgetType;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AverageSpeedWidget extends TextInfoWidget {

	private static final int UPDATE_INTERVAL_MILLIS = 1000;
	private static final String DASH = "—";

	private final AverageSpeedComputer averageSpeedComputer;

	private long lastUpdateTime = 0;

	public AverageSpeedWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity, WidgetType.AVERAGE_SPEED);
		averageSpeedComputer = app.getAverageSpeedComputer();
		setIcons(WidgetType.AVERAGE_SPEED);
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		long time = System.currentTimeMillis();
		if (isUpdateNeeded() || time - lastUpdateTime > UPDATE_INTERVAL_MILLIS) {
			lastUpdateTime = time;
			updateAverageSpeed();
		}
	}

	private void updateAverageSpeed() {
		float averageSpeed = averageSpeedComputer.getAverageSpeed();
		if (Float.isNaN(averageSpeed)) {
			setText(DASH, null);
		} else {
			FormattedValue formattedAverageSpeed = OsmAndFormatter.getFormattedSpeedValue(averageSpeed, app);
			setText(formattedAverageSpeed.value, formattedAverageSpeed.unit);
		}
	}

	@Override
	public boolean isMetricSystemDepended() {
		return true;
	}
}
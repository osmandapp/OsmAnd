package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.AVERAGE_POWER;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.FormattedValue;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.shared.gpx.GpxParameter;
import net.osmand.shared.gpx.GpxTrackAnalysis;

public class AveragePowerWidget extends SimpleWidget {

	private Float cachedAveragePower;

	public AveragePowerWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, AVERAGE_POWER, customId, widgetsPanel);
	}

	@Override
	protected void setupView(@NonNull View view) {
		super.setupView(view);
		setIcons(AVERAGE_POWER);
		setText(null, null);
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		float averagePower = getAveragePower();
		if (isUpdateNeeded() || cachedAveragePower == null || cachedAveragePower != averagePower) {
			cachedAveragePower = averagePower;
			if (cachedAveragePower > 0) {
				FormattedValue formattedPower = OsmAndFormatter.getFormattedPower(cachedAveragePower, app);
				setText(formattedPower.value, formattedPower.unit);
			} else {
				setText(NO_VALUE, null);
			}
		}
	}

	private float getAveragePower() {
		GpxTrackAnalysis analysis = getCurrentTrackAnalysis();
		if (analysis != null) {
			Object avgPowerParam = analysis.getGpxParameter(GpxParameter.AVG_SENSOR_POWER);
			if (avgPowerParam != null) {
				return ((Double) avgPowerParam).floatValue();
			}
		}
		return 0;
	}

	@Nullable
	private GpxTrackAnalysis getCurrentTrackAnalysis() {
		// Try to get analysis from current GPX route
		if (routingHelper.getCurrentGPXRoute() != null) {
			return routingHelper.getCurrentGPXRoute().getFile().getTrackAnalysis(app);
		}
		// Try to get analysis from saving track helper
		if (app.getSavingTrackHelper() != null && app.getSavingTrackHelper().getCurrentTrack() != null) {
			return app.getSavingTrackHelper().getCurrentTrack().getTrackAnalysis(app);
		}
		return null;
	}

	@Override
	public boolean isMetricSystemDepended() {
		return false;
	}
}

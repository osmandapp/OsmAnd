package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.MAX_POWER;

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

public class MaxPowerWidget extends SimpleWidget {

	private Integer cachedMaxPower;

	public MaxPowerWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, MAX_POWER, customId, widgetsPanel);
	}

	@Override
	protected void setupView(@NonNull View view) {
		super.setupView(view);
		setIcons(MAX_POWER);
		setText(null, null);
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		int maxPower = getMaxPower();
		if (isUpdateNeeded() || cachedMaxPower == null || cachedMaxPower != maxPower) {
			cachedMaxPower = maxPower;
			if (cachedMaxPower > 0) {
				FormattedValue formattedPower = OsmAndFormatter.getFormattedPower(cachedMaxPower.floatValue(), app);
				setText(formattedPower.value, formattedPower.unit);
			} else {
				setText(NO_VALUE, null);
			}
		}
	}

	private int getMaxPower() {
		GpxTrackAnalysis analysis = getCurrentTrackAnalysis();
		if (analysis != null) {
			Object maxPowerParam = analysis.getGpxParameter(GpxParameter.MAX_SENSOR_POWER);
			if (maxPowerParam != null) {
				return (Integer) maxPowerParam;
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

package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.AVERAGE_POWER;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.FormattedValue;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;

public class AveragePowerWidget extends SimpleWidget {

	private Float cachedAveragePower;
	private final NormalizedPowerCalculator powerCalculator;

	public AveragePowerWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, AVERAGE_POWER, customId, widgetsPanel);
		this.powerCalculator = NormalizedPowerCalculator.getInstance();
	}

	@Override
	protected void setupView(@NonNull View view) {
		super.setupView(view);
		setIcons(AVERAGE_POWER);
		setText(null, null);
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		float averagePower = powerCalculator.updateAndGetNormalizedPower();
		if (isUpdateNeeded() || cachedAveragePower == null || Math.abs(cachedAveragePower - averagePower) > 0.01f) {
			cachedAveragePower = averagePower;
			float currentAveragePower = powerCalculator.getAveragePower();
			if (currentAveragePower > 0) {
				FormattedValue formattedPower = OsmAndFormatter.getFormattedPowerValue(currentAveragePower, app);
				setText(formattedPower.value, formattedPower.unit);
			} else {
				// Keep the last shown value if sensor is temporarily unavailable
				if (cachedAveragePower > 0) {
					FormattedValue formattedPower = OsmAndFormatter.getFormattedPowerValue(cachedAveragePower, app);
					setText(formattedPower.value, formattedPower.unit);
				} else {
					setText(NO_VALUE, null);
				}
			}
		}
	}

	@Override
	public boolean isMetricSystemDepended() {
		return false;
	}
}

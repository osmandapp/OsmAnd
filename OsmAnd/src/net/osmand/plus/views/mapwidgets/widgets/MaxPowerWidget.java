package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.MAX_POWER;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.FormattedValue;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;

public class MaxPowerWidget extends SimpleWidget {

	private Float cachedMaxPower;
	private final NormalizedPowerCalculator powerCalculator;

	public MaxPowerWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, MAX_POWER, customId, widgetsPanel);
		this.powerCalculator = NormalizedPowerCalculator.getInstance();
	}

	@Override
	protected void setupView(@NonNull View view) {
		super.setupView(view);
		setIcons(MAX_POWER);
		setText(null, null);
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		float maxPower = powerCalculator.updateAndGetNormalizedPower();
		if (isUpdateNeeded() || cachedMaxPower == null || Math.abs(cachedMaxPower - maxPower) > 0.01f) {
			cachedMaxPower = maxPower;
			float currentMaxPower = powerCalculator.getMaxPower();
			if (currentMaxPower > 0) {
				FormattedValue formattedPower = OsmAndFormatter.getFormattedPowerValue(currentMaxPower, app);
				setText(formattedPower.value, formattedPower.unit);
			} else {
				// Keep the last shown value if sensor is temporarily unavailable
				if (cachedMaxPower > 0) {
					FormattedValue formattedPower = OsmAndFormatter.getFormattedPowerValue(cachedMaxPower, app);
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

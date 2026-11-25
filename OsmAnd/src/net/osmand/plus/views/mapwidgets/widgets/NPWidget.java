package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.NP;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;

/**
 * Live Normalized Power (NP) widget, displaying the normalized power value
 * calculated by the {@link NormalizedPowerCalculator} singleton.
 *
 * Normalized Power implements Andrew Coggan's method:
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
 */
public class NPWidget extends SimpleWidget {

	private final NormalizedPowerCalculator npCalculator;

	public NPWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, NP, customId, widgetsPanel);
		this.npCalculator = NormalizedPowerCalculator.getInstance();
	}

	@Override
	protected void setupView(@NonNull View view) {
		super.setupView(view);
		setIcons(NP);
		setText(null, null);
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		float normalizedPower = npCalculator.updateAndGetNormalizedPower();
		
		if (normalizedPower > 0) {
			setText(String.valueOf(Math.round(normalizedPower)), "W");
		} else {
			// A null currentPower means the sensor is temporarily unavailable;
			// keep the last shown value instead of clearing the widget.
		}
	}

	@Override
	public boolean isMetricSystemDepended() {
		return false;
	}
}

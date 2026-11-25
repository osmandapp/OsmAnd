package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.IF;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;

/**
 * Intensity Factor (IF) widget that displays the ratio of Normalized Power to FTP.
 * 
 * Intensity Factor is calculated as: IF = NP / FTP
 * 
 * Where NP is the Normalized Power calculated using Andrew Coggan's method,
 * and FTP is the Functional Threshold Power set by the user.
 * 
 * IF provides a measure of the intensity of a ride relative to the rider's
 * functional threshold. An IF of 1.0 means riding at FTP, >1.0 is above FTP.
 */
public class IFWidget extends SimpleWidget {

	private final NormalizedPowerCalculator npCalculator;

	public IFWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, IF, customId, widgetsPanel);
		this.npCalculator = NormalizedPowerCalculator.getInstance();
	}

	@Override
	protected void setupView(@NonNull View view) {
		super.setupView(view);
		setIcons(IF);
		setText(null, null);
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		float normalizedPower = npCalculator.updateAndGetNormalizedPower();
		int ftpValue = settings.TRAINING_FTP.get();

		if (normalizedPower > 0 && ftpValue > 0) {
			// Calculate Intensity Factor: IF = NP / FTP
			float intensityFactor = normalizedPower / ftpValue;
			// Round to 2 decimal places for display
			String ifValue = String.format("%.2f", intensityFactor);
			setText(ifValue, null);
		} else {
			setText(isVerticalWidget() ? NO_VALUE : null, null);
		}
	}

	@Override
	public boolean isMetricSystemDepended() {
		return false;
	}
}

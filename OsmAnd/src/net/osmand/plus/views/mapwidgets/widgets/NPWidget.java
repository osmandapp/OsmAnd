package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.NP;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;

public class NPWidget extends SimpleWidget {

	public NPWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, NP, customId, widgetsPanel);
	}

	@Override
	protected void setupView(@NonNull View view) {
		super.setupView(view);
		setIcons(NP);
		setText(null, null);
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		float npValue = settings.TRAINING_FTP.get()/2;
		if (npValue > 0) {
			setText(String.valueOf(npValue), "W");
		} else {
			setText(isVerticalWidget() ? NO_VALUE : null, null);
		}
	}

	@Override
	public boolean isMetricSystemDepended() {
		return false;
	}
}

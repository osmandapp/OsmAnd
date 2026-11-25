package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.FTP;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;

public class FtpWidget extends SimpleWidget {

	public FtpWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, FTP, customId, widgetsPanel);
	}

	@Override
	protected void setupView(@NonNull View view) {
		super.setupView(view);
		setIcons(FTP);
		setText(null, null);
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		int ftpValue = settings.TRAINING_FTP.get();
		if (ftpValue > 0) {
			setText(String.valueOf(ftpValue), "W");
		} else {
			setText(isVerticalWidget() ? NO_VALUE : null, null);
		}
	}

	@Override
	public boolean isMetricSystemDepended() {
		return false;
	}
}

package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.GPS_INFO;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmAndLocationProvider.GPSInfo;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.actions.StartGPSStatus;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;

public class GpsInfoWidget extends SimpleWidget {

	private int usedSatellites = -1;
	private int foundSatellites = -1;

	public GpsInfoWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, GPS_INFO, customId, widgetsPanel);
		setIcons(GPS_INFO);
		setText(null, null);
	}

	@Override
	protected View.OnClickListener getOnClickListener() {
		return v -> new StartGPSStatus(mapActivity).run();
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		GPSInfo gpsInfo = locationProvider.getGPSInfo();
		if (isUpdateNeeded()
				|| gpsInfo.usedSatellites != usedSatellites
				|| gpsInfo.foundSatellites != foundSatellites) {
			usedSatellites = gpsInfo.usedSatellites;
			foundSatellites = gpsInfo.foundSatellites;
			setText(gpsInfo.usedSatellites + "/" + gpsInfo.foundSatellites, "");
		}
	}
}
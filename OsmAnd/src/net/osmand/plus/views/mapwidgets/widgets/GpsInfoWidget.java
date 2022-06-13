package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.GPS_INFO;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmAndLocationProvider.GPSInfo;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.actions.StartGPSStatus;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;

public class GpsInfoWidget extends TextInfoWidget {

	private int usedSatellites = -1;
	private int foundSatellites = -1;

	public GpsInfoWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity, GPS_INFO);
		setIcons(GPS_INFO);
		setText(null, null);
		setOnClickListener(v -> new StartGPSStatus(mapActivity).run());
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
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
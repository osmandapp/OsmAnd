package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.COORDINATES_CURRENT_LOCATION;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.plus.OsmAndLocationProvider.GPSInfo;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;

public class CoordinatesCurrentLocationWidget extends CoordinatesBaseWidget {

	public CoordinatesCurrentLocationWidget(@NonNull MapActivity mapActivity,
			@Nullable String customId, @Nullable WidgetsPanel panel) {
		super(mapActivity, COORDINATES_CURRENT_LOCATION, customId, panel);
	}

	@Override
	public void updateInfo(@NonNull View view, @Nullable DrawSettings drawSettings) {
		super.updateInfo(view, drawSettings);
		boolean visible = visibilityHelper.shouldShowTopCoordinatesWidget();
		updateVisibility(visible);
		if (visible) {
			Location location = locationProvider.getLastKnownLocation();
			if (location == null) {
				showSearchingGpsMessage();
			} else {
				showFormattedCoordinates(location.getLatitude(), location.getLongitude());
			}
		}
	}

	private void showSearchingGpsMessage() {
		AndroidUiHelper.updateVisibility(firstIcon, false);
		AndroidUiHelper.updateVisibility(divider, false);
		AndroidUiHelper.updateVisibility(firstContainer, true);
		AndroidUiHelper.updateVisibility(secondContainer, false);
		GPSInfo gpsInfo = locationProvider.getGPSInfo();
		String message = getString(R.string.searching_gps) + "… " + gpsInfo.usedSatellites + "/" + gpsInfo.foundSatellites;
		setFirstCoordinateText(message);
	}
}
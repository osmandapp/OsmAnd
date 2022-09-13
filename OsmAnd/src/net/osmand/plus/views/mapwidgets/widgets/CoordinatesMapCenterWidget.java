package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.COORDINATES_MAP_CENTER;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;

public class CoordinatesMapCenterWidget extends CoordinatesBaseWidget {

	private final OsmandMapTileView mapTileView;

	@Nullable
	@Override
	public OsmandPreference<Boolean> getWidgetVisibilityPref() {
		return settings.SHOW_MAP_CENTER_COORDINATES_WIDGET;
	}

	public CoordinatesMapCenterWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity, COORDINATES_MAP_CENTER);
		mapTileView = app.getOsmandMap().getMapView();
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		boolean visible = mapActivity.getWidgetsVisibilityHelper().shouldShowTopMapCenterCoordinatesWidget();
		LatLon loc = mapTileView.getCurrentRotatedTileBox().getCenterLatLon();

		updateVisibility(visible);
		if (visible) {
			showFormattedCoordinates(loc.getLatitude(), loc.getLongitude());
		}
	}
}
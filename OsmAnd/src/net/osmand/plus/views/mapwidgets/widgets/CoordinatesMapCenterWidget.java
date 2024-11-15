package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.COORDINATES_MAP_CENTER;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;

public class CoordinatesMapCenterWidget extends CoordinatesBaseWidget {

	private final OsmandMapTileView mapTileView;

	public CoordinatesMapCenterWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity, COORDINATES_MAP_CENTER);
		mapTileView = app.getOsmandMap().getMapView();
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		super.updateInfo(drawSettings);
		boolean visible = mapActivity.getWidgetsVisibilityHelper().shouldShowTopCoordinatesWidget();

		updateVisibility(visible);
		if (visible) {
			LatLon latLon = mapTileView.getCurrentRotatedTileBox().getCenterLatLon();
			showFormattedCoordinates(latLon.getLatitude(), latLon.getLongitude());
		}
	}

	@Override
	@NonNull
	protected Drawable getUtmIcon(){
		int utmIconId = isNightMode()
				? R.drawable.widget_coordinates_map_center_utm_night
				: R.drawable.widget_coordinates_map_center_utm_day;
		return iconsCache.getIcon(utmIconId);
	}

	@Override
	@NonNull
	protected Drawable getLatitudeIcon(double lat) {
		int latDayIconId = lat >= 0
				? R.drawable.widget_coordinates_map_center_latitude_north_day
				: R.drawable.widget_coordinates_map_center_latitude_south_day;
		int latNightIconId = lat >= 0
				? R.drawable.widget_coordinates_map_center_latitude_north_night
				: R.drawable.widget_coordinates_map_center_latitude_south_night;
		int latIconId = isNightMode() ? latNightIconId : latDayIconId;
		return iconsCache.getIcon(latIconId);
	}

	@Override
	@NonNull
	protected Drawable getLongitudeIcon(double lon) {
		int lonDayIconId = lon >= 0
				? R.drawable.widget_coordinates_map_center_longitude_east_day
				: R.drawable.widget_coordinates_map_center_longitude_west_day;
		int lonNightIconId = lon >= 0
				? R.drawable.widget_coordinates_map_center_longitude_east_night
				: R.drawable.widget_coordinates_map_center_longitude_west_night;
		int lonIconId = isNightMode() ? lonNightIconId : lonDayIconId;
		return iconsCache.getIcon(lonIconId);
	}
}
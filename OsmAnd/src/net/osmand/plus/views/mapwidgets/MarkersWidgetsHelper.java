package net.osmand.plus.views.mapwidgets;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.widgets.MapMarkerSideWidget;
import net.osmand.plus.views.mapwidgets.widgets.MapMarkersBarWidget;

public class MarkersWidgetsHelper {

	private final MapActivity mapActivity;

	private final MapMarkersBarWidget mapMarkersBarWidget;
	private final MapMarkerSideWidget mapMarkerSideWidget;
	private final MapMarkerSideWidget mapMarkerSideWidget2nd;

	public MarkersWidgetsHelper(@NonNull MapActivity mapActivity) {
		this.mapActivity = mapActivity;

		mapMarkersBarWidget = new MapMarkersBarWidget(mapActivity);
		mapMarkerSideWidget = new MapMarkerSideWidget(mapActivity, true);
		mapMarkerSideWidget2nd = new MapMarkerSideWidget(mapActivity, false);
	}

	public void setCustomLatLon(@Nullable LatLon customLatLon) {
		mapMarkersBarWidget.setCustomLatLon(customLatLon);
		mapMarkerSideWidget.setCustomLatLon(customLatLon);
		mapMarkerSideWidget2nd.setCustomLatLon(customLatLon);
	}

	public int getMapMarkersBarWidgetHeight() {
		return mapMarkersBarWidget.getView().getHeight();
	}

	public boolean isMapMarkersBarWidgetVisible() {
		return mapMarkersBarWidget.isViewVisible()
				&& mapActivity.findViewById(R.id.MapHudButtonsOverlay).getVisibility() == View.VISIBLE;
	}

	@NonNull
	public MapMarkersBarWidget getMapMarkersBarWidget() {
		return mapMarkersBarWidget;
	}

	@NonNull
	public MapMarkerSideWidget getMapMarkerSideWidget(boolean first) {
		return first ? mapMarkerSideWidget : mapMarkerSideWidget2nd;
	}

	public static void showMarkerOnMap(@NonNull MapActivity mapActivity, int index) {
		MapMarkersHelper markersHelper = mapActivity.getMyApplication().getMapMarkersHelper();
		if (index < markersHelper.getMapMarkers().size()) {
			MapMarker marker = markersHelper.getMapMarkers().get(index);
			OsmandMapTileView mapView = mapActivity.getMapView();
			AnimateDraggingMapThread thread = mapView.getAnimatedDraggingThread();
			LatLon pointToNavigate = marker.point;
			if (pointToNavigate != null) {
				int fZoom = Math.max(mapView.getZoom(), 15);
				thread.startMoving(pointToNavigate.getLatitude(), pointToNavigate.getLongitude(), fZoom, true);
			}
		}
	}

	@NonNull
	public static LatLon getDefaultLatLon(@NonNull MapActivity mapActivity) {
		OsmAndLocationProvider locationProvider = mapActivity.getMyApplication().getLocationProvider();
		Location lastStaleKnownLocation = locationProvider.getLastStaleKnownLocation();
		return lastStaleKnownLocation != null
				? new LatLon(lastStaleKnownLocation.getLatitude(), lastStaleKnownLocation.getLongitude())
				: mapActivity.getMapLocation();
	}
}
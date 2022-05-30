package net.osmand.plus.views.mapwidgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.MARKERS_TOP_BAR;
import static net.osmand.plus.views.mapwidgets.WidgetType.SIDE_MARKER_1;
import static net.osmand.plus.views.mapwidgets.WidgetType.SIDE_MARKER_2;

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
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WidgetsRegistryListener;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;

import java.util.ArrayList;
import java.util.List;

public class MarkersWidgetsHelper implements WidgetsRegistryListener {

	private final MapActivity mapActivity;
	private final MapWidgetRegistry widgetRegistry;

	private final List<MapWidget> barWidgets = new ArrayList<>();
	private final List<MapWidget> sideFirstWidgets = new ArrayList<>();
	private final List<MapWidget> sideSecondWidgets = new ArrayList<>();

	public MarkersWidgetsHelper(@NonNull MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		widgetRegistry = mapActivity.getMapLayers().getMapWidgetRegistry();
		widgetRegistry.addWidgetsRegistryListener(this);
	}

	public void setCustomLatLon(@Nullable LatLon latLon) {
		setCustomLatLon(barWidgets, latLon);
		setCustomLatLon(sideFirstWidgets, latLon);
		setCustomLatLon(sideSecondWidgets, latLon);
	}

	public void setCustomLatLon(@NonNull List<MapWidget> widgets, @Nullable LatLon latLon) {
		for (MapWidget widget : widgets) {
			if (widget instanceof CustomLatLonListener) {
				((CustomLatLonListener) widget).setCustomLatLon(latLon);
			}
		}
	}

	public int getMapMarkersBarWidgetHeight() {
		int height = 0;
		for (MapWidget widget : barWidgets) {
			height += widget.getView().getHeight();
		}
		return height;
	}

	public boolean isMapMarkersBarWidgetVisible() {
		boolean visible = isBarWidgetsVisible();
		return visible && mapActivity.findViewById(R.id.MapHudButtonsOverlay).getVisibility() == View.VISIBLE;
	}

	private boolean isBarWidgetsVisible() {
		for (MapWidget widget : barWidgets) {
			if (widget.isViewVisible()) {
				return true;
			}
		}
		return false;
	}

	public void clearListeners() {
		widgetRegistry.removeWidgetsRegistryListener(this);
	}

	@Override
	public void onWidgetRegistered(@NonNull MapWidgetInfo widgetInfo, @Nullable WidgetType widgetType) {
		if (widgetType == MARKERS_TOP_BAR) {
			barWidgets.add(widgetInfo.widget);
		} else if (widgetType == SIDE_MARKER_1) {
			sideFirstWidgets.add(widgetInfo.widget);
		} else if (widgetType == SIDE_MARKER_2) {
			sideSecondWidgets.add(widgetInfo.widget);
		}
	}

	@Override
	public void onWidgetVisibilityChanged(@NonNull MapWidgetInfo widgetInfo) {

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

	public interface CustomLatLonListener {
		void setCustomLatLon(@Nullable LatLon customLatLon);
	}
}
package net.osmand.plus.views.mapwidgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.MARKERS_TOP_BAR;
import static net.osmand.plus.views.mapwidgets.WidgetType.SIDE_MARKER_1;
import static net.osmand.plus.views.mapwidgets.WidgetType.SIDE_MARKER_2;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WidgetsRegistryListener;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;

import java.util.HashSet;
import java.util.Set;

public class MarkersWidgetsHelper implements WidgetsRegistryListener {

	private final MapActivity mapActivity;
	private final OsmandSettings settings;
	private final MapWidgetRegistry widgetRegistry;

	private final Set<MapWidget> barWidgets = new HashSet<>();
	private final Set<MapWidget> sideFirstWidgets = new HashSet<>();
	private final Set<MapWidget> sideSecondWidgets = new HashSet<>();

	public MarkersWidgetsHelper(@NonNull MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		settings = mapActivity.getMyApplication().getSettings();
		widgetRegistry = mapActivity.getMapLayers().getMapWidgetRegistry();
		widgetRegistry.addWidgetsRegistryListener(this);
	}

	public void setCustomLatLon(@Nullable LatLon latLon) {
		setCustomLatLon(barWidgets, latLon);
		setCustomLatLon(sideFirstWidgets, latLon);
		setCustomLatLon(sideSecondWidgets, latLon);
	}

	public void setCustomLatLon(@NonNull Set<MapWidget> widgets, @Nullable LatLon latLon) {
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
	public void onWidgetRegistered(@NonNull MapWidgetInfo widgetInfo) {
		WidgetType widgetType = widgetInfo.getWidgetType();
		if (isMarkerWidget(widgetType) && widgetInfo.isEnabledForAppMode(settings.getApplicationMode())) {
			addWidget(widgetInfo, widgetType);
		}
	}

	@Override
	public void onWidgetVisibilityChanged(@NonNull MapWidgetInfo widgetInfo) {
		WidgetType widgetType = widgetInfo.getWidgetType();
		if (isMarkerWidget(widgetType)) {
			if (widgetInfo.isEnabledForAppMode(settings.getApplicationMode())) {
				addWidget(widgetInfo, widgetType);
			} else {
				removeWidget(widgetInfo, widgetType);
			}
		}
	}

	@Override
	public void onWidgetsCleared() {
		clearWidgets();
	}

	private boolean isMarkerWidget(@Nullable WidgetType widgetType) {
		return widgetType == MARKERS_TOP_BAR || widgetType == SIDE_MARKER_1 || widgetType == SIDE_MARKER_2;
	}

	public void clearWidgets() {
		barWidgets.clear();
		sideFirstWidgets.clear();
		sideSecondWidgets.clear();
	}

	private void addWidget(@NonNull MapWidgetInfo widgetInfo, @Nullable WidgetType widgetType) {
		if (widgetType == MARKERS_TOP_BAR) {
			barWidgets.add(widgetInfo.widget);
		} else if (widgetType == SIDE_MARKER_1) {
			sideFirstWidgets.add(widgetInfo.widget);
		} else if (widgetType == SIDE_MARKER_2) {
			sideSecondWidgets.add(widgetInfo.widget);
		}
	}

	private void removeWidget(@NonNull MapWidgetInfo widgetInfo, @Nullable WidgetType widgetType) {
		if (widgetType == MARKERS_TOP_BAR) {
			barWidgets.remove(widgetInfo.widget);
		} else if (widgetType == SIDE_MARKER_1) {
			sideFirstWidgets.remove(widgetInfo.widget);
		} else if (widgetType == SIDE_MARKER_2) {
			sideSecondWidgets.remove(widgetInfo.widget);
		}
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
				thread.startMoving(pointToNavigate.getLatitude(), pointToNavigate.getLongitude(), fZoom);
			}
		}
	}

	public interface CustomLatLonListener {
		void setCustomLatLon(@Nullable LatLon customLatLon);
	}
}
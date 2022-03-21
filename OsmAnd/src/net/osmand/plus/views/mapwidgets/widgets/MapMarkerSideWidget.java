package net.osmand.plus.views.mapwidgets.widgets;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.enums.MetricsConstants;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.MarkersWidgetsHelper;

import java.util.List;

public class MapMarkerSideWidget extends RightTextInfoWidget {

	private final MapMarkersHelper mapMarkersHelper;

	private final boolean firstMarker;

	private int cachedMeters;
	private int cachedMarkerColorIndex = -1;
	private boolean cachedNightMode;

	private LatLon customLatLon;

	public MapMarkerSideWidget(@NonNull MapActivity mapActivity, boolean firstMarker) {
		super(mapActivity);
		this.firstMarker = firstMarker;
		this.mapMarkersHelper = app.getMapMarkersHelper();

		cachedNightMode = isNightMode();

		setText(null, null);
		setOnClickListener(v -> MarkersWidgetsHelper.showMarkerOnMap(mapActivity, firstMarker ? 0 : 1));
	}

	public void setCustomLatLon(@Nullable LatLon customLatLon) {
		this.customLatLon = customLatLon;
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		RoutingHelper routingHelper = app.getRoutingHelper();
		MapMarker marker = getMarker();

		boolean hideWidget = marker == null
				|| routingHelper.isRoutePlanningMode()
				|| routingHelper.isFollowingMode();
		if (hideWidget) {
			cachedMeters = 0;
			setText(null, null);
			return;
		}

		updateTextIfNeeded();
		updateIconIfNeeded(marker);
	}

	private void updateTextIfNeeded() {
		int distance = getDistance();
		if (isUpdateNeeded() || cachedMeters != distance) {
			cachedMeters = distance;
			MetricsConstants metricsConstants = settings.METRIC_SYSTEM.get();
			FormattedValue formattedDistance = OsmAndFormatter.getFormattedDistanceValue(cachedMeters,
					app, false, metricsConstants);
			setText(formattedDistance.value, formattedDistance.unit);
		}
	}

	private void updateIconIfNeeded(@NonNull MapMarker marker) {
		int colorIndex = marker.colorIndex;
		boolean updateIcon = colorIndex != -1
				&& (colorIndex != cachedMarkerColorIndex || cachedNightMode != isNightMode());
		if (updateIcon) {
			int backgroundIconId = isNightMode()
					? R.drawable.widget_marker_night
					: R.drawable.widget_marker_day;
			int foregroundColorId = MapMarker.getColorId(marker.colorIndex);
			Drawable drawable = iconsCache.getLayeredIcon(backgroundIconId,
					R.drawable.widget_marker_triangle, 0, foregroundColorId);
			setImageDrawable(drawable);
			cachedMarkerColorIndex = marker.colorIndex;
			cachedNightMode = isNightMode();
		}
	}

	public int getDistance() {
		int distance = 0;
		LatLon pointToNavigate = getPointToNavigate();
		if (pointToNavigate != null) {
			LatLon latLon = customLatLon != null ? customLatLon : MarkersWidgetsHelper.getDefaultLatLon(mapActivity);
			float[] calc = new float[1];
			Location.distanceBetween(latLon.getLatitude(), latLon.getLongitude(), pointToNavigate.getLatitude(), pointToNavigate.getLongitude(), calc);
			distance = (int) calc[0];
		}
		return distance;
	}

	@Nullable
	private LatLon getPointToNavigate() {
		MapMarker marker = getMarker();
		return marker != null ? marker.point : null;
	}

	@Nullable
	private MapMarker getMarker() {
		List<MapMarker> markers = mapMarkersHelper.getMapMarkers();
		if (markers.size() > 0) {
			if (firstMarker) {
				return markers.get(0);
			} else if (markers.size() > 1) {
				return markers.get(1);
			}
		}
		return null;
	}

	@Override
	public boolean isMetricSystemDepended() {
		return true;
	}
}
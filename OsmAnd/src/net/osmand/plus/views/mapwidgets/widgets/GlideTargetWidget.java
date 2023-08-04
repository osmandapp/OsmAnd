package net.osmand.plus.views.mapwidgets.widgets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.Location;
import net.osmand.core.android.MapRendererView;
import net.osmand.data.LatLon;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.util.MapUtils;

import java.util.Objects;

public class GlideTargetWidget extends GlideBaseWidget {

	private Location cachedCurrentLocation = null;
	private LatLon cachedTargetLatLon = null;
	private Float cachedGlideRatio = null;

	public GlideTargetWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity, WidgetType.GLIDE_TARGET);
		updateInfo(null);
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		if (isUpdateNeeded() || isTimeToUpdate()) {
			lastUpdateTime = System.currentTimeMillis();
			calculateGlideRatio(result -> {
				if (cachedGlideRatio == null || !Objects.equals(cachedGlideRatio, result)) {
					cachedGlideRatio = result;
					if (cachedGlideRatio != null) {
						setText(format(cachedGlideRatio), null);
					} else {
						setText(NO_VALUE, null);
					}
				}
				return true;
			});
		}
	}

	private void calculateGlideRatio(@NonNull CallbackWithObject<Float> callback) {
		MapRendererView mapRenderer = mapActivity.getMapView().getMapRenderer();
		if (mapRenderer == null
				|| cachedTargetLatLon == null
				|| cachedCurrentLocation == null
				|| !cachedCurrentLocation.hasAltitude()) {
			callback.processResult(null);
			return;
		}

		LatLon l1 = new LatLon(cachedCurrentLocation.getLatitude(), cachedCurrentLocation.getLongitude());
		double a1 = cachedCurrentLocation.getAltitude();
		LatLon l2 = cachedTargetLatLon;

		NativeUtilities.getAltitudeForLatLon(mapRenderer, l2, altitude ->
				callback.processResult(altitude != null ? MapUtils.calculateGlideRatio(l1, l2, a1, altitude) : null));
	}

	/**
	 * Calculation is needed only if device location changed or if marker position changed.
	 *
	 * @return 'true' if device location changed or marker position changed.
	 */
	@Override
	public boolean isUpdateNeeded() {
		Location location = locationProvider.getLastKnownLocation();
		boolean updateNeeded = cachedCurrentLocation == null || !MapUtils.areLatLonEqual(cachedCurrentLocation, location);
		cachedCurrentLocation = location;

		LatLon targetLatLon = null;
		MapMarker mapMarker = app.getMapMarkersHelper().getFirstMapMarker();
		if (mapMarker != null) {
			targetLatLon = new LatLon(mapMarker.getLatitude(), mapMarker.getLongitude());
		}
		updateNeeded |= cachedTargetLatLon == null || !MapUtils.areLatLonEqual(cachedTargetLatLon, targetLatLon);
		cachedTargetLatLon = targetLatLon;

		return updateNeeded;
	}
}

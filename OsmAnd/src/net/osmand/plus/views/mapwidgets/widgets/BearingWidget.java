package net.osmand.plus.views.mapwidgets.widgets;

import android.hardware.GeomagneticField;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.util.Algorithms;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static net.osmand.plus.views.mapwidgets.WidgetParams.MAGNETIC_BEARING;
import static net.osmand.plus.views.mapwidgets.WidgetParams.RELATIVE_BEARING;

public class BearingWidget extends RightTextInfoWidget {

	private static final float MIN_SPEED = 1f;
	private static final int INVALID_BEARING = -1000;

	private final OsmAndLocationProvider locationProvider;
	private final boolean relative;

	private int cachedBearing;

	public BearingWidget(@NonNull MapActivity mapActivity, boolean relative) {
		super(mapActivity);
		this.locationProvider = app.getLocationProvider();
		this.relative = relative;

		setText(null, null);
		setIcons(relative ? RELATIVE_BEARING : MAGNETIC_BEARING);
		setContentTitle(relative ? R.string.map_widget_bearing : R.string.map_widget_magnetic_bearing);
	}

	@Override
	public void updateInfo(DrawSettings drawSettings) {
		int bearing = getBearing();
		boolean bearingChanged = cachedBearing != bearing;
		if (isUpdateNeeded() || bearingChanged) {
			cachedBearing = bearing;
			if (bearing != INVALID_BEARING) {
				setText(OsmAndFormatter.getFormattedAzimuth(bearing, app) + (relative ? "" : " M"), null);
			} else {
				setText(null, null);
			}
		}
	}

	private int getBearing() {
		Location myLocation = locationProvider.getLastKnownLocation();
		Location destination = myLocation != null ? getDestinationLocation(myLocation) : null;

		if (myLocation == null || destination == null) {
			return INVALID_BEARING;
		}

		GeomagneticField destGf = getGeomagneticField(destination);
		float bearingToDest = destination.getBearing() - destGf.getDeclination();

		return relative
				? getRelativeBearing(myLocation, bearingToDest)
				: (int) bearingToDest;
	}

	@Nullable
	private Location getDestinationLocation(@NonNull Location fromLocation) {
		LatLon destLatLon = null;
		List<TargetPoint> points = app.getTargetPointsHelper().getIntermediatePointsWithTarget();
		if (!Algorithms.isEmpty(points)) {
			destLatLon = points.get(0).point;
		}

		List<MapMarker> markers = app.getMapMarkersHelper().getMapMarkers();
		if (destLatLon == null && !Algorithms.isEmpty(markers)) {
			destLatLon = markers.get(0).point;
		}

		if (destLatLon != null) {
			Location destLocation = new Location("", destLatLon.getLatitude(), destLatLon.getLongitude());
			destLocation.setBearing(fromLocation.bearingTo(destLocation));
			return destLocation;
		}

		return null;
	}

	private int getRelativeBearing(@NonNull Location myLocation, float bearingToDest) {
		float bearing = INVALID_BEARING;
		Float heading = locationProvider.getHeading();

		if (heading != null && (myLocation.getSpeed() < MIN_SPEED || !myLocation.hasBearing())) {
			bearing = heading;
		} else if (myLocation.hasBearing()) {
			GeomagneticField myLocGf = getGeomagneticField(myLocation);
			bearing = myLocation.getBearing() - myLocGf.getDeclination();
		}

		if (bearing > INVALID_BEARING) {
			bearingToDest -= bearing;
			if (bearingToDest > 180f) {
				bearingToDest -= 360f;
			} else if (bearingToDest < -180f) {
				bearingToDest += 360f;
			}
			return (int) bearingToDest;
		}

		return INVALID_BEARING;
	}

	@NonNull
	private GeomagneticField getGeomagneticField(@NonNull Location location) {
		float lat = (float) location.getLatitude();
		float lon = (float) location.getLongitude();
		float alt = ((float) location.getAltitude());
		return new GeomagneticField(lat, lon, alt, System.currentTimeMillis());
	}

	@Override
	public boolean isAngularUnitsDepended() {
		return true;
	}
}
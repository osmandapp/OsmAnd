package net.osmand.plus.views.mapwidgets.widgets;

import android.hardware.GeomagneticField;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetParams;
import net.osmand.util.Algorithms;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static net.osmand.plus.views.mapwidgets.widgets.BearingWidget.BearingType.MAGNETIC_BEARING;
import static net.osmand.plus.views.mapwidgets.widgets.BearingWidget.BearingType.RELATIVE_BEARING;
import static net.osmand.plus.views.mapwidgets.widgets.BearingWidget.BearingType.TRUE_BEARING;

public class BearingWidget extends TextInfoWidget {

	private static final float MIN_SPEED = 1f;
	private static final int INVALID_BEARING = -1000;

	private final OsmAndLocationProvider locationProvider;
	private final BearingType bearingType;

	private int cachedBearing;

	public BearingWidget(@NonNull MapActivity mapActivity, @NonNull BearingType bearingType) {
		super(mapActivity);
		this.locationProvider = app.getLocationProvider();
		this.bearingType = bearingType;

		setText(null, null);
		setIcons(bearingType.widget);
		setContentTitle(bearingType.widget.titleId);
	}

	@Override
	public void updateInfo(DrawSettings drawSettings) {
		int bearing = getBearing();
		boolean bearingChanged = cachedBearing != bearing;
		if (isUpdateNeeded() || bearingChanged) {
			cachedBearing = bearing;
			if (bearing != INVALID_BEARING) {
				String formattedAzimuth = OsmAndFormatter.getFormattedAzimuth(bearing, app);
				String postfix = bearingType == MAGNETIC_BEARING ? " M" : "";
				setText(formattedAzimuth + postfix, null);
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

		float trueBearing = destination.getBearing();
		if (bearingType == TRUE_BEARING) {
			return (int) trueBearing;
		}

		GeomagneticField destGf = getGeomagneticField(destination);
		float magneticBearing = trueBearing - destGf.getDeclination();

		if (bearingType == MAGNETIC_BEARING) {
			return (int) magneticBearing;
		} else if (bearingType == RELATIVE_BEARING) {
			return getRelativeBearing(myLocation, magneticBearing);
		} else {
			throw new IllegalStateException("Unsupported bearing type");
		}
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

	private int getRelativeBearing(@NonNull Location myLocation, float magneticBearingToDest) {
		float bearing = INVALID_BEARING;
		Float heading = locationProvider.getHeading();

		if (heading != null && (myLocation.getSpeed() < MIN_SPEED || !myLocation.hasBearing())) {
			bearing = heading;
		} else if (myLocation.hasBearing()) {
			GeomagneticField myLocGf = getGeomagneticField(myLocation);
			bearing = myLocation.getBearing() - myLocGf.getDeclination();
		}

		if (bearing > INVALID_BEARING) {
			magneticBearingToDest -= bearing;
			if (magneticBearingToDest > 180f) {
				magneticBearingToDest -= 360f;
			} else if (magneticBearingToDest < -180f) {
				magneticBearingToDest += 360f;
			}
			return (int) magneticBearingToDest;
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

	public enum BearingType {

		RELATIVE_BEARING(WidgetParams.RELATIVE_BEARING),
		MAGNETIC_BEARING(WidgetParams.MAGNETIC_BEARING),
		TRUE_BEARING(WidgetParams.TRUE_BEARING);

		public final WidgetParams widget;

		BearingType(@NonNull WidgetParams widget) {
			this.widget = widget;
		}
	}
}
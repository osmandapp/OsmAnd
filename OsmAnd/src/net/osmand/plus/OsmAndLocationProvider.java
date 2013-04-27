package net.osmand.plus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


import net.osmand.GeoidAltitudeCorrection;
import net.osmand.PlatformUtil;
import net.osmand.access.NavigationInfo;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.util.MapUtils;
import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

public class OsmAndLocationProvider implements SensorEventListener {
	
	public interface OsmAndLocationListener {
		void updateLocation(net.osmand.Location location);
	}

	public interface OsmAndCompassListener {
		void updateCompassValue(float value);
	}

	private static final int INTERVAL_TO_CLEAR_SET_LOCATION = 30 * 1000;
	private static final boolean USE_MAGNETIC_FIELD_SENSOR = true;
	private static final int LOST_LOCATION_MSG_ID = 10;
	private static final long LOST_LOCATION_CHECK_DELAY = 18000;

	private static final float ACCURACY_FOR_GPX_AND_ROUTING = 50;

	private static final int GPS_TIMEOUT_REQUEST = 0;
	private static final int GPS_DIST_REQUEST = 0;
	private static final int NOT_SWITCH_TO_NETWORK_WHEN_GPS_LOST_MS = 12000;

	private long lastTimeGPSLocationFixed = 0;

	private boolean sensorRegistered = false;
	private float[] mGravs;
	private float[] mGeoMags;
	private float previousCorrectionValue = 360;
	
	
	
	private final boolean USE_KALMAN_FILTER = true;
	private final float KALMAN_COEFFICIENT = 0.02f;
	
	float avgValSin = 0;
	float avgValCos = 0;
	float lastValSin = 0;
	float lastValCos = 0;
	private float[] previousCompassValuesA = new float[50];
	private float[] previousCompassValuesB = new float[50];
	private int previousCompassIndA = 0;
	private int previousCompassIndB = 0;
	
	private long lastHeadingCalcTime = 0;
	private Float heading = null;

	// Current screen orientation
	private int currentScreenOrientation;

	private OsmandApplication app;
	private OsmandSettings settings;
	
	private NavigationInfo navigationInfo;
	private CurrentPositionHelper currentPositionHelper;
	private OsmAndLocationSimulation locationSimulation;

	private net.osmand.Location location = null;
	
	private GPSInfo gpsInfo = new GPSInfo(); 

	private List<OsmAndLocationListener> locationListeners = new ArrayList<OsmAndLocationProvider.OsmAndLocationListener>();
	private List<OsmAndCompassListener> compassListeners = new ArrayList<OsmAndLocationProvider.OsmAndCompassListener>();
	private Listener gpsStatusListener;
	
	public OsmAndLocationProvider(OsmandApplication app) {
		this.app = app;
		navigationInfo = new NavigationInfo(app);
		settings = app.getSettings();
		currentPositionHelper = new CurrentPositionHelper(app);
		locationSimulation = new OsmAndLocationSimulation(app, this);
	}

	public void resumeAllUpdates() {
		final LocationManager service = (LocationManager) app.getSystemService(Context.LOCATION_SERVICE);
		service.addGpsStatusListener(getGpsStatusListener(service));
		try {
			service.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_TIMEOUT_REQUEST, GPS_DIST_REQUEST, gpsListener);
		} catch (IllegalArgumentException e) {
			Log.d(PlatformUtil.TAG, "GPS location provider not available"); //$NON-NLS-1$
		}
		// try to always ask for network provide : it is faster way to find location
		try {
			service.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, GPS_TIMEOUT_REQUEST, GPS_DIST_REQUEST, networkListener);
		} catch (IllegalArgumentException e) {
			Log.d(PlatformUtil.TAG, "Network location provider not available"); //$NON-NLS-1$
		}
	}

	private Listener getGpsStatusListener(final LocationManager service) {
		gpsStatusListener = new Listener() {
			private GpsStatus gpsStatus;
			@Override
			public void onGpsStatusChanged(int event) {
				gpsStatus = service.getGpsStatus(gpsStatus);
				updateGPSInfo(gpsStatus);
				updateLocation(location);
			}
		};
		return gpsStatusListener;
	}
	
	private void updateGPSInfo(GpsStatus s) {
		boolean fixed = false;
		int n = 0;
		int u = 0;
		if (s != null) {
			Iterator<GpsSatellite> iterator = s.getSatellites().iterator();
			while (iterator.hasNext()) {
				GpsSatellite g = iterator.next();
				n++;
				if (g.usedInFix()) {
					u++;
					fixed = true;
				}
			}
		}
		gpsInfo.fixed = fixed;
		gpsInfo.foundSatellites = n;
		gpsInfo.usedSatellites = u;
	}
	
	public GPSInfo getGPSInfo(){
		return gpsInfo;
	}
	
	public void updateScreenOrientation(int orientation) {
		currentScreenOrientation = orientation;
	}
	
	public void addLocationListener(OsmAndLocationListener listener){
		if(!locationListeners.contains(listener)) {
			locationListeners.add(listener);
		}
	}
	
	public void removeLocationListener(OsmAndLocationListener listener){
		locationListeners.remove(listener);
	}
	
	public void addCompassListener(OsmAndCompassListener listener){
		if(!compassListeners.contains(listener)) {
			compassListeners.add(listener);
		}
	}
	
	public void removeCompassListener(OsmAndCompassListener listener){
		compassListeners.remove(listener);
	}

	public net.osmand.Location getFirstTimeRunDefaultLocation() {
		LocationManager service = (LocationManager) app.getSystemService(Context.LOCATION_SERVICE);
		List<String> providers = new ArrayList<String>(service.getProviders(true));
		// note, passive provider is from API_LEVEL 8 but it is a constant, we can check for it.
		// constant should not be changed in future
		int passiveFirst = providers.indexOf("passive"); // LocationManager.PASSIVE_PROVIDER
		// put passive provider to first place
		if (passiveFirst > -1) {
			providers.add(0, providers.remove(passiveFirst));
		}
		// find location
		for (String provider : providers) {
			net.osmand.Location location = convertLocation(service.getLastKnownLocation(provider), app);
			if (location != null) {
				return location;
			}
		}
		return null;
	}

	public void registerOrUnregisterCompassListener(boolean register) {
		if (sensorRegistered && !register) {
			Log.d(PlatformUtil.TAG, "Disable sensor"); //$NON-NLS-1$
			((SensorManager) app.getSystemService(Context.SENSOR_SERVICE)).unregisterListener(this);
			sensorRegistered = false;
			heading = null;
		} else if (!sensorRegistered && register) {
			Log.d(PlatformUtil.TAG, "Enable sensor"); //$NON-NLS-1$
			SensorManager sensorMgr = (SensorManager) app.getSystemService(Context.SENSOR_SERVICE);
			if (USE_MAGNETIC_FIELD_SENSOR) {
				Sensor s = sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
				if (s == null || !sensorMgr.registerListener(this, s, SensorManager.SENSOR_DELAY_UI)) {
					Log.e(PlatformUtil.TAG, "Sensor accelerometer could not be enabled");
				}
				s = sensorMgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
				if (s == null || !sensorMgr.registerListener(this, s, SensorManager.SENSOR_DELAY_UI)) {
					Log.e(PlatformUtil.TAG, "Sensor magnetic field could not be enabled");
				}
			} else {
				Sensor s = sensorMgr.getDefaultSensor(Sensor.TYPE_ORIENTATION);
				if (s == null || !sensorMgr.registerListener(this, s, SensorManager.SENSOR_DELAY_UI)) {
					Log.e(PlatformUtil.TAG, "Sensor orientation could not be enabled");
				}
			}
			sensorRegistered = true;
		}
	}

	// location not null!
	private void updateSpeedEmulator(net.osmand.Location location) {
		// For network/gps it's bad way (not accurate). It's widely used for testing purposes
		// possibly keep using only for emulator case
		if (location != null) {
			if (location.distanceTo(location) > 3) {
				float d = location.distanceTo(location);
				long time = location.getTime() - location.getTime();
				float speed;
				if (time == 0) {
					speed = 0;
				} else {
					speed = ((float) d * 1000) / time;
				}
				// Be aware only for emulator ! code is incorrect in case of airplane
				if (speed > 100) {
					speed = 100;
				}
				location.setSpeed(speed);
			}
		}
	}

	public static boolean isPointAccurateForRouting(net.osmand.Location loc) {
		return loc != null && (!loc.hasAccuracy() || loc.getAccuracy() < ACCURACY_FOR_GPX_AND_ROUTING * 3 / 2);
	}

	private boolean isRunningOnEmulator() {
		if (Build.DEVICE.equals("generic")) { //$NON-NLS-1$ 
			return true;
		}
		return false;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// Attention : sensor produces a lot of events & can hang the system
		float val = 0;
		switch (event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			if (mGravs == null) {
				mGravs = new float[3];
			}
			System.arraycopy(event.values, 0, mGravs, 0, 3);
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			if (mGeoMags == null) {
				mGeoMags = new float[3];
			}
			System.arraycopy(event.values, 0, mGeoMags, 0, 3);
			break;
		case Sensor.TYPE_ORIENTATION:
			val = event.values[0];
			if (mGravs != null && mGeoMags != null) {
				return;
			}
			break;
		default:
			return;
		}
		
		if (mGravs != null && mGeoMags != null) {
			float[] mRotationM = new float[9];
			boolean success = SensorManager.getRotationMatrix(mRotationM, null, mGravs, mGeoMags);
			if (!success) {
				return;
			}
			float[] orientation = SensorManager.getOrientation(mRotationM, new float[3]);
			val = (float) Math.toDegrees(orientation[0]);
		} else if(event.sensor.getType() != Sensor.TYPE_ORIENTATION){
			return;
		}

		if (currentScreenOrientation == 1) {
			val += 90;
		} else if (currentScreenOrientation == 2) {
			val += 180;
		} else if (currentScreenOrientation == 3) {
			val -= 90;
		}
		if (previousCorrectionValue == 360 && getLastKnownLocation() != null) {
			net.osmand.Location l = getLastKnownLocation();
			GeomagneticField gf = new GeomagneticField((float) l.getLatitude(), (float) l.getLongitude(), (float) l.getAltitude(),
					System.currentTimeMillis());
			previousCorrectionValue = gf.getDeclination();
		}
		if (previousCorrectionValue != 360) {
			val += previousCorrectionValue;
		}
		float valRad = (float) (val / 180f * Math.PI);
		lastValSin = (float) Math.sin(valRad);
		lastValCos = (float) Math.cos(valRad);
		lastHeadingCalcTime = System.currentTimeMillis();
		if(heading == null && previousCompassIndA == 0) {
			Arrays.fill(previousCompassValuesA, lastValSin);
			Arrays.fill(previousCompassValuesB, lastValCos);
			avgValSin = lastValSin;
			avgValCos = lastValCos;
		} else {
			if (USE_KALMAN_FILTER) {
				avgValSin = KALMAN_COEFFICIENT * lastValSin + avgValSin * (1 - KALMAN_COEFFICIENT);
				avgValCos = KALMAN_COEFFICIENT * lastValCos + avgValCos * (1 - KALMAN_COEFFICIENT);
			} else {
				int l = previousCompassValuesA.length;
				previousCompassIndA = (previousCompassIndA + 1) % l;
				previousCompassIndB = (previousCompassIndB + 1) % l;
				// update average
				avgValSin = avgValSin + (-previousCompassValuesA[previousCompassIndA] + lastValSin) / l;
				previousCompassValuesA[previousCompassIndA] = lastValSin;
				avgValCos = avgValCos + (-previousCompassValuesB[previousCompassIndB] + lastValCos) / l;
				previousCompassValuesB[previousCompassIndB] = lastValCos;
			}
		}
		updateCompassVal();
	}	

	private void updateCompassVal() {
		heading = (float) getAngle(avgValSin, avgValCos);
		for(OsmAndCompassListener c : compassListeners){
			c.updateCompassValue(heading.floatValue());
		}
	}
	
	public Float getHeading() {
		if (heading != null && lastValSin != avgValSin && System.currentTimeMillis() - lastHeadingCalcTime > 700) {
			avgValSin = lastValSin;
			avgValCos = lastValCos;
			lastHeadingCalcTime = System.currentTimeMillis();
			Arrays.fill(previousCompassValuesA, avgValSin);
			Arrays.fill(previousCompassValuesB, avgValCos);
			updateCompassVal();
		}
		return heading;
	}
	
	private float getAngle(float sinA, float cosA) {
		return MapUtils.unifyRotationTo360((float) (Math.atan2(sinA, cosA) * 180 / Math.PI));
	}

	
	private void updateLocation(net.osmand.Location loc ) {
		for(OsmAndLocationListener l : locationListeners){
			l.updateLocation(loc);
		}
	}

	
	
	private LocationListener gpsListener = new LocationListener() {
		@Override
		public void onLocationChanged(Location location) {
			if (location != null) {
				lastTimeGPSLocationFixed = location.getTime();
			}
			if(!locationSimulation.isRouteAnimating()) {
				setLocation(convertLocation(location, app));
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	};

	private boolean useOnlyGPS() {
		if(app.getRoutingHelper().isFollowingMode()) {
			return true;
		}
		if((System.currentTimeMillis() - lastTimeGPSLocationFixed) < NOT_SWITCH_TO_NETWORK_WHEN_GPS_LOST_MS) {
			return true;
		}
		if(isRunningOnEmulator()) {
			return true;
		}
		return false;
	}

	// Working with location listeners
	private LocationListener networkListener = new LocationListener() {

		@Override
		public void onLocationChanged(Location location) {
			// double check about use only gps
			// that strange situation but it could happen?
			if (!useOnlyGPS() && !locationSimulation.isRouteAnimating()) {
				setLocation(convertLocation(location, app));
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

	};

	private void stopLocationRequests() {
		LocationManager service = (LocationManager) app.getSystemService(Context.LOCATION_SERVICE);
		service.removeGpsStatusListener(gpsStatusListener);
		service.removeUpdates(gpsListener);
		service.removeUpdates(networkListener);
	}

	public void pauseAllUpdates() {
		stopLocationRequests();
		SensorManager sensorMgr = (SensorManager) app.getSystemService(Context.SENSOR_SERVICE);
		sensorMgr.unregisterListener(this);
		sensorRegistered = false;
	}

	public static net.osmand.Location convertLocation(Location l, OsmandApplication app) {
		if (l == null) {
			return null;
		}
		net.osmand.Location r = new net.osmand.Location(l.getProvider());
		r.setLatitude(l.getLatitude());
		r.setLongitude(l.getLongitude());
		r.setTime(l.getTime());
		if (l.hasAccuracy()) {
			r.setAccuracy(l.getAccuracy());
		}
		if (l.hasSpeed()) {
			r.setSpeed(l.getSpeed());
		}
		if (l.hasAltitude()) {
			r.setAltitude(l.getAltitude());
		}
		if (l.hasBearing()) {
			r.setBearing(l.getBearing());
		}
		if (l.hasAltitude() && app != null) {
			double alt = l.getAltitude();
			final GeoidAltitudeCorrection geo = app.getResourceManager().getGeoidAltitudeCorrection();
			if (geo != null) {
				alt -= geo.getGeoidHeight(l.getLatitude(), l.getLongitude());
				r.setAltitude(alt);
			}
		}
		return r;
	}
	
	
	private void scheduleCheckIfGpsLost(net.osmand.Location location) {
		final RoutingHelper routingHelper = app.getRoutingHelper();
		if (location != null) {
			final long fixTime = location.getTime();
			app.runMessageInUIThreadAndCancelPrevious(LOST_LOCATION_MSG_ID, new Runnable() {

				@Override
				public void run() {
					net.osmand.Location lastKnown = getLastKnownLocation();
					if (lastKnown != null && lastKnown.getTime() > fixTime) {
						// false positive case, still strange how we got here with removeMessages
						return;
					}
					if (routingHelper.isFollowingMode() && routingHelper.getLeftDistance() > 0) {
						routingHelper.getVoiceRouter().gpsLocationLost();
					}
					setLocation(null);
				}
			}, LOST_LOCATION_CHECK_DELAY);
		}
	}
	public void setLocationFromService(net.osmand.Location location, boolean continuous) {
		// if continuous notify about lost location
		if (continuous) {
			scheduleCheckIfGpsLost(location);
		}
		app.getSavingTrackHelper().updateLocation(location);
		app.getLiveMonitoringHelper().updateLocation(location);
		// 2. accessibility routing
		navigationInfo.setLocation(location);
		
		app.getRoutingHelper().updateLocation(location);
	}
	
	public void setLocationFromSimulation(net.osmand.Location location) {
		setLocation(location);
	}

	private void setLocation(net.osmand.Location location) {
		if(location == null){
			updateGPSInfo(null);
		}
		enhanceLocation(location);
		scheduleCheckIfGpsLost(location);
		final RoutingHelper routingHelper = app.getRoutingHelper();
		// 1. Logging services
		if (location != null) {
			app.getSavingTrackHelper().updateLocation(location);
			app.getLiveMonitoringHelper().updateLocation(location);
		}
		// 2. accessibility routing
		navigationInfo.setLocation(location);

		// 3. routing
		net.osmand.Location updatedLocation = location;
		if (routingHelper.isFollowingMode()) {
			if (location == null || isPointAccurateForRouting(location)) {
				// Update routing position and get location for sticking mode
				updatedLocation = routingHelper.setCurrentLocation(location, settings.SNAP_TO_ROAD.get());
			}
		}
		this.location = updatedLocation;
		
		// Update information
		updateLocation(location);
	}

	private void enhanceLocation(net.osmand.Location location) {
		if (location != null && isRunningOnEmulator()) {
			// only for emulator
			updateSpeedEmulator(location);
		}
	}

	public void checkIfLastKnownLocationIsValid() {
		net.osmand.Location loc = getLastKnownLocation();
		if (loc != null && (System.currentTimeMillis() - loc.getTime()) > INTERVAL_TO_CLEAR_SET_LOCATION) {
			setLocation(null);
		}
	}

	public NavigationInfo getNavigationInfo() {
		return navigationInfo;
	}

	public String getNavigationHint(LatLon point) {
		String hint = navigationInfo.getDirectionString(point, getHeading());
		if (hint == null)
			hint = app.getString(R.string.no_info);
		return hint;
	}

	public void emitNavigationHint() {
		final LatLon point = app.getTargetPointsHelper().getPointToNavigate();
		if (point != null) {
			if (app.getRoutingHelper().isRouteCalculated()) {
				app.getRoutingHelper().getVoiceRouter().announceCurrentDirection(getLastKnownLocation());
			} else {
				app.showToastMessage(getNavigationHint(point));
			}
		} else {
			app.showToastMessage(R.string.mark_final_location_first);
		}
	}

	public RouteDataObject getLastKnownRouteSegment() {
		return currentPositionHelper.getLastKnownRouteSegment(getLastKnownLocation());
	}

	public net.osmand.Location getLastKnownLocation() {
		return location;
	}


	public void showNavigationInfo(LatLon pointToNavigate, Context uiActivity) {
		getNavigationInfo().show(pointToNavigate, getHeading(), uiActivity);
		
	}
	
	public OsmAndLocationSimulation getLocationSimulation() {
		return locationSimulation;
	}
	
	
	public static class GPSInfo {
		public int foundSatellites = 0;
		public int usedSatellites = 0;
		public boolean fixed = false;
	}

}

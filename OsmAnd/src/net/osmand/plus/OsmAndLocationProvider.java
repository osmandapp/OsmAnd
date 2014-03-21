package net.osmand.plus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.osmand.GeoidAltitudeCorrection;
import net.osmand.PlatformUtil;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.access.NavigationInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.router.RouteSegmentResult;
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
	private static final int LOST_LOCATION_MSG_ID = OsmAndConstants.UI_HANDLER_LOCATION_SERVICE + 1;
	private static final int START_SIMULATE_LOCATION_MSG_ID = OsmAndConstants.UI_HANDLER_LOCATION_SERVICE + 2;
	private static final int RUN_SIMULATE_LOCATION_MSG_ID = OsmAndConstants.UI_HANDLER_LOCATION_SERVICE + 3;
	private static final long LOST_LOCATION_CHECK_DELAY = 18000;
	private static final long START_LOCATION_SIMULATION_DELAY = 2000;

	private static final float ACCURACY_FOR_GPX_AND_ROUTING = 50;

	private static final int GPS_TIMEOUT_REQUEST = 0;
	private static final int GPS_DIST_REQUEST = 0;
	private static final int NOT_SWITCH_TO_NETWORK_WHEN_GPS_LOST_MS = 12000;

	private long lastTimeGPSLocationFixed = 0;
	private boolean gpsSignalLost;
	private SimulationProvider simulatePosition = null;

	private boolean sensorRegistered = false;
	private float[] mGravs = new float[3];
	private float[] mGeoMags = new float[3];
	private float previousCorrectionValue = 360;
	
	
	
	private static final boolean USE_KALMAN_FILTER = true;
	private static final float KALMAN_COEFFICIENT = 0.04f;
	
	float avgValSin = 0;
	float avgValCos = 0;
	float lastValSin = 0;
	float lastValCos = 0;
	private float[] previousCompassValuesA = new float[50];
	private float[] previousCompassValuesB = new float[50];
	private int previousCompassIndA = 0;
	private int previousCompassIndB = 0;
	private boolean inUpdateValue = false;
	
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
	private float[] mRotationM =  new float[9];
	private OsmandPreference<Boolean> USE_MAGNETIC_FIELD_SENSOR_COMPASS;
	private OsmandPreference<Boolean> USE_FILTER_FOR_COMPASS;
	
	
	public class SimulationProvider {
		private int currentRoad;
		private int currentSegment;
		private QuadPoint currentPoint;
		private net.osmand.Location startLocation;
		private List<RouteSegmentResult> roads;
		
		
		public void startSimulation(List<RouteSegmentResult> roads, 
				net.osmand.Location currentLocation) {
			this.roads = roads;
			startLocation = new net.osmand.Location(currentLocation);
			long ms = System.currentTimeMillis();
			if(ms - startLocation.getTime() > 5000 ||
					ms < startLocation.getTime()) {
				startLocation.setTime(ms);
			}
			currentRoad = -1;
			int px = MapUtils.get31TileNumberX(currentLocation.getLongitude());
			int py = MapUtils.get31TileNumberY(currentLocation.getLatitude());
			double dist = 1000;
			for(int i = 0; i < roads.size(); i++) {
				RouteSegmentResult road = roads.get(i);
				boolean plus = road.getStartPointIndex() < road.getEndPointIndex();
				for(int j = road.getStartPointIndex() + 1; j <= road.getEndPointIndex(); ) {
					RouteDataObject obj = road.getObject();
					QuadPoint proj = MapUtils.getProjectionPoint31(px, py, obj.getPoint31XTile(j-1), obj.getPoint31YTile(j-1), 
							obj.getPoint31XTile(j), obj.getPoint31YTile(j));
					double dd = MapUtils.squareRootDist31((int)proj.x, (int)proj.y, px, py);
					if (dd < dist) {
						dist = dd;
						currentRoad = i;
						currentSegment = j;
						currentPoint = proj;
					}
					j += plus ? 1 : -1;
				}
			}
		}
		
		private float proceedMeters(float meters, net.osmand.Location l) {
			for(int i = currentRoad; i < roads.size(); i++) {
				RouteSegmentResult road = roads.get(i);
				boolean firstRoad = i == currentRoad;
				boolean plus = road.getStartPointIndex() < road.getEndPointIndex();
				for(int j = firstRoad ? currentSegment : road.getStartPointIndex() + 1; j <= road.getEndPointIndex(); ) {
					RouteDataObject obj = road.getObject();
					int st31x = obj.getPoint31XTile(j-1);
					int st31y = obj.getPoint31YTile(j-1);
					int end31x = obj.getPoint31XTile(j);
					int end31y = obj.getPoint31YTile(j);
					boolean last = i == roads.size() - 1 && j == road.getEndPointIndex();
					boolean first = firstRoad && j == currentSegment;
					if(first) {
						st31x = (int) currentPoint.x;
						st31y = (int) currentPoint.y;
					}
					double dd = MapUtils.measuredDist31(st31x, st31y, end31x, end31y);
					if(meters > dd && !last){
						meters -= dd;
					} else {
						int prx = (int) (st31x + (end31x - st31x) * (meters / dd));
						int pry = (int) (st31y + (end31y - st31y) * (meters / dd));
						l.setLongitude(MapUtils.get31LongitudeX(prx));
						l.setLatitude(MapUtils.get31LatitudeY(pry));
						return (float) Math.max(meters - dd, 0);
					}
					j += plus ? 1 : -1;
				}
			}
			return -1;
		}
		
		/**
		 * @return null if it is not available of far from boundaries
		 */
		public net.osmand.Location getSimulatedLocation() {
			if(!isSimulatedDataAvailable()) {
				return null;
			}
			
			net.osmand.Location loc = new net.osmand.Location("OsmAnd");
			loc.setSpeed(startLocation.getSpeed());
			loc.setAltitude(startLocation.getAltitude());
			loc.setTime(System.currentTimeMillis());
			float meters = startLocation.getSpeed() * ((System.currentTimeMillis() - startLocation.getTime()) / 1000);
			float proc = proceedMeters(meters, loc);
			if(proc < 0 || proc >= 100){
				return null;
			}
			return loc;
		}
		
		public boolean isSimulatedDataAvailable() {
			return startLocation != null && startLocation.getSpeed() > 0 && currentRoad >= 0;
		}
	}
	
	public OsmAndLocationProvider(OsmandApplication app) {
		this.app = app;
		navigationInfo = new NavigationInfo(app);
		settings = app.getSettings();
		USE_MAGNETIC_FIELD_SENSOR_COMPASS = settings.USE_MAGNETIC_FIELD_SENSOR_COMPASS;
		USE_FILTER_FOR_COMPASS = settings.USE_KALMAN_FILTER_FOR_COMPASS;
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
		
		List<String> providers = service.getProviders(true);
		for (String provider : providers) {
			if (provider == null || provider.equals(LocationManager.GPS_PROVIDER)) {
				continue;
			}
			try {
				NetworkListener networkListener = new NetworkListener();
				service.requestLocationUpdates(provider, GPS_TIMEOUT_REQUEST, GPS_DIST_REQUEST, networkListener);
				networkListeners.add(networkListener);
			} catch (IllegalArgumentException e) {
				Log.d(PlatformUtil.TAG, provider + " location provider not available"); //$NON-NLS-1$
			}
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
		List<String> ps = service.getProviders(true);
		if(ps == null) {
			return null;
		}
		List<String> providers = new ArrayList<String>(ps);
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
			if (USE_MAGNETIC_FIELD_SENSOR_COMPASS.get()) {
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
		if(inUpdateValue) {
			return;
		}
		synchronized (this) {
			inUpdateValue = true;
			try {
				float val = 0;
				switch (event.sensor.getType()) {
				case Sensor.TYPE_ACCELEROMETER:
					System.arraycopy(event.values, 0, mGravs, 0, 3);
					break;
				case Sensor.TYPE_MAGNETIC_FIELD:
					System.arraycopy(event.values, 0, mGeoMags, 0, 3);
					break;
				case Sensor.TYPE_ORIENTATION:
					val = event.values[0];
					break;
				default:
					return;
				}
				if (USE_MAGNETIC_FIELD_SENSOR_COMPASS.get()) {
					if (mGravs != null && mGeoMags != null) {
						boolean success = SensorManager.getRotationMatrix(mRotationM, null, mGravs, mGeoMags);
						if (!success) {
							return;
						}
						float[] orientation = SensorManager.getOrientation(mRotationM, new float[3]);
						val = (float) Math.toDegrees(orientation[0]);
					} else {
						return;
					}
				}
				val = calcScreenOrientationCorrection(val);
				val = calcGeoMagneticCorrection(val);

				float valRad = (float) (val / 180f * Math.PI);
				lastValSin = (float) Math.sin(valRad);
				lastValCos = (float) Math.cos(valRad);
				// lastHeadingCalcTime = System.currentTimeMillis();
				boolean filter = USE_FILTER_FOR_COMPASS.get(); //USE_MAGNETIC_FIELD_SENSOR_COMPASS.get();
				if (filter) {
					filterCompassValue();
				} else {
					avgValSin = lastValSin;
					avgValCos = lastValCos;
				}

				updateCompassVal();
			} finally {
				inUpdateValue = false;
			}
		}
	}

	private float calcGeoMagneticCorrection(float val) {
		if (previousCorrectionValue == 360 && getLastKnownLocation() != null) {
			net.osmand.Location l = getLastKnownLocation();
			GeomagneticField gf = new GeomagneticField((float) l.getLatitude(), (float) l.getLongitude(), (float) l.getAltitude(),
					System.currentTimeMillis());
			previousCorrectionValue = gf.getDeclination();
		}
		if (previousCorrectionValue != 360) {
			val += previousCorrectionValue;
		}
		return val;
	}

	private float calcScreenOrientationCorrection(float val) {
		if (currentScreenOrientation == 1) {
			val += 90;
		} else if (currentScreenOrientation == 2) {
			val += 180;
		} else if (currentScreenOrientation == 3) {
			val -= 90;
		}
		return val;
	}

	private void filterCompassValue() {
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
	}	

	private void updateCompassVal() {
		heading = (float) getAngle(avgValSin, avgValCos);
		for(OsmAndCompassListener c : compassListeners){
			c.updateCompassValue(heading);
		}
	}
	
	public Float getHeading() {
//		if (heading != null && lastValSin != avgValSin && System.currentTimeMillis() - lastHeadingCalcTime > 700) {
//			avgValSin = lastValSin;
//			avgValCos = lastValCos;
//			Arrays.fill(previousCompassValuesA, avgValSin);
//			Arrays.fill(previousCompassValuesB, avgValCos);
//			updateCompassVal();
//		}
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
	private LinkedList<LocationListener> networkListeners = new LinkedList<LocationListener>();

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
	private class NetworkListener implements LocationListener {

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
		while(!networkListeners.isEmpty()) {
			service.removeUpdates(networkListeners.poll());
		}
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
	
	
	private void scheduleCheckIfGpsLost(final net.osmand.Location location) {
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
					gpsSignalLost = true;
					if (routingHelper.isFollowingMode() && routingHelper.getLeftDistance() > 0) {
						routingHelper.getVoiceRouter().gpsLocationLost();
					}
					setLocation(null);
				}
			}, LOST_LOCATION_CHECK_DELAY);
			if (routingHelper.isFollowingMode() && routingHelper.getLeftDistance() > 0 && simulatePosition == null) {
				app.runMessageInUIThreadAndCancelPrevious(START_SIMULATE_LOCATION_MSG_ID, new Runnable() {

					@Override
					public void run() {
						net.osmand.Location lastKnown = getLastKnownLocation();
						if (lastKnown != null && lastKnown.getTime() > fixTime) {
							// false positive case, still strange how we got here with removeMessages
							return;
						}
						List<RouteSegmentResult> tunnel = routingHelper.getUpcomingTunnel(1000);
						if(tunnel != null) {
							simulatePosition = new SimulationProvider();
							simulatePosition.startSimulation(tunnel, location);
							simulatePositionImpl();
						}
					}
				}, START_LOCATION_SIMULATION_DELAY);
			}
		}
	}
	
	public void simulatePosition() {
		app.runMessageInUIThreadAndCancelPrevious(RUN_SIMULATE_LOCATION_MSG_ID, new Runnable() {

			@Override
			public void run() {
				simulatePositionImpl();
			}
		}, 600);
	}
	
	private void simulatePositionImpl() {
		if(simulatePosition != null){
			net.osmand.Location loc = simulatePosition.getSimulatedLocation();
			if(loc != null){
				setLocation(loc);
				simulatePosition();
			}  else {
				simulatePosition = null;
			}
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
		if(location != null) {
			simulatePosition = null;
			if(gpsSignalLost) {
				gpsSignalLost = false;
				final RoutingHelper routingHelper = app.getRoutingHelper();
				if (routingHelper.isFollowingMode() && routingHelper.getLeftDistance() > 0) {
					routingHelper.getVoiceRouter().gpsLocationRecover();
				}
			}
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
		updateLocation(this.location);
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

	public boolean emitNavigationHint() {
		final LatLon point = app.getTargetPointsHelper().getPointToNavigate();
		if (point != null) {
			if (app.getRoutingHelper().isRouteCalculated()) {
				app.getRoutingHelper().getVoiceRouter().announceCurrentDirection(getLastKnownLocation());
			} else {
				app.showToastMessage(getNavigationHint(point));
			}
			return true;
		} else {
			app.showShortToastMessage(R.string.access_mark_final_location_first);
			return false;
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

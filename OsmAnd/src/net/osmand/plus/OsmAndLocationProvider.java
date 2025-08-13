package net.osmand.plus;

import static android.content.Context.LOCATION_SERVICE;
import static android.location.LocationManager.GPS_PROVIDER;
import static android.location.LocationManager.NETWORK_PROVIDER;

import static net.osmand.plus.simulation.SimulationProvider.isTunnelLocationSimulated;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import net.osmand.GeoidAltitudeCorrection;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.StateChangedListener;
import net.osmand.binary.GeocodingUtilities.GeocodingResult;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.plus.auto.NavigationSession;
import net.osmand.plus.helpers.CurrentPositionHelper;
import net.osmand.plus.helpers.LocationCallback;
import net.osmand.plus.helpers.LocationServiceHelper;
import net.osmand.plus.helpers.TargetPoint;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.accessibility.NavigationInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.LocationSource;
import net.osmand.plus.simulation.OsmAndLocationSimulation;
import net.osmand.plus.simulation.SimulationProvider;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.router.RouteSegmentResult;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class OsmAndLocationProvider implements SensorEventListener {

	public static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(OsmAndLocationProvider.class);

	public static final int REQUEST_LOCATION_PERMISSION = 100;

	public interface OsmAndLocationListener {
		void updateLocation(net.osmand.Location location);
	}

	public interface OsmAndCompassListener {
		void updateCompassValue(float value);
	}

	private static final int LOST_LOCATION_MSG_ID = OsmAndConstants.UI_HANDLER_LOCATION_SERVICE + 1;
	private static final int START_SIMULATE_LOCATION_MSG_ID = OsmAndConstants.UI_HANDLER_LOCATION_SERVICE + 2;
	private static final int RUN_SIMULATE_LOCATION_MSG_ID = OsmAndConstants.UI_HANDLER_LOCATION_SERVICE + 3;
	private static final long LOST_LOCATION_CHECK_DELAY = 18000;
	private static final long START_LOCATION_SIMULATION_DELAY = 2000;
	private static final int UPCOMING_TUNNEL_DISTANCE = 250;

	public  static final float ACCURACY_FOR_GPX_AND_ROUTING = 50;

	public static final int NOT_SWITCH_TO_NETWORK_WHEN_GPS_LOST_MS = 12000;

	private static final long LOCATION_TIMEOUT_TO_BE_STALE = 1000 * 60 * 2; // 2 minutes
	private static final long STALE_LOCATION_TIMEOUT_TO_BE_GONE = 1000 * 60 * 20; // 20 minutes

	private static final long AGPS_TO_REDOWNLOAD = 16 * 60 * 60 * 1000; // 16 hours

	private static final int REQUESTS_BEFORE_CHECK_LOCATION = 100;
	private final AtomicInteger locationRequestsCounter = new AtomicInteger();
	private final AtomicInteger staleLocationRequestsCounter = new AtomicInteger();


	private long lastTimeGPSLocationFixed;
	private long lastTimeLocationFixed;
	private boolean gpsSignalLost;
	private SimulationProvider simulatePosition;

	private long cachedLocationTimeFix;
	private long timeToNotUseOtherGPS;
	private net.osmand.Location cachedLocation;
	private net.osmand.Location customLocation;
	private net.osmand.Location prevLocation;

	private boolean sensorRegistered;
	private final float[] mGravs = new float[3];
	private final float[] mGeoMags = new float[3];
	private float previousCorrectionValue = 360;

	private static final boolean USE_KALMAN_FILTER = true;
	private static final float KALMAN_COEFFICIENT = 0.04f;

	float avgValSin;
	float avgValCos;
	float lastValSin;
	float lastValCos;
	private final float[] previousCompassValuesA = new float[50];
	private final float[] previousCompassValuesB = new float[50];
	private int previousCompassIndA;
	private int previousCompassIndB;
	private boolean inUpdateValue;

	private Float heading;

	// Current screen orientation
	private int currentScreenOrientation;

	private final OsmandApplication app;

	private final NavigationInfo navigationInfo;
	private final CurrentPositionHelper currentPositionHelper;
	private final OsmAndLocationSimulation locationSimulation;
	private LocationServiceHelper locationServiceHelper;

	private net.osmand.Location location;

	private final GPSInfo gpsInfo = new GPSInfo();

	private List<OsmAndLocationListener> locationListeners = new ArrayList<>();
	private List<OsmAndCompassListener> compassListeners = new ArrayList<>();
	private GnssStatus.Callback gpsStatusListener;
	private final float[] mRotationM = new float[9];

	private StateChangedListener<LocationSource> locationSourceListener;

	public OsmAndLocationProvider(@NonNull OsmandApplication app) {
		this.app = app;
		navigationInfo = new NavigationInfo(app);
		currentPositionHelper = new CurrentPositionHelper(app);
		locationSimulation = new OsmAndLocationSimulation(app);
		locationServiceHelper = app.createLocationServiceHelper();
		addLocationSourceListener();
		addLocationListener(navigationInfo);
		addCompassListener(navigationInfo);
	}

	public void resumeAllUpdates() {
		LOG.info(">>>> resumeAllUpdates");

		registerOrUnregisterCompassListener(true);

		if (app.getSettings().isInternetConnectionAvailable()) {
			if (System.currentTimeMillis() - app.getSettings().AGPS_DATA_LAST_TIME_DOWNLOADED.get() > AGPS_TO_REDOWNLOAD) {
				// force an updated check for internet connectivity here before destroying A-GPS-data
				if (app.getSettings().isInternetConnectionAvailable(true)) {
					redownloadAGPS();
				}
			}
		}

		if (isLocationPermissionAvailable(app)) {
			LocationManager locationService = (LocationManager) app.getSystemService(LOCATION_SERVICE);
			registerGpsStatusListener(locationService);
			try {
				locationServiceHelper.requestLocationUpdates(new LocationCallback() {
					@Override
					public void onLocationResult(@NonNull List<net.osmand.Location> locations) {
						net.osmand.Location location = null;
						if (!locations.isEmpty()) {
							location = locations.get(locations.size() - 1);
							if (useOnlyGPS() && location.hasAccuracy() &&
									location.getAccuracy() > ACCURACY_FOR_GPX_AND_ROUTING) {
								// fused provider could return network locations
								return;
							}
							lastTimeGPSLocationFixed = System.currentTimeMillis();
						}
						if (!locationSimulation.isRouteAnimating()) {
							setLocation(location);
						}
					}
				});
			} catch (SecurityException e) {
				// Location service permission not granted
			} catch (IllegalArgumentException e) {
				// GPS location provider not available
			}
			// try to always ask for network provide : it is faster way to find location
			if (locationServiceHelper.isNetworkLocationUpdatesSupported()) {
				locationServiceHelper.requestNetworkLocationUpdates(new LocationCallback() {
					@Override
					public void onLocationResult(@NonNull List<net.osmand.Location> locations) {
						if (!locations.isEmpty() && !useOnlyGPS() && !locationSimulation.isRouteAnimating()) {
 							setLocation(locations.get(locations.size() - 1));
						}
					}
				});
			}
		}
	}

	public void redownloadAGPS() {
		try {
			LocationManager service = (LocationManager) app.getSystemService(LOCATION_SERVICE);
			// Issue 6410: Test not forcing cold start here
			//service.sendExtraCommand(LocationManager.GPS_PROVIDER,"delete_aiding_data", null);
			Bundle bundle = new Bundle();
			service.sendExtraCommand(GPS_PROVIDER, "force_xtra_injection", bundle);
			service.sendExtraCommand(GPS_PROVIDER, "force_time_injection", bundle);
			app.getSettings().AGPS_DATA_LAST_TIME_DOWNLOADED.set(System.currentTimeMillis());
		} catch (Exception e) {
			app.getSettings().AGPS_DATA_LAST_TIME_DOWNLOADED.set(0L);
		}
	}

	@SuppressLint("MissingPermission")
	private void registerGpsStatusListener(@NonNull LocationManager service) {
		if (hasFineLocationPermission(app)) {
			gpsStatusListener = new GpsStatusListener(gpsInfo);
			service.registerGnssStatusCallback(gpsStatusListener, null);
		}
	}

	@NonNull
	public GPSInfo getGPSInfo() {
		return gpsInfo;
	}

	public void updateScreenOrientation(int orientation) {
		currentScreenOrientation = orientation;
	}

	public void addLocationListener(@NonNull OsmAndLocationListener listener) {
		if (!locationListeners.contains(listener)) {
			locationListeners = CollectionUtils.addToList(locationListeners, listener);
		}
	}

	public void removeLocationListener(@NonNull OsmAndLocationListener listener) {
		locationListeners = CollectionUtils.removeFromList(locationListeners, listener);
	}

	public void addCompassListener(@NonNull OsmAndCompassListener listener) {
		if (!compassListeners.contains(listener)) {
			compassListeners = CollectionUtils.addToList(compassListeners, listener);
		}
	}

	public void removeCompassListener(@NonNull OsmAndCompassListener listener) {
		compassListeners = CollectionUtils.removeFromList(compassListeners, listener);
	}

	private void addLocationSourceListener() {
		locationSourceListener = change -> {
			pauseAllUpdates();
			locationServiceHelper = app.createLocationServiceHelper();
			resumeAllUpdates();
		};
		app.getSettings().LOCATION_SOURCE.addListener(locationSourceListener);
	}

	@Nullable
	public net.osmand.Location getFirstTimeRunDefaultLocation(@Nullable OsmAndLocationListener locationListener) {
		if (isLocationPermissionAvailable(app)) {
			LocationCallback callback = locationListener == null ? null : new LocationCallback() {
				@Override
				public void onLocationResult(@NonNull List<net.osmand.Location> locations) {
					locationListener.updateLocation(locations.isEmpty() ? null : locations.get(0));
				}
			};
			return locationServiceHelper.getFirstTimeRunDefaultLocation(callback);
		}
		return null;
	}

	public boolean hasOrientationSensor() {
		SensorManager sensorMgr = (SensorManager) app.getSystemService(Context.SENSOR_SERVICE);
		return hasOrientationSensor(sensorMgr);
	}

	public boolean hasOrientationSensor(@NonNull SensorManager sensorMgr) {
		return sensorMgr.getDefaultSensor(Sensor.TYPE_ORIENTATION) != null
				|| sensorMgr.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null;
	}

	public synchronized void registerOrUnregisterCompassListener(boolean register) {
		if (sensorRegistered && !register) {
			Log.d(PlatformUtil.TAG, "Disable sensor");
			((SensorManager) app.getSystemService(Context.SENSOR_SERVICE)).unregisterListener(this);
			sensorRegistered = false;
			heading = null;
		} else if (!sensorRegistered && register) {
			Log.d(PlatformUtil.TAG, "Enable sensor");
			SensorManager sensorMgr = (SensorManager) app.getSystemService(Context.SENSOR_SERVICE);
			if (app.getSettings().USE_MAGNETIC_FIELD_SENSOR_COMPASS.get() || !hasOrientationSensor(sensorMgr)) {
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
					Log.e(PlatformUtil.TAG, "Sensor orientation could not be enabled.");
					s = sensorMgr.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
					if (s == null || !sensorMgr.registerListener(this, s, SensorManager.SENSOR_DELAY_UI)) {
						Log.e(PlatformUtil.TAG, "Sensor rotation could not be enabled.");
					}
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
					speed = (d * 1000) / time;
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
		return loc != null && (!loc.hasAccuracy() || loc.getAccuracy() < ACCURACY_FOR_GPX_AND_ROUTING);
	}

	public static boolean isRunningOnEmulator() {
		return Build.DEVICE.equals("generic");
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// Attention : sensor produces a lot of events & can hang the system
		if (inUpdateValue) {
			return;
		}
		synchronized (this) {
			if (!sensorRegistered) {
				return;
			}
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
					case Sensor.TYPE_ROTATION_VECTOR:
						val = event.values[0];
						break;
					default:
						return;
				}
				OsmandSettings settings = app.getSettings();
				if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER || event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
					boolean success = SensorManager.getRotationMatrix(mRotationM, null, mGravs, mGeoMags);
					if (!success) {
						return;
					}
					float[] orientation = SensorManager.getOrientation(mRotationM, new float[3]);
					val = (float) Math.toDegrees(orientation[0]);
				} else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
					SensorManager.getRotationMatrixFromVector(mRotationM, event.values);
					float[] orientation = SensorManager.getOrientation(mRotationM, new float[3]);
					val = (float) Math.toDegrees(orientation[0]);
				}
				val = calcScreenOrientationCorrection(val);
				val = calcGeoMagneticCorrection(val);

				float valRad = (float) (val / 180f * Math.PI);
				lastValSin = (float) Math.sin(valRad);
				lastValCos = (float) Math.cos(valRad);
				// lastHeadingCalcTime = System.currentTimeMillis();
				boolean filter = settings.USE_KALMAN_FILTER_FOR_COMPASS.get();
				if (filter) {
					filterCompassValue();
				} else {
					avgValSin = lastValSin;
					avgValCos = lastValCos;
				}

				heading = getAngle(avgValSin, avgValCos);
				updateCompassVal();
			} finally {
				inUpdateValue = false;
			}
		}
	}

	private float calcGeoMagneticCorrection(float val) {
		net.osmand.Location l = getLastKnownLocation();
		if (previousCorrectionValue == 360 && l != null) {
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
		if (heading == null && previousCompassIndA == 0) {
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
		for (OsmAndCompassListener c : compassListeners) {
			c.updateCompassValue(heading);
		}
	}

	public Float getHeading() {
		return heading;
	}

	private float getAngle(float sinA, float cosA) {
		return MapUtils.unifyRotationTo360((float) (Math.atan2(sinA, cosA) * 180 / Math.PI));
	}

	private void updateLocation(net.osmand.Location location) {
		for (OsmAndLocationListener listener : locationListeners) {
			listener.updateLocation(location);
		}
	}

	private boolean useOnlyGPS() {
		if (app.getRoutingHelper().isFollowingMode()) {
			return true;
		}
		if (lastTimeGPSLocationFixed > 0 && (System.currentTimeMillis() - lastTimeGPSLocationFixed) < NOT_SWITCH_TO_NETWORK_WHEN_GPS_LOST_MS) {
			return true;
		}
		return isRunningOnEmulator();
	}

	private void stopLocationRequests() {
		LOG.info(">>>> stopLocationRequests");

		gpsInfo.reset();
		LocationManager service = (LocationManager) app.getSystemService(LOCATION_SERVICE);
		if (gpsStatusListener != null) {
			service.unregisterGnssStatusCallback(gpsStatusListener);
		}
		try {
			locationServiceHelper.removeLocationUpdates();
		} catch (SecurityException e) {
			// Location service permission not granted
		}
	}

	public void pauseAllUpdates() {
		stopLocationRequests();
		registerOrUnregisterCompassListener(false);
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
		if (VERSION.SDK_INT >= VERSION_CODES.O) {
			if (l.hasVerticalAccuracy()) {
				r.setVerticalAccuracy(l.getVerticalAccuracyMeters());
			}
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
			GeoidAltitudeCorrection geo = app.getResourceManager().getGeoidAltitudeCorrection();
			if (geo != null) {
				alt -= geo.getGeoidHeight(l.getLatitude(), l.getLongitude());
				r.setAltitude(alt);
			}
		}
		return r;
	}

	private void scheduleCheckIfGpsLost(@NonNull net.osmand.Location location) {
		RoutingHelper routingHelper = app.getRoutingHelper();
		if (routingHelper.isFollowingMode() && routingHelper.getLeftDistance() > 0 && simulatePosition == null) {
			long fixTime = location.getTime();
			app.runInUIThreadAndCancelPrevious(LOST_LOCATION_MSG_ID, () -> {
				net.osmand.Location lastKnown = getLastKnownLocation();
				if (lastKnown != null && lastKnown.getTime() > fixTime) {
					// false positive case, still strange how we got here with removeMessages
					return;
				}
				gpsSignalLost = true;
				if (routingHelper.isFollowingMode() && routingHelper.getLeftDistance() > 0
						&& simulatePosition == null) {
					routingHelper.getVoiceRouter().gpsLocationLost();
					setLocation(null);
				}
			}, LOST_LOCATION_CHECK_DELAY);
			app.runInUIThreadAndCancelPrevious(START_SIMULATE_LOCATION_MSG_ID, () -> {
				net.osmand.Location lastKnown = getLastKnownLocation();
				if (lastKnown != null && lastKnown.getTime() > fixTime) {
					// false positive case, still strange how we got here with removeMessages
					return;
				}
				// Speed 120kmh, 2 seconds -> 60 m
				List<RouteSegmentResult> tunnel = routingHelper.getUpcomingTunnel(UPCOMING_TUNNEL_DISTANCE);
				if (tunnel != null) {
					simulatePosition = new SimulationProvider(location, tunnel);
					scheduleSimulatedPositionRun();
				}
			}, START_LOCATION_SIMULATION_DELAY);
		}
	}

	private void scheduleSimulatedPositionRun() {
		if (simulatePosition != null) {
			net.osmand.Location loc = simulatePosition.getSimulatedLocationForTunnel();
			if (loc != null) {
				setLocation(loc);
				app.runInUIThreadAndCancelPrevious(RUN_SIMULATE_LOCATION_MSG_ID, this::scheduleSimulatedPositionRun, 600);
			} else {
				simulatePosition = null;
			}
		}
	}

	public void setCustomLocation(net.osmand.Location location, long ignoreLocationsTime) {
		timeToNotUseOtherGPS = System.currentTimeMillis() + ignoreLocationsTime;
		customLocation = location;
		setLocation(location);
	}

	private boolean shouldIgnoreLocation(net.osmand.Location location) {
		if (customLocation != null && timeToNotUseOtherGPS >= System.currentTimeMillis()) {
			return location == null || !Algorithms.stringsEqual(customLocation.getProvider(), location.getProvider());
		}
		return prevLocation != null && location != null && prevLocation.getTime() == location.getTime();
	}

	private net.osmand.Location setLocationForRouting(net.osmand.Location location, RoutingHelper routingHelper) {
		net.osmand.Location updatedLocation = location;
		if (routingHelper.isFollowingMode()) {
			if (location == null || isPointAccurateForRouting(location)) {
				// Update routing position and get location for sticking mode
				updatedLocation = routingHelper.setCurrentLocation(location, app.getSettings().SNAP_TO_ROAD.get());
			}
		} else if (routingHelper.isRoutePlanningMode() && app.getSettings().getPointToStart() == null) {
			routingHelper.setCurrentLocation(location, false);
		} else if (getLocationSimulation().isRouteAnimating()) {
			routingHelper.setCurrentLocation(location, false);
		}
		return updatedLocation;
	}

	public void setLocationFromService(net.osmand.Location location) {
		if (locationSimulation.isRouteAnimating() || shouldIgnoreLocation(location)) {
			return;
		}
		prevLocation = location;
		if (location != null && isPointAccurateForRouting(location) && !isTunnelLocationSimulated(location)) {
			lastTimeLocationFixed = System.currentTimeMillis();
			simulatePosition = null;
			notifyGpsLocationRecovered();
			scheduleCheckIfGpsLost(location);
		}
		RoutingHelper routingHelper = app.getRoutingHelper();
		app.getSavingTrackHelper().updateLocation(location, heading);
		app.getAverageSpeedComputer().updateLocation(location);
		app.getAverageGlideComputer().updateLocation(location);
		PluginsHelper.updateLocationPlugins(location);

		net.osmand.Location updatedLocation = setLocationForRouting(location, routingHelper);
		app.getWaypointHelper().locationChanged(location);
		NavigationSession carNavigationSession = app.getCarNavigationSession();
		if (carNavigationSession != null && carNavigationSession.hasStarted()) {
			carNavigationSession.updateLocation(location);
			this.location = updatedLocation;
			updateLocation(this.location);
		}
	}

	public void setLocationFromSimulation(net.osmand.Location location) {
		setLocation(location);
	}

	private void setLocation(@Nullable net.osmand.Location location) {
		if (shouldIgnoreLocation(location)) {
			return;
		}
		prevLocation = location;
		if (location == null) {
			gpsInfo.reset();
		}
		enhanceLocation(location);

		if (location != null && isPointAccurateForRouting(location) && !isTunnelLocationSimulated(location)) {
			// use because there is a bug on some devices with location.getTime()
			lastTimeLocationFixed = System.currentTimeMillis();
			simulatePosition = null;
			notifyGpsLocationRecovered();
			scheduleCheckIfGpsLost(location);
		}

		RoutingHelper routingHelper = app.getRoutingHelper();
		// 1. Logging services
		if (location != null) {
			app.getSavingTrackHelper().updateLocation(location, heading);
			app.getAverageSpeedComputer().updateLocation(location);
			app.getAverageGlideComputer().updateLocation(location);
			PluginsHelper.updateLocationPlugins(location);
		}

		// 2. routing
		net.osmand.Location updatedLocation = setLocationForRouting(location, routingHelper);
		app.getWaypointHelper().locationChanged(location);
		this.location = updatedLocation;

		// Update information
		updateLocation(this.location);
	}

	public void ensureLatestLocation() {
		if (prevLocation != null && (location == null || prevLocation.getTime() > location.getTime())) {
			cachedLocation = location = prevLocation;
			cachedLocationTimeFix = lastTimeLocationFixed;
			updateLocation(location);
		}
	}

	private void notifyGpsLocationRecovered() {
		if (gpsSignalLost) {
			gpsSignalLost = false;
			RoutingHelper routingHelper = app.getRoutingHelper();
			if (routingHelper.isFollowingMode() && routingHelper.getLeftDistance() > 0) {
				routingHelper.getVoiceRouter().gpsLocationRecover();
			}
		}
	}

	private void enhanceLocation(net.osmand.Location location) {
		if (location != null && isRunningOnEmulator()) {
			// only for emulator
			updateSpeedEmulator(location);
		}
	}

	public NavigationInfo getNavigationInfo() {
		return navigationInfo;
	}

	public String getNavigationHint(TargetPoint point) {
		String hint = navigationInfo.getDirectionString(point == null ? null : point.getLatLon(), getHeading());
		if (hint == null)
			hint = app.getString(R.string.no_info);
		return hint;
	}

	public String getNavigationHint(LatLon point) {
		String hint = navigationInfo.getDirectionString(point, getHeading());
		if (hint == null)
			hint = app.getString(R.string.no_info);
		return hint;
	}

	public void emitNavigationHint() {
		TargetPoint point = app.getTargetPointsHelper().getPointToNavigate();
		if (point != null) {
			if (app.getRoutingHelper().isRouteCalculated()) {
				app.getRoutingHelper().getVoiceRouter().announceCurrentDirection(getLastKnownLocation());
			} else {
				app.showToastMessage(getNavigationHint(point));
			}
		} else {
			app.showShortToastMessage(R.string.access_no_destination);
		}
	}

	public RouteDataObject getLastKnownRouteSegment() {
		return currentPositionHelper.getLastKnownRouteSegment(getLastKnownLocation());
	}

	public boolean getRouteSegment(net.osmand.Location loc,
	                               @Nullable ApplicationMode appMode,
	                               boolean cancelPreviousSearch,
	                               ResultMatcher<RouteDataObject> result) {
		return currentPositionHelper.getRouteSegment(loc, appMode, cancelPreviousSearch, result);
	}

	public boolean getGeocodingResult(net.osmand.Location loc, ResultMatcher<GeocodingResult> result) {
		return currentPositionHelper.getGeocodingResult(loc, result);
	}

	@Nullable
	public net.osmand.Location getLastKnownLocation() {
		net.osmand.Location loc = this.location;
		if (loc != null) {
			int counter = locationRequestsCounter.incrementAndGet();
			if (counter >= REQUESTS_BEFORE_CHECK_LOCATION && locationRequestsCounter.compareAndSet(counter, 0)) {
				if (System.currentTimeMillis() - lastTimeLocationFixed > LOCATION_TIMEOUT_TO_BE_STALE) {
					location = null;
				}
			}
		}
		return location;
	}

	@Nullable
	public net.osmand.Location getLastStaleKnownLocation() {
		net.osmand.Location newLoc = getLastKnownLocation();
		if (newLoc == null && cachedLocation != null) {
			int counter = staleLocationRequestsCounter.incrementAndGet();
			if (counter >= REQUESTS_BEFORE_CHECK_LOCATION && staleLocationRequestsCounter.compareAndSet(counter, 0)) {
				net.osmand.Location cached = cachedLocation;
				if (cached != null && System.currentTimeMillis() - cachedLocationTimeFix > STALE_LOCATION_TIMEOUT_TO_BE_GONE) {
					cachedLocation = null;
				}
			}
		} else {
			cachedLocation = newLoc;
			cachedLocationTimeFix = lastTimeLocationFixed;
		}
		return cachedLocation;
	}

	public void showNavigationInfo(TargetPoint pointToNavigate, Context uiActivity) {
		getNavigationInfo().show(pointToNavigate, getHeading(), uiActivity);

	}

	public OsmAndLocationSimulation getLocationSimulation() {
		return locationSimulation;
	}

	public static class GPSInfo {
		public int foundSatellites;
		public int usedSatellites;
		public boolean fixed;

		public void reset() {
			fixed = false;
			foundSatellites = 0;
			usedSatellites = 0;
		}
	}

	public boolean checkGPSEnabled(Context context) {
		if (!isGPSEnabled() && !isNetworkEnabled()) {
			// notify user
			AlertDialog.Builder dialog = new AlertDialog.Builder(context);
			dialog.setMessage(context.getResources().getString(R.string.gps_network_not_enabled));
			dialog.setPositiveButton(context.getResources().getString(R.string.shared_string_settings), (paramDialogInterface, paramInt) -> {
				Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				AndroidUtils.startActivityIfSafe(context, intent);
			});
			dialog.setNegativeButton(context.getString(R.string.shared_string_cancel), null);
			dialog.show();
			return false;
		}
		return true;
	}

	public boolean isGPSEnabled() {
		try {
			LocationManager manager = (LocationManager) app.getSystemService(LOCATION_SERVICE);
			return manager.isProviderEnabled(GPS_PROVIDER);
		} catch (Exception ignored) {
		}
		return false;
	}

	public boolean isNetworkEnabled() {
		try {
			LocationManager manager = (LocationManager) app.getSystemService(LOCATION_SERVICE);
			return manager.isProviderEnabled(NETWORK_PROVIDER);
		} catch (Exception ignored) {
		}
		return false;
	}

	public static boolean isLocationPermissionAvailable(@NonNull Context context) {
		boolean accessFineLocation = hasFineLocationPermission(context);
		boolean accessCoarseLocation = hasCoarseLocationPermission(context);
		return accessFineLocation || accessCoarseLocation;
	}

	public static boolean hasFineLocationPermission(@NonNull Context context) {
		return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
				== PackageManager.PERMISSION_GRANTED;
	}

	public static boolean hasCoarseLocationPermission(@NonNull Context context) {
		return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
				== PackageManager.PERMISSION_GRANTED;
	}

	public static void requestFineLocationPermissionIfNeeded(Activity activity) {
		if (!isLocationPermissionAvailable(activity)) {
			ActivityCompat.requestPermissions(activity, new String[] {
							Manifest.permission.ACCESS_FINE_LOCATION,
							Manifest.permission.ACCESS_COARSE_LOCATION},
					REQUEST_LOCATION_PERMISSION);
		}
	}
}

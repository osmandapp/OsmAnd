package net.osmand.core.samples.android.sample1;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.GeocodingUtilities;
import net.osmand.binary.GeocodingUtilities.GeocodingResult;
import net.osmand.data.LatLon;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class SampleLocationProvider implements SensorEventListener {

	public interface SampleLocationListener {
		void updateLocation(net.osmand.Location location);
	}

	public interface SampleCompassListener {
		void updateCompassValue(float value);
	}

	private static final int INTERVAL_TO_CLEAR_SET_LOCATION = 30 * 1000;

	private static final int GPS_TIMEOUT_REQUEST = 0;
	private static final int GPS_DIST_REQUEST = 0;
	private static final int NOT_SWITCH_TO_NETWORK_WHEN_GPS_LOST_MS = 12000;

	private long lastTimeGPSLocationFixed = 0;
	private boolean gpsSignalLost;

	private boolean sensorRegistered = false;
	private float[] mGravs = new float[3];
	private float[] mGeoMags = new float[3];
	private float previousCorrectionValue = 360;

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

	private SampleApplication app;
	private CurrentPositionHelper currentPositionHelper;

	private net.osmand.Location location = null;

	private GPSInfo gpsInfo = new GPSInfo();

	private List<SampleLocationListener> locationListeners = new ArrayList<>();
	private List<SampleCompassListener> compassListeners = new ArrayList<>();
	private Listener gpsStatusListener;
	private float[] mRotationM = new float[9];
	private static final long AGPS_TO_REDOWNLOAD = 16 * 60 * 60 * 1000; // 16 hours
	private long agpsDataLastTimeDownloaded;
	private boolean useMagneticFieldSensorCompass = false;

	public SampleLocationProvider(SampleApplication app) {
		this.app = app;
		currentPositionHelper = new CurrentPositionHelper(app);
	}

	public void resumeAllUpdates() {
		final LocationManager service = (LocationManager) app.getSystemService(Context.LOCATION_SERVICE);
		if (app.isInternetConnectionAvailable()) {
			if (System.currentTimeMillis() - agpsDataLastTimeDownloaded > AGPS_TO_REDOWNLOAD) {
				//force an updated check for internet connectivity here before destroying A-GPS-data
				if (app.isInternetConnectionAvailable(true)) {
					redownloadAGPS();
				}
			}
		}
		if (isLocationPermissionAvailable(app)) {
			service.addGpsStatusListener(getGpsStatusListener(service));
			try {
				service.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_TIMEOUT_REQUEST, GPS_DIST_REQUEST, gpsListener);
			} catch (IllegalArgumentException e) {
				Log.d(PlatformUtil.TAG, "GPS location provider not available"); //$NON-NLS-1$
			}
			// try to always ask for network provide : it is faster way to find location

			List<String> providers = service.getProviders(true);
			if (providers == null) {
				return;
			}
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
	}

	public void redownloadAGPS() {
		try {
			final LocationManager service = (LocationManager) app.getSystemService(Context.LOCATION_SERVICE);
			// Issue 6410: Test not forcing cold start here
			//service.sendExtraCommand(LocationManager.GPS_PROVIDER, "delete_aiding_data", null);
			Bundle bundle = new Bundle();
			service.sendExtraCommand(LocationManager.GPS_PROVIDER, "force_xtra_injection", bundle);
			service.sendExtraCommand(LocationManager.GPS_PROVIDER, "force_time_injection", bundle);
			agpsDataLastTimeDownloaded = System.currentTimeMillis();
		} catch (Exception e) {
			agpsDataLastTimeDownloaded = 0L;
			e.printStackTrace();
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

	public GPSInfo getGPSInfo() {
		return gpsInfo;
	}

	public void updateScreenOrientation(int orientation) {
		currentScreenOrientation = orientation;
	}

	public void addLocationListener(SampleLocationListener listener) {
		if (!locationListeners.contains(listener)) {
			locationListeners.add(listener);
		}
	}

	public void removeLocationListener(SampleLocationListener listener) {
		locationListeners.remove(listener);
	}

	public void addCompassListener(SampleCompassListener listener) {
		if (!compassListeners.contains(listener)) {
			compassListeners.add(listener);
		}
	}

	public void removeCompassListener(SampleCompassListener listener) {
		compassListeners.remove(listener);
	}

	public net.osmand.Location getFirstTimeRunDefaultLocation() {
		if (!isLocationPermissionAvailable(app)) {
			return null;
		}
		LocationManager service = (LocationManager) app.getSystemService(Context.LOCATION_SERVICE);
		List<String> ps = service.getProviders(true);
		if (ps == null) {
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

	public synchronized void registerOrUnregisterCompassListener(boolean register) {
		if (sensorRegistered && !register) {
			Log.d(PlatformUtil.TAG, "Disable sensor"); //$NON-NLS-1$
			((SensorManager) app.getSystemService(Context.SENSOR_SERVICE)).unregisterListener(this);
			sensorRegistered = false;
			heading = null;
		} else if (!sensorRegistered && register) {
			Log.d(PlatformUtil.TAG, "Enable sensor"); //$NON-NLS-1$
			SensorManager sensorMgr = (SensorManager) app.getSystemService(Context.SENSOR_SERVICE);
			if (useMagneticFieldSensorCompass) {
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
						val = event.values[0];
						break;
					default:
						return;
				}
				if (useMagneticFieldSensorCompass) {
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

				avgValSin = lastValSin;
				avgValCos = lastValCos;

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
		if (heading == null && previousCompassIndA == 0) {
			Arrays.fill(previousCompassValuesA, lastValSin);
			Arrays.fill(previousCompassValuesB, lastValCos);
			avgValSin = lastValSin;
			avgValCos = lastValCos;
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

	private void updateCompassVal() {
		heading = getAngle(avgValSin, avgValCos);
		for (SampleCompassListener c : compassListeners) {
			c.updateCompassValue(heading);
		}
	}

	public synchronized Float getHeading() {
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


	private void updateLocation(net.osmand.Location loc) {
		for (SampleLocationListener l : locationListeners) {
			l.updateLocation(loc);
		}
	}


	private LocationListener gpsListener = new LocationListener() {
		@Override
		public void onLocationChanged(Location location) {
			if (location != null) {
				lastTimeGPSLocationFixed = location.getTime();
			}
			setLocation(convertLocation(location, app));
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
		if ((System.currentTimeMillis() - lastTimeGPSLocationFixed) < NOT_SWITCH_TO_NETWORK_WHEN_GPS_LOST_MS) {
			return true;
		}
		return false;
	}

	// Working with location checkListeners
	private class NetworkListener implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			if (!useOnlyGPS()) {
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

	}

	;

	private void stopLocationRequests() {
		LocationManager service = (LocationManager) app.getSystemService(Context.LOCATION_SERVICE);
		service.removeGpsStatusListener(gpsStatusListener);
		service.removeUpdates(gpsListener);
		while (!networkListeners.isEmpty()) {
			service.removeUpdates(networkListeners.poll());
		}
	}

	public void pauseAllUpdates() {
		stopLocationRequests();
		registerOrUnregisterCompassListener(false);
	}

	public static net.osmand.Location convertLocation(Location l, SampleApplication app) {
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
		return r;
	}

	private void setLocation(net.osmand.Location location) {
		if (location == null) {
			updateGPSInfo(null);
		}
		if (location != null) {
			if (gpsSignalLost) {
				gpsSignalLost = false;
			}
		}
		this.location = location;

		// Update information
		updateLocation(this.location);
	}

	public void checkIfLastKnownLocationIsValid() {
		net.osmand.Location loc = getLastKnownLocation();
		if (loc != null && (System.currentTimeMillis() - loc.getTime()) > INTERVAL_TO_CLEAR_SET_LOCATION) {
			setLocation(null);
		}
	}

	/*
	public RouteDataObject getLastKnownRouteSegment() {
		return currentPositionHelper.getLastKnownRouteSegment(getLastKnownLocation());
	}

	public boolean getRouteSegment(net.osmand.Location loc, ResultMatcher<RouteDataObject> result) {
		return currentPositionHelper.getRouteSegment(loc, result);
	}
    */

	public boolean getGeocodingResult(net.osmand.Location loc, ResultMatcher<GeocodingResult> result) {
		return currentPositionHelper.getGeocodingResult(loc, result);
	}


	public net.osmand.Location getLastKnownLocation() {
		return location;
	}

	public LatLon getLastKnownLocationLatLon() {
		if (location != null) {
			return new LatLon(location.getLatitude(), location.getLongitude());
		} else {
			return null;
		}
	}

	public static class GPSInfo {
		public int foundSatellites = 0;
		public int usedSatellites = 0;
		public boolean fixed = false;
	}


	public boolean checkGPSEnabled(final Context context) {
		LocationManager lm = (LocationManager) app.getSystemService(Context.LOCATION_SERVICE);
		boolean gpsenabled = false;
		boolean networkenabled = false;

		try {
			gpsenabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
		} catch (Exception ex) {
		}

		try {
			networkenabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		} catch (Exception ex) {
		}

		if (!gpsenabled && !networkenabled) {
			// notify user
			AlertDialog.Builder dialog = new AlertDialog.Builder(context);
			dialog.setMessage(app.getString("gps_network_not_enabled"));
			dialog.setPositiveButton(app.getString("shared_string_settings"), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface paramDialogInterface, int paramInt) {
					Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
					context.startActivity(myIntent);
				}
			});
			dialog.setNegativeButton(context.getString(R.string.shared_string_cancel), null);
			dialog.show();
			return false;
		}
		return true;
	}

	public static boolean isLocationPermissionAvailable(Context context) {
		return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
				== PackageManager.PERMISSION_GRANTED;
	}
}

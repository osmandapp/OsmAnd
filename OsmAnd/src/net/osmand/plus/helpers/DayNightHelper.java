package net.osmand.plus.helpers;


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;

import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle.State;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.StateChangedListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.auto.NavigationSession;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.DayNightMode;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.util.SunriseSunset;

import org.apache.commons.logging.Log;

import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Class to help determine if we want to render day or night map - it uses the
 * DayNightMode enumeration for its behavior<BR> 
 * - it uses the LightSensor and needs calls from MapActivity on onPause and onResume to
 * register/unregister the sensor listener<BR>
 * - it uses the {@link SunriseSunset} and {@link LocationManager} to find 
 * out about sunset/sunrise and use it
 * 
 * Note: the usage of SunriseSunset is not optimized in any way, it is
 * recalculated on each demand. If this way it would be resource consuming, some
 * recalculation threshold could be specified to recalculate the sun-rise/set
 * only sometimes.<BR>
 * Note2: the light sensor threshold is hard coded to
 * {@link SensorManager#LIGHT_CLOUDY} and could be made customizable
 * 
 * @author pavol.zibrita
 */
public class DayNightHelper implements SensorEventListener {

	private static final Log log = PlatformUtil.getLog(DayNightHelper.class);
	
	private final OsmandApplication app;
	private final OsmandSettings settings;

	public DayNightHelper(OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();
	}

	private DayNightHelper listener;
	private MapThemeProvider mapThemeProvider;

	private long lastTime;
	private boolean lastNightMode;
	private StateChangedListener<Boolean> sensorStateListener;

	public boolean isNightModeForMapControls() {
		return isNightModeForMapControlsForProfile(settings.APPLICATION_MODE.get());
	}

	public boolean isNightModeForMapControlsForProfile(ApplicationMode mode) {
		if (settings.isLightContentForMode(mode)) {
			return isNightModeForProfile(mode);
		} else {
			return true;
		}
	}

	/**
	 * @return true if day is supposed to be
	 */
	public boolean isNightMode() {
		return isNightModeForProfile(settings.APPLICATION_MODE.get());
	}

	public boolean isNightModeForProfile(ApplicationMode mode) {
		DayNightMode dayNightMode = settings.DAYNIGHT_MODE.getModeValue(mode);
		if (mapThemeProvider != null) {
			DayNightMode providedTheme = mapThemeProvider.getMapTheme();
			dayNightMode = providedTheme != null ? providedTheme : dayNightMode;
		}
		NavigationSession carNavigationSession = app.getCarNavigationSession();
		if (carNavigationSession != null && carNavigationSession.isStateAtLeast(State.CREATED)) {
			boolean carDarkMode = carNavigationSession.getCarContext().isDarkMode();
			dayNightMode = carDarkMode ? DayNightMode.NIGHT : DayNightMode.DAY;
		}
		if (dayNightMode.isDay()) {
			return false;
		} else if (dayNightMode.isNight()) {
			return true;
		} else if (dayNightMode.isAuto()) { // We are in auto mode!
			long currentTime = System.currentTimeMillis();
			// allow recalculation each 60 seconds
			if (currentTime - lastTime > 60000) {
				lastTime = System.currentTimeMillis();
				try {
					SunriseSunset daynightSwitch = getSunriseSunset();
					if (daynightSwitch != null) {
						boolean daytime = daynightSwitch.isDaytime();
						log.debug("Sunrise/sunset setting to day: " + daytime); //$NON-NLS-1$
						lastNightMode = !daytime;
					}
				} catch (IllegalArgumentException e) {
					log.warn("Network location provider not available"); //$NON-NLS-1$
				} catch (SecurityException e) {
					log.warn("Missing permissions to get actual location!"); //$NON-NLS-1$
				}
			}
			return lastNightMode;
		} else if (dayNightMode.isSensor()) {
			return lastNightMode;
		}
		return false;
	}
	
	public SunriseSunset getSunriseSunset() {
		Location lastKnownLocation = app.getLocationProvider().getLastKnownLocation();
		if (lastKnownLocation == null) {
			lastKnownLocation = app.getLocationProvider().getFirstTimeRunDefaultLocation(null);
		}
		if (lastKnownLocation == null) {
			return null;
		}
		double longitude = lastKnownLocation.getLongitude();
		Date actualTime = new Date();
		return new SunriseSunset(lastKnownLocation.getLatitude(),
				longitude < 0 ? 360 + longitude : longitude,
				actualTime, TimeZone.getDefault());
	}

	public void setMapThemeProvider(@Nullable MapThemeProvider mapThemeProvider) {
		this.mapThemeProvider = mapThemeProvider;
	}

	public void stopSensorIfNeeded() {
		if (listener != null) {
			SensorManager mSensorManager = (SensorManager) app
					.getSystemService(Context.SENSOR_SERVICE);
			Sensor mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
			mSensorManager.unregisterListener(listener, mLight);
			listener = null;
		}
	}

	public void startSensorIfNeeded(StateChangedListener<Boolean> sensorStateListener) {
		this.sensorStateListener = sensorStateListener;
		DayNightMode dayNightMode = settings.DAYNIGHT_MODE.get();
		if (listener == null && dayNightMode.isSensor()) {
			SensorManager mSensorManager = (SensorManager) app.getSystemService(Context.SENSOR_SERVICE);
			Sensor mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
			List<Sensor> list = mSensorManager.getSensorList(Sensor.TYPE_LIGHT);
			log.info("Light sensors:" + list.size()); //$NON-NLS-1$
			mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);
			listener = this;
		} else if (listener != null && !dayNightMode.isSensor()) {
			stopSensorIfNeeded();
		}
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// nothing to do here
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.values.length > 0) {
			float lux = event.values[0];
			//			log.debug("lux value:" + lux + " setting to day: " + (lux > SensorManager.LIGHT_CLOUDY)); //$NON-NLS-1$ //$NON-NLS-2$
			boolean nightMode = !(lux > SensorManager.LIGHT_CLOUDY);
			if (nightMode != lastNightMode) {
				if (System.currentTimeMillis() - lastTime > 10000) {
					lastTime = System.currentTimeMillis();
					lastNightMode = nightMode;
					sensorStateListener.stateChanged(nightMode);
				}
			}
		}
	}

	public interface MapThemeProvider {
		DayNightMode getMapTheme();
	}
}

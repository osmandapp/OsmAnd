package net.osmand.plus.helpers;


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle.State;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.StateChangedListener;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.auto.NavigationSession;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.DayNightMode;
import net.osmand.plus.settings.enums.ThemeUsageContext;
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
 * <p>
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
	private final OsmAndLocationProvider locationProvider;

	private MapThemeProvider externalMapThemeProvider;
	private OsmAndLocationListener locationListener;
	private SensorEventListener sensorEventListener;
	private StateChangedListener<Boolean> sensorStateListener;
	private StateChangedListener<DayNightMode> preferenceStateListener;

	private long lastTime;
	private boolean lastNightMode;

	public DayNightHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		this.locationProvider = app.getLocationProvider();

		if (settings.DAYNIGHT_MODE.get().isAuto()) {
			locationProvider.addLocationListener(getLocationListener());
		}
		settings.DAYNIGHT_MODE.addListener(getPreferenceStateListener());
	}

	private void resetLastTime() {
		lastTime = 0;
	}

	public void setExternalMapThemeProvider(@Nullable MapThemeProvider externalMapThemeProvider) {
		this.externalMapThemeProvider = externalMapThemeProvider;
	}

	@NonNull
	private OsmAndLocationListener getLocationListener() {
		if (locationListener == null) {
			locationListener = location -> app.runInUIThread(() -> {
				resetLastTime();
				app.getOsmandMap().refreshMap();
				locationProvider.removeLocationListener(locationListener);
			});
		}
		return locationListener;
	}

	@NonNull
	private StateChangedListener<DayNightMode> getPreferenceStateListener() {
		if (preferenceStateListener == null) {
			preferenceStateListener = dayNightMode -> {
				if (dayNightMode.isAuto()) {
					resetLastTime();
					locationProvider.addLocationListener(getLocationListener());
				} else {
					locationProvider.removeLocationListener(getLocationListener());
				}
			};
		}
		return preferenceStateListener;
	}

	/**
	 * @deprecated This method relies on the active application mode, which may produce incorrect theme results
	 * when editing a profile different from the active one. Use {@link #isNightMode(ApplicationMode, ThemeUsageContext)}
	 * instead and pass an explicit {@link ApplicationMode} to ensure correct theme resolution.
	 */
	@Deprecated
	public boolean isNightMode(@NonNull ThemeUsageContext usageContext) {
		return isNightMode(settings.getApplicationMode(), usageContext);
	}

	public boolean isNightMode(@NonNull ApplicationMode appMode,
	                           @NonNull ThemeUsageContext usageContext) {
		boolean appNightMode = !settings.isLightContentForMode(appMode);
		if (usageContext == ThemeUsageContext.APP) {
			return appNightMode;
		}

		if (usageContext == ThemeUsageContext.OVER_MAP && appNightMode) {
			// Use Dark theme for UI over map if App Theme is Dark
			return true;
		}

		DayNightMode dayNightMode = settings.DAYNIGHT_MODE.getModeValue(appMode);
		if (externalMapThemeProvider != null) {
			DayNightMode providedTheme = externalMapThemeProvider.getMapTheme();
			dayNightMode = providedTheme != null ? providedTheme : dayNightMode;
		}

		NavigationSession carNavigationSession = app.getCarNavigationSession();
		if (carNavigationSession != null && carNavigationSession.isStateAtLeast(State.CREATED)) {
			boolean carDarkMode = carNavigationSession.getCarContext().isDarkMode();
			dayNightMode = carDarkMode ? DayNightMode.NIGHT : DayNightMode.DAY;
		}

		if (dayNightMode.isDay()) {
			return false;
		}
		if (dayNightMode.isNight()) {
			return true;
		}
		if (dayNightMode.isAuto()) {
			long currentTime = System.currentTimeMillis();
			// allow recalculation each 60 seconds
			if (currentTime - lastTime > 60000) {
				lastTime = currentTime;
				try {
					SunriseSunset daynightSwitch = getSunriseSunset();
					if (daynightSwitch != null) {
						boolean daytime = daynightSwitch.isDaytime();
						log.debug("Sunrise/sunset setting to day: " + daytime);
						lastNightMode = !daytime;
					}
				} catch (IllegalArgumentException e) {
					log.warn("Network location provider not available");
				} catch (SecurityException e) {
					log.warn("Missing permissions to get actual location!");
				}
			}
			return lastNightMode;
		}
		if (dayNightMode.isSensor()) {
			return lastNightMode;
		}
		if (dayNightMode.isAppTheme()) {
			return appNightMode;
		}
		return false;
	}

	@Nullable
	public SunriseSunset getSunriseSunset() {
		Location location = locationProvider.getLastKnownLocation();
		if (location == null) {
			location = locationProvider.getFirstTimeRunDefaultLocation(null);
		}
		if (location == null) {
			return null;
		}
		return getSunriseSunset(location.getLatitude(), location.getLongitude(), new Date());
	}

	@NonNull
	public SunriseSunset getSunriseSunset(double lat, double lon, Date actualTime) {
		if (lon < 0) {
			lon = 360 + lon;
		}
		return new SunriseSunset(lat, lon, actualTime, TimeZone.getDefault());
	}

	public void stopSensorIfNeeded() {
		if (sensorEventListener != null) {
			SensorManager manager = (SensorManager) app.getSystemService(Context.SENSOR_SERVICE);
			Sensor mLight = manager.getDefaultSensor(Sensor.TYPE_LIGHT);
			manager.unregisterListener(sensorEventListener, mLight);
			sensorEventListener = null;
		}
	}

	public void startSensorIfNeeded(@NonNull StateChangedListener<Boolean> sensorStateListener) {
		this.sensorStateListener = sensorStateListener;
		DayNightMode dayNightMode = settings.DAYNIGHT_MODE.get();
		if (sensorEventListener == null && dayNightMode.isSensor()) {
			SensorManager manager = (SensorManager) app.getSystemService(Context.SENSOR_SERVICE);
			Sensor light = manager.getDefaultSensor(Sensor.TYPE_LIGHT);
			List<Sensor> list = manager.getSensorList(Sensor.TYPE_LIGHT);
			log.info("Light sensors:" + list.size());
			manager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);
			sensorEventListener = this;
		} else if (sensorEventListener != null && !dayNightMode.isSensor()) {
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
//			log.debug("lux value:" + lux + " setting to day: " + (lux > SensorManager.LIGHT_CLOUDY));
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

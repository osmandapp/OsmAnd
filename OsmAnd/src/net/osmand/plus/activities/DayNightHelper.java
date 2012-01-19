package net.osmand.plus.activities;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import net.osmand.LogUtil;
import net.osmand.SunriseSunset;
import net.osmand.plus.OsmandSettings.DayNightMode;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;

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
	private static final Log log = LogUtil.getLog(DayNightHelper.class);
	
	private final OsmandApplication osmandApplication;

	public DayNightHelper(OsmandApplication osmandApplication) {
		this.osmandApplication = osmandApplication;
		setDayNightMode(osmandApplication.getSettings().DAYNIGHT_MODE.get());
	}

	DayNightMode dayNightMode = DayNightMode.AUTO;
	private DayNightHelper listener;
	private float lux = SensorManager.LIGHT_SUNLIGHT;

	private long lastAutoCall = 0;
	private Boolean lastAutoValue = null;
	
	public void setDayNightMode(DayNightMode mode) {
		if (this.dayNightMode != mode) {
			this.dayNightMode = mode;
			osmandApplication.getResourceManager().getRenderer().clearCache();
			unregisterServiceListener();
			if(dayNightMode.isSensor()){
				registerServiceListener();
			}
		}
	}

	/**
	 * @return null if could not be determined (in case of error)
	 * @return true if day is supposed to be 
	 */
	public Boolean getDayNightRenderer() {
		if (dayNightMode.isDay()) {
			return Boolean.TRUE;
		} else if (dayNightMode.isNight()) {
			return Boolean.FALSE;
		} else // We are in auto mode!
		if (dayNightMode.isAuto()) {
			long currentTime = System.currentTimeMillis();
			// allow recalculation each 60 seconds
			if (currentTime - lastAutoCall > 60000) {
				lastAutoCall = System.currentTimeMillis();
				try {
					Location lastKnownLocation = getLocation();
					if (lastKnownLocation == null) {
						return null;
					}
					double longitude = lastKnownLocation.getLongitude();
					Date actualTime = new Date();
					SunriseSunset daynightSwitch = new SunriseSunset(lastKnownLocation.getLatitude(),
																	 longitude < 0 ? 360 - longitude : longitude, actualTime,
																	 TimeZone.getDefault());
					boolean daytime = daynightSwitch.isDaytime();
					log.debug("Sunrise/sunset setting to day: " + daytime); //$NON-NLS-1$
					lastAutoValue = Boolean.valueOf(daytime);
					return lastAutoValue;
				} catch (IllegalArgumentException e) {
					log.warn("Network location provider not available"); //$NON-NLS-1$
				} catch (SecurityException e) {
					log.warn("Missing permitions to get actual location!"); //$NON-NLS-1$
				}
				return null;
			} else {
				return lastAutoValue;
			}
		} else if (dayNightMode.isSensor()) {
			log.debug("lux value:" + lux + " setting to day: " + (lux > SensorManager.LIGHT_CLOUDY)); //$NON-NLS-1$ //$NON-NLS-2$
			return lux > SensorManager.LIGHT_CLOUDY ? Boolean.TRUE : Boolean.FALSE;
		}
		return null;
	}

	private Location getLocation() {
		Location lastKnownLocation = null;
		LocationManager locationProvider = (LocationManager) osmandApplication.getSystemService(Context.LOCATION_SERVICE);
		List<String> providers = new ArrayList<String>(locationProvider.getProviders(true));
		//note, passive provider is from API_LEVEL 8 but it is a constant, we can check for it.
		// constant should not be changed in future
		int passiveFirst = providers.indexOf("passive"); //LocationManager.PASSIVE_PROVIDER
		//put passive provider to first place
		if (passiveFirst > -1) {
			providers.add(0,providers.remove(passiveFirst));
		}
		//find location
		for (String provider : providers) {
			lastKnownLocation = locationProvider.getLastKnownLocation(provider);
			if (lastKnownLocation == null) {
				break;
			}
		}
		return lastKnownLocation;
	}

	public void onMapPause() {
		unregisterServiceListener();
	}

	private void unregisterServiceListener() {
		if (listener != null) {
			SensorManager mSensorManager = (SensorManager) osmandApplication
					.getSystemService(Context.SENSOR_SERVICE);
			Sensor mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
			mSensorManager.unregisterListener(listener, mLight);
			listener = null;
		}
	}

	public void onMapResume() {
		registerServiceListener();
	}

	private void registerServiceListener() {
		if (listener == null && dayNightMode.isSensor()) {
			SensorManager mSensorManager = (SensorManager) osmandApplication.getSystemService(Context.SENSOR_SERVICE);
			Sensor mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
			List<Sensor> list = mSensorManager.getSensorList(Sensor.TYPE_LIGHT);
			log.info("Light sensors:" + list.size()); //$NON-NLS-1$
			mSensorManager.registerListener(this, mLight,
					SensorManager.SENSOR_DELAY_NORMAL);
			listener = this;
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// nothing to do here
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.values.length > 0) {
			lux = event.values[0];
		}
	}
}

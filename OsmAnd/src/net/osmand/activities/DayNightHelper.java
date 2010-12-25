package net.osmand.activities;

import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import net.osmand.LogUtil;
import net.osmand.OsmandSettings;
import net.osmand.OsmandSettings.DayNightMode;
import net.osmand.SunriseSunset;
import net.osmand.render.RendererRegistry;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;

/**
 * Class to help determine if we want to render day or night map
 * - it uses the DayNightMode enumeration for its behavior
 * - it uses the {@link RendererRegistry} to check if the night part is present
 * - it uses the LightSensor and needs calls from MapActivity on onPause and onResume to register/unregister
 *   the sensor listener
 * - it uses the {@link SunriseSunset} and {@link LocationManager} to find out about sunset/sunrise and use it
 * 
 * Note: the usage of SunriseSunset is not optimized in any way, it is recalculated on each demand. If this
 * way it would be resource consuming, some recalculation threshold could be specified to recalculate the sun-rise/set
 * only sometimes.
 * Note2: the light sensor threshold is hard coded to {@link SensorManager#LIGHT_CLOUDY} and could be made customizable
 * 
 * @author pavol.zibrita
 */
public class DayNightHelper implements SensorEventListener {
	private static final Log log = LogUtil.getLog(DayNightHelper.class);
	
	String currentRenderName = "";
	boolean daynightcheck = false;
	boolean sensorcheck = false;
	private final OsmandApplication osmandApplication;

	public DayNightHelper(OsmandApplication osmandApplication) {
		this.osmandApplication = osmandApplication;
		setDayNightMode(OsmandSettings.getDayNightMode(OsmandSettings
				.getPrefs(osmandApplication)));
	}

	DayNightMode dayNightMode = DayNightMode.AUTO;
	private DayNightHelper listener;
	private float lux = SensorManager.LIGHT_SUNLIGHT;

	public void setDayNightMode(DayNightMode mode) {
		if (this.dayNightMode != mode) {
			this.dayNightMode = mode;
			this.currentRenderName = "";
			this.daynightcheck = false;
			this.sensorcheck = false;
			osmandApplication.getResourceManager().getRenderer().clearCache();
		}
		unregisterServiceListener();
		registerServiceListener();
	}

	public boolean getDayNightRenderer() {
		RendererRegistry registry = RendererRegistry.getRegistry();
		String name = registry.getCurrentSelectedRenderer().name;
		boolean day = true;
		if (!currentRenderName.equals(name)) {
			currentRenderName = name;
			if (registry.hasDayNightRenderer(name)) {
				if (dayNightMode.isDay()) {
					day = true;
				} else if (dayNightMode.isNight()) {
					day = false;
				} else if (dayNightMode.isAuto()) {
					daynightcheck = true;
				} else if (dayNightMode.isSensor()) {
					sensorcheck = true;
					registerServiceListener();
				}
			}
		}
		// We are in auto mode!
		if (daynightcheck) {
			LocationManager locationProvider = (LocationManager) osmandApplication
					.getSystemService(Context.LOCATION_SERVICE); // Or use
																	// LocationManager.GPS_PROVIDER
			Location lastKnownLocation = locationProvider
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			SunriseSunset daynightSwitch = new SunriseSunset(
					lastKnownLocation.getLatitude(),
					lastKnownLocation.getLongitude(), new Date(), TimeZone
							.getDefault().getRawOffset());
			day = daynightSwitch.isDaytime();
			log.debug("Sunrise/sunset setting to day: " + day);
		}
		if (sensorcheck) {
			day = lux > SensorManager.LIGHT_CLOUDY;
			log.debug("lux value:" + lux + " setting to day: " + day);
		}
		return day;
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
		if (dayNightMode.isSensor() && sensorcheck) {
			SensorManager mSensorManager = (SensorManager) osmandApplication
					.getSystemService(Context.SENSOR_SERVICE);
			Sensor mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
			List<Sensor> list = mSensorManager.getSensorList(Sensor.TYPE_LIGHT);
			log.info("Light sensors:" + list.size());
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

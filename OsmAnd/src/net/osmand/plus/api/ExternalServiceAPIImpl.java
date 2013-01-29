package net.osmand.plus.api;

import org.apache.commons.logging.Log;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;

public class ExternalServiceAPIImpl implements ExternalServiceAPI {

	private OsmandApplication app;
	private static final Log log = PlatformUtil.getLog(ExternalServiceAPIImpl.class);

	public ExternalServiceAPIImpl(OsmandApplication app) {
		this.app = app;
	}

	@Override
	public boolean isWifiConnected() {
		ConnectivityManager mgr =  (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = mgr.getActiveNetworkInfo();
		return ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI;
	}

	@Override
	public boolean isInternetConnected() {
		ConnectivityManager mgr = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo active = mgr.getActiveNetworkInfo();
		if(active == null){
			return false;
		} else {
			NetworkInfo.State state = active.getState();
			return state != NetworkInfo.State.DISCONNECTED && state != NetworkInfo.State.DISCONNECTING;
		}
	}

	@Override
	public boolean isLightSensorEnabled() {
		SensorManager mSensorManager = (SensorManager)app.getSystemService(Context.SENSOR_SERVICE);         
	    Sensor mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		return mLight != null;
	}

	@Override
	public String getExternalStorageDirectory() {
		return Environment.getExternalStorageDirectory().getAbsolutePath();
	}

	@Override
	public AudioFocusHelper getAudioFocuseHelper() {
		if (android.os.Build.VERSION.SDK_INT >= 8) {
			try {
				return (AudioFocusHelper) Class.forName("net.osmand.plus.api.AudioFocusHelperImpl").newInstance();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				return null;
			}
		}
		return null;
	}

}

package net.osmand.plus.helpers;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.PowerManager;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.routing.VoiceRouter;

public class LockHelper implements SensorEventListener {
	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(LockHelper.class);

	private static final int SENSOR_SENSITIVITY = 4;

	private final SensorManager mSensorManager;
	private final Sensor mProximity;
	private PowerManager.WakeLock wakeLock = null;
	private Handler uiHandler;
	private OsmandApplication app;
	private LockRunnable lockRunnable;
	private LockUIAdapter lockUIAdapter;
	private OsmandSettings settings;

	private VoiceRouter.VoiceMessageListener voiceMessageListener;

	public interface LockUIAdapter {

		void lock();

		void unlock();
	}

	private class LockRunnable implements Runnable {
		@Override
		public void run() {
			lock();
		}
	}

	public LockHelper(final OsmandApplication app) {
		this.app = app;
		uiHandler = new Handler();
		lockRunnable = new LockRunnable();
		settings = app.getSettings();

		mSensorManager = (SensorManager) app.getSystemService(Context.SENSOR_SERVICE);
		mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

		voiceMessageListener = new VoiceRouter.VoiceMessageListener() {
			@Override
			public void onVoiceMessage() {
				unlockEvent();
			}
		};
		
		refreshGlobalSettings();
	}

	private void releaseWakeLocks() {
		if (wakeLock != null) {
			if (wakeLock.isHeld()) {
				wakeLock.release();
			}
			wakeLock = null;
		}
	}

	private void unlock(long timeInMills) {
		releaseWakeLocks();
		if (lockUIAdapter != null) {
			lockUIAdapter.unlock();
		}

		PowerManager pm = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
				| PowerManager.ACQUIRE_CAUSES_WAKEUP, "tso:wakelocktag");
		wakeLock.acquire(timeInMills);
	}

	private void lock() {
		releaseWakeLocks();
		if (lockUIAdapter != null) {
			lockUIAdapter.lock();
		}
	}

	private void timedUnlock(long millis) {
		uiHandler.removeCallbacks(lockRunnable);
		unlock(millis);
		uiHandler.postDelayed(lockRunnable, millis);
	}

	public void unlockEvent() {
		int screenPowerSave = app.getSettings().TURN_SCREEN_ON_TIME_INT.get();

		if (screenPowerSave > 0) {
			timedUnlock(screenPowerSave * 1000L);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
			if (event.values[0] >= -SENSOR_SENSITIVITY && event.values[0] <= SENSOR_SENSITIVITY) {
				unlockEvent();
			}
		}
	}

	public void setSensor(boolean enable) {
		if (enable) {
			mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
		} else {
			mSensorManager.unregisterListener(this);
		}
	}

	public void setVoiceRouterListener(boolean enable) {
		VoiceRouter vr = app.getRoutingHelper().getVoiceRouter();
		if (enable) {
			vr.addVoiceMessageListener(voiceMessageListener);
		} else {
			vr.removeVoiceMessageListener(voiceMessageListener);
		}
	}

	public void refreshProfilesSettings() {
		boolean isVRListenerEnabled = settings.TURN_SCREEN_ON_ROUTER.get();
		setVoiceRouterListener(isVRListenerEnabled);
	}
	
	public void refreshGlobalSettings() {
		boolean isSensorEnabled = settings.TURN_SCREEN_ON_SENSOR.get();
		setSensor(isSensorEnabled);
	}

	public void setLockUIAdapter(LockUIAdapter adapter) {
		lockUIAdapter = adapter;
	}

}
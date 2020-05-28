package net.osmand.plus.helpers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.routing.VoiceRouter.VoiceMessageListener;
import net.osmand.plus.settings.backend.OsmAndAppCustomization.OsmAndAppCustomizationListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.OsmandSettings.CommonPreference;

import java.util.List;

public class LockHelper implements SensorEventListener {

	private static final int SENSOR_SENSITIVITY = 4;

	@Nullable
	private WakeLock wakeLock = null;

	private Handler uiHandler;
	private OsmandApplication app;
	private CommonPreference<Integer> turnScreenOnTime;
	private CommonPreference<Boolean> turnScreenOnSensor;
	private CommonPreference<Boolean> turnScreenOnNavigationInstructions;

	@Nullable
	private LockUIAdapter lockUIAdapter;
	private Runnable lockRunnable;
	private VoiceMessageListener voiceMessageListener;

	public interface LockUIAdapter {

		void lock();

		void unlock();
	}

	public LockHelper(final OsmandApplication app) {
		this.app = app;
		uiHandler = new Handler();
		OsmandSettings settings = app.getSettings();
		turnScreenOnTime = settings.TURN_SCREEN_ON_TIME_INT;
		turnScreenOnSensor = settings.TURN_SCREEN_ON_SENSOR;
		turnScreenOnNavigationInstructions = settings.TURN_SCREEN_ON_NAVIGATION_INSTRUCTIONS;

		lockRunnable = new Runnable() {
			@Override
			public void run() {
				lock();
			}
		};
		voiceMessageListener = new VoiceMessageListener() {
			@Override
			public void onVoiceMessage(List<String> listCommands, List<String> played) {
				if (turnScreenOnNavigationInstructions.get()) {
					unlockEvent();
				}
			}
		};
		OsmAndAppCustomizationListener customizationListener = new OsmAndAppCustomizationListener() {
			@Override
			public void onOsmAndSettingsCustomized() {
				OsmandSettings settings = app.getSettings();
				turnScreenOnTime = settings.TURN_SCREEN_ON_TIME_INT;
				turnScreenOnSensor = settings.TURN_SCREEN_ON_SENSOR;
				turnScreenOnNavigationInstructions = settings.TURN_SCREEN_ON_NAVIGATION_INSTRUCTIONS;
			}
		};
		app.getAppCustomization().addListener(customizationListener);
		app.getRoutingHelper().getVoiceRouter().addVoiceMessageListener(voiceMessageListener);
	}

	private void releaseWakeLocks() {
		if (wakeLock != null) {
			if (wakeLock.isHeld()) {
				wakeLock.release();
			}
			wakeLock = null;
		}
	}

	@SuppressLint("WakelockTimeout")
	private void unlock() {
		if (lockUIAdapter != null) {
			lockUIAdapter.unlock();
		}
		PowerManager pm = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
		if (pm != null) {
			wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
					| PowerManager.ACQUIRE_CAUSES_WAKEUP, "OsmAnd:OnVoiceWakeupTag");
			wakeLock.acquire();
		}
	}

	private void lock() {
		releaseWakeLocks();
		int unlockTime = turnScreenOnTime.get();
		if (lockUIAdapter != null && isFollowingMode() && unlockTime != -1) {
			lockUIAdapter.lock();
		}
	}

	private boolean isFollowingMode() {
		return app.getRoutingHelper().isFollowingMode();
	}

	private void timedUnlock(final long millis) {
		uiHandler.removeCallbacks(lockRunnable);
		if (wakeLock == null) {
			uiHandler.post(new Runnable() {
				@Override
				public void run() {
					if (wakeLock == null) {
						unlock();
					}
				}
			});
		}
		if (millis > 0) {
			uiHandler.postDelayed(lockRunnable, millis);
		}
	}

	private void unlockEvent() {
		int unlockTime = turnScreenOnTime.get();
		if (unlockTime > 0) {
			timedUnlock(unlockTime * 1000L);
		} else if (unlockTime == -1) {
			timedUnlock(-1);
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

	private void switchSensorOn() {
		switchSensor(true);
	}

	private void switchSensorOff() {
		switchSensor(false);
	}

	private void switchSensor(boolean on) {
		SensorManager sensorManager = (SensorManager) app.getSystemService(Context.SENSOR_SERVICE);
		if (sensorManager != null) {
			Sensor proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
			if (on) {
				sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
			} else {
				sensorManager.unregisterListener(this);
			}
		}
	}

	private boolean isSensorEnabled() {
		return turnScreenOnSensor.get() && isFollowingMode();
	}

	public void onStart(@NonNull Activity activity) {
		switchSensorOff();
	}

	public void onStop(@NonNull Activity activity) {
		lock();
		if (!activity.isFinishing() && isSensorEnabled()) {
			switchSensorOn();
		}
	}

	public void setLockUIAdapter(@Nullable LockUIAdapter adapter) {
		lockUIAdapter = adapter;
	}

}
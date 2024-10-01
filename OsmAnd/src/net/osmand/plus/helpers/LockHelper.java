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
import android.provider.Settings;
import android.view.GestureDetector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.StateChangedListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.actions.LockScreenAction;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmAndAppCustomization.OsmAndAppCustomizationListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.routing.VoiceRouter.VoiceMessageListener;
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState;

import org.apache.commons.logging.Log;

import java.util.List;

public class LockHelper implements SensorEventListener, StateChangedListener<ApplicationMode> {

	private static final Log LOG = PlatformUtil.getLog(LockHelper.class);

	private static final int SENSOR_SENSITIVITY = 4;

	private static boolean lockScreen = false;

	@Nullable
	private WakeLock wakeLock;

	private final Handler uiHandler;
	private final OsmandApplication app;
	private CommonPreference<Integer> turnScreenOnTime;
	private CommonPreference<Boolean> turnScreenOnSensor;
	private CommonPreference<Boolean> useSystemScreenTimeout;
	private CommonPreference<Boolean> turnScreenOnPowerButton;
	private CommonPreference<Boolean> turnScreenOnNavigationInstructions;

	private ApplicationMode lastApplicationMode;
	@Nullable
	private LockUIAdapter lockUIAdapter;
	private final Runnable lockRunnable;
	private final VoiceMessageListener voiceMessageListener;
	private LockGestureDetector gestureDetector;

	public interface LockUIAdapter {

		void lock();

		void unlock();
	}

	public LockHelper(OsmandApplication app) {
		this.app = app;
		uiHandler = new Handler();
		OsmandSettings settings = app.getSettings();
		turnScreenOnTime = settings.TURN_SCREEN_ON_TIME_INT;
		turnScreenOnSensor = settings.TURN_SCREEN_ON_SENSOR;
		useSystemScreenTimeout = settings.USE_SYSTEM_SCREEN_TIMEOUT;
		turnScreenOnPowerButton = settings.TURN_SCREEN_ON_POWER_BUTTON;
		turnScreenOnNavigationInstructions = settings.TURN_SCREEN_ON_NAVIGATION_INSTRUCTIONS;

		settings.APPLICATION_MODE.addListener(this);
		lockRunnable = this::lock;
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
				useSystemScreenTimeout = settings.USE_SYSTEM_SCREEN_TIMEOUT;
				turnScreenOnPowerButton = settings.TURN_SCREEN_ON_POWER_BUTTON;
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
	public void unlock() {
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

	public void lock() {
		releaseWakeLocks();
		if (lockUIAdapter != null) {
			boolean useSystemTimeout = useSystemScreenTimeout.get();
			boolean usePowerButton = useSystemTimeout && turnScreenOnPowerButton.get()
					|| !useSystemTimeout && turnScreenOnTime.get() == 0 && turnScreenOnPowerButton.get();
			if (!usePowerButton) {
				lockUIAdapter.lock();
			}
		}
	}

	private void timedUnlock(long millis) {
		uiHandler.removeCallbacks(lockRunnable);
		if (wakeLock == null) {
			uiHandler.post(() -> {
				if (wakeLock == null) {
					unlock();
				}
			});
		}
		if (millis > 0) {
			uiHandler.postDelayed(lockRunnable, millis);
		}
	}

	private void unlockEvent() {
		int unlockTime = getUnlockTime();
		if (unlockTime > 0) {
			timedUnlock(unlockTime * 1000L);
		} else {
			timedUnlock(0);
		}
	}

	private int getUnlockTime() {
		int unlockTime = turnScreenOnTime.get();
		if (useSystemScreenTimeout.get()) {
			try {
				int screenOffTimeout = Settings.System.getInt(app.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 0);
				if (screenOffTimeout > 0) {
					unlockTime = screenOffTimeout / 1000;
				}
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
		}
		return unlockTime;
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

	public void onStart() {
		switchSensor(false);
	}

	public void onStop(@NonNull Activity activity) {
		lock();
		if (!activity.isFinishing() && turnScreenOnSensor.get()) {
			switchSensor(true);
		}
	}

	public void setLockUIAdapter(@Nullable LockUIAdapter adapter) {
		lockUIAdapter = adapter;
	}

	/**
	 * LockScreenAction part
	 */


	public void toggleLockScreen() {
		lockScreen = !lockScreen;
	}

	public boolean isScreenLocked() {
		return lockScreen;
	}

	public void unlockScreen() {
		lockScreen = false;
	}

	@Override
	public void stateChanged(ApplicationMode mode) {
		List<QuickActionButtonState> buttonStates = app.getMapButtonsHelper().getButtonsStates();
		ApplicationMode currentMode = app.getSettings().getApplicationMode();

		if (isScreenLocked() && lastApplicationMode != currentMode) {
			if (!hasLockScreenAction(buttonStates)) {
				unlockScreen();
			}
		}
		lastApplicationMode = currentMode;
	}

	private boolean hasLockScreenAction(@NonNull List<QuickActionButtonState> mapButtonStates) {
		for (QuickActionButtonState buttonState : mapButtonStates) {
			if (!buttonState.isEnabled()) {
				continue;
			}
			for (QuickAction action : buttonState.getQuickActions()) {
				if (action instanceof LockScreenAction) {
					return true;
				}
			}
		}
		return false;
	}

	public GestureDetector getLockGestureDetector(@NonNull MapActivity mapActivity) {
		if (gestureDetector == null) {
			gestureDetector = LockGestureDetector.getDetector(mapActivity);
		}
		return gestureDetector;
	}
}
package net.osmand.plus.helpers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;
import android.view.GestureDetector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.StateChangedListener;
import net.osmand.plus.OsmAndConstants;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LockHelper implements SensorEventListener, StateChangedListener<ApplicationMode> {

	private static final Log LOG = PlatformUtil.getLog(LockHelper.class);
	private static final int LOCK_SCREEN_MESSAGE = OsmAndConstants.UI_HANDLER_MAP_VIEW + 8;
	private static final int LOCK_TIMEOUT_MESSAGE = OsmAndConstants.UI_HANDLER_MAP_VIEW + 9;

	private static final int SENSOR_SENSITIVITY = 4;

	private static boolean lockScreen = false;

	private final KeyguardManager keyguardManager;
	private final PowerManager powerManager;
	private final WakeLock wakeLock;

	private final Handler uiHandler;
	private final OsmandApplication app;
	private CommonPreference<Integer> turnScreenOnTime;
	private CommonPreference<Boolean> turnScreenOnSensor;
	private CommonPreference<Boolean> useSystemScreenTimeout;
	private CommonPreference<Boolean> turnScreenOnPowerButton;
	private CommonPreference<Boolean> turnScreenOnNavigationInstructions;

	private ApplicationMode lastApplicationMode;
	private final Set<String> keepScreenAwakeReasons = new HashSet<>();
	@Nullable
	private LockUIAdapter lockUIAdapter;
	private final VoiceMessageListener voiceMessageListener;
	private LockGestureDetector gestureDetector;

	public interface LockUIAdapter {

		// Prevents the display from timing out normally; does not wake it.
		void setKeepScreenOn(boolean keepScreenOn);

		boolean getKeepScreenOn();

		// Flags to show app on top of the keyguard
		void setKeyguardFlags(boolean enable);
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

		voiceMessageListener = (listCommands, played) -> {
			if (turnScreenOnNavigationInstructions.get()) {
				doUnlock();
			}
		};

		OsmAndAppCustomizationListener customizationListener = () -> {
			OsmandSettings osmSettings = app.getSettings();
			turnScreenOnTime = osmSettings.TURN_SCREEN_ON_TIME_INT;
			turnScreenOnSensor = osmSettings.TURN_SCREEN_ON_SENSOR;
			useSystemScreenTimeout = osmSettings.USE_SYSTEM_SCREEN_TIMEOUT;
			turnScreenOnPowerButton = osmSettings.TURN_SCREEN_ON_POWER_BUTTON;
			turnScreenOnNavigationInstructions = osmSettings.TURN_SCREEN_ON_NAVIGATION_INSTRUCTIONS;
		};
		app.getAppCustomization().addListener(customizationListener);
		app.getRoutingHelper().getVoiceRouter().addVoiceMessageListener(voiceMessageListener);
		
		keyguardManager = (KeyguardManager) app.getSystemService(Context.KEYGUARD_SERVICE);
		powerManager = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK
					| PowerManager.ACQUIRE_CAUSES_WAKEUP, "OsmAnd:OnVoiceWakeupTag");
	}

	@SuppressLint("WakelockTimeout")
	public void doUnlock() {
		if (lockUIAdapter != null && keyguardManager.isKeyguardLocked()) {
			lockUIAdapter.setKeyguardFlags(true);
		}

		if (!powerManager.isInteractive()
				|| keyguardManager.isKeyguardLocked()
				|| keyguardManager.isDeviceLocked()) {
			if (!wakeLock.isHeld()) {
				wakeLock.acquire();
			}
		}

		restartLockTimeout();
	}

	public void doLock() {
		doKeepOn(false);

		if (lockUIAdapter != null) {
			lockUIAdapter.setKeyguardFlags(turnScreenOnPowerButton.get());
		}
	}

	private void doKeepOn(boolean keepOn) {
		if (keepOn) {
			if (!wakeLock.isHeld() && powerManager.isInteractive()) {
				wakeLock.acquire();
			}
		} else {
			if (wakeLock.isHeld()) {
				wakeLock.release();
			}
			
			if (lockUIAdapter != null) {
				if (lockUIAdapter.getKeepScreenOn()) {
					lockUIAdapter.setKeepScreenOn(false);
				}
			}
		}
	}

	public void addKeepScreenAwakeReason(@NonNull String reason) {
		if (keepScreenAwakeReasons.add(reason) && keepScreenAwakeReasons.size() == 1) {
			restartLockTimeout();
		}
	}

	public void removeKeepScreenAwakeReason(@NonNull String reason) {
		if (keepScreenAwakeReasons.remove(reason) && keepScreenAwakeReasons.isEmpty()) {
			restartLockTimeout();
		}
	}

	/**
	 * The map holds the screen on for the "Timeout after wake-up" since the last interaction,
	 * 0 s ("Keep screen on") means no timeout. The screen is never turned off earlier than the
	 * system does it on its own - an application can only keep it on for longer.
	 *
	 * Some corner cases to consider:
	 * 		doUnlock() when still clearly on (eg upon voice instruction)
	 *		doUnlock() when about to turn back off from wake-up and showing notifications
	 *		the short period after system timeout, when screen off but phone not yet locked
	 */
	public void restartLockTimeout() {
		uiHandler.removeMessages(LOCK_TIMEOUT_MESSAGE);
		doKeepOn(true);

		int timeout = turnScreenOnTime.get();
		if (useSystemScreenTimeout.get()) {
			try {
				int screenOffTimeout = Settings.System.getInt(app.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 0);
				if (screenOffTimeout > 0) {
					timeout = screenOffTimeout / 1000;
				}
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
		}

		if (!keepScreenAwakeReasons.isEmpty() || timeout == 0) {
			doKeepOn(true);
		} else {
			Message message = Message.obtain(uiHandler, this::doLock);
			message.what = LOCK_TIMEOUT_MESSAGE;
			uiHandler.sendMessageDelayed(message, timeout * 1000L);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
			if (event.values[0] >= -SENSOR_SENSITIVITY && event.values[0] <= SENSOR_SENSITIVITY) {
				doUnlock();
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
		doLock();
		if (!activity.isFinishing() && turnScreenOnSensor.get()) {
			switchSensor(true);
		}
	}

	public void setLockUIAdapter(@Nullable LockUIAdapter adapter) {
		lockUIAdapter = adapter;
	}

	/**
	 * LockScreenAction part
	 * This feature disables touch input to prevent accidental clicks on the screen.
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
		List<QuickActionButtonState> buttonStates = app.getMapButtonsHelper().getQuickActionButtonsStates();
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
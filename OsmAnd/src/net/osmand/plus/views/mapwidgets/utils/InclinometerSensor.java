package net.osmand.plus.views.mapwidgets.utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Display;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Measures the vehicle pitch (positive = front raised) and roll (positive = right side lowered)
 * from the device gravity vector. {@link #calibrate()} stores the current device attitude as the
 * level position, so the device can be mounted at any angle; without calibration the device is
 * assumed to lie flat with the top of the screen forward. The sensor is registered only while at
 * least one {@link InclinometerListener} is active. Must be used from the main thread.
 */
public class InclinometerSensor implements SensorEventListener {

	private static final Log LOG = PlatformUtil.getLog(InclinometerSensor.class);

	private static final String CALIBRATION_PREF_ID = "inclinometer_calibration";
	private static final long NOTIFY_INTERVAL_MS = 100;
	private static final long LISTENERS_CHECK_INTERVAL_MS = 1000;
	private static final float ACCELEROMETER_FILTER_ALPHA = 0.2f;
	private static final float MAX_ANGLE_DEGREES = 85f;
	private static final float MIN_FORWARD_PROJECTION = 0.05f;

	public interface InclinometerListener {

		void onInclinationChanged(float pitchDegrees, float rollDegrees);

		// false when the listener no longer needs updates; the sensor is released once every listener is inactive
		boolean isInclinometerActive();
	}

	private final OsmandApplication app;
	private final CommonPreference<String> calibrationPref;
	private final Handler handler = new Handler(Looper.getMainLooper());
	private final List<InclinometerListener> listeners = new ArrayList<>();
	private final Runnable listenersCheck = this::removeInactiveListeners;

	private final float[] gravity = new float[3];
	private boolean hasGravity;
	private boolean registered;
	private boolean usingAccelerometer;
	private long lastNotifyTime;

	private float pitch;
	private float roll;

	// Level position in device coordinates, null until calibrated
	@Nullable
	private float[] calibratedUp;
	@Nullable
	private float[] calibratedForward;

	public InclinometerSensor(@NonNull OsmandApplication app) {
		this.app = app;
		calibrationPref = app.getSettings().registerStringPreference(CALIBRATION_PREF_ID, "").makeGlobal();
		parseCalibration(calibrationPref.get());
	}

	public boolean isSensorAvailable() {
		return getTiltSensor() != null;
	}

	public boolean hasValue() {
		return hasGravity;
	}

	public float getPitch() {
		return pitch;
	}

	public float getRoll() {
		return roll;
	}

	public boolean isCalibrated() {
		return calibratedUp != null && calibratedForward != null;
	}

	/**
	 * @return the device tilt stored as the level position, {pitch, roll} in degrees
	 * relative to the device lying flat, or null when not calibrated
	 */
	@Nullable
	public float[] getCalibrationOffset() {
		if (calibratedUp == null) {
			return null;
		}
		float[] forward = getScreenTopAxis();
		float[] right = cross(forward, new float[] {0, 0, 1});
		float offsetPitch = (float) Math.toDegrees(Math.asin(clampUnit(dot(calibratedUp, forward))));
		float offsetRoll = -(float) Math.toDegrees(Math.asin(clampUnit(dot(calibratedUp, right))));
		return new float[] {offsetPitch, offsetRoll};
	}

	public void requestUpdates(@NonNull InclinometerListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
		registerIfNeeded();
		if (hasGravity) {
			listener.onInclinationChanged(pitch, roll);
		}
	}

	public void removeListener(@NonNull InclinometerListener listener) {
		listeners.remove(listener);
		if (listeners.isEmpty()) {
			unregister();
		}
	}

	/**
	 * Stores the current device attitude as the level position of the vehicle.
	 *
	 * @return false when no sensor reading is available yet
	 */
	public boolean calibrate() {
		if (!hasGravity) {
			return false;
		}
		float[] up = normalize(gravity);
		// Vehicle forward direction: top of the screen for a device lying flat,
		// back of the device for an upright one facing the driver; a blend covers the mounts in between
		float[] top = getScreenTopAxis();
		float[] forward = {top[0], top[1], top[2] - 1};
		float along = dot(forward, up);
		forward = new float[] {forward[0] - up[0] * along, forward[1] - up[1] * along, forward[2] - up[2] * along};
		if (length(forward) < MIN_FORWARD_PROJECTION) {
			forward = top;
		}
		forward = normalize(forward);

		calibratedUp = up;
		calibratedForward = forward;
		calibrationPref.set(String.format(Locale.US, "%.5f,%.5f,%.5f,%.5f,%.5f,%.5f",
				up[0], up[1], up[2], forward[0], forward[1], forward[2]));
		computeInclination();
		notifyListeners();
		return true;
	}

	public void resetCalibration() {
		calibratedUp = null;
		calibratedForward = null;
		calibrationPref.resetToDefault();
		if (hasGravity) {
			computeInclination();
			notifyListeners();
		}
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		float[] values = event.values;
		if (usingAccelerometer && hasGravity) {
			for (int i = 0; i < 3; i++) {
				gravity[i] += ACCELEROMETER_FILTER_ALPHA * (values[i] - gravity[i]);
			}
		} else {
			System.arraycopy(values, 0, gravity, 0, 3);
		}
		if (length(gravity) == 0) {
			return;
		}
		hasGravity = true;
		computeInclination();

		long now = SystemClock.elapsedRealtime();
		if (now - lastNotifyTime >= NOTIFY_INTERVAL_MS) {
			lastNotifyTime = now;
			notifyListeners();
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	private void computeInclination() {
		float[] up = normalize(gravity);
		float[] forward;
		float[] levelUp;
		if (calibratedUp != null && calibratedForward != null) {
			levelUp = calibratedUp;
			forward = calibratedForward;
		} else {
			levelUp = new float[] {0, 0, 1};
			forward = getScreenTopAxis();
		}
		float[] right = cross(forward, levelUp);
		pitch = clampAngle((float) Math.toDegrees(Math.asin(clampUnit(dot(up, forward)))));
		roll = clampAngle(-(float) Math.toDegrees(Math.asin(clampUnit(dot(up, right)))));
	}

	private void notifyListeners() {
		for (InclinometerListener listener : new ArrayList<>(listeners)) {
			listener.onInclinationChanged(pitch, roll);
		}
	}

	private void registerIfNeeded() {
		if (registered) {
			return;
		}
		SensorManager sensorManager = getSensorManager();
		Sensor sensor = getTiltSensor();
		if (sensorManager == null || sensor == null) {
			LOG.error("Tilt sensor is not available");
			return;
		}
		usingAccelerometer = sensor.getType() == Sensor.TYPE_ACCELEROMETER;
		hasGravity = false;
		if (sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)) {
			registered = true;
			handler.postDelayed(listenersCheck, LISTENERS_CHECK_INTERVAL_MS);
		} else {
			LOG.error("Tilt sensor could not be enabled");
		}
	}

	private void unregister() {
		if (!registered) {
			return;
		}
		SensorManager sensorManager = getSensorManager();
		if (sensorManager != null) {
			sensorManager.unregisterListener(this);
		}
		registered = false;
		hasGravity = false;
		handler.removeCallbacks(listenersCheck);
	}

	private void removeInactiveListeners() {
		Iterator<InclinometerListener> iterator = listeners.iterator();
		while (iterator.hasNext()) {
			if (!iterator.next().isInclinometerActive()) {
				iterator.remove();
			}
		}
		if (listeners.isEmpty()) {
			unregister();
		} else {
			handler.postDelayed(listenersCheck, LISTENERS_CHECK_INTERVAL_MS);
		}
	}

	@Nullable
	private SensorManager getSensorManager() {
		return (SensorManager) app.getSystemService(Context.SENSOR_SERVICE);
	}

	@Nullable
	private Sensor getTiltSensor() {
		SensorManager sensorManager = getSensorManager();
		if (sensorManager == null) {
			return null;
		}
		Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
		return sensor != null ? sensor : sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	}

	/**
	 * @return the device axis that points to the top of the screen in the current display rotation
	 */
	@NonNull
	private float[] getScreenTopAxis() {
		Display display = AndroidUtils.getDisplay(app);
		int rotation = display != null ? display.getRotation() : Surface.ROTATION_0;
		return switch (rotation) {
			case Surface.ROTATION_90 -> new float[] {1, 0, 0};
			case Surface.ROTATION_180 -> new float[] {0, -1, 0};
			case Surface.ROTATION_270 -> new float[] {-1, 0, 0};
			default -> new float[] {0, 1, 0};
		};
	}

	private void parseCalibration(@Nullable String value) {
		if (Algorithms.isEmpty(value)) {
			return;
		}
		String[] parts = value.split(",");
		if (parts.length != 6) {
			return;
		}
		try {
			float[] up = {Float.parseFloat(parts[0]), Float.parseFloat(parts[1]), Float.parseFloat(parts[2])};
			float[] forward = {Float.parseFloat(parts[3]), Float.parseFloat(parts[4]), Float.parseFloat(parts[5])};
			if (length(up) > 0 && length(forward) > 0) {
				calibratedUp = normalize(up);
				calibratedForward = normalize(forward);
			}
		} catch (NumberFormatException e) {
			LOG.error("Invalid inclinometer calibration: " + value, e);
		}
	}

	private static float clampAngle(float degrees) {
		return Math.max(-MAX_ANGLE_DEGREES, Math.min(MAX_ANGLE_DEGREES, degrees));
	}

	private static float clampUnit(float value) {
		return Math.max(-1f, Math.min(1f, value));
	}

	private static float dot(@NonNull float[] a, @NonNull float[] b) {
		return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
	}

	@NonNull
	private static float[] cross(@NonNull float[] a, @NonNull float[] b) {
		return new float[] {
				a[1] * b[2] - a[2] * b[1],
				a[2] * b[0] - a[0] * b[2],
				a[0] * b[1] - a[1] * b[0]};
	}

	private static float length(@NonNull float[] v) {
		return (float) Math.sqrt(dot(v, v));
	}

	@NonNull
	private static float[] normalize(@NonNull float[] v) {
		float length = length(v);
		return new float[] {v[0] / length, v[1] / length, v[2] / length};
	}
}

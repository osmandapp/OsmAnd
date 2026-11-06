package net.osmand.plus.views.layers;

/**
 * Decides whether the My Location marker should show the movement (bearing) icon.
 * GPS speed of a standing device jitters around a few tenths of m/s, so a single
 * threshold makes the marker flip between the stationary and the movement icon.
 * The speed is smoothed with a time-based exponential moving average, and the state
 * changes with hysteresis: it starts moving above {@link #START_SPEED} and stops below
 * {@link #STOP_SPEED}.
 */
public class MovementStateFilter {

	static final float START_SPEED = 0.5f; // m/s
	static final float STOP_SPEED = 0.2f; // m/s
	static final float TIME_CONSTANT_SEC = 2f;
	static final float RESET_GAP_SEC = 10f;

	private boolean moving;
	private float smoothedSpeed;
	private long lastTime;
	private boolean initialized;

	/**
	 * @param time  fix time in ms; repeated calls with the same fix do not change the state
	 * @param speed GPS speed in m/s
	 * @return true if the device is considered moving
	 */
	public boolean update(long time, float speed) {
		if (initialized && time == lastTime) {
			return moving;
		}
		float dt = (time - lastTime) / 1000f;
		if (!initialized || dt < 0 || dt > RESET_GAP_SEC) {
			smoothedSpeed = speed;
			moving = speed > START_SPEED;
		} else {
			float alpha = (float) (1 - Math.exp(-dt / TIME_CONSTANT_SEC));
			smoothedSpeed += alpha * (speed - smoothedSpeed);
			moving = moving ? smoothedSpeed >= STOP_SPEED : smoothedSpeed > START_SPEED;
		}
		lastTime = time;
		initialized = true;
		return moving;
	}
}

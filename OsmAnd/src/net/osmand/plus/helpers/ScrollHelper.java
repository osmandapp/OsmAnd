package net.osmand.plus.helpers;

import android.view.KeyEvent;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.osmand.plus.settings.backend.OsmandSettings.PARROT_EXTERNAL_DEVICE;
import static net.osmand.plus.settings.backend.OsmandSettings.WUNDERLINQ_EXTERNAL_DEVICE;

public class ScrollHelper {

	private final static int LONG_PRESS_TIME_MS = 250;
	private final static int MAX_KEY_UP_TIME_MS = 10;
	private final static int REFRESHING_DELAY_MS = 3;
	private final static int INVALID_VALUE = -1;

	private OsmandApplication app;
	private OnScrollEventListener onScrollEventListener;
	
	private final Direction UP = new Direction(KeyEvent.KEYCODE_DPAD_UP);
	private final Direction DOWN = new Direction(KeyEvent.KEYCODE_DPAD_DOWN);
	private final Direction LEFT = new Direction(KeyEvent.KEYCODE_DPAD_LEFT);
	private final Direction RIGHT = new Direction(KeyEvent.KEYCODE_DPAD_RIGHT);

	private final Map<Integer, Direction> availableDirections;
	private boolean isInContinuousScrolling = false;
	private long startContinuousScrollingTime = INVALID_VALUE;

	private Runnable scrollingRunnable = new Runnable() {
		@Override
		public void run() {
			isInContinuousScrolling = true;
			while (hasActiveDirections()) {
				notifyListener(true);
				try {
					Thread.sleep(REFRESHING_DELAY_MS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			isInContinuousScrolling = false;
		}
	};

	public ScrollHelper(OsmandApplication app) {
		this.app = app;
		
		availableDirections = new HashMap<Integer, Direction>() {
			{
				put(UP.keyCode, UP);
				put(DOWN.keyCode, DOWN);
				put(LEFT.keyCode, LEFT);
				put(RIGHT.keyCode, RIGHT);
			}
		};
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (isInContinuousScrolling) {
			addDirection(keyCode);
		} else {
			startScrolling(keyCode);
			return true;
		}
		return true;
	}

	public boolean onKeyUp(int keyCode, KeyEvent event) {
		removeDirection(keyCode);
		boolean shortPress = !hasActiveDirections() && ((System.currentTimeMillis() - startContinuousScrollingTime) < LONG_PRESS_TIME_MS);
		if (shortPress) {
			List<Direction> lastDirections = getLastDirections();
			addDirections(lastDirections);
			notifyListener(false);
			removeDirections(lastDirections);
		}
		return true;
	}

	public void startScrolling(int keyCode) {
		startContinuousScrollingTime = System.currentTimeMillis(); 
		addDirection(keyCode);
		if (!isInContinuousScrolling) {
			new Thread(scrollingRunnable).start();
		}
	}
	
	public void addDirections(List<Direction> directions) {
		for (Direction direction : directions) {
			direction.setActive(true);
		}
	}

	public void removeDirections(List<Direction> directions) {
		for (Direction direction : directions) {
			direction.setActive(false);
			direction.setTimeUp(INVALID_VALUE);
		}
	}

	public void addDirection(int keyCode) {
		if (availableDirections.containsKey(keyCode)) {
			availableDirections.get(keyCode).setActive(true);
		}
	}

	public void removeDirection(int keyCode) {
		if (availableDirections.containsKey(keyCode)) {
			long keyUpTime = System.currentTimeMillis();
			Direction direction = availableDirections.get(keyCode);
			direction.setTimeUp(keyUpTime);
			direction.setActive(false);
		}
	}
	
	private boolean hasActiveDirections() {
		for (Direction direction : availableDirections.values()) {
			if (direction.isActive()) {
				return true;
			}
		}
		return false;
	}

	private void notifyListener(boolean continuousScrolling) {
		if (onScrollEventListener != null) {
			onScrollEventListener.onScrollEvent(continuousScrolling, 
					UP.isActive(), DOWN.isActive(), LEFT.isActive(), RIGHT.isActive());
		}
	}

	public void setListener(OnScrollEventListener onScrollEventListener) {
		this.onScrollEventListener = onScrollEventListener;
	}

	public boolean isAvailableKeyCode(int keyCode) {
		return availableDirections.containsKey(keyCode)
				&& !isOverrideBySelectedExternalDevice(keyCode);
	}

	public boolean isOverrideBySelectedExternalDevice(int keyCode) {
		OsmandSettings settings = app.getSettings();

		if (settings.EXTERNAL_INPUT_DEVICE.get() == PARROT_EXTERNAL_DEVICE) {
			return keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT;

		} else if (settings.EXTERNAL_INPUT_DEVICE.get() == WUNDERLINQ_EXTERNAL_DEVICE) {
			return keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN;
		}
		return false;
	}

	public List<Direction> getLastDirections() {
		List<Direction> directions = new ArrayList<>();
		for (Direction direction : availableDirections.values()) {
			if (System.currentTimeMillis() - direction.getTimeUp() <= MAX_KEY_UP_TIME_MS) {
				directions.add(direction);
			}
		}
		return directions;
	}

	private static class Direction {
		private final int keyCode;
		private long timeUp = INVALID_VALUE;
		private boolean isActive;
		
		public Direction(int keyCode) {
			this.keyCode = keyCode;
		}

		public long getTimeUp() {
			return timeUp;
		}

		public void setTimeUp(long timeUp) {
			this.timeUp = timeUp;
		}

		public boolean isActive() {
			return isActive;
		}

		public void setActive(boolean active) {
			isActive = active;
		}
	}
	
	public interface OnScrollEventListener {
		void onScrollEvent(boolean continuousScrolling, boolean up, boolean down, boolean left, boolean right);
	}
	
}
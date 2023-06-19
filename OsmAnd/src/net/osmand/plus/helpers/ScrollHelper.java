package net.osmand.plus.helpers;

import android.view.KeyEvent;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.InputDevice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScrollHelper {

	private static final int LONG_PRESS_TIME_MS = 250;
	private static final int MAX_KEY_UP_TIME_MS = 10;
	private static final int REFRESHING_DELAY_MS = 3;
	private static final int INVALID_VALUE = -1;

	private final OsmandApplication app;
	private OnScrollEventListener onScrollEventListener;
	
	private final Direction UP = new Direction(KeyEvent.KEYCODE_DPAD_UP);
	private final Direction DOWN = new Direction(KeyEvent.KEYCODE_DPAD_DOWN);
	private final Direction LEFT = new Direction(KeyEvent.KEYCODE_DPAD_LEFT);
	private final Direction RIGHT = new Direction(KeyEvent.KEYCODE_DPAD_RIGHT);

	private final Map<Integer, Direction> availableDirections;
	private volatile boolean isInContinuousScrolling;
	private volatile long startContinuousScrollingTime = INVALID_VALUE;

	private final Runnable scrollingRunnable = () -> {
		isInContinuousScrolling = true;
		while (hasActiveDirections()) {
			notifyListener(true, false);
			try {
				Thread.sleep(REFRESHING_DELAY_MS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (System.currentTimeMillis() - startContinuousScrollingTime < LONG_PRESS_TIME_MS) {
			List<Direction> lastDirections = getLastDirections();
			addDirections(lastDirections);
			notifyListener(false, false);
			removeDirections(lastDirections);
		}
		notifyListener(false, true);
		isInContinuousScrolling = false;
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

	public boolean onKeyDown(int keyCode) {
		if (isInContinuousScrolling) {
			addDirection(keyCode);
		} else {
			startScrolling(keyCode);
			return true;
		}
		return true;
	}

	public boolean onKeyUp(int keyCode) {
		removeDirection(keyCode);
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
			if (direction.isActive()) {
				direction.setTimeUp(keyUpTime);
			}
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

	private void notifyListener(boolean continuousScrolling, boolean stop) {
		if (onScrollEventListener != null) {
			onScrollEventListener.onScrollEvent(continuousScrolling, stop,
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
		InputDevice selected = settings.getSelectedInputDevice();
		if (selected == InputDevice.PARROT) {
			return keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT;
		} else if (selected == InputDevice.WUNDER_LINQ) {
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
		private volatile long timeUp = INVALID_VALUE;
		private volatile boolean isActive;
		
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
		void onScrollEvent(boolean continuousScrolling, boolean stop, boolean up, boolean down, boolean left, boolean right);
	}
	
}
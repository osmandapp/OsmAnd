package net.osmand.turnScreenOn.helpers;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;

import net.osmand.turnScreenOn.app.TurnScreenApp;
import net.osmand.turnScreenOn.listener.LockObservable;
import net.osmand.turnScreenOn.listener.OnLockListener;
import net.osmand.turnScreenOn.receiver.DeviceAdminRecv;

import java.util.ArrayList;

public class LockHelper implements LockObservable {
	private PowerManager.WakeLock wakeLock = null;
	private DevicePolicyManager mDevicePolicyManager;
	private ComponentName mDeviceAdmin;
	private Handler uiHandler;
	private TurnScreenApp app;
	private LockRunnable lockRunnable;
	private ArrayList<OnLockListener> onLockListeners;

	private boolean functionEnable = false;

	public LockHelper(TurnScreenApp app) {
		this.app = app;
		uiHandler = new Handler();
		mDeviceAdmin = new ComponentName(app, DeviceAdminRecv.class);
		mDevicePolicyManager = (DevicePolicyManager) app.getSystemService(Context.DEVICE_POLICY_SERVICE);
		lockRunnable = new LockRunnable();
		onLockListeners = new ArrayList<>();
	}

	private void releaseWakeLocks() {
		if (wakeLock != null) {
			if (wakeLock.isHeld()) {
				wakeLock.release();
			}
			wakeLock = null;
		}
	}

	public void lock() {
		if (readyToLock()) {
			notifyOnLock();
			releaseWakeLocks();
			/*if (functionEnable) {
				mDevicePolicyManager.lockNow();
			}*/
		}
	}

	public void unlock(long timeInMills) {
		if (readyToUnlock()) {
			notifyOnUnlock();
			PowerManager pm = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
			wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
					| PowerManager.ACQUIRE_CAUSES_WAKEUP, "tso:wakelocktag");
			wakeLock.acquire(timeInMills);
		}
	}

	public void timedUnlock(long millis) {
		Log.d("ttpl", "timedUnlock: try to unlock");
		uiHandler.removeCallbacks(lockRunnable);
		unlock(millis);
		uiHandler.postDelayed(lockRunnable, millis);
	}

	private boolean readyToLock() {
		return mDevicePolicyManager != null
				&& mDeviceAdmin != null
				&& mDevicePolicyManager.isAdminActive(mDeviceAdmin);
	}

	private boolean readyToUnlock() {
		releaseWakeLocks();
		return wakeLock == null;
	}

	public void disableFunction() {
		functionEnable = false;
	}

	public void enableFunction() {
		functionEnable = true;
	}

	@Override
	public void addLockListener(OnLockListener listener) {
		if (onLockListeners != null && !onLockListeners.contains(listener)) {
			onLockListeners.add(listener);
		}
	}

	@Override
	public void removeLockListener(OnLockListener listener) {
		if (onLockListeners != null && onLockListeners.size() > 0) {
			onLockListeners.remove(listener);
		}
	}

	@Override
	public void notifyOnLock() {
		if (onLockListeners != null) {
			for (OnLockListener l : onLockListeners) {
				l.onLock();
			}
		}
	}

	@Override
	public void notifyOnUnlock() {
		if (onLockListeners != null) {
			for (OnLockListener l : onLockListeners) {
				l.onUnlock();
			}
		}
	}

	private class LockRunnable implements Runnable {
		@Override
		public void run() {
			lock();
		}
	}
}

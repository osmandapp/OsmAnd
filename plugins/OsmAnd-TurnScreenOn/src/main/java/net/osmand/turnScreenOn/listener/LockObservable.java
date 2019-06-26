package net.osmand.turnScreenOn.listener;

public interface LockObservable {
	void addLockListener(OnLockListener listener);
	void removeLockListener(OnLockListener listener);
	void notifyOnLock();
	void notifyOnUnlock();
}

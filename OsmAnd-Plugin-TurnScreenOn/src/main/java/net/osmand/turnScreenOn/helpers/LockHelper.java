package net.osmand.turnScreenOn.helpers;

import android.Manifest;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import net.osmand.turnScreenOn.app.TurnScreenApp;
import net.osmand.turnScreenOn.receiver.DeviceAdminRecv;

public class LockHelper {
    private PowerManager.WakeLock wakeLock = null;
    private DevicePolicyManager mDevicePolicyManager;
    private ComponentName mDeviceAdmin;
    private Handler uiHandler;
    private Context context;
    private TurnScreenApp app;
    private KeyguardManager.KeyguardLock keyguardLock;
    private LockRunnable lockRunnable;

    private boolean lockCanceled = false;

    private final static String TAG = "LockHelperTag";

    public LockHelper(TurnScreenApp app) {
        this.app = app;
        this.context = app;
        uiHandler = new Handler();
        mDeviceAdmin = new ComponentName(context, DeviceAdminRecv.class);
        mDevicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        lockRunnable = new LockRunnable();
        keyguardLock = ((KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE))
                .newKeyguardLock(TAG);
    }

    private void releaseWakeLocks() {
        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    private class LockRunnable implements Runnable {
        @Override
        public void run() {
            lock();
        }
    }

    public void lock() {
        if (readyToLock()) {
//            Log.d("ttpl2", "LockHelper: release wakelocks");
            releaseWakeLocks();
            keyguardLock.reenableKeyguard();
            if (!lockCanceled) {
                mDevicePolicyManager.lockNow();
//                Log.d("ttpl2", "LockHelper: device lock");
            } else {
                lockCanceled = false;
            }
        }
    }

    public void unlock() {
        if (readyToUnlock()) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                    | PowerManager.ACQUIRE_CAUSES_WAKEUP, "tso:wakelocktag");
            wakeLock.acquire();
            keyguardLock.disableKeyguard();

//            Log.d("ttpl2", "LockHelper: device unlock");
        }
    }

    public void timedUnlock(long millis) {
        uiHandler.removeCallbacks(lockRunnable);
        unlock();
        uiHandler.postDelayed(lockRunnable, millis);
    }

    private boolean readyToLock() {
        return mDevicePolicyManager != null
                && mDeviceAdmin != null
                && mDevicePolicyManager.isAdminActive(mDeviceAdmin)
                && ContextCompat.checkSelfPermission(context, Manifest.permission.DISABLE_KEYGUARD)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean readyToUnlock() {
        return wakeLock==null
                && ContextCompat.checkSelfPermission(context, Manifest.permission.DISABLE_KEYGUARD)
                == PackageManager.PERMISSION_GRANTED;
    }

    public void cancelLock() {
//        Log.d("ttpl2", "LockHelper: lock canceled");
        lockCanceled = true;
    }
}

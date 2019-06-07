package net.osmand.turn_screen_on.helpers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import net.osmand.turn_screen_on.receiver.DeviceAdminRecv;

@SuppressLint("NewApi")
public class WakeLockHelper {

    private PowerManager.WakeLock wakeLock = null;
    private ReleaseWakeLocksRunnable releaseWakeLocksRunnable = new ReleaseWakeLocksRunnable();
    private DevicePolicyManager mDevicePolicyManager;
    private ComponentName mDeviceAdmin;
    private Handler uiHandler;
//    private TurnScreenOnApplication app;
//    private Application app;
    private Context context;
    private boolean active;
    private KeyguardManager.KeyguardLock lock;

    private boolean isSet = false;

    public WakeLockHelper(Context context) {
        this.context = context;
        uiHandler = new Handler();
        mDeviceAdmin = new ComponentName(context, DeviceAdminRecv.class);
        mDevicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        lock = keyguardManager.newKeyguardLock("TAG");
    }

    private void releaseWakeLocks() {
        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }

//        changeKeyguardFlags();

        if (mDevicePolicyManager != null && mDeviceAdmin != null) {
            //TODO
            //OsmandSettings settings = app.getSettings();
            // final Integer screenPowerSave = settings.WAKE_ON_VOICE_INT.get();
            //if (screenPowerSave > 0 && settings.MAP_ACTIVITY_ENABLED.get()) {
                if (mDevicePolicyManager.isAdminActive(mDeviceAdmin)) {
                    try {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.DISABLE_KEYGUARD)
                                == PackageManager.PERMISSION_GRANTED) {
                            lock.reenableKeyguard();
                        }
                        mDevicePolicyManager.lockNow();

                        Log.d("ttpl", "device lock");

                    } catch (SecurityException e) {
						Log.d("ttpl", "SecurityException: No device admin permission to lock the screen!");
                    }
                } else {
					Log.d("ttpl", "No device admin permission to lock the screen!");
                }
//            }
        }
    }

    private class ReleaseWakeLocksRunnable implements Runnable {

        @Override
        public void run() {
            releaseWakeLocks();
        }

    }

    public void onStart(Activity a) {
        //TODO voice routing with aidl
        this.active = true;
        /*if (wakeLock == null) {
            VoiceRouter voiceRouter = app.getRoutingHelper().getVoiceRouter();
            voiceRouter.removeVoiceMessageListener(this);
        }*/
    }

    public void onStop(Activity a) {
        this.active = false;
        //TODO voice routing with aidl
        /*OsmandSettings settings = app.getSettings();
        if (!a.isFinishing() && (settings.WAKE_ON_VOICE_INT.get() > 0)) {
            VoiceRouter voiceRouter = app.getRoutingHelper().getVoiceRouter();
            voiceRouter.addVoiceMessageListener(this);
        }*/
    }

//    @Override
    public void onVoiceMessage() {
//        OsmandSettings settings = app.getSettings();
//        final Integer screenPowerSave = settings.WAKE_ON_VOICE_INT.get();
//        if (screenPowerSave > 0) {
            uiHandler.removeCallbacks(releaseWakeLocksRunnable);

            if (/*!active && */wakeLock == null) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                                | PowerManager.ACQUIRE_CAUSES_WAKEUP, "ttpl:wakelocktag");
//                changeKeyguardFlags();
                wakeLock.acquire();

                if (ContextCompat.checkSelfPermission(context, Manifest.permission.DISABLE_KEYGUARD)
                        == PackageManager.PERMISSION_GRANTED) {
                    lock.disableKeyguard();
                }

                Log.d("ttpl", "device unlock");
            }

            uiHandler.postDelayed(releaseWakeLocksRunnable, 4000L);
//                    screenPowerSave * 1000L);
//        }
    }

/*    private void changeKeyguardFlags() {
        if (!isSet) {
            Log.d("ttpl", "set flags");
            activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            isSet = true;
        } else {
            Log.d("ttpl", "reset flags");
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            isSet = false;
        }
    }*/

}

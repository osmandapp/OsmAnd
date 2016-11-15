package net.osmand.core.samples.android.sample1;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.view.WindowManager;

public class SampleUtils {

	private static final int ORIENTATION_0 = 0;
	private static final int ORIENTATION_90 = 3;
	private static final int ORIENTATION_270 = 1;
	private static final int ORIENTATION_180 = 2;

	public static void doRestart(Context c) {
		boolean res = false;
		try {
			//check if the context is given
			if (c != null) {
				//fetch the packagemanager so we can get the default launch activity
				// (you can replace this intent with any other activity if you want
				PackageManager pm = c.getPackageManager();
				//check if we got the PackageManager
				if (pm != null) {
					//create the intent with the default start activity for your application
					Intent mStartActivity = pm.getLaunchIntentForPackage(
							c.getPackageName()
					);
					if (mStartActivity != null) {
						mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						//create a pending intent so the application is restarted after System.exit(0) was called.
						// We use an AlarmManager to call this intent in 100ms
						int mPendingIntentId = 84523123;
						PendingIntent mPendingIntent = PendingIntent
								.getActivity(c, mPendingIntentId, mStartActivity,
										PendingIntent.FLAG_CANCEL_CURRENT);
						AlarmManager mgr = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
						mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
						//kill the application
						res = true;
						android.os.Process.killProcess(android.os.Process.myPid());
						//System.exit(0);
					}
				}
			}
		} catch (Exception ex) {
			//ignore
		}
		if (!res) {
			android.os.Process.killProcess(android.os.Process.myPid());
		}
	}

	public static boolean isPackageInstalled(String packageInfo, Context ctx) {
		if (packageInfo == null) {
			return false;
		}
		boolean installed = false;
		try {
			installed = ctx.getPackageManager().getPackageInfo(packageInfo, 0) != null;
		} catch (PackageManager.NameNotFoundException e) {
			//ignore
		}
		return installed;
	}

	public static int getScreenOrientation(Activity a) {
		int screenOrientation = ((WindowManager) a.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
		switch (screenOrientation) {
			case ORIENTATION_0:   // Device default (normally portrait)
				screenOrientation = 0;
				break;
			case ORIENTATION_90:  // Landscape right
				screenOrientation = 90;
				break;
			case ORIENTATION_270: // Landscape left
				screenOrientation = 270;
				break;
			case ORIENTATION_180: // Upside down
				screenOrientation = 180;
				break;
		}
		//Looks like screenOrientation correction must not be applied for devices without compass?
		Sensor compass = ((SensorManager) a.getSystemService(Context.SENSOR_SERVICE)).getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		if (compass == null) {
			screenOrientation = 0;
		}
		return screenOrientation;
	}
}

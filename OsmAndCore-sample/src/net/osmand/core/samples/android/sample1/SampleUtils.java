package net.osmand.core.samples.android.sample1;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;

import net.osmand.PlatformUtil;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;

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

	/*
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
	*/

	public static int getScreenOrientation(Activity activity) {
		int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
		DisplayMetrics dm = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
		int width = dm.widthPixels;
		int height = dm.heightPixels;
		int orientation;
		// if the device's natural orientation is portrait:
		if ((rotation == Surface.ROTATION_0
				|| rotation == Surface.ROTATION_180) && height > width ||
				(rotation == Surface.ROTATION_90
						|| rotation == Surface.ROTATION_270) && width > height) {
			switch(rotation) {
				case Surface.ROTATION_0:
					orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
					break;
				case Surface.ROTATION_90:
					orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
					break;
				case Surface.ROTATION_180:
					orientation =
							ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
					break;
				case Surface.ROTATION_270:
					orientation =
							ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
					break;
				default:
					Log.e(PlatformUtil.TAG, "Unknown screen orientation. Defaulting to " +
							"portrait.");
					orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
					break;
			}
		}
		// if the device's natural orientation is landscape or if the device
		// is square:
		else {
			switch(rotation) {
				case Surface.ROTATION_0:
					orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
					break;
				case Surface.ROTATION_90:
					orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
					break;
				case Surface.ROTATION_180:
					orientation =
							ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
					break;
				case Surface.ROTATION_270:
					orientation =
							ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
					break;
				default:
					Log.e(PlatformUtil.TAG, "Unknown screen orientation. Defaulting to " +
							"landscape.");
					orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
					break;
			}
		}

		return orientation;
	}

	public static boolean isOrientationPortrait(Activity ctx) {
		int orientation = getScreenOrientation(ctx);
		return orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ||
				orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
	}

	public static List<File> collectFiles(File dir, String ext, List<File> files) {
		if (dir.exists() && dir.canRead()) {
			File[] lf = dir.listFiles();
			if (lf == null || lf.length == 0) {
				return files;
			}
			for (File f : lf) {
				if (f.getName().endsWith(ext)) {
					files.add(f);
				}
			}
		}
		return files;
	}

	@Nullable
	public static byte[] getDrawableAsByteArray(@NonNull Drawable drawable) {
		if (drawable instanceof BitmapDrawable) {
			Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
			return getBitmapAsByteArray(bitmap);
		}
		return null;
	}

	@NonNull
	public static byte[] getBitmapAsByteArray(@NonNull Bitmap bitmap) {
		int size = bitmap.getRowBytes() * bitmap.getHeight();
		ByteBuffer byteBuffer = ByteBuffer.allocate(size);
		bitmap.copyPixelsToBuffer(byteBuffer);
		return byteBuffer.array();
	}
}

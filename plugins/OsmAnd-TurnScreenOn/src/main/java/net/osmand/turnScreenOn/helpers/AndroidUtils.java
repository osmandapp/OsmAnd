package net.osmand.turnScreenOn.helpers;

import android.Manifest;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Build;
import android.os.PowerManager;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;

import java.util.List;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.Context.POWER_SERVICE;
import static android.util.TypedValue.COMPLEX_UNIT_DIP;

public class AndroidUtils {
	public static int dpToPx(Context ctx, float dp) {
		Resources r = ctx.getResources();
		return (int) TypedValue.applyDimension(
				COMPLEX_UNIT_DIP,
				dp,
				r.getDisplayMetrics()
		);
	}

	public static Paint createPaintWithGreyScale() {
		Paint pGreyScale = new Paint();
		ColorMatrix cm = new ColorMatrix();
		cm.setSaturation(0);
		pGreyScale.setColorFilter(new ColorMatrixColorFilter(cm));
		return pGreyScale;
	}

	public static boolean isScreenOn(Context context) {
		PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH && powerManager.isInteractive()
				|| Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT_WATCH && powerManager.isScreenOn();
	}

	public static boolean isScreenLocked(Context context) {
		KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
		return keyguardManager.inKeyguardRestrictedInputMode();
	}

	public static boolean isOnForeground(Context ctx, String appPackage) {
		if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.GET_TASKS)
				== PackageManager.PERMISSION_GRANTED) {
			ActivityManager manager = (ActivityManager) ctx.getSystemService(ACTIVITY_SERVICE);
			List<ActivityManager.RunningTaskInfo> runningTaskInfo = manager.getRunningTasks(1);

			ComponentName componentInfo = runningTaskInfo.get(0).topActivity;
			if (componentInfo.getPackageName().equals(appPackage)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isOpened(Context ctx, String appPackage) {
		ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfo = am.getRunningAppProcesses();

		for (int i = 0; i < runningAppProcessInfo.size(); i++) {
			if (runningAppProcessInfo.get(i).processName.equals(appPackage)) {
				return true;
			}
		}
		
		return false;
	}
}

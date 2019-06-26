package net.osmand.turnScreenOn.helpers;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Build;
import android.os.PowerManager;
import android.util.TypedValue;

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
		return Build.VERSION.SDK_INT>= Build.VERSION_CODES.KITKAT_WATCH&&powerManager.isInteractive()
				|| Build.VERSION.SDK_INT< Build.VERSION_CODES.KITKAT_WATCH&&powerManager.isScreenOn();
	}
	
	public static boolean isScreenLocked(Context context) {
		KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
		return keyguardManager.inKeyguardRestrictedInputMode();
	}
}

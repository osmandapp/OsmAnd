package net.osmand.plus.helpers;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.WindowInsetsController;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import net.osmand.PlatformUtil;

import static android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS;

/**
 * Created by dummy on 28.01.15.
 */
public class AndroidUiHelper {

    public static int getScreenOrientation(@NonNull Activity activity) {
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
    
    public static boolean updateVisibility(@Nullable View view, boolean visible) {
		if (view != null && visible != (view.getVisibility() == View.VISIBLE)) {
			if (visible) {
				view.setVisibility(View.VISIBLE);
			} else {
				view.setVisibility(View.GONE);
			}
			view.invalidate();
			return true;
		}
		return false;
	}

	public static void setVisibility(@NonNull Activity activity, int visibility, @IdRes int... widgets) {
		for (int widget : widgets) {
			View view = activity.findViewById(widget);
			if (view != null && view.getVisibility() != visibility) {
				view.setVisibility(visibility);
			}
		}
	}

	public static void setVisibility(int visibility, View ... views) {
		for (View view : views) {
			if (view != null && view.getVisibility() != visibility) {
				view.setVisibility(visibility);
			}
		}
	}

	public static void setEnabled(View rootView, boolean enabled, int ... ids) {
		for (int id : ids) {
			View view = rootView.findViewById(id);
			if (view != null && view.isEnabled() != enabled) {
				view.setEnabled(enabled);
			}
		}
	}

	public static boolean isXLargeDevice(@NonNull Activity ctx) {
		int lt = (ctx.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK);
		return lt == Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}

	public static boolean isOrientationPortrait(@NonNull Activity ctx) {
		int orientation = getScreenOrientation(ctx);
		return orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ||
				orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
	}

	public static void setStatusBarContentColor(@Nullable View view, boolean isNightMode) {
		if (view != null) {
			setStatusBarContentColor(view, view.getSystemUiVisibility(), !isNightMode);
		}
	}

	public static void setStatusBarContentColor(@NonNull View view, int flags, boolean addLightFlag) {
		if (Build.VERSION.SDK_INT >= 30) {
			WindowInsetsController controller = view.getWindowInsetsController();
			if (controller != null) {
				int flag = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS;
				if (addLightFlag) {
					controller.setSystemBarsAppearance(flag, flag);
					makeAbilityToResetLightStatusBar(view);
				} else {
					controller.setSystemBarsAppearance(0, flag);
				}
			}
		} else {
		    if (addLightFlag) {
			    flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
		    } else {
			    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
		    }
		    view.setSystemUiVisibility(flags);
	    }
	}

	@RequiresApi(api = VERSION_CODES.R)
	private static void makeAbilityToResetLightStatusBar(@NonNull View view) {
		view.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
			@Override
			public void onViewAttachedToWindow(View v) { }

			@Override
			public void onViewDetachedFromWindow(View v) {
				// Automatically reset APPEARANCE_LIGHT_STATUS_BARS flag
				// when user close the screen on which this flag was applied.
				WindowInsetsController controller = view.getWindowInsetsController();
				if (controller != null) {
					controller.setSystemBarsAppearance(0, APPEARANCE_LIGHT_STATUS_BARS);
				}
			}
		});
	}
}

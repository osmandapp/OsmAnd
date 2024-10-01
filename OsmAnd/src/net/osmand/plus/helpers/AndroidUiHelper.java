package net.osmand.plus.helpers;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
import static android.view.Surface.ROTATION_0;
import static android.view.Surface.ROTATION_180;
import static android.view.Surface.ROTATION_270;
import static android.view.Surface.ROTATION_90;
import static android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.UiContext;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.transition.MaterialContainerTransform;

import net.osmand.PlatformUtil;

/**
 * Created by dummy on 28.01.15.
 */
public class AndroidUiHelper {

	private static final int ORIENTATION_0 = 0;
	private static final int ORIENTATION_90 = 3;
	private static final int ORIENTATION_270 = 1;
	private static final int ORIENTATION_180 = 2;

	//TODO check constants correctness, looks like ORIENTATION_.. differs from Surface.ROTATION_.. is it intended?
	public static int getScreenRotation(@NonNull @UiContext Context context) {
		WindowManager windowManager = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE));
		int rotation = windowManager.getDefaultDisplay().getRotation();
		switch (rotation) {
			case ORIENTATION_0:   // Device default (normally portrait)
				rotation = 0;
				break;
			case ORIENTATION_90:  // Landscape right
				rotation = 90;
				break;
			case ORIENTATION_270: // Landscape left
				rotation = 270;
				break;
			case ORIENTATION_180: // Upside down
				rotation = 180;
				break;
		}
		//Looks like rotation correction must not be applied for devices without compass?
		PackageManager manager = context.getPackageManager();
		boolean hasCompass = manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS);
		if (!hasCompass) {
			rotation = 0;
		}
		return rotation;
	}

	public static int getScreenOrientation(@NonNull Activity activity) {
		WindowManager windowManager = activity.getWindowManager();
		int rotation = windowManager.getDefaultDisplay().getRotation();
		DisplayMetrics dm = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
		int width = dm.widthPixels;
		int height = dm.heightPixels;
		int orientation;
		// if the device's natural orientation is portrait:
		if ((rotation == ROTATION_0 || rotation == ROTATION_180) && height > width ||
				(rotation == ROTATION_90 || rotation == ROTATION_270) && width > height) {
			switch (rotation) {
				case ROTATION_0 -> orientation = SCREEN_ORIENTATION_PORTRAIT;
				case ROTATION_90 -> orientation = SCREEN_ORIENTATION_LANDSCAPE;
				case ROTATION_180 -> orientation = SCREEN_ORIENTATION_REVERSE_PORTRAIT;
				case ROTATION_270 -> orientation = SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
				default -> {
					Log.e(PlatformUtil.TAG, "Unknown screen orientation. Defaulting to portrait.");
					orientation = SCREEN_ORIENTATION_PORTRAIT;
				}
			}
		} else {
			// if the device's natural orientation is landscape or if the device is square:
			switch (rotation) {
				case ROTATION_0 -> orientation = SCREEN_ORIENTATION_LANDSCAPE;
				case ROTATION_90 -> orientation = SCREEN_ORIENTATION_PORTRAIT;
				case ROTATION_180 -> orientation = SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
				case ROTATION_270 -> orientation = SCREEN_ORIENTATION_REVERSE_PORTRAIT;
				default -> {
					Log.e(PlatformUtil.TAG, "Unknown screen orientation. Defaulting to landscape.");
					orientation = SCREEN_ORIENTATION_LANDSCAPE;
				}
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

	public static void setVisibility(int visibility, View... views) {
		for (View view : views) {
			if (view != null && view.getVisibility() != visibility) {
				view.setVisibility(visibility);
			}
		}
	}

	public static void setEnabled(View rootView, boolean enabled, int... ids) {
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
		return orientation == SCREEN_ORIENTATION_PORTRAIT || orientation == SCREEN_ORIENTATION_REVERSE_PORTRAIT;
	}

	public static void setStatusBarContentColor(@Nullable View view, boolean nightMode) {
		if (view != null) {
			setStatusBarContentColor(view, view.getSystemUiVisibility(), !nightMode);
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
			public void onViewAttachedToWindow(View v) {
			}

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

	public static void updateActionBarVisibility(@Nullable AppCompatActivity activity, boolean visible) {
		ActionBar actionBar = activity != null ? activity.getSupportActionBar() : null;
		if (actionBar != null) {
			actionBar.setShowHideAnimationEnabled(false);
			if (visible) {
				actionBar.show();
			} else {
				actionBar.hide();
			}
		}
	}

	public static void setSharedElementTransition(@NonNull Fragment fragment, @NonNull View view, @NonNull String transitionName) {
		MaterialContainerTransform transform = new MaterialContainerTransform();
		transform.setScrimColor(Color.TRANSPARENT);
		fragment.setSharedElementEnterTransition(transform);
		view.setTransitionName(transitionName);
	}
}
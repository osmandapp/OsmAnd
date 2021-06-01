package net.osmand.plus.base;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.activities.OsmandInAppPurchaseActivity;

public class BaseOsmAndFragment extends Fragment implements TransitionAnimator {
	private UiUtilities iconsCache;

	private int statusBarColor = -1;
	private boolean transitionAnimationAllowed = true;

	@Override
	public void onResume() {
		super.onResume();
		if (Build.VERSION.SDK_INT >= 21) {
			Activity activity = getActivity();
			if (activity != null) {
				int colorId = getStatusBarColorId();
				if (colorId != -1) {
					if (activity instanceof MapActivity) {
						((MapActivity) activity).updateStatusBarColor();
					} else {
						statusBarColor = activity.getWindow().getStatusBarColor();
						activity.getWindow().setStatusBarColor(ContextCompat.getColor(activity, colorId));
					}
				}
				if (!isFullScreenAllowed() && activity instanceof MapActivity) {
					((MapActivity) activity).exitFromFullScreen(getView());
				}
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (Build.VERSION.SDK_INT >= 21) {
			Activity activity = getActivity();
			if (activity != null) {
				if (!(activity instanceof MapActivity) && statusBarColor != -1) {
					activity.getWindow().setStatusBarColor(statusBarColor);
				}
				if (!isFullScreenAllowed() && activity instanceof MapActivity) {
					((MapActivity) activity).enterToFullScreen();
				}
			}
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		if (Build.VERSION.SDK_INT >= 21 && getStatusBarColorId() != -1) {
			Activity activity = getActivity();
			if (activity instanceof MapActivity) {
				((MapActivity) activity).updateStatusBarColor();
			}
		}
	}

	@Override
	public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
		if (transitionAnimationAllowed) {
			return super.onCreateAnimation(transit, enter, nextAnim);
		}
		Animation anim = new Animation() {
		};
		anim.setDuration(0);
		return anim;
	}

	@Override
	public void disableTransitionAnimation() {
		transitionAnimationAllowed = false;
	}

	@Override
	public void enableTransitionAnimation() {
		transitionAnimationAllowed = true;
	}

	@ColorRes
	public int getStatusBarColorId() {
		return -1;
	}

	protected boolean isFullScreenAllowed() {
		return true;
	}

	@Nullable
	protected OsmandApplication getMyApplication() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			return (OsmandApplication) activity.getApplication();
		} else {
			return null;
		}
	}

	@NonNull
	protected OsmandApplication requireMyApplication() {
		FragmentActivity activity = requireActivity();
		return (OsmandApplication) activity.getApplication();
	}

	@Nullable
	protected OsmandActionBarActivity getMyActivity() {
		return (OsmandActionBarActivity) getActivity();
	}

	@NonNull
	protected OsmandActionBarActivity requireMyActivity() {
		return (OsmandActionBarActivity) requireActivity();
	}

	@Nullable
	protected OsmandInAppPurchaseActivity getInAppPurchaseActivity() {
		Activity activity = getActivity();
		if (activity instanceof OsmandInAppPurchaseActivity) {
			return (OsmandInAppPurchaseActivity) getActivity();
		} else {
			return null;
		}
	}

	@Nullable
	protected UiUtilities getIconsCache() {
		OsmandApplication app = getMyApplication();
		if (iconsCache == null && app != null) {
			iconsCache = app.getUIUtilities();
		}
		return iconsCache;
	}

	protected Drawable getPaintedContentIcon(@DrawableRes int id, @ColorInt int color) {
		UiUtilities cache = getIconsCache();
		return cache != null ? cache.getPaintedIcon(id, color) : null;
	}

	protected Drawable getIcon(@DrawableRes int id) {
		UiUtilities cache = getIconsCache();
		return cache != null ? cache.getIcon(id) : null;
	}

	protected Drawable getIcon(@DrawableRes int id, @ColorRes int colorId) {
		UiUtilities cache = getIconsCache();
		return cache != null ? cache.getIcon(id, colorId) : null;
	}

	protected Drawable getContentIcon(@DrawableRes int id) {
		UiUtilities cache = getIconsCache();
		return cache != null ? cache.getThemedIcon(id) : null;
	}

	protected void setThemedDrawable(View parent, @IdRes int viewId, @DrawableRes int iconId) {
		((ImageView) parent.findViewById(viewId)).setImageDrawable(getContentIcon(iconId));
	}

	protected void setThemedDrawable(View view, @DrawableRes int iconId) {
		((ImageView) view).setImageDrawable(getContentIcon(iconId));
	}

	@Nullable
	protected OsmandSettings getSettings() {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			return app.getSettings();
		} else {
			return null;
		}
	}

	@NonNull
	protected OsmandSettings requireSettings() {
		OsmandApplication app = requireMyApplication();
		return app.getSettings();
	}
}

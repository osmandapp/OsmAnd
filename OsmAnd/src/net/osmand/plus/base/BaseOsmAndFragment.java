package net.osmand.plus.base;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageView;

import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandActionBarActivity;

public class BaseOsmAndFragment extends Fragment implements TransitionAnimator {
	private IconsCache iconsCache;

	private int statusBarColor = -1;
	private boolean transitionAnimationAllowed = true;

	@Override
	public void onResume() {
		super.onResume();
		if (Build.VERSION.SDK_INT >= 21) {
			Activity activity = getActivity();
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
				((MapActivity) activity).exitFromFullScreen();
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (Build.VERSION.SDK_INT >= 21) {
			Activity activity = getActivity();
			if (!(activity instanceof MapActivity) && statusBarColor != -1) {
				activity.getWindow().setStatusBarColor(statusBarColor);
			}
			if (!isFullScreenAllowed() && activity instanceof MapActivity) {
				((MapActivity) activity).enterToFullScreen();
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

	protected OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	protected OsmandActionBarActivity getMyActivity() {
		return (OsmandActionBarActivity) getActivity();
	}

	protected IconsCache getIconsCache() {
		if (iconsCache == null) {
			iconsCache = getMyApplication().getIconsCache();
		}
		return iconsCache;
	}

	protected Drawable getPaintedContentIcon(@DrawableRes int id, @ColorInt int color){
		return getIconsCache().getPaintedIcon(id, color);
	}

	protected Drawable getIcon(@DrawableRes int id, @ColorRes int colorId){
		return getIconsCache().getIcon(id, colorId);
	}

	protected Drawable getContentIcon(@DrawableRes int id){
		return getIconsCache().getThemedIcon(id);
	}

	protected void setThemedDrawable(View parent, @IdRes int viewId, @DrawableRes int iconId) {
		((ImageView) parent.findViewById(viewId)).setImageDrawable(getContentIcon(iconId));
	}

	protected void setThemedDrawable(View view, @DrawableRes int iconId) {
		((ImageView) view).setImageDrawable(getContentIcon(iconId));
	}

	protected OsmandSettings getSettings() {
		return getMyApplication().getSettings();
	}
}

package net.osmand.plus.base;

import android.app.Activity;
import android.view.animation.Animation;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;

/**
 * Base class for full-screen fragments that are displayed as standalone screens
 * (e.g., over MapActivity or other activities).
 *
 * This fragment manages system UI aspects such as:
 * - Entering/exiting fullscreen mode
 * - Updating the status bar color
 * - Handling custom transition animations
 *
 * Use this class when the fragment is meant to act as a primary full-screen UI container,
 * not as a nested or embedded part of another screen (see {@link BaseNestedFragment}).
 */
public class BaseFullScreenFragment extends BaseOsmAndFragment implements TransitionAnimator {

	private int statusBarColor = -1;
	private boolean transitionAnimationAllowed = true;

	@Override
	public void onResume() {
		super.onResume();

		Activity activity = getActivity();
		if (activity != null) {
			updateStatusBar(activity);
			if (!isFullScreenAllowed() && activity instanceof MapActivity) {
				((MapActivity) activity).exitFromFullScreen(getView());
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		Activity activity = getActivity();
		if (activity != null) {
			if (!(activity instanceof MapActivity) && statusBarColor != -1) {
				AndroidUiHelper.setStatusBarColor(activity, statusBarColor);
			}
			if (!isFullScreenAllowed() && activity instanceof MapActivity) {
				((MapActivity) activity).enterToFullScreen();
			}
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();

		if (getStatusBarColorId() != -1) {
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
		Animation anim = new Animation() {};
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

	public void updateStatusBar() {
		Activity activity = getActivity();
		if (activity != null) {
			updateStatusBar(activity);
		}
	}

	protected void updateStatusBar(@NonNull Activity activity) {
		int colorId = getStatusBarColorId();
		if (colorId != -1) {
			if (activity instanceof MapActivity) {
				((MapActivity) activity).updateStatusBarColor();
			} else {
				statusBarColor = AndroidUiHelper.setStatusBarColor(activity, getColor(colorId));
			}
		}
	}

	@ColorRes
	public int getStatusBarColorId() {
		return -1;
	}

	public boolean getContentStatusBarNightMode() {
		return true;
	}

	protected boolean isFullScreenAllowed() {
		return true;
	}
}

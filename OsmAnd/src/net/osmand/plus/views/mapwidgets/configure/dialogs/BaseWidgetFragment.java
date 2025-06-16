package net.osmand.plus.views.mapwidgets.configure.dialogs;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;

public abstract class BaseWidgetFragment extends BaseFullScreenFragment implements InAppPurchaseListener {

	protected ApplicationMode appMode;

	protected void dismiss() {
		Activity activity = getActivity();
		if (activity != null) {
			activity.onBackPressed();
		}
	}

	protected void addVerticalSpace(@NonNull ViewGroup container, int space) {
		View spaceView = new View(getContext());
		spaceView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, space));
		container.addView(spaceView);
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return nightMode ? R.color.status_bar_main_dark : R.color.activity_background_color_light;
	}

	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	protected abstract String getFragmentTag();

	protected void recreateFragment() {
		FragmentManager fragmentManager = requireMyActivity().getSupportFragmentManager();
		String tag = getFragmentTag();
		Fragment fragment = fragmentManager.findFragmentByTag(tag);
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, tag)) {
			fragmentManager.beginTransaction()
					.detach(fragment)
					.attach(fragment)
					.commitAllowingStateLoss();
		}
	}
}

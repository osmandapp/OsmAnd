package net.osmand.plus.views.mapwidgets.configure.dialogs;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseTaskType;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;

public abstract class BaseWidgetFragment extends BaseOsmAndFragment implements InAppPurchaseListener {

	protected OsmandApplication app;
	protected ApplicationMode appMode;
	protected OsmandSettings settings;
	protected boolean nightMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		settings = app.getSettings();
		nightMode = !app.getSettings().isLightContent();
	}

	protected void dismiss() {
		Activity activity = getActivity();
		if (activity != null) {
			activity.onBackPressed();
		}
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return nightMode ? R.color.status_bar_color_dark : R.color.activity_background_color_light;
	}

	@NonNull
	public MapActivity requireMapActivity() {
		FragmentActivity activity = getActivity();
		if (!(activity instanceof MapActivity)) {
			throw new IllegalStateException("Fragment " + this + " not attached to an activity.");
		}
		return (MapActivity) activity;
	}

	@Nullable
	public MapActivity getMapActivity() {
		Activity activity = getActivity();
		return activity instanceof MapActivity ? ((MapActivity) activity) : null;
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

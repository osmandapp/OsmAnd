package net.osmand.plus.settings.bottomsheets;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.preference.DialogPreference.TargetFragment;
import android.support.v7.preference.Preference;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;

import java.util.List;

public abstract class BasePreferenceBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String PREFERENCE_ID = "preference_id";
	private static final String APP_MODE_KEY = "app_mode_key";

	private String prefId;
	private Preference preference;
	private ApplicationMode appMode;

	protected void setAppMode(ApplicationMode appMode) {
		this.appMode = appMode;
	}

	public ApplicationMode getAppMode() {
		return appMode != null ? appMode : requiredMyApplication().getSettings().getApplicationMode();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			appMode = ApplicationMode.valueOfStringKey(savedInstanceState.getString(APP_MODE_KEY), null);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (appMode != null) {
			outState.putString(APP_MODE_KEY, appMode.getStringKey());
		}
	}

	@Override
	protected boolean isNightMode(@NonNull OsmandApplication app) {
		if (usedOnMap) {
			return app.getDaynightHelper().isNightModeForMapControlsForProfile(getAppMode());
		} else {
			return !app.getSettings().isLightContentForMode(getAppMode());
		}
	}

	public String getPrefId() {
		if (prefId == null) {
			Bundle args = getArguments();
			if (args != null && args.containsKey(PREFERENCE_ID)) {
				prefId = args.getString(PREFERENCE_ID);
			}
		}
		return prefId;
	}

	public Preference getPreference() {
		if (preference == null) {
			String prefId = getPrefId();
			if (prefId != null) {
				TargetFragment targetFragment = (TargetFragment) getTargetFragment();
				if (targetFragment != null) {
					preference = targetFragment.findPreference(prefId);
				}
			}
		}
		return preference;
	}

	public boolean shouldDismissOnChange() {
		return true;
	}

	public static BasePreferenceBottomSheet findPreferenceBottomSheet(@NonNull FragmentManager manager, @NonNull String prefId) {
		List<Fragment> fragments = manager.getFragments();
		for (Fragment fragment : fragments) {
			if (fragment instanceof BasePreferenceBottomSheet) {
				BasePreferenceBottomSheet bottomSheet = (BasePreferenceBottomSheet) fragment;
				if (prefId.equals(bottomSheet.getPrefId())) {
					return bottomSheet;
				}
			}
		}
		return null;
	}
}

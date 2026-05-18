package net.osmand.plus.settings.bottomsheets;

import static net.osmand.plus.settings.fragments.BaseSettingsFragment.APP_MODE_KEY;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.DialogPreference.TargetFragment;
import androidx.preference.Preference;

import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.settings.fragments.ApplyQueryType;

import java.util.List;

public abstract class BasePreferenceBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String PREFERENCE_ID = "preference_id";
	private static final String APPLY_QUERY_TYPE = "apply_query_type";
	private static final String PROFILE_DEPENDENT = "profile_dependent";

	private String prefId;
	private Preference preference;
	private ApplicationMode appMode;
	private boolean profileDependent;
	private ApplyQueryType applyQueryType;

	public void setAppMode(ApplicationMode appMode) {
		this.appMode = appMode;
	}

	public ApplicationMode getAppMode() {
		return appMode != null ? appMode : requiredMyApplication().getSettings().getApplicationMode();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			appMode = ApplicationMode.valueOfStringKey(savedInstanceState.getString(APP_MODE_KEY), null);
			applyQueryType = ApplyQueryType.valueOf(savedInstanceState.getString(APPLY_QUERY_TYPE));
			profileDependent = savedInstanceState.getBoolean(PROFILE_DEPENDENT, false);
		}
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(PROFILE_DEPENDENT, profileDependent);
		if (appMode != null) {
			outState.putString(APP_MODE_KEY, appMode.getStringKey());
		}
		outState.putString(APPLY_QUERY_TYPE, applyQueryType != null ?
				applyQueryType.name() : ApplyQueryType.NONE.name());
	}

	@Override
	public boolean isNightMode(@NonNull OsmandApplication app) {
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

	public void setProfileDependent(boolean profileDependent) {
		this.profileDependent = profileDependent;
	}

	public boolean isProfileDependent() {
		return profileDependent;
	}

	public void setApplyQueryType(ApplyQueryType applyQueryType) {
		this.applyQueryType = applyQueryType;
	}

	public ApplyQueryType getApplyQueryType() {
		return applyQueryType != null ? applyQueryType : ApplyQueryType.NONE;
	}
}

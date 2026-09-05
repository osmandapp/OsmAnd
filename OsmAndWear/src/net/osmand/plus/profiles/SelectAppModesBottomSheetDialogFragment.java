package net.osmand.plus.profiles;

import static net.osmand.plus.settings.fragments.BaseSettingsFragment.APP_MODE_KEY;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.R;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class SelectAppModesBottomSheetDialogFragment extends AppModesBottomSheetDialogFragment<SelectProfileMenuAdapter> {

	public static final String TAG = "SelectAppModesBottomSheetDialogFragment";

	private static final String APP_MODE_CHANGEABLE_KEY = "app_mode_changeable_key";

	private static final Log LOG = PlatformUtil.getLog(SelectAppModesBottomSheetDialogFragment.class);

	private List<ApplicationMode> activeModes = new ArrayList<>();
	private ApplicationMode appMode;
	private boolean appModeChangeable;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			appMode = ApplicationMode.valueOfStringKey(savedInstanceState.getString(APP_MODE_KEY), null);
			appModeChangeable = savedInstanceState.getBoolean(APP_MODE_CHANGEABLE_KEY);
		}
		OsmandApplication app = requiredMyApplication();
		if (appMode == null) {
			appMode = app.getSettings().getApplicationMode();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		activeModes = new ArrayList<>(ApplicationMode.values(getMyApplication()));
		adapter.updateItemsList(activeModes);
		setupHeightAndBackground(getView());
	}

	public ApplicationMode getAppMode() {
		return appMode;
	}

	public void setAppMode(ApplicationMode appMode) {
		this.appMode = appMode;
	}

	public boolean isAppModeChangeable() {
		return appModeChangeable;
	}

	public void setAppModeChangeable(boolean appModeChangeable) {
		this.appModeChangeable = appModeChangeable;
	}

	@Override
	protected void getData() {
		activeModes.addAll(ApplicationMode.values(getMyApplication()));
	}

	@Override
	protected SelectProfileMenuAdapter getMenuAdapter() {
		return new SelectProfileMenuAdapter(activeModes, requiredMyApplication(), getString(R.string.shared_string_manage), nightMode, appMode);
	}

	@Override
	protected String getTitle() {
		return getString(R.string.switch_profile);
	}

	@Override
	public void onProfilePressed(ApplicationMode appMode) {
		OsmandSettings settings = getMyApplication().getSettings();
		if (appMode != this.appMode) {
			if (appModeChangeable) {
				settings.setApplicationMode(appMode);
			}
			Fragment targetFragment = getTargetFragment();
			if (targetFragment instanceof AppModeChangedListener) {
				AppModeChangedListener listener = (AppModeChangedListener) targetFragment;
				listener.onAppModeChanged(appMode);
			}
		}
		dismiss();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		if (appMode != null) {
			outState.putString(APP_MODE_KEY, appMode.getStringKey());
		}
		outState.putBoolean(APP_MODE_CHANGEABLE_KEY, appModeChangeable);
	}

	@Override
	public boolean isNightMode(@NonNull OsmandApplication app) {
		if (usedOnMap) {
			return app.getDaynightHelper().isNightModeForMapControlsForProfile(getAppMode());
		} else {
			return !app.getSettings().isLightContentForMode(getAppMode());
		}
	}

	public static void showInstance(@NonNull FragmentManager fm, Fragment target, boolean usedOnMap,
									@Nullable ApplicationMode appMode, boolean appModeChangeable) {
		try {
			if (fm.findFragmentByTag(TAG) == null) {
				SelectAppModesBottomSheetDialogFragment fragment = new SelectAppModesBottomSheetDialogFragment();
				fragment.setTargetFragment(target, 0);
				fragment.setUsedOnMap(usedOnMap);
				fragment.setAppMode(appMode);
				fragment.setAppModeChangeable(appModeChangeable);
				fragment.show(fm, TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}

	public interface AppModeChangedListener {
		void onAppModeChanged(ApplicationMode appMode);
	}
}

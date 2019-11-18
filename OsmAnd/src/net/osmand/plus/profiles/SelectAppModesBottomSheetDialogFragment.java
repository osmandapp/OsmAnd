package net.osmand.plus.profiles;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class SelectAppModesBottomSheetDialogFragment extends AppModesBottomSheetDialogFragment<SelectProfileMenuAdapter> {

	public static final String TAG = "SelectAppModesBottomSheetDialogFragment";

	private static final String APP_MODE_KEY = "app_mode_key";
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
				settings.APPLICATION_MODE.set(appMode);
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
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (appMode != null) {
			outState.putString(APP_MODE_KEY, appMode.getStringKey());
		}
		outState.putBoolean(APP_MODE_CHANGEABLE_KEY, appModeChangeable);
	}

	@Override
	protected boolean isNightMode(@NonNull OsmandApplication app) {
		if (usedOnMap) {
			return app.getDaynightHelper().isNightModeForMapControlsForProfile(getAppMode());
		} else {
			return !app.getSettings().isLightContentForMode(getAppMode());
		}
	}

	public static void showInstance(@NonNull FragmentManager fm, Fragment target, boolean usedOnMap,
									@Nullable ApplicationMode appMode, boolean appModeChangeable) {
		try {
			if (fm.findFragmentByTag(SelectAppModesBottomSheetDialogFragment.TAG) == null) {
				SelectAppModesBottomSheetDialogFragment fragment = new SelectAppModesBottomSheetDialogFragment();
				fragment.setTargetFragment(target, 0);
				fragment.setUsedOnMap(usedOnMap);
				fragment.setAppMode(appMode);
				fragment.setAppModeChangeable(appModeChangeable);
				fragment.show(fm, SelectAppModesBottomSheetDialogFragment.TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}

	public interface AppModeChangedListener {
		void onAppModeChanged(ApplicationMode appMode);
	}
}

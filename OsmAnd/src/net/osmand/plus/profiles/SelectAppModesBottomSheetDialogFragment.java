package net.osmand.plus.profiles;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class SelectAppModesBottomSheetDialogFragment extends AppModesBottomSheetDialogFragment<SelectProfileMenuAdapter> {

	public static final String TAG = "SelectAppModesBottomSheetDialogFragment";

	private static final String APP_MODE_CHANGEABLE_KEY = "app_mode_changeable_key";

	private static final Log LOG = PlatformUtil.getLog(SelectAppModesBottomSheetDialogFragment.class);

	private List<ApplicationMode> activeModes = new ArrayList<>();
	private boolean appModeChangeable;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			appModeChangeable = savedInstanceState.getBoolean(APP_MODE_CHANGEABLE_KEY);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		activeModes = new ArrayList<>(ApplicationMode.values(app));
		adapter.updateItemsList(activeModes);
		setupHeightAndBackground(getView());
	}

	public boolean isAppModeChangeable() {
		return appModeChangeable;
	}

	public void setAppModeChangeable(boolean appModeChangeable) {
		this.appModeChangeable = appModeChangeable;
	}

	@Override
	protected void getData() {
		activeModes.addAll(ApplicationMode.values(app));
	}

	@Override
	protected SelectProfileMenuAdapter getMenuAdapter() {
		return new SelectProfileMenuAdapter(activeModes, app, getString(R.string.shared_string_manage), nightMode, appMode);
	}

	@Override
	protected String getTitle() {
		return getString(R.string.switch_profile);
	}

	@Override
	public void onProfilePressed(ApplicationMode appMode) {
		if (appMode != this.appMode) {
			if (appModeChangeable) {
				settings.setApplicationMode(appMode);
			}
			Fragment targetFragment = getTargetFragment();
			if (targetFragment instanceof AppModeChangedListener listener) {
				listener.onAppModeChanged(appMode);
			}
		}
		dismiss();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(APP_MODE_CHANGEABLE_KEY, appModeChangeable);
	}

	public static void showInstance(@NonNull FragmentManager fm, Fragment target, boolean usedOnMap,
									@Nullable ApplicationMode appMode, boolean appModeChangeable) {
		if (AndroidUtils.isFragmentCanBeAdded(fm, TAG, true)) {
			SelectAppModesBottomSheetDialogFragment fragment = new SelectAppModesBottomSheetDialogFragment();
			fragment.setTargetFragment(target, 0);
			fragment.setUsedOnMap(usedOnMap);
			fragment.setAppMode(appMode);
			fragment.setAppModeChangeable(appModeChangeable);
			fragment.show(fm, TAG);
		}
	}

	public interface AppModeChangedListener {
		void onAppModeChanged(ApplicationMode appMode);
	}
}

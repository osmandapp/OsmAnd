package net.osmand.plus.profiles;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class SelectCopyAppModeBottomSheet extends AppModesBottomSheetDialogFragment<SelectCopyProfilesMenuAdapter> {

	public static final String TAG = "SelectCopyAppModeBottomSheet";

	private static final String SELECTED_APP_MODE_KEY = "selected_app_mode_key";
	private static final String CURRENT_APP_MODE_KEY = "current_app_mode_key";

	private static final Log LOG = PlatformUtil.getLog(SelectCopyAppModeBottomSheet.class);

	private List<ApplicationMode> appModes = new ArrayList<>();

	private ApplicationMode selectedAppMode;
	private ApplicationMode currentAppMode;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Bundle args = getArguments();
		if (args != null && args.containsKey(CURRENT_APP_MODE_KEY)) {
			currentAppMode = ApplicationMode.valueOfStringKey(args.getString(CURRENT_APP_MODE_KEY), null);
		}
		if (currentAppMode == null) {
			currentAppMode = requiredMyApplication().getSettings().getApplicationMode();
		}
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			selectedAppMode = ApplicationMode.valueOfStringKey(savedInstanceState.getString(SELECTED_APP_MODE_KEY), null);
		}
	}

	public ApplicationMode getSelectedAppMode() {
		return selectedAppMode;
	}

	@Override
	protected void getData() {
		appModes = new ArrayList<>();
		for (ApplicationMode mode : ApplicationMode.allPossibleValues()) {
			if (mode != currentAppMode) {
				appModes.add(mode);
			}
		}
	}

	@Override
	protected SelectCopyProfilesMenuAdapter getMenuAdapter() {
		return new SelectCopyProfilesMenuAdapter(appModes, requiredMyApplication(), nightMode, selectedAppMode);
	}

	@Override
	protected String getTitle() {
		return getString(R.string.copy_from_other_profile);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(CURRENT_APP_MODE_KEY, currentAppMode.getStringKey());
		if (selectedAppMode != null) {
			outState.putString(SELECTED_APP_MODE_KEY, selectedAppMode.getStringKey());
		}
	}

	@Override
	public boolean isNightMode(@NonNull OsmandApplication app) {
		if (usedOnMap) {
			return app.getDaynightHelper().isNightModeForMapControlsForProfile(currentAppMode);
		} else {
			return !app.getSettings().isLightContentForMode(currentAppMode);
		}
	}

	@Override
	public void onProfilePressed(ApplicationMode item) {
		selectedAppMode = item;
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_copy;
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment targetFragment = getTargetFragment();
		if (selectedAppMode != null && targetFragment instanceof CopyAppModePrefsListener) {
			CopyAppModePrefsListener listener = (CopyAppModePrefsListener) targetFragment;
			listener.copyAppModePrefs(selectedAppMode);
		}
		dismiss();
	}

	public static void showInstance(@NonNull FragmentManager fm, Fragment target,
	                                @NonNull ApplicationMode currentMode) {
		showInstance(fm, target, false, currentMode);
	}

	public static void showInstance(@NonNull FragmentManager fm, Fragment target,
	                                boolean usedOnMap, @NonNull ApplicationMode currentMode) {
		try {
			if (fm.findFragmentByTag(TAG) == null) {
				Bundle args = new Bundle();
				args.putString(CURRENT_APP_MODE_KEY, currentMode.getStringKey());

				SelectCopyAppModeBottomSheet fragment = new SelectCopyAppModeBottomSheet();
				fragment.setTargetFragment(target, 0);
				fragment.setUsedOnMap(usedOnMap);
				fragment.setArguments(args);
				fragment.show(fm, TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}

	public interface CopyAppModePrefsListener {
		void copyAppModePrefs(@NonNull ApplicationMode appMode);
	}
}
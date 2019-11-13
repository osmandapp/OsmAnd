package net.osmand.plus.profiles;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class SelectAppModesBottomSheetDialogFragment extends AppModesBottomSheetDialogFragment<SelectProfileMenuAdapter> {

	public static final String TAG = "SelectAppModesBottomSheetDialogFragment";

	private static final Log LOG = PlatformUtil.getLog(SelectAppModesBottomSheetDialogFragment.class);

	private List<ApplicationMode> activeModes = new ArrayList<>();

	@Override
	public void onResume() {
		super.onResume();
		activeModes = new ArrayList<>(ApplicationMode.values(getMyApplication()));
		adapter.updateItemsList(activeModes);
		setupHeightAndBackground(getView());
	}

	@Override
	protected void getData() {
		activeModes.addAll(ApplicationMode.values(getMyApplication()));
	}

	@Override
	protected SelectProfileMenuAdapter getMenuAdapter() {
		return new SelectProfileMenuAdapter(activeModes, getMyApplication(), getString(R.string.shared_string_manage), nightMode);
	}

	@Override
	protected String getTitle() {
		return getString(R.string.switch_profile);
	}

	@Override
	public void onProfilePressed(ApplicationMode appMode) {
		OsmandSettings settings = getMyApplication().getSettings();
		if (appMode != settings.APPLICATION_MODE.get()) {
			settings.APPLICATION_MODE.set(appMode);

			Fragment targetFragment = getTargetFragment();
			if (targetFragment instanceof AppModeChangedListener) {
				AppModeChangedListener listener = (AppModeChangedListener) targetFragment;
				listener.onAppModeChanged(appMode);
			}
		}
		dismiss();
	}

	public static void showInstance(@NonNull FragmentManager fm, Fragment target, boolean usedOnMap) {
		try {
			if (fm.findFragmentByTag(SelectAppModesBottomSheetDialogFragment.TAG) == null) {
				SelectAppModesBottomSheetDialogFragment fragment = new SelectAppModesBottomSheetDialogFragment();
				fragment.setTargetFragment(target, 0);
				fragment.setUsedOnMap(usedOnMap);
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

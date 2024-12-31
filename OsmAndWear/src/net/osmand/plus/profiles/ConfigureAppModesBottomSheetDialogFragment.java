package net.osmand.plus.profiles;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.profiles.ConfigureProfileMenuAdapter.ProfileSelectedListener;
import net.osmand.plus.settings.backend.ApplicationMode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ConfigureAppModesBottomSheetDialogFragment extends AppModesBottomSheetDialogFragment<ConfigureProfileMenuAdapter>
		implements ProfileSelectedListener {

	public static final String TAG = "ConfigureAppModesBottomSheetDialogFragment";

	private List<ApplicationMode> allModes = new ArrayList<>();
	private final Set<ApplicationMode> selectedModes = new HashSet<>();
	
	@Override
	public void onResume() {
		super.onResume();
		
		adapter.setProfileSelectedListener(this);
		allModes = new ArrayList<>(ApplicationMode.allPossibleValues());
		allModes.remove(ApplicationMode.DEFAULT);
		adapter.updateItemsList(allModes,
				new LinkedHashSet<>(ApplicationMode.values(getMyApplication())));
		setupHeightAndBackground(getView());
	}

	@Override
	protected String getTitle() {
		return getString(R.string.application_profiles);
	}

	@Override
	protected void getData() {
		allModes.addAll(ApplicationMode.allPossibleValues());
		allModes.remove(ApplicationMode.DEFAULT);
		selectedModes.addAll(ApplicationMode.values(getMyApplication()));
		selectedModes.remove(ApplicationMode.DEFAULT);
	}

	@Override
	protected ConfigureProfileMenuAdapter getMenuAdapter() {
		return new ConfigureProfileMenuAdapter(allModes, selectedModes, getMyApplication(), getString(R.string.shared_string_manage), nightMode);
	}

	@Override
	public void onProfileSelected(ApplicationMode item, boolean isChecked) {
		if (isChecked) {
			selectedModes.add(item);
		} else {
			selectedModes.remove(item);
		}
		ApplicationMode.changeProfileAvailability(item, isChecked, getMyApplication());
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, boolean usedOnMap,
	                                @Nullable UpdateMapRouteMenuListener listener) {
		if (fragmentManager.findFragmentByTag(TAG) == null) {
			ConfigureAppModesBottomSheetDialogFragment fragment = new ConfigureAppModesBottomSheetDialogFragment();
			fragment.setUsedOnMap(usedOnMap);
			fragment.setUpdateMapRouteMenuListener(listener);
			fragmentManager.beginTransaction()
					.add(fragment, TAG)
					.commitAllowingStateLoss();
		}
	}
}
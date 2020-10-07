package net.osmand.plus.profiles;

import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ConfigureAppModesBottomSheetDialogFragment extends AppModesBottomSheetDialogFragment<ConfigureProfileMenuAdapter> 
		implements ConfigureProfileMenuAdapter.ProfileSelectedListener {

	public static final String TAG = "ConfigureAppModesBottomSheetDialogFragment";

	private List<ApplicationMode> allModes = new ArrayList<>();
	private Set<ApplicationMode> selectedModes = new HashSet<>();
	
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
}
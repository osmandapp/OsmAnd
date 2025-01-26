package net.osmand.plus.profiles;

import static net.osmand.plus.settings.fragments.search.SearchableInfoHelper.getProfileDescriptions;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.common.collect.ImmutableList;

import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.profiles.data.ProfileDataObject;
import net.osmand.plus.profiles.data.ProfileDataUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.bottomsheets.BasePreferenceBottomSheetInitializer;
import net.osmand.plus.settings.fragments.search.SearchablePreferenceDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SelectBaseProfileBottomSheet extends SelectProfileBottomSheet implements SearchablePreferenceDialog {

	private final List<ProfileDataObject> profiles = new ArrayList<>();

	@NonNull
	public static SelectBaseProfileBottomSheet createInstance(final Optional<Fragment> target,
															  final ApplicationMode appMode,
															  final String selectedItemKey,
															  final boolean usedOnMap) {
		final SelectBaseProfileBottomSheet bottomSheet = new SelectBaseProfileBottomSheet();
		{
			final Bundle args = new Bundle();
			args.putString(SELECTED_KEY, selectedItemKey);
			bottomSheet.setArguments(args);
		}
		return BasePreferenceBottomSheetInitializer
				.initialize(bottomSheet)
				.with(Optional.empty(), appMode, usedOnMap, target);

	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getTitle()));
		items.add(new LongDescriptionItem(getDescription()));
		for (int i = 0; i < profiles.size(); i++) {
			addProfileItem(profiles.get(i));
		}
	}

	@NonNull
	private String getTitle() {
		return getString(R.string.select_base_profile_dialog_title);
	}

	@NonNull
	private String getDescription() {
		return getString(R.string.select_base_profile_dialog_message);
	}

	@Override
	protected void refreshProfiles() {
		profiles.clear();
		profiles.addAll(getProfiles());
	}

	@NonNull
	private List<ProfileDataObject> getProfiles() {
		final List<ApplicationMode> appModes = new ArrayList<>(ApplicationMode.allPossibleValues());
		appModes.remove(ApplicationMode.DEFAULT);
		return ProfileDataUtils.getDataObjects(app, appModes);
	}

	@Override
	public void show(final FragmentManager fragmentManager) {
		if (!fragmentManager.isStateSaved()) {
			show(fragmentManager, TAG);
		}
	}

	@Override
	public String getSearchableInfo() {
		return String.join(
				", ",
				ImmutableList
						.<String>builder()
						.add(getTitle())
						.add(getDescription())
						.addAll(getProfileDescriptions(getProfiles()))
						.build());
	}
}

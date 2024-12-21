package net.osmand.plus.profiles;

import static net.osmand.plus.profiles.SelectDefaultProfileBottomSheet.getProfileDescriptions;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.common.collect.ImmutableList;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.profiles.data.ProfileDataObject;
import net.osmand.plus.profiles.data.ProfileDataUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.search.SearchablePreferenceDialog;

import java.util.ArrayList;
import java.util.List;

public class SelectBaseProfileBottomSheet extends SelectProfileBottomSheet implements SearchablePreferenceDialog {

	private final List<ProfileDataObject> profiles = new ArrayList<>();

	@NonNull
	public static SelectBaseProfileBottomSheet createInstance(final @Nullable Fragment target, final ApplicationMode appMode, final String selectedItemKey, final boolean usedOnMap) {
		final SelectBaseProfileBottomSheet fragment = new SelectBaseProfileBottomSheet();
		Bundle args = new Bundle();
		args.putString(SELECTED_KEY, selectedItemKey);
		fragment.setArguments(args);
		fragment.setUsedOnMap(usedOnMap);
		fragment.setAppMode(appMode);
		fragment.setTargetFragment(target, 0);
		return fragment;
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
		final List<ProfileDataObject> dataObjects = getProfiles();
		profiles.addAll(dataObjects);
	}

	@NonNull
	private List<ProfileDataObject> getProfiles() {
		final List<ApplicationMode> appModes = new ArrayList<>(ApplicationMode.allPossibleValues());
		appModes.remove(ApplicationMode.DEFAULT);
		return ProfileDataUtils.getDataObjects(app, appModes);
	}

	@Override
	public void show(final FragmentManager fragmentManager, final OsmandApplication app) {
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

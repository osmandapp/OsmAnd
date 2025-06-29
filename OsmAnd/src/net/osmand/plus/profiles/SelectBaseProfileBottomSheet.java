package net.osmand.plus.profiles;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.profiles.data.ProfileDataObject;
import net.osmand.plus.profiles.data.ProfileDataUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;

import java.util.ArrayList;
import java.util.List;

public class SelectBaseProfileBottomSheet extends SelectProfileBottomSheet {

	private final List<ProfileDataObject> profiles = new ArrayList<>();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.select_base_profile_dialog_title)));
		items.add(new LongDescriptionItem(getString(R.string.select_base_profile_dialog_message)));
		for (int i = 0; i < profiles.size(); i++) {
			addProfileItem(profiles.get(i));
		}
	}

	@Override
	protected void refreshProfiles() {
		profiles.clear();
		List<ApplicationMode> appModes = new ArrayList<>(ApplicationMode.allPossibleValues());
		appModes.remove(ApplicationMode.DEFAULT);
		profiles.addAll(ProfileDataUtils.getDataObjects(app, appModes));
	}

	public static void showInstance(@NonNull FragmentActivity activity, @Nullable Fragment target,
	                                @NonNull ApplicationMode appMode, @Nullable String selectedItemKey,
	                                boolean usedOnMap) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			SelectBaseProfileBottomSheet fragment = new SelectBaseProfileBottomSheet();
			Bundle args = new Bundle();
			args.putString(SELECTED_KEY, selectedItemKey);
			fragment.setArguments(args);
			fragment.setUsedOnMap(usedOnMap);
			fragment.setAppMode(appMode);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}
}

package net.osmand.plus.profiles;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SimpleDividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.profiles.data.ProfileDataObject;
import net.osmand.plus.profiles.data.ProfileDataUtils;
import net.osmand.plus.settings.backend.ApplicationMode;

import java.util.ArrayList;
import java.util.List;

public class SelectOnlineApproxProfileBottomSheet extends SelectProfileBottomSheet {

	private final List<ProfileDataObject> profiles = new ArrayList<>();

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @Nullable Fragment target,
	                                ApplicationMode appMode,
	                                String selectedItemKey,
	                                boolean usedOnMap) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (!fragmentManager.isStateSaved()) {
			SelectOnlineApproxProfileBottomSheet fragment = new SelectOnlineApproxProfileBottomSheet();
			Bundle args = new Bundle();
			args.putString(SELECTED_KEY, selectedItemKey);
			fragment.setArguments(args);
			fragment.setUsedOnMap(usedOnMap);
			fragment.setAppMode(appMode);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}

	@Override
	public void createMenuItems(@Nullable Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.select_nav_profile_dialog_title)));
		items.add(new LongDescriptionItem(getString(R.string.select_base_profile_dialog_title)));

		addCheckableItem(R.string.shared_string_none, false, v -> {
			Bundle args = new Bundle();
			args.putString(PROFILE_KEY_ARG, "");
			Fragment target = getTargetFragment();
			if (target instanceof OnSelectProfileCallback) {
				((OnSelectProfileCallback) target).onProfileSelected(args);
			}
			dismiss();
		});

		items.add(new SimpleDividerItem(app));
		for (int i = 0; i < profiles.size(); i++) {
			addProfileItem(profiles.get(i));
		}
	}

	@Override
	protected void refreshProfiles() {
		profiles.clear();
		List<ApplicationMode> values = ApplicationMode.values(app);
		values.remove(ApplicationMode.DEFAULT);
		profiles.addAll(ProfileDataUtils.getDataObjects(app, values));
	}
}

package net.osmand.plus.profiles;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

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

public class SelectDefaultProfileBottomSheet extends SelectProfileBottomSheet {

	private final List<ProfileDataObject> profiles = new ArrayList<>();

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @Nullable Fragment target,
	                                ApplicationMode appMode,
	                                String selectedItemKey,
	                                boolean usedOnMap) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (!fragmentManager.isStateSaved()) {
			SelectDefaultProfileBottomSheet fragment = new SelectDefaultProfileBottomSheet();
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
		items.add(new TitleItem(getString(R.string.settings_preset)));
		items.add(new LongDescriptionItem(getString(R.string.profile_by_default_description)));

		boolean useLastAppModeByDefault = app.getSettings().USE_LAST_APPLICATION_MODE_BY_DEFAULT.get();
		addCheckableItem(R.string.shared_string_last_used, useLastAppModeByDefault, new OnClickListener() {
			@Override
			public void onClick(View v) {
				Bundle args = new Bundle();
				args.putBoolean(USE_LAST_PROFILE_ARG, true);
				Fragment target = getTargetFragment();
				if (target instanceof OnSelectProfileCallback) {
					((OnSelectProfileCallback) target).onProfileSelected(args);
				}
				dismiss();
			}
		});

		items.add(new SimpleDividerItem(app));
		for (int i = 0; i < profiles.size(); i++) {
			addProfileItem(profiles.get(i));
		}
	}

	@Override
	protected boolean isSelected(ProfileDataObject profile) {
		return !app.getSettings().USE_LAST_APPLICATION_MODE_BY_DEFAULT.get()
				&& super.isSelected(profile);
	}

	@Override
	protected void refreshProfiles() {
		profiles.clear();
		profiles.addAll(ProfileDataUtils.getDataObjects(app, ApplicationMode.values(app)));
	}

}

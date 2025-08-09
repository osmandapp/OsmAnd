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
import net.osmand.plus.profiles.data.ProfilesGroup;
import net.osmand.plus.profiles.data.RoutingDataObject;
import net.osmand.plus.profiles.data.RoutingDataUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class SelectOnlineApproxProfileBottomSheet extends SelectProfileBottomSheet {

	public static final String SELECTED_DERIVED_PROFILE_KEY = "selected_derived_profile";

	private RoutingDataUtils dataUtils;
	private List<ProfilesGroup> profileGroups = new ArrayList<>();
	private String selectedDerivedProfile;
	private boolean networkApproximateRoute;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		if (args != null) {
			selectedDerivedProfile = args.getString(SELECTED_DERIVED_PROFILE_KEY, null);
		}
	}

	@Override
	public void createMenuItems(@Nullable Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.select_nav_profile_dialog_title)));
		items.add(new LongDescriptionItem(getString(R.string.select_base_profile_dialog_title)));
		addCheckableItem(R.string.shared_string_none, Algorithms.isEmpty(selectedItemKey) && !networkApproximateRoute, v -> {
			Bundle args = new Bundle();
			args.putString(PROFILE_KEY_ARG, "");
			Fragment target = getTargetFragment();
			if (target instanceof OnSelectProfileCallback) {
				((OnSelectProfileCallback) target).onProfileSelected(args);
			}
			dismiss();
		});

		createProfilesList();
	}

	private void createProfilesList() {
		for (ProfilesGroup group : profileGroups) {
			List<RoutingDataObject> items = group.getProfiles();
			if (!Algorithms.isEmpty(items)) {
				addGroupHeader(group.getTitle(), group.getDescription(app, nightMode));
				for (RoutingDataObject item : items) {
					addProfileItem(item);
				}
				addDivider();
			}
		}
	}

	@Override
	protected boolean isSelected(ProfileDataObject profile) {
		boolean isSelected = super.isSelected(profile) && !networkApproximateRoute;
		if (isSelected && profile instanceof RoutingDataObject data) {
			boolean checkForDerived = !Algorithms.objectEquals(selectedDerivedProfile, "default");
			if (checkForDerived) {
				isSelected = Algorithms.objectEquals(selectedDerivedProfile, data.getDerivedProfile());
			} else {
				isSelected = data.getDerivedProfile() == null;
			}
		}
		return isSelected;
	}

	@Override
	protected void refreshProfiles() {
		profileGroups.clear();
		profileGroups = getDataUtils().getOfflineProfiles();
	}

	@Override
	protected int getIconColor(ProfileDataObject profile) {
		return getColor(isSelected(profile) ? getActiveColorId() : getDefaultIconColorId());
	}

	private RoutingDataUtils getDataUtils() {
		if (dataUtils == null) {
			dataUtils = new RoutingDataUtils(app);
		}
		return dataUtils;
	}

	public static void showInstance(@NonNull FragmentActivity activity, @Nullable Fragment target,
	                                @Nullable ApplicationMode appMode, @Nullable String selectedItemKey,
	                                @Nullable String selectedDerivedProfile, boolean usedOnMap) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			SelectOnlineApproxProfileBottomSheet fragment = new SelectOnlineApproxProfileBottomSheet();
			Bundle args = new Bundle();
			args.putString(SELECTED_KEY, selectedItemKey);
			args.putString(SELECTED_DERIVED_PROFILE_KEY, selectedDerivedProfile);
			fragment.setArguments(args);
			fragment.setUsedOnMap(usedOnMap);
			fragment.setAppMode(appMode);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}
}

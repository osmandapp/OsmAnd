package net.osmand.plus.profiles;

import static net.osmand.plus.settings.fragments.search.SearchableInfoHelper.getProfileDescriptions;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.common.collect.ImmutableList;

import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SimpleDividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.profiles.data.ProfileDataObject;
import net.osmand.plus.profiles.data.ProfileDataUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.bottomsheets.BasePreferenceBottomSheetInitializer;
import net.osmand.plus.settings.fragments.search.SearchablePreferenceDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SelectDefaultProfileBottomSheet extends SelectProfileBottomSheet implements SearchablePreferenceDialog {

	private final List<ProfileDataObject> profiles = new ArrayList<>();

	public static SelectDefaultProfileBottomSheet createInstance(final Optional<Fragment> target,
																 final ApplicationMode appMode,
																 final String selectedItemKey,
																 final boolean usedOnMap) {
		final SelectDefaultProfileBottomSheet bottomSheet = new SelectDefaultProfileBottomSheet();
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

		items.add(new SimpleDividerItem(requireContext()));
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
		profiles.addAll(getProfiles());
	}

	@NonNull
	private List<ProfileDataObject> getProfiles() {
		return ProfileDataUtils.getDataObjects(app, ApplicationMode.values(app));
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
						.add(getString(R.string.profile_by_default_description))
						.addAll(getProfileDescriptions(getProfiles()))
						.build());
	}
}

package net.osmand.plus.profiles;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.backend.ApplicationMode;

import java.util.ArrayList;
import java.util.List;

public class SelectBaseProfileBottomSheet extends SelectProfileBottomSheet {

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @Nullable Fragment target,
	                                ApplicationMode appMode,
	                                String selectedItemKey,
	                                boolean usedOnMap) {
		SelectBaseProfileBottomSheet fragment = new SelectBaseProfileBottomSheet();
		Bundle args = new Bundle();
		args.putString(SELECTED_KEY, selectedItemKey);
		fragment.setArguments(args);
		fragment.setUsedOnMap(usedOnMap);
		fragment.setAppMode(appMode);
		fragment.setTargetFragment(target, 0);
		fragment.show(activity.getSupportFragmentManager(), TAG);
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		List<ProfileDataObject> profiles = getProfiles();
		items.add(new TitleItem(getString(R.string.select_base_profile_dialog_title)));
		items.add(new LongDescriptionItem(getString(R.string.select_base_profile_dialog_message)));
		for (int i = 0; i < profiles.size(); i++) {
			addProfileItem(profiles.get(i));
		}
		/*items.add(new DividerItem(app));
		addButtonItem(R.string.import_from_file, R.drawable.ic_action_folder, new OnClickListener() {

			@Override
			public void onClick(View v) {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity == null) {
					return;
				}
				mapActivity.getImportHelper().chooseFileToImport(SETTINGS, false,
						new CallbackWithObject<List<SettingsItem>>() {
							@Override
							public boolean processResult(List<SettingsItem> result) {
								for (SettingsItem item : result) {
									if (SettingsItemType.PROFILE.equals(item.getType())) {
										if (listener == null) {
											getListener();
										}
										Bundle args = new Bundle();
										args.putString(PROFILE_KEY_ARG, item.getName());
										args.putBoolean(PROFILES_LIST_UPDATED_ARG, true);
										listener.onSelectedType(args);
										dismiss();
										break;
									}
								}
								return false;
							}
						});
			}
		});
		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(bottomSpaceView)
				.create());*/
	}

	@Override
	protected void fillProfilesList(List<ProfileDataObject> profiles) {
		List<ApplicationMode> appModes = new ArrayList<>(ApplicationMode.allPossibleValues());
		appModes.remove(ApplicationMode.DEFAULT);
		profiles.addAll(ProfileDataUtils.getDataObjects(app, appModes));
	}

}

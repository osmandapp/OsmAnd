package net.osmand.plus.settings.bottomsheets;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.ApplyQueryType;

import org.apache.commons.logging.Log;

public class ScreenTimeoutBottomSheet extends BooleanPreferenceBottomSheet {

	public static final String TAG = ScreenTimeoutBottomSheet.class.getSimpleName();

	private static final Log LOG = PlatformUtil.getLog(ScreenTimeoutBottomSheet.class);

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		super.createMenuItems(savedInstanceState);

		BaseBottomSheetItem preferenceDescription = new BottomSheetItemWithDescription.Builder()
				.setTitle(getString(R.string.change_default_settings))
				.setIcon(getContentIcon(R.drawable.ic_action_external_link))
				.setTitleColorId(nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light)
				.setLayoutId(R.layout.bottom_sheet_item_simple_right_icon)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
						if (AndroidUtils.isIntentSafe(v.getContext(), intent)) {
							startActivity(intent);
						}
					}
				})
				.create();
		items.add(preferenceDescription);
	}

	public static void showInstance(@NonNull FragmentManager fm, String prefId, Fragment target, boolean usedOnMap,
	                                @Nullable ApplicationMode appMode, ApplyQueryType applyQueryType,
	                                boolean profileDependent) {
		try {
			if (fm.findFragmentByTag(ScreenTimeoutBottomSheet.TAG) == null) {
				Bundle args = new Bundle();
				args.putString(PREFERENCE_ID, prefId);

				ScreenTimeoutBottomSheet fragment = new ScreenTimeoutBottomSheet();
				fragment.setArguments(args);
				fragment.setUsedOnMap(usedOnMap);
				fragment.setAppMode(appMode);
				fragment.setApplyQueryType(applyQueryType);
				fragment.setTargetFragment(target, 0);
				fragment.setProfileDependent(profileDependent);
				fragment.show(fm, ScreenTimeoutBottomSheet.TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}
}
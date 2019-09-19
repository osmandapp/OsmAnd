package net.osmand.plus.settings.bottomsheets;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.View;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.BaseSettingsFragment;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ChangeGeneralProfilesPrefBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = "ChangeGeneralProfilesPrefBottomSheet";

	private static final Log LOG = PlatformUtil.getLog(ChangeGeneralProfilesPrefBottomSheet.class);

	private Object newValue;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final OsmandApplication app = getMyApplication();
		Bundle args = getArguments();
		if (app == null || args == null || newValue == null || !args.containsKey(PREFERENCE_ID)) {
			return;
		}

		final String prefId = args.getString(PREFERENCE_ID);
		CommonPreference pref = getPreference(prefId);
		OsmandPreference osmandPref = app.getSettings().getPreference(prefId);
		if (pref == null || osmandPref == null) {
			return;
		}

		items.add(new TitleItem(getString(R.string.change_default_settings)));

		StringBuilder builder = new StringBuilder();
		final List<ApplicationMode> values = ApplicationMode.values(app);
		List<ApplicationMode> appModesDefaultValue = new ArrayList<>();

		for (int i = 0; i < values.size(); i++) {
			ApplicationMode mode = values.get(i);
			if (!osmandPref.isSetForMode(mode)) {
				appModesDefaultValue.add(mode);
			}
		}

		Iterator<ApplicationMode> iterator = appModesDefaultValue.iterator();
		while (iterator.hasNext()) {
			builder.append(iterator.next().toHumanString(app));
			builder.append(iterator.hasNext() ? ", " : '.');
		}

		if (builder.length() > 0) {
			CharSequence description = AndroidUtils.getStyledString(app.getString(R.string.pref_selected_by_default_for_profiles), builder.toString(), Typeface.BOLD);
			items.add(new LongDescriptionItem(description));
		}

		BaseBottomSheetItem applyToAllProfiles = new SimpleBottomSheetItem.Builder()
				.setTitle(getString(R.string.apply_to_all_profiles))
				.setIcon(getActiveIcon(R.drawable.ic_action_copy))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						for (ApplicationMode mode : values) {
							app.getSettings().setPreference(prefId, newValue, mode);
						}
						BaseSettingsFragment target = (BaseSettingsFragment) getTargetFragment();
						if (target != null) {
							target.updateAllSettings();
						}
						dismiss();
					}
				})
				.create();
		items.add(applyToAllProfiles);

		ApplicationMode selectedAppMode = getMyApplication().getSettings().APPLICATION_MODE.get();

		BaseBottomSheetItem applyToCurrentProfile = new SimpleBottomSheetItem.Builder()
				.setTitle(getString(R.string.apply_to_current_profile, selectedAppMode.toHumanString(app)))
				.setIcon(getActiveIcon(selectedAppMode.getIconRes()))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						app.getSettings().setPreference(prefId, newValue);
						BaseSettingsFragment target = (BaseSettingsFragment) getTargetFragment();
						if (target != null) {
							target.updateAllSettings();
						}
						dismiss();
					}
				})
				.create();
		items.add(applyToCurrentProfile);

		BaseBottomSheetItem discardChanges = new SimpleBottomSheetItem.Builder()
				.setTitle(getString(R.string.discard_changes))
				.setIcon(getActiveIcon(R.drawable.ic_action_undo_dark))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						dismiss();
					}
				})
				.create();
		items.add(discardChanges);
	}

	@Override
	protected boolean hideButtonsContainer() {
		return true;
	}

	private CommonPreference getPreference(String prefId) {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			OsmandPreference pref = app.getSettings().getPreference(prefId);
			if (pref instanceof CommonPreference) {
				return (CommonPreference) pref;
			}
		}

		return null;
	}

	public static void showInstance(@NonNull FragmentManager fm, String prefId, Object newValue, Fragment target, boolean usedOnMap) {
		try {
			if (fm.findFragmentByTag(ChangeGeneralProfilesPrefBottomSheet.TAG) == null) {
				Bundle args = new Bundle();
				args.putString(PREFERENCE_ID, prefId);

				ChangeGeneralProfilesPrefBottomSheet fragment = new ChangeGeneralProfilesPrefBottomSheet();
				fragment.setArguments(args);
				fragment.setUsedOnMap(usedOnMap);
				fragment.newValue = newValue;
				fragment.setTargetFragment(target, 0);
				fragment.show(fm, ChangeGeneralProfilesPrefBottomSheet.TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}
}
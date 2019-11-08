package net.osmand.plus.settings.bottomsheets;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.View;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.OnPreferenceChanged;
import net.osmand.plus.settings.preferences.MultiSelectBooleanPreference;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MultiSelectPreferencesBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = MultiSelectPreferencesBottomSheet.class.getSimpleName();

	private static final Log LOG = PlatformUtil.getLog(MultiSelectPreferencesBottomSheet.class);

	private static final String PREFERENCES_IDS = "preferences_ids";
	private static final String PREFERENCE_CHANGED = "preference_changed";
	private static final String PREFERENCES_ENTRIES = "preferences_entries";
	private static final String ENABLED_PREFERENCES_IDS = "enabled_preferences_ids";

	private MultiSelectBooleanPreference multiSelectBooleanPreference;

	private String[] prefsIds;
	private CharSequence[] entries;
	private Set<String> enabledPrefs = new HashSet<>();

	private boolean prefChanged;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final OsmandApplication app = getMyApplication();
		multiSelectBooleanPreference = getListPreference();
		if (app == null || multiSelectBooleanPreference == null) {
			return;
		}
		readSavedState(savedInstanceState);

		String title = multiSelectBooleanPreference.getDialogTitle().toString();
		items.add(new TitleItem(title));

		String description = multiSelectBooleanPreference.getDescription();
		if (!Algorithms.isEmpty(description)) {
			items.add(new LongDescriptionItem(description));
		}

		for (int i = 0; i < entries.length; i++) {
			String prefId = prefsIds[i];

			String prefTitle = entries[i].toString();
			boolean selected = enabledPrefs.contains(prefId);

			final int index = i;
			final BottomSheetItemWithCompoundButton[] item = new BottomSheetItemWithCompoundButton[1];
			item[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
					.setChecked(selected)
					.setTitle(prefTitle)
					.setLayoutId(R.layout.bottom_sheet_item_with_switch_no_icon)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							boolean checked = !item[0].isChecked();
							if (checked) {
								prefChanged |= enabledPrefs.add(prefsIds[index]);
							} else {
								prefChanged |= enabledPrefs.remove(prefsIds[index]);
							}
							item[0].setChecked(checked);
						}
					})
					.setTag(prefId)
					.create();
			items.add(item[0]);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		for (BaseBottomSheetItem item : items) {
			if (item instanceof BottomSheetItemWithCompoundButton) {
				String prefId = (String) item.getTag();
				((BottomSheetItemWithCompoundButton) item).setChecked(enabledPrefs.contains(prefId));
			}
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(PREFERENCE_CHANGED, prefChanged);
		outState.putStringArray(PREFERENCES_IDS, prefsIds);
		outState.putStringArrayList(ENABLED_PREFERENCES_IDS, new ArrayList<>(enabledPrefs));
		outState.putCharSequenceArray(PREFERENCES_ENTRIES, entries);
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	protected void onRightBottomButtonClick() {
		if (prefChanged) {
			final Set<String> values = enabledPrefs;
			if (multiSelectBooleanPreference.callChangeListener(values)) {
				multiSelectBooleanPreference.setValues(values);

				Fragment target = getTargetFragment();
				if (target instanceof OnPreferenceChanged) {
					((OnPreferenceChanged) target).onPreferenceChanged(multiSelectBooleanPreference.getKey());
				}
			}
		}
		prefChanged = false;
		dismiss();
	}

	private MultiSelectBooleanPreference getListPreference() {
		return (MultiSelectBooleanPreference) getPreference();
	}

	private void readSavedState(Bundle savedInstanceState) {
		if (savedInstanceState == null) {
			if (multiSelectBooleanPreference.getEntries() == null || multiSelectBooleanPreference.getPrefsIds() == null) {
				LOG.error("MultiSelectListPreference requires an entries array and an entryValues array.");
				return;
			}
			enabledPrefs.clear();
			enabledPrefs.addAll(multiSelectBooleanPreference.getValues());
			prefChanged = false;
			entries = multiSelectBooleanPreference.getEntries();
			prefsIds = multiSelectBooleanPreference.getPrefsIds();
		} else {
			enabledPrefs.clear();
			enabledPrefs.addAll(savedInstanceState.getStringArrayList(ENABLED_PREFERENCES_IDS));
			prefChanged = savedInstanceState.getBoolean(PREFERENCE_CHANGED, false);
			entries = savedInstanceState.getCharSequenceArray(PREFERENCES_ENTRIES);
			prefsIds = savedInstanceState.getStringArray(PREFERENCES_IDS);
		}
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager, String prefId, Fragment target, boolean usedOnMap) {
		try {
			Bundle args = new Bundle();
			args.putString(PREFERENCE_ID, prefId);

			MultiSelectPreferencesBottomSheet fragment = new MultiSelectPreferencesBottomSheet();
			fragment.setArguments(args);
			fragment.setUsedOnMap(usedOnMap);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}
}
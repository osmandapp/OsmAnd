package net.osmand.plus.settings;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.preference.DialogPreference;
import android.view.View;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.preferences.MultiSelectBooleanPreference;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MultiSelectPreferencesBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = MultiSelectPreferencesBottomSheet.class.getSimpleName();

	private static final String PREFERENCE_ID = "preference_id";
	private static final String PREFERENCES_IDS = "preferences_ids";
	private static final String PREFERENCE_CHANGED = "preference_changed";
	private static final String PREFERENCES_ENTRIES = "preferences_entries";
	private static final String ENABLED_PREFERENCES_IDS = "enabled_preferences_ids";

	private MultiSelectBooleanPreference multiSelectBooleanPreference;

	private String[] prefsIds;
	private CharSequence[] entries;
	private Set<String> enabledPrefs = new HashSet<>();

	private boolean preferenceChanged;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		multiSelectBooleanPreference = getListPreference();

		if (savedInstanceState == null) {
			if (multiSelectBooleanPreference.getEntries() == null || multiSelectBooleanPreference.getPrefsIds() == null) {
				throw new IllegalStateException("MultiSelectListPreference requires an entries array and an entryValues array.");
			}
			enabledPrefs.clear();
			enabledPrefs.addAll(multiSelectBooleanPreference.getValues());
			preferenceChanged = false;
			entries = multiSelectBooleanPreference.getEntries();
			prefsIds = multiSelectBooleanPreference.getPrefsIds();
		} else {
			enabledPrefs.clear();
			enabledPrefs.addAll(savedInstanceState.getStringArrayList(ENABLED_PREFERENCES_IDS));
			preferenceChanged = savedInstanceState.getBoolean(PREFERENCE_CHANGED, false);
			entries = savedInstanceState.getCharSequenceArray(PREFERENCES_ENTRIES);
			prefsIds = savedInstanceState.getStringArray(PREFERENCES_IDS);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final OsmandApplication app = getMyApplication();
		if (app == null || multiSelectBooleanPreference == null) {
			return;
		}

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
								preferenceChanged |= enabledPrefs.add(prefsIds[index]);
							} else {
								preferenceChanged |= enabledPrefs.remove(prefsIds[index]);
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
		outState.putStringArrayList(ENABLED_PREFERENCES_IDS, new ArrayList<>(enabledPrefs));
		outState.putBoolean(PREFERENCE_CHANGED, preferenceChanged);
		outState.putCharSequenceArray(PREFERENCES_ENTRIES, entries);
		outState.putStringArray(PREFERENCES_IDS, prefsIds);
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	protected void onRightBottomButtonClick() {
		if (preferenceChanged) {
			final Set<String> values = enabledPrefs;
			if (multiSelectBooleanPreference.callChangeListener(values)) {
				multiSelectBooleanPreference.setValues(values);
			}
		}
		preferenceChanged = false;
		dismiss();
	}

	private MultiSelectBooleanPreference getListPreference() {
		return (MultiSelectBooleanPreference) getPreference();
	}

	public DialogPreference getPreference() {
		Bundle args = getArguments();
		if (multiSelectBooleanPreference == null && args != null) {
			final String key = args.getString(PREFERENCE_ID);
			final DialogPreference.TargetFragment targetFragment = (DialogPreference.TargetFragment) getTargetFragment();
			if (targetFragment != null) {
				multiSelectBooleanPreference = (MultiSelectBooleanPreference) targetFragment.findPreference(key);
			}
		}
		return multiSelectBooleanPreference;
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager, String prefId, Fragment target) {
		try {
			Bundle args = new Bundle();
			args.putString(PREFERENCE_ID, prefId);

			MultiSelectPreferencesBottomSheet fragment = new MultiSelectPreferencesBottomSheet();
			fragment.setTargetFragment(target, 0);
			fragment.setArguments(args);
			fragment.show(fragmentManager, TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}
}
package net.osmand.plus.settings.bottomsheets;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.StateChangedListener;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.fragments.OnPreferenceChanged;
import net.osmand.plus.settings.preferences.MultiSelectBooleanPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
	private final Set<String> enabledPrefs = new HashSet<>();
	private final Map<CommonPreference<Boolean>, StateChangedListener<Boolean>> prefListeners = new HashMap<>();

	private boolean prefChanged;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		multiSelectBooleanPreference = getListPreference();
		if (multiSelectBooleanPreference == null) {
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

			int index = i;
			BottomSheetItemWithCompoundButton[] item = new BottomSheetItemWithCompoundButton[1];
			item[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
					.setChecked(selected)
					.setTitle(prefTitle)
					.setLayoutId(R.layout.bottom_sheet_item_with_switch_no_icon)
					.setOnClickListener(v -> {
						boolean checked = !item[0].isChecked();
						if (checked) {
							prefChanged |= enabledPrefs.add(prefsIds[index]);
						} else {
							prefChanged |= enabledPrefs.remove(prefsIds[index]);
						}
						item[0].setChecked(checked);
					})
					.setTag(prefId)
					.create();
			if (isProfileDependent()) {
				item[0].setCompoundButtonColor(getAppMode().getProfileColor(nightMode));
			}
			items.add(item[0]);
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
			Set<String> values = enabledPrefs;
			if (multiSelectBooleanPreference.callChangeListener(values)) {
				multiSelectBooleanPreference.setValues(values);

				if (getTargetFragment() instanceof OnPreferenceChanged listener) {
					listener.onPreferenceChanged(multiSelectBooleanPreference.getKey());
				}
			}
		}
		prefChanged = false;
		dismiss();
	}

	@Override
	public void onStart() {
		super.onStart();
		addPreferenceListeners();
	}

	@Override
	public void onStop() {
		removePreferenceListeners();
		super.onStop();
	}

	private void addPreferenceListeners() {
		removePreferenceListeners();
		if (prefsIds != null) {
			for (String prefId : prefsIds) {
				OsmandPreference<?> pref = settings.getPreference(prefId);
				if (pref instanceof CommonPreference<?> commonPref && commonPref.getDefaultValue() instanceof Boolean) {
					@SuppressWarnings("unchecked")
					CommonPreference<Boolean> booleanPref = (CommonPreference<Boolean>) commonPref;
					StateChangedListener<Boolean> listener = change -> updatePreferenceState(prefId);
					prefListeners.put(booleanPref, listener);
					booleanPref.addListener(listener);
				}
			}
		}
	}

	private void removePreferenceListeners() {
		for (Map.Entry<CommonPreference<Boolean>, StateChangedListener<Boolean>> entry : prefListeners.entrySet()) {
			entry.getKey().removeListener(entry.getValue());
		}
		prefListeners.clear();
	}

	private void updatePreferenceState(String prefId) {
		app.runInUIThread(() -> {
			if (!isAdded()) {
				return;
			}
			OsmandPreference<?> pref = settings.getPreference(prefId);
			if (pref instanceof CommonPreference<?> commonPref) {
				boolean enabled = Boolean.TRUE.equals(commonPref.getModeValue(getAppMode()));
				if (enabled) {
					enabledPrefs.add(prefId);
				} else {
					enabledPrefs.remove(prefId);
				}
				for (BaseBottomSheetItem item : items) {
					if (item instanceof BottomSheetItemWithCompoundButton buttonItem) {
						if (Algorithms.objectEquals(buttonItem.getTag(), prefId)) {
							buttonItem.setChecked(enabled);
							break;
						}
					}
				}
			}
		});
	}

	@Nullable
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
			entries = multiSelectBooleanPreference.getEntries();
			prefsIds = multiSelectBooleanPreference.getPrefsIds();
			for (String prefId : prefsIds) {
				OsmandPreference<?> pref = settings.getPreference(prefId);
				if (pref instanceof CommonPreference<?> commonPref) {
					if (Boolean.TRUE.equals(commonPref.getModeValue(getAppMode()))) {
						enabledPrefs.add(prefId);
					}
				} else if (multiSelectBooleanPreference.getValues().contains(prefId)) {
					enabledPrefs.add(prefId);
				}
			}
			prefChanged = false;
		} else {
			enabledPrefs.clear();
			enabledPrefs.addAll(savedInstanceState.getStringArrayList(ENABLED_PREFERENCES_IDS));
			prefChanged = savedInstanceState.getBoolean(PREFERENCE_CHANGED, false);
			entries = savedInstanceState.getCharSequenceArray(PREFERENCES_ENTRIES);
			prefsIds = savedInstanceState.getStringArray(PREFERENCES_IDS);
		}
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager, @NonNull String prefId,
	                                   @NonNull Fragment targetFragment, boolean usedOnMap,
	                                   @Nullable ApplicationMode appMode, boolean profileDependent) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			Bundle args = new Bundle();
			args.putString(PREFERENCE_ID, prefId);

			MultiSelectPreferencesBottomSheet fragment = new MultiSelectPreferencesBottomSheet();
			fragment.setArguments(args);
			fragment.setUsedOnMap(usedOnMap);
			fragment.setAppMode(appMode);
			fragment.setTargetFragment(targetFragment, 0);
			fragment.show(fragmentManager, TAG);
			fragment.setProfileDependent(profileDependent);
			return true;
		}
		return false;
	}
}
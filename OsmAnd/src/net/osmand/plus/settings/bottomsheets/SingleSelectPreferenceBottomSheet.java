package net.osmand.plus.settings.bottomsheets;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.View;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.OnPreferenceChanged;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.util.Algorithms;

public class SingleSelectPreferenceBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = SingleSelectPreferenceBottomSheet.class.getSimpleName();

	private static final String SELECTED_ENTRY_INDEX_KEY = "selected_entry_index_key";

	private ListPreferenceEx listPreference;

	private int selectedEntryIndex = -1;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context ctx = getContext();
		listPreference = getListPreference();
		if (ctx == null || listPreference == null || listPreference.getEntries() == null || listPreference.getEntryValues() == null) {
			return;
		}
		if (savedInstanceState != null) {
			selectedEntryIndex = savedInstanceState.getInt(SELECTED_ENTRY_INDEX_KEY);
		} else {
			selectedEntryIndex = listPreference.findIndexOfValue(listPreference.getValue());
		}

		String title = listPreference.getDialogTitle().toString();
		items.add(new TitleItem(title));

		String description = listPreference.getDescription();
		if (!Algorithms.isEmpty(description)) {
			items.add(new LongDescriptionItem(description));
		}

		String[] entries = listPreference.getEntries();

		for (int i = 0; i < entries.length; i++) {
			final BaseBottomSheetItem[] preferenceItem = new BottomSheetItemWithCompoundButton[1];
			preferenceItem[0] = new BottomSheetItemWithCompoundButton.Builder()
					.setChecked(i == selectedEntryIndex)
					.setButtonTintList(AndroidUtils.createCheckedColorStateList(ctx, R.color.icon_color_default_light, getActiveColorId()))
					.setTitle(entries[i])
					.setTag(i)
					.setLayoutId(R.layout.bottom_sheet_item_with_radio_btn_left)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							selectedEntryIndex = (int) preferenceItem[0].getTag();
							updateItems();
						}
					})
					.create();
			items.add(preferenceItem[0]);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		updateItems();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(SELECTED_ENTRY_INDEX_KEY, selectedEntryIndex);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	protected void onRightBottomButtonClick() {
		Object[] entryValues = listPreference.getEntryValues();
		if (entryValues != null && selectedEntryIndex >= 0) {
			Object value = entryValues[selectedEntryIndex];
			if (listPreference.callChangeListener(value)) {
				listPreference.setValue(value);
			}
			Fragment target = getTargetFragment();
			if (target instanceof OnPreferenceChanged) {
				((OnPreferenceChanged) target).onPreferenceChanged(listPreference.getKey());
			}
		}

		dismiss();
	}

	private void updateItems() {
		for (BaseBottomSheetItem item : items) {
			if (item instanceof BottomSheetItemWithCompoundButton) {
				boolean checked = item.getTag().equals(selectedEntryIndex);
				((BottomSheetItemWithCompoundButton) item).setChecked(checked);
			}
		}
	}

	private ListPreferenceEx getListPreference() {
		return (ListPreferenceEx) getPreference();
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager, String key, Fragment target, boolean usedOnMap) {
		try {
			Bundle args = new Bundle();
			args.putString(PREFERENCE_ID, key);

			SingleSelectPreferenceBottomSheet fragment = new SingleSelectPreferenceBottomSheet();
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
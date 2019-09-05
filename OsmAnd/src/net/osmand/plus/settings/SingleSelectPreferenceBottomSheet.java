package net.osmand.plus.settings;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.preference.DialogPreference;
import android.view.View;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.util.Algorithms;

public class SingleSelectPreferenceBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = SingleSelectPreferenceBottomSheet.class.getSimpleName();

	private static final String PREFERENCE_ID = "preference_id";
	private static final String SELECTED_ENTRY_INDEX_KEY = "selected_entry_index_key";

	private ListPreferenceEx listPreference;

	private String[] entries;
	private Object[] entryValues;
	private int selectedEntryIndex = -1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		listPreference = getListPreference();
		if (listPreference != null) {
			entries = listPreference.getEntries();
			entryValues = listPreference.getEntryValues();

			if (savedInstanceState != null) {
				selectedEntryIndex = savedInstanceState.getInt(SELECTED_ENTRY_INDEX_KEY);
			} else {
				selectedEntryIndex = listPreference.findIndexOfValue(listPreference.getValue());
			}
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final Context context = getContext();
		if (context == null || listPreference == null || entries == null || entryValues == null) {
			return;
		}

		items.add(new TitleItem(listPreference.getDialogTitle().toString()));

		String description = listPreference.getDescription();
		if (!Algorithms.isEmpty(description)) {
			items.add(new LongDescriptionItem(description));
		}

		for (int i = 0; i < entries.length; i++) {
			final BaseBottomSheetItem[] preferenceItem = new BottomSheetItemWithCompoundButton[1];
			preferenceItem[0] = new BottomSheetItemWithCompoundButton.Builder()
					.setChecked(i == selectedEntryIndex)
					.setButtonTintList(AndroidUtils.createCheckedColorStateList(context, R.color.icon_color_default_light, getActiveColorId()))
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

	private void updateItems() {
		for (BaseBottomSheetItem item : items) {
			if (item instanceof BottomSheetItemWithCompoundButton) {
				boolean checked = item.getTag().equals(selectedEntryIndex);
				((BottomSheetItemWithCompoundButton) item).setChecked(checked);
			}
		}
	}

	private ListPreferenceEx getListPreference() {
		if (listPreference == null) {
			Bundle args = getArguments();
			if (args != null) {
				final String key = args.getString(PREFERENCE_ID);
				Fragment targetFragment = getTargetFragment();
				if (targetFragment instanceof DialogPreference.TargetFragment) {
					DialogPreference.TargetFragment fragment = (DialogPreference.TargetFragment) targetFragment;
					listPreference = (ListPreferenceEx) fragment.findPreference(key);
				}
			}
		}
		return listPreference;
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
		if (selectedEntryIndex >= 0) {
			Object value = entryValues[selectedEntryIndex];
			if (listPreference.callChangeListener(value)) {
				listPreference.setValue(value);
			}
		}
		Fragment target = getTargetFragment();
		if (target instanceof OnPreferenceChanged) {
			((OnPreferenceChanged) target).onPreferenceChanged(listPreference.getKey());
		}
		dismiss();
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager, String key, Fragment target) {
		try {
			Bundle args = new Bundle();
			args.putString(PREFERENCE_ID, key);

			SingleSelectPreferenceBottomSheet fragment = new SingleSelectPreferenceBottomSheet();
			fragment.setTargetFragment(target, 0);
			fragment.setArguments(args);
			fragment.show(fragmentManager, TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}
}
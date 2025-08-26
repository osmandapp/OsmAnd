package net.osmand.plus.settings.bottomsheets;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.R;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemTitleWithDescrAndButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.fragments.OnPreferenceChanged;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.util.Algorithms;

public class SingleSelectPreferenceBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = SingleSelectPreferenceBottomSheet.class.getSimpleName();

	public static final String SELECTED_ENTRY_INDEX_KEY = "selected_entry_index_key";
	private static final String USE_COLLAPSIBLE_DESCRIPTION = "use_collapsible_description";
	private static final int COLLAPSED_DESCRIPTION_LINES = 4;

	private ListPreferenceEx listPreference;

	private int selectedEntryIndex = -1;
	private boolean descriptionExpanded;
	private boolean collapsibleDescription;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context ctx = getContext();
		listPreference = getListPreference();
		if (ctx == null || listPreference == null || listPreference.getEntries() == null || listPreference.getEntryValues() == null) {
			return;
		}
		ctx = UiUtilities.getThemedContext(ctx, nightMode);
		Bundle args = getArguments();
		if (args != null && args.containsKey(USE_COLLAPSIBLE_DESCRIPTION)) {
			collapsibleDescription = args.getBoolean(USE_COLLAPSIBLE_DESCRIPTION);
		}
		if (savedInstanceState != null) {
			selectedEntryIndex = savedInstanceState.getInt(SELECTED_ENTRY_INDEX_KEY);
			collapsibleDescription = savedInstanceState.getBoolean(USE_COLLAPSIBLE_DESCRIPTION);
		} else {
			selectedEntryIndex = listPreference.findIndexOfValue(listPreference.getValue());
		}

		String title = listPreference.getDialogTitle().toString();
		items.add(new TitleItem(title));

		String description = listPreference.getDescription();
		if (!Algorithms.isEmpty(description)) {
			buildDescriptionItem(ctx, description);
		}

		String[] entries = listPreference.getEntries();

		for (int i = 0; i < entries.length; i++) {
			BaseBottomSheetItem[] preferenceItem = new BottomSheetItemWithCompoundButton[1];
			preferenceItem[0] = new BottomSheetItemWithCompoundButton.Builder()
					.setChecked(i == selectedEntryIndex)
					.setButtonTintList(AndroidUtils.createCheckedColorIntStateList(
							ColorUtilities.getDefaultIconColor(ctx, nightMode),
							isProfileDependent() ?
									getAppMode().getProfileColor(nightMode) :
									ContextCompat.getColor(ctx, getActiveColorId())))
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
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(SELECTED_ENTRY_INDEX_KEY, selectedEntryIndex);
		outState.putBoolean(USE_COLLAPSIBLE_DESCRIPTION, collapsibleDescription);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
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

	private void buildDescriptionItem(Context ctx, String description) {
		if (collapsibleDescription) {
			BottomSheetItemTitleWithDescrAndButton[] preferenceDescription = new BottomSheetItemTitleWithDescrAndButton[1];
			preferenceDescription[0] = (BottomSheetItemTitleWithDescrAndButton) new BottomSheetItemTitleWithDescrAndButton.Builder()
					.setButtonTitle(getString(R.string.shared_string_read_more))
					.setButtonTextColor(AndroidUtils.resolveAttribute(ctx, R.attr.active_color_basic))
					.setOnButtonClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							descriptionExpanded = !descriptionExpanded;
							int maxLines = descriptionExpanded ? Integer.MAX_VALUE : COLLAPSED_DESCRIPTION_LINES;
							preferenceDescription[0].setDescriptionMaxLines(maxLines);
							setupHeightAndBackground(getView());
						}
					})
					.setDescriptionMaxLines(COLLAPSED_DESCRIPTION_LINES)
					.setDescription(description)
					.setLayoutId(R.layout.bottom_sheet_item_with_expandable_descr)
					.create();
			items.add(preferenceDescription[0]);
		} else {
			BaseBottomSheetItem preferenceDescription = new BottomSheetItemWithDescription.Builder()
					.setDescription(description)
					.setLayoutId(R.layout.bottom_sheet_item_preference_descr)
					.create();
			items.add(preferenceDescription);
		}
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager, String key, Fragment target, boolean usedOnMap,
	                                   @Nullable ApplicationMode appMode, boolean profileDependent, boolean collapsibleDescription) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			Bundle args = new Bundle();
			args.putString(PREFERENCE_ID, key);
			args.putBoolean(USE_COLLAPSIBLE_DESCRIPTION, collapsibleDescription);

			SingleSelectPreferenceBottomSheet fragment = new SingleSelectPreferenceBottomSheet();
			fragment.setArguments(args);
			fragment.setUsedOnMap(usedOnMap);
			fragment.setAppMode(appMode);
			fragment.setTargetFragment(target, 0);
			fragment.setProfileDependent(profileDependent);
			fragment.show(fragmentManager, TAG);
			return true;
		}
		return false;
	}
}
package net.osmand.plus.settings.bottomsheets;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.widget.EditText;

import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.OnPreferenceChanged;
import net.osmand.plus.settings.preferences.EditTextPreferenceEx;
import net.osmand.util.Algorithms;

public class EditTextPreferenceBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = EditTextPreferenceBottomSheet.class.getSimpleName();

	private static final String EDIT_TEXT_PREFERENCE_KEY = "edit_text_preference_key";

	private EditText editText;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context ctx = getContext();
		EditTextPreferenceEx editTextPreference = getEditTextPreference();
		if (ctx == null || editTextPreference == null) {
			return;
		}

		items.add(new TitleItem(editTextPreference.getDialogTitle().toString()));
		String text;
		if (savedInstanceState != null) {
			text = savedInstanceState.getString(EDIT_TEXT_PREFERENCE_KEY);
		} else {
			text = editTextPreference.getText();
		}

		editText = new EditText(ctx);
		editText.setText(text);
		items.add(new SimpleBottomSheetItem.Builder().setCustomView(editText).create());

		String description = editTextPreference.getDescription();
		if (!Algorithms.isEmpty(description)) {
			items.add(new LongDescriptionItem(description));
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(EDIT_TEXT_PREFERENCE_KEY, editText.getText().toString());
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
		EditTextPreferenceEx editTextPreference = getEditTextPreference();
		if (editTextPreference != null) {
			String value = editText.getText().toString();
			if (editTextPreference.callChangeListener(value)) {
				editTextPreference.setText(value);

				Fragment target = getTargetFragment();
				if (target instanceof OnPreferenceChanged) {
					((OnPreferenceChanged) target).onPreferenceChanged(editTextPreference.getKey());
				}
			}
		}

		dismiss();
	}

	private EditTextPreferenceEx getEditTextPreference() {
		return (EditTextPreferenceEx) getPreference();
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager, String key, Fragment target, boolean usedOnMap) {
		try {
			Bundle args = new Bundle();
			args.putString(PREFERENCE_ID, key);

			EditTextPreferenceBottomSheet fragment = new EditTextPreferenceBottomSheet();
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
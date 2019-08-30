package net.osmand.plus.settings;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.preference.DialogPreference;
import android.widget.EditText;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.preferences.EditTextPreferenceEx;
import net.osmand.util.Algorithms;

public class EditTextPreferenceBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = EditTextPreferenceBottomSheet.class.getSimpleName();

	private static final String PREFERENCE_ID = "preference_id";
	private static final String EDIT_TEXT_PREFERENCE_KEY = "edit_text_preference_key";

	private EditTextPreferenceEx editTextPreference;

	private EditText editText;
	private String text;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		editTextPreference = getEditTextPreference();
		if (savedInstanceState == null) {
			text = getEditTextPreference().getText();
		} else {
			text = savedInstanceState.getString(EDIT_TEXT_PREFERENCE_KEY);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final Context context = getContext();
		if (context == null || editTextPreference == null) {
			return;
		}

		items.add(new TitleItem(editTextPreference.getDialogTitle().toString()));

		editText = new EditText(context);
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
		outState.putString(EDIT_TEXT_PREFERENCE_KEY, text);
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
		String value = editText.getText().toString();
		if (editTextPreference.callChangeListener(value)) {
			editTextPreference.setText(value);
		}
		dismiss();
	}

	private EditTextPreferenceEx getEditTextPreference() {
		if (editTextPreference == null) {
			Bundle args = getArguments();
			if (args != null) {
				final String key = args.getString(PREFERENCE_ID);
				Fragment targetFragment = getTargetFragment();
				if (targetFragment instanceof DialogPreference.TargetFragment) {
					DialogPreference.TargetFragment fragment = (DialogPreference.TargetFragment) targetFragment;
					editTextPreference = (EditTextPreferenceEx) fragment.findPreference(key);
				}
			}
		}
		return editTextPreference;
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager, String key, Fragment target) {
		try {
			Bundle args = new Bundle();
			args.putString(PREFERENCE_ID, key);

			EditTextPreferenceBottomSheet fragment = new EditTextPreferenceBottomSheet();
			fragment.setTargetFragment(target, 0);
			fragment.setArguments(args);
			fragment.show(fragmentManager, TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}
}
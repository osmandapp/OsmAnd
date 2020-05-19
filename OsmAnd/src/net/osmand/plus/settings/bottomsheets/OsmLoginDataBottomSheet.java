package net.osmand.plus.settings.bottomsheets;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.osmedit.SettingsOsmEditingActivity;
import net.osmand.plus.settings.fragments.OnPreferenceChanged;

public class OsmLoginDataBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = EditTextPreferenceBottomSheet.class.getSimpleName();

	private static final String USER_NAME_KEY = "user_name_key";
	private static final String PASSWORD_KEY = "password_key";

	private EditText userNameEditText;
	private EditText passwordEditText;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context context = getContext();
		if (context == null) {
			return;
		}
		OsmandApplication app = requiredMyApplication();

		View view = UiUtilities.getInflater(context, nightMode).inflate(R.layout.osm_login_data, null);

		userNameEditText = view.findViewById(R.id.name_edit_text);
		passwordEditText = view.findViewById(R.id.password_edit_text);

		String name = app.getSettings().USER_NAME.get();
		String password = app.getSettings().USER_PASSWORD.get();

		if (savedInstanceState != null) {
			name = savedInstanceState.getString(USER_NAME_KEY, null);
			password = savedInstanceState.getString(PASSWORD_KEY, null);
		}

		userNameEditText.setText(name);
		passwordEditText.setText(password);

		BaseBottomSheetItem titleItem = new SimpleBottomSheetItem.Builder()
				.setCustomView(view)
				.create();
		items.add(titleItem);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(USER_NAME_KEY, userNameEditText.getText().toString());
		outState.putString(PASSWORD_KEY, passwordEditText.getText().toString());
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
		OsmandApplication app = requiredMyApplication();

		app.getSettings().USER_NAME.set(userNameEditText.getText().toString());
		app.getSettings().USER_PASSWORD.set(passwordEditText.getText().toString());
		new SettingsOsmEditingActivity.ValidateOsmLoginDetailsTask(app).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

		Fragment target = getTargetFragment();
		Preference preference = getPreference();
		if (target instanceof OnPreferenceChanged && preference != null) {
			((OnPreferenceChanged) target).onPreferenceChanged(preference.getKey());
		}

		dismiss();
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager, String key, Fragment target,
	                                   boolean usedOnMap, @Nullable ApplicationMode appMode) {
		try {
			Bundle args = new Bundle();
			args.putString(PREFERENCE_ID, key);

			OsmLoginDataBottomSheet fragment = new OsmLoginDataBottomSheet();
			fragment.setArguments(args);
			fragment.setUsedOnMap(usedOnMap);
			fragment.setAppMode(appMode);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}
}
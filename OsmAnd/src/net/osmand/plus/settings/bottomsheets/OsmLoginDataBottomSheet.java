package net.osmand.plus.settings.bottomsheets;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.textfield.TextInputLayout;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.osmedit.ValidateOsmLoginDetailsTask;
import net.osmand.plus.osmedit.ValidateOsmLoginDetailsTask.ValidateOsmLoginListener;
import net.osmand.plus.settings.backend.ApplicationMode;

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

		LayoutInflater themedInflater = UiUtilities.getInflater(requireContext(), nightMode);
		View view = themedInflater.inflate(R.layout.osm_login_data, null);

		userNameEditText = view.findViewById(R.id.name_edit_text);
		passwordEditText = view.findViewById(R.id.password_edit_text);
		userNameEditText.setBackgroundColor(getResolvedColor(R.color.background_field));
		passwordEditText.setBackgroundColor(getResolvedColor(R.color.background_field));

		String name = app.getSettings().USER_NAME.get();
		String password = app.getSettings().USER_PASSWORD.get();

		if (savedInstanceState != null) {
			name = savedInstanceState.getString(USER_NAME_KEY, null);
			password = savedInstanceState.getString(PASSWORD_KEY, null);
		}

		userNameEditText.setText(name);
		passwordEditText.setText(password);

		TextInputLayout loginBox = view.findViewById(R.id.name_text_box);
		TextInputLayout passwordBox = view.findViewById(R.id.password_text_box);

		loginBox.setBoxStrokeColor(getResolvedColor(R.color.osmand_orange));
		loginBox.setHintTextColor(AppCompatResources.getColorStateList(getContext(),R.color.hint_label));
		passwordBox.setBoxStrokeColor(getResolvedColor(R.color.osmand_orange));
		passwordBox.setHintTextColor(AppCompatResources.getColorStateList(getContext(),R.color.hint_label));

		passwordBox.setStartIconDrawable(R.drawable.ic_action_lock);
		loginBox.setStartIconDrawable(R.drawable.ic_action_user_account);
		loginBox.setEndIconMode(TextInputLayout.END_ICON_CLEAR_TEXT);
		passwordBox.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);

		SimpleBottomSheetItem titleItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
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
		return R.string.user_login;
	}

	@Override
	protected void onRightBottomButtonClick() {
		OsmandApplication app = requiredMyApplication();

		app.getSettings().USER_NAME.set(userNameEditText.getText().toString());
		app.getSettings().USER_PASSWORD.set(passwordEditText.getText().toString());

		Fragment targetFragment = getTargetFragment();
		if (targetFragment instanceof ValidateOsmLoginListener) {
			ValidateOsmLoginDetailsTask validateTask = new ValidateOsmLoginDetailsTask(app, (ValidateOsmLoginListener) targetFragment);
			validateTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
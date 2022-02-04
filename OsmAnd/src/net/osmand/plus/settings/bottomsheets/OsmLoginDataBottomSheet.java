package net.osmand.plus.settings.bottomsheets;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.textfield.TextInputLayout;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.plugins.osmedit.asynctasks.ValidateOsmLoginDetailsTask;
import net.osmand.plus.plugins.osmedit.asynctasks.ValidateOsmLoginDetailsTask.ValidateOsmLoginListener;
import net.osmand.plus.settings.backend.ApplicationMode;

public class OsmLoginDataBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = EditTextPreferenceBottomSheet.class.getSimpleName();

	private static final String USER_NAME_KEY = "user_name_key";
	private static final String PASSWORD_KEY = "password_key";

	private EditText userNameEditText;
	private EditText passwordEditText;

	private OsmEditingPlugin plugin;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context context = getContext();
		plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);
		if (plugin == null || context == null) {
			return;
		}
		OsmandApplication app = requiredMyApplication();

		LayoutInflater themedInflater = UiUtilities.getInflater(requireContext(), nightMode);
		View view = themedInflater.inflate(R.layout.osm_login_data, null);
		view.getViewTreeObserver().addOnGlobalLayoutListener(getShadowLayoutListener());

		userNameEditText = view.findViewById(R.id.name_edit_text);
		passwordEditText = view.findViewById(R.id.password_edit_text);

		String name = plugin.OSM_USER_NAME_OR_EMAIL.get();
		String password = plugin.OSM_USER_PASSWORD.get();

		if (savedInstanceState != null) {
			name = savedInstanceState.getString(USER_NAME_KEY, null);
			password = savedInstanceState.getString(PASSWORD_KEY, null);
		}

		userNameEditText.setText(name);
		passwordEditText.setText(password);

		TextInputLayout loginBox = view.findViewById(R.id.name_text_box);
		TextInputLayout passwordBox = view.findViewById(R.id.password_text_box);

		passwordBox.setStartIconDrawable(R.drawable.ic_action_lock);
		loginBox.setStartIconDrawable(R.drawable.ic_action_user_account);
		loginBox.setEndIconMode(TextInputLayout.END_ICON_CLEAR_TEXT);
		passwordBox.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);

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
		return R.string.user_login;
	}

	@Override
	protected void onRightBottomButtonClick() {
		plugin.OSM_USER_NAME_OR_EMAIL.set(userNameEditText.getText().toString());
		plugin.OSM_USER_PASSWORD.set(passwordEditText.getText().toString());
		if (getTargetFragment() instanceof ValidateOsmLoginListener) {
			ValidateOsmLoginListener listener = (ValidateOsmLoginListener) getTargetFragment();
			ValidateOsmLoginDetailsTask.execute(requiredMyApplication(), plugin, listener);
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
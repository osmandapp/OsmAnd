package net.osmand.plus.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment;
import net.osmand.plus.profiles.SettingsProfileFragment;
import net.osmand.plus.widgets.OsmandTextFieldBoxes;
import net.osmand.util.Algorithms;

import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.DIALOG_TYPE;
import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.SELECTED_KEY;
import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.TYPE_BASE_APP_PROFILE;

public class ProfileAppearanceFragment extends BaseSettingsFragment {

	private static final String MASTER_PROFILE = "master_profile";
	private static final String PROFILE_NAME = "profile_name";
	private static final String SELECT_COLOR = "select_color";
	private static final String SELECT_ICON = "select_icon";
	private SelectProfileBottomSheetDialogFragment.SelectProfileListener parentProfileListener;
	private EditText baseProfileName;
	private ApplicationProfileObject profile;
	private ApplicationProfileObject changedProfile;
	private Button cancelButton;
	private Button saveButton;
	private EditText profileName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			profile.parent = getSelectedAppMode().getParent();
		} else {
			profile = new ApplicationProfileObject();
			profile.parent = getSelectedAppMode().getParent();
			profile.name = "";
			changedProfile = new ApplicationProfileObject();
			changedProfile.parent = profile.parent;
			changedProfile.name = profile.name;
		}
	}

	@Override
	protected void setupPreferences() {
		PreferenceCategory selectColor = (PreferenceCategory) findPreference(SELECT_COLOR);
		selectColor.setIconSpaceReserved(false);
		PreferenceCategory selectIcon = (PreferenceCategory) findPreference(SELECT_ICON);
		selectIcon.setIconSpaceReserved(false);


	}

	@SuppressLint("InlinedApi")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			FrameLayout frameLayout = view.findViewById(android.R.id.list_container);
			View inflatedLayout = UiUtilities.getInflater(getContext(), isNightMode())
					.inflate(R.layout.preference_cancel_save_button, frameLayout, false);
			(frameLayout).addView(inflatedLayout);
			cancelButton=inflatedLayout.findViewById(R.id.cancel_button);
			saveButton=inflatedLayout.findViewById(R.id.save_profile_btn);
			cancelButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (getActivity() != null) {
						getActivity().onBackPressed();
					}
				}
			});
			saveButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (getActivity() != null) {
						getActivity().onBackPressed();
					}
				}
			});
		}
		return view;
	}

	@Override
	protected void createToolbar(LayoutInflater inflater, View view) {
		super.createToolbar(inflater, view);
		View profileIcon = view.findViewById(R.id.profile_button);
		profileIcon.setVisibility(View.VISIBLE);
	}

	@Override
	protected void onBindPreferenceViewHolder(Preference preference, PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);
		if (PROFILE_NAME.equals(preference.getKey())) {
			profileName = (EditText) holder.findViewById(R.id.profile_name_et);
			profileName.setText(getSelectedAppMode().toHumanString(getContext()));

			profileName.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}

				@Override
				public void afterTextChanged(Editable s) {
					profile.name = s.toString();
				}
			});
		} else if (MASTER_PROFILE.equals(preference.getKey())) {
			baseProfileName = (EditText) holder.findViewById(R.id.navigation_type_et);
			baseProfileName.setText(getSelectedAppMode().getParent() != null ? getSelectedAppMode().getParent().toHumanString(getContext()) : getSelectedAppMode().toHumanString(getContext()));
			OsmandTextFieldBoxes baseProfileNameHint = (OsmandTextFieldBoxes) holder.findViewById(R.id.navigation_type_otfb);
			baseProfileNameHint.setLabelText(getString(R.string.master_profile));
			FrameLayout selectNavTypeBtn = (FrameLayout) holder.findViewById(R.id.select_nav_type_btn);
			selectNavTypeBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (getSelectedAppMode().isCustomProfile()) {
						hideKeyboard();
						final SelectProfileBottomSheetDialogFragment fragment = new SelectProfileBottomSheetDialogFragment();
						Bundle bundle = new Bundle();
						if (getSelectedAppMode() != null) {
							bundle.putString(SELECTED_KEY, getSelectedAppMode().getRoutingProfile());
						}
						bundle.putString(DIALOG_TYPE, TYPE_BASE_APP_PROFILE);
						fragment.setArguments(bundle);
						if (getActivity() != null) {
							getActivity().getSupportFragmentManager().beginTransaction()
									.add(fragment, "select_nav_type").commitAllowingStateLoss();
						}
					}
				}
			});
		}
	}

	private void hideKeyboard() {
		Activity activity = getActivity();
		if (activity != null) {
			View cf = activity.getCurrentFocus();
			if (cf != null) {
				InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
				if (imm != null) {
					imm.hideSoftInputFromWindow(cf.getWindowToken(), 0);
				}
			}
		}
	}

	public SelectProfileBottomSheetDialogFragment.SelectProfileListener getParentProfileListener() {
		if (parentProfileListener == null) {
			parentProfileListener = new SelectProfileBottomSheetDialogFragment.SelectProfileListener() {
				@Override
				public void onSelectedType(int pos, String stringRes) {
					updateParentProfile(pos);
				}
			};
		}
		return parentProfileListener;
	}

	void updateParentProfile(int pos) {

		String key = SettingsProfileFragment.getBaseProfiles(getMyApplication()).get(pos).getStringKey();
		setupBaseProfileView(key);
		profile.parent = ApplicationMode.valueOfStringKey(key, ApplicationMode.DEFAULT);
	}

	private void setupBaseProfileView(String stringKey) {
		for (ApplicationMode am : ApplicationMode.getDefaultValues()) {
			if (am.getStringKey().equals(stringKey)) {
				baseProfileName.setText(Algorithms.capitalizeFirstLetter(am.toHumanString(app)));
			}
		}
	}

	class ApplicationProfileObject {
		ApplicationMode parent = null;
		String name;
	}
}

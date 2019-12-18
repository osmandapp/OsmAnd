package net.osmand.plus.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
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
import android.widget.ImageView;

import net.osmand.AndroidUtils;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment;
import net.osmand.plus.profiles.SettingsProfileFragment;
import net.osmand.plus.widgets.FlowLayout;
import net.osmand.plus.widgets.OsmandTextFieldBoxes;
import net.osmand.util.Algorithms;

import java.util.ArrayList;

import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.DIALOG_TYPE;
import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.SELECTED_KEY;
import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.TYPE_BASE_APP_PROFILE;

public class ProfileAppearanceFragment extends BaseSettingsFragment {

	private static final String MASTER_PROFILE = "master_profile";
	private static final String PROFILE_NAME = "profile_name";
	private static final String SELECT_COLOR = "select_color";
	private static final String SELECT_ICON = "select_icon";
	private static final String COLOR_ITEMS = "color_items";
	private static final String ICON_ITEMS = "icon_items";
	public static final String PROFILE_NAME_KEY = "profile_name_key";
	public static final String PROFILE_ICON_RES_KEY = "profile_icon_res_key";
	public static final String PROFILE_COLOR_KEY = "profile_color_key";
	public static final String PROFILE_PARENT_KEY = "profile_parent_key";
	private SelectProfileBottomSheetDialogFragment.SelectProfileListener parentProfileListener;
	private EditText baseProfileName;
	private ApplicationProfileObject profile;
	private ApplicationProfileObject changedProfile;
	private Button cancelButton;
	private Button saveButton;
	private EditText profileName;
	private FlowLayout colorItems;
	private FlowLayout iconItems;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			changedProfile = new ApplicationProfileObject();
			restoreState(savedInstanceState);
		} else {
			profile = new ApplicationProfileObject();
			profile.parent = getSelectedAppMode().getParent();
			profile.name = getSelectedAppMode().toHumanString(getContext());
			profile.color = getSelectedAppMode().getIconColorInfo();
			profile.iconRes = getSelectedAppMode().getIconRes();
			changedProfile = new ApplicationProfileObject();
			changedProfile.parent = profile.parent;
			changedProfile.name = profile.name;
			changedProfile.color = profile.color;
			changedProfile.iconRes = profile.iconRes;
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
						if (profile.equals(changedProfile)) {
							getActivity().onBackPressed();
						} else {

						}
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
	public void onSaveInstanceState(Bundle outState) {
		saveState(outState);
		super.onSaveInstanceState(outState);
	}

	private void saveState(Bundle outState) {
		outState.putString(PROFILE_NAME_KEY, changedProfile.name);
		outState.putInt(PROFILE_ICON_RES_KEY, changedProfile.iconRes);
		outState.putSerializable(PROFILE_COLOR_KEY, changedProfile.color);
		outState.putString(PROFILE_PARENT_KEY, changedProfile.parent.getStringKey());
	}

	private void restoreState(Bundle savedInstanceState) {
		changedProfile.name = savedInstanceState.getString(PROFILE_NAME_KEY);
		changedProfile.iconRes = savedInstanceState.getInt(PROFILE_ICON_RES_KEY);
		changedProfile.color = (ApplicationMode.ProfileIconColors) savedInstanceState.getSerializable(PROFILE_COLOR_KEY);
		String stringKey = savedInstanceState.getString(PROFILE_PARENT_KEY);
		changedProfile.parent = ApplicationMode.valueOfStringKey(stringKey, null);
		OsmandApplication app = getMyApplication();
		if (changedProfile.parent == null) {
			changedProfile.parent = app.getSettings().getApplicationMode();
		}
	}

	@Override
	protected void updateProfileButton() {
		View view = getView();
		if (view == null) {
			return;
		}
		View profileButton = view.findViewById(R.id.profile_button);
		if (profileButton != null) {
			int iconColor = ContextCompat.getColor(getContext(), changedProfile.color.getColor(isNightMode()));
			AndroidUtils.setBackground(profileButton, UiUtilities.tintDrawable(ContextCompat.getDrawable(getContext(),
					R.drawable.circle_background_light), UiUtilities.getColorWithAlpha(iconColor, 0.1f)));
			ImageView profileIcon = view.findViewById(R.id.profile_icon);
			if (profileIcon != null) {
				profileIcon.setImageDrawable(getPaintedIcon(changedProfile.iconRes, iconColor));
			}
		}
	}

	@Override
	protected void updatePreference(Preference preference) {
		super.updatePreference(preference);
	}

	@Override
	protected void onBindPreferenceViewHolder(Preference preference, PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);
		if (PROFILE_NAME.equals(preference.getKey())) {
			profileName = (EditText) holder.findViewById(R.id.profile_name_et);
			profileName.setText(changedProfile.name);

			profileName.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}

				@Override
				public void afterTextChanged(Editable s) {
					changedProfile.name = s.toString();
				}
			});
		} else if (MASTER_PROFILE.equals(preference.getKey())) {
			baseProfileName = (EditText) holder.findViewById(R.id.navigation_type_et);
			baseProfileName.setText(changedProfile.parent != null
					? changedProfile.parent.toHumanString(getContext())
					: getSelectedAppMode().toHumanString(getContext()));
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
		} else if (COLOR_ITEMS.equals(preference.getKey())) {
			colorItems = (FlowLayout) holder.findViewById(R.id.color_items);
			colorItems.removeAllViews();
			for (ApplicationMode.ProfileIconColors color : ApplicationMode.ProfileIconColors.values()) {
				View view = createColorView(color);
				colorItems.addView(view, new FlowLayout.LayoutParams(0, 0));
				ImageView outlineCircle = view.findViewById(R.id.outlineCircle);
				ImageView checkMark = view.findViewById(R.id.checkMark);
				GradientDrawable gradientDrawable = (GradientDrawable) ContextCompat.getDrawable(getContext(), R.drawable.circle_contour_bg_light);
				if (gradientDrawable != null) {
					gradientDrawable.setStroke(AndroidUtils.dpToPx(getContext(), 2),
							UiUtilities.getColorWithAlpha(ContextCompat.getColor(getContext(), color.getColor(isNightMode())), 0.3f));
					outlineCircle.setImageDrawable(gradientDrawable);
				}
				checkMark.setVisibility(View.GONE);
				outlineCircle.setVisibility(View.GONE);
			}
			updateColorSelector(changedProfile.color);
		} else if (ICON_ITEMS.equals(preference.getKey())) {
			iconItems = (FlowLayout) holder.findViewById(R.id.color_items);
			iconItems.removeAllViews();
			ArrayList<Integer> icons = new ArrayList<>();
			icons.add(R.drawable.ic_action_car_dark);
			icons.add(R.drawable.ic_action_taxi);
			icons.add(R.drawable.ic_action_truck_dark);
			icons.add(R.drawable.ic_action_shuttle_bus);
			icons.add(R.drawable.ic_action_bus_dark);
			icons.add(R.drawable.ic_action_subway);
			icons.add(R.drawable.ic_action_motorcycle_dark);
			icons.add(R.drawable.ic_action_bicycle_dark);
			icons.add(R.drawable.ic_action_horse);
			icons.add(R.drawable.ic_action_pedestrian_dark);
			icons.add(R.drawable.ic_action_trekking_dark);
			icons.add(R.drawable.ic_action_skiing);
			icons.add(R.drawable.ic_action_sail_boat_dark);
			icons.add(R.drawable.ic_action_aircraft);
			icons.add(R.drawable.ic_action_helicopter);
			icons.add(R.drawable.ic_action_personal_transporter);
			icons.add(R.drawable.ic_action_monowheel);
			icons.add(R.drawable.ic_action_scooter);
			icons.add(R.drawable.ic_action_ufo);
			icons.add(R.drawable.ic_action_offroad);
			icons.add(R.drawable.ic_action_campervan);
			icons.add(R.drawable.ic_action_camper);
			icons.add(R.drawable.ic_action_pickup_truck);
			icons.add(R.drawable.ic_action_wagon);
			icons.add(R.drawable.ic_action_utv);
			for (int iconRes : icons) {
				View view = createIconView(iconRes);
				iconItems.addView(view, new FlowLayout.LayoutParams(0, 0));
				ImageView outlineCircle = view.findViewById(R.id.outlineCircle);
				outlineCircle.setVisibility(View.GONE);
			}
			setIconNewColor(changedProfile.iconRes);
		}
	}

	private View createColorView(final ApplicationMode.ProfileIconColors colorRes) {
		FrameLayout colorView = (FrameLayout) UiUtilities.getInflater(getContext(), isNightMode())
				.inflate(R.layout.preference_circle_item, null, false);
		ImageView coloredCircle = colorView.findViewById(R.id.bckgroundCircle);
		coloredCircle.setImageDrawable(getPaintedIcon(R.drawable.circle_background_light,
				ContextCompat.getColor(getContext(), colorRes.getColor(isNightMode()))));
		coloredCircle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (colorRes != changedProfile.color) {
					updateColorSelector(colorRes);
				}
			}
		});
		colorView.setTag(colorRes);
		return colorView;
	}

	private void updateColorSelector(ApplicationMode.ProfileIconColors color) {
		View view = colorItems.findViewWithTag(changedProfile.color);
		view.findViewById(R.id.outlineCircle).setVisibility(View.GONE);
		view.findViewById(R.id.checkMark).setVisibility(View.GONE);
		view = colorItems.findViewWithTag(color);
		view.findViewById(R.id.outlineCircle).setVisibility(View.VISIBLE);
		view.findViewById(R.id.checkMark).setVisibility(View.VISIBLE);
		changedProfile.color = color;
		if (iconItems != null) {
			setIconNewColor(changedProfile.iconRes);
		}
		updateProfileButton();
	}

	private View createIconView(final int iconRes) {
		FrameLayout iconView = (FrameLayout) UiUtilities.getInflater(getContext(), isNightMode())
				.inflate(R.layout.preference_circle_item, null, false);
		ImageView checkMark = iconView.findViewById(R.id.checkMark);
		checkMark.setImageDrawable(app.getUIUtilities().getIcon(iconRes, R.color.icon_color_default_light));
		ImageView coloredCircle = iconView.findViewById(R.id.bckgroundCircle);
		coloredCircle.setImageDrawable(getPaintedIcon(R.drawable.circle_background_light,
				UiUtilities.getColorWithAlpha(ContextCompat.getColor(getContext(), R.color.icon_color_default_light), 0.1f)));
		coloredCircle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (iconRes != changedProfile.iconRes) {
					updateIconSelector(iconRes);
				}
			}
		});
		iconView.setTag(iconRes);
		return iconView;
	}

	private void updateIconSelector(int iconRes) {
		setIconNewColor(iconRes);
		View view = iconItems.findViewWithTag(changedProfile.iconRes);
		view.findViewById(R.id.outlineCircle).setVisibility(View.GONE);
		ImageView checkMark = view.findViewById(R.id.checkMark);
		checkMark.setImageDrawable(app.getUIUtilities().getIcon(changedProfile.iconRes, R.color.icon_color_default_light));
		ImageView coloredCircle = view.findViewById(R.id.bckgroundCircle);
		coloredCircle.setImageDrawable(getPaintedIcon(R.drawable.circle_background_light,
				UiUtilities.getColorWithAlpha(ContextCompat.getColor(getContext(), R.color.icon_color_default_light), 0.1f)));
		changedProfile.iconRes = iconRes;
		updateProfileButton();
	}

	private void setIconNewColor(int iconRes) {
		int changedProfileColor = ContextCompat.getColor(getContext(), changedProfile.color.getColor(
				app.getDaynightHelper().isNightModeForMapControls()));
		View view = iconItems.findViewWithTag(iconRes);
		ImageView coloredCircle = view.findViewById(R.id.bckgroundCircle);
		coloredCircle.setImageDrawable(getPaintedIcon(R.drawable.circle_background_light,
				UiUtilities.getColorWithAlpha(ContextCompat.getColor(getContext(), changedProfile.color.getColor(isNightMode())), 0.1f)));
		ImageView outlineCircle = view.findViewById(R.id.outlineCircle);
		GradientDrawable gradientDrawable = (GradientDrawable) ContextCompat.getDrawable(getContext(), R.drawable.circle_contour_bg_light);
		if (gradientDrawable != null) {
			gradientDrawable.setStroke(AndroidUtils.dpToPx(getContext(), 2), changedProfileColor);
		}
		outlineCircle.setImageDrawable(gradientDrawable);
		outlineCircle.setVisibility(View.VISIBLE);
		ImageView checkMark = view.findViewById(R.id.checkMark);
		checkMark.setImageDrawable(app.getUIUtilities().getPaintedIcon(iconRes, changedProfileColor));
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
		changedProfile.parent = ApplicationMode.valueOfStringKey(key, ApplicationMode.DEFAULT);
	}

	private void setupBaseProfileView(String stringKey) {
		for (ApplicationMode am : ApplicationMode.getDefaultValues()) {
			if (am.getStringKey().equals(stringKey)) {
				baseProfileName.setText(Algorithms.capitalizeFirstLetter(am.toHumanString(app)));
			}
		}
	}

/*	private boolean saveNewProfile() {
    boolean isNew = true;
		if (changedProfile.name.isEmpty()
				|| changedProfile.name.replace(" ", "").length() < 1
				|| profileName.getText().toString().replace(" ", "").length() < 1) {
			showSaveWarningDialog(
					getString(R.string.profile_alert_need_profile_name_title),
					getString(R.string.profile_alert_need_profile_name_msg),
					getActivity()
			);
			return false;
		}

		for (ApplicationMode m : ApplicationMode.allPossibleValues()) {
			if (m.getCustomProfileName() != null && getActivity() != null &&
					m.getCustomProfileName().equals(changedProfile.name) && isNew) {
				AlertDialog.Builder bld = new AlertDialog.Builder(getActivity());
				bld.setTitle(R.string.profile_alert_duplicate_name_title);
				bld.setMessage(R.string.profile_alert_duplicate_name_msg);
				bld.setNegativeButton(R.string.shared_string_dismiss, null);
				bld.show();
				bld.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						profileName.requestFocus();
					}
				});
				return false;
			}
		}
		String customStringKey = profile.stringKey;
		if (isNew) {
			customStringKey =
					profile.parent.getStringKey() + "_" + System.currentTimeMillis();
		}

		ApplicationMode.ApplicationModeBuilder builder = ApplicationMode
				.createCustomMode(changedProfile.parent, changedProfile.name.trim(), customStringKey)
				.icon(app, profile.iconStringName);

		if(profile.routingProfileDataObject.getStringKey().equals(
				EditProfileFragment.RoutingProfilesResources.STRAIGHT_LINE_MODE.name())) {
			builder.setRouteService(RouteProvider.RouteService.STRAIGHT);
		} else if(profile.routingProfileDataObject.getStringKey().equals(
				EditProfileFragment.RoutingProfilesResources.BROUTER_MODE.name())) {
			builder.setRouteService(RouteProvider.RouteService.BROUTER);
		} else if (profile.routingProfileDataObject != null) {
			builder.setRoutingProfile(profile.routingProfileDataObject.getStringKey());
		}
		builder.setColor(changedProfile.color);

		mode = ApplicationMode.saveProfile(builder, getMyApplication());
		if (!ApplicationMode.values(app).contains(mode)) {
			ApplicationMode.changeProfileAvailability(mode, true, getMyApplication());
		}
		return true;
	}*/


	private void showSaveWarningDialog(String title, String message, Activity activity) {
		AlertDialog.Builder bld = new AlertDialog.Builder(activity);
		bld.setTitle(title);
		bld.setMessage(message);
		bld.setNegativeButton(R.string.shared_string_dismiss, null);
		bld.show();
	}

	class ApplicationProfileObject {
		ApplicationMode parent = null;
		String name;
		ApplicationMode.ProfileIconColors color;
		int iconRes;
	}
}

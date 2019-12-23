package net.osmand.plus.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment;
import net.osmand.plus.profiles.SettingsProfileFragment;
import net.osmand.plus.routing.RouteProvider;
import net.osmand.plus.widgets.FlowLayout;
import net.osmand.plus.widgets.OsmandTextFieldBoxes;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SETTINGS_ID;
import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.DIALOG_TYPE;
import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.SELECTED_KEY;
import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.TYPE_BASE_APP_PROFILE;

public class ProfileAppearanceFragment extends BaseSettingsFragment {

	private static final Log LOG = PlatformUtil.getLog(ProfileAppearanceFragment.class);

	public static final String TAG = ProfileAppearanceFragment.class.getName();
	private static final String MASTER_PROFILE = "master_profile";
	private static final String PROFILE_NAME = "profile_name";
	private static final String SELECT_COLOR = "select_color";
	private static final String SELECT_ICON = "select_icon";
	private static final String COLOR_ITEMS = "color_items";
	private static final String ICON_ITEMS = "icon_items";
//	private static final String SELECT_MAP_ICON = "select_map_icon";
//	private static final String SELECT_NAV_ICON = "select_nav_icon";

	public static final String PROFILE_NAME_KEY = "profile_name_key";
	public static final String PROFILE_STRINGKEY_KEY = "profile_stringkey_key";
	public static final String PROFILE_ICON_RES_KEY = "profile_icon_res_key";
	public static final String PROFILE_COLOR_KEY = "profile_color_key";
	public static final String PROFILE_PARENT_KEY = "profile_parent_key";
	public static final String BASE_PROFILE_FOR_NEW = "base_profile_for_new";
	private SelectProfileBottomSheetDialogFragment.SelectProfileListener parentProfileListener;
	private EditText baseProfileName;
	private ApplicationProfileObject profile;
	private ApplicationProfileObject changedProfile;
	private EditText profileName;
	private FlowLayout colorItems;
	private FlowLayout iconItems;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		profile = new ApplicationProfileObject();
		ApplicationMode baseModeForNewProfile = null;
		if (getArguments() != null) {
			Bundle arguments = getArguments();
			String keyBaseProfileForNew = arguments.getString(BASE_PROFILE_FOR_NEW, null);
			for (ApplicationMode mode : ApplicationMode.getDefaultValues()) {
				if (mode.getStringKey().equals(keyBaseProfileForNew)) {
					baseModeForNewProfile = mode;
					break;
				}
			}
		}
		if (baseModeForNewProfile != null) {
			profile.stringKey = baseModeForNewProfile.getStringKey() + "_" + System.currentTimeMillis();
			profile.parent = baseModeForNewProfile;
			profile.name = baseModeForNewProfile.toHumanString(getContext());
			profile.color = baseModeForNewProfile.getIconColorInfo();
			profile.iconRes = baseModeForNewProfile.getIconRes();
			profile.routingProfile = baseModeForNewProfile.getRoutingProfile();
			profile.routeService = baseModeForNewProfile.getRouteService();
		} else {
			profile.stringKey = getSelectedAppMode().getStringKey();
			profile.parent = getSelectedAppMode().getParent();
			profile.name = getSelectedAppMode().toHumanString(getContext());
			profile.color = getSelectedAppMode().getIconColorInfo();
			profile.iconRes = getSelectedAppMode().getIconRes();
			profile.routingProfile = getSelectedAppMode().getRoutingProfile();
			profile.routeService = getSelectedAppMode().getRouteService();
		}
		changedProfile = new ApplicationProfileObject();
		if (savedInstanceState != null) {
			restoreState(savedInstanceState);
		} else {
			changedProfile.stringKey = profile.stringKey;
			changedProfile.parent = profile.parent;
			changedProfile.name = profile.name;
			changedProfile.color = profile.color;
			changedProfile.iconRes = profile.iconRes;
			changedProfile.routingProfile = profile.routingProfile;
			changedProfile.routeService = profile.routeService;
		}
	}

	@Override
	protected void setupPreferences() {
		findPreference(SELECT_COLOR).setIconSpaceReserved(false);
		findPreference(SELECT_ICON).setIconSpaceReserved(false);
//		findPreference(SELECT_MAP_ICON).setIconSpaceReserved(false);
//		findPreference(SELECT_NAV_ICON).setIconSpaceReserved(false);
	}

	@SuppressLint("InlinedApi")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			FrameLayout preferencesContainer = view.findViewById(android.R.id.list_container);
			LayoutInflater themedInflater = UiUtilities.getInflater(getContext(), isNightMode());
			View buttonsContainer = themedInflater.inflate(R.layout.bottom_buttons, preferencesContainer, false);

			preferencesContainer.addView(buttonsContainer);
			View cancelButton = buttonsContainer.findViewById(R.id.dismiss_button);
			View saveButton = buttonsContainer.findViewById(R.id.right_bottom_button);

			saveButton.setVisibility(View.VISIBLE);
			buttonsContainer.findViewById(R.id.buttons_divider).setVisibility(View.VISIBLE);

			AndroidUtils.setBackground(getContext(), buttonsContainer, isNightMode(), R.color.list_background_color_light, R.color.list_background_color_dark);

			UiUtilities.setupDialogButton(false, cancelButton, DialogButtonType.SECONDARY, R.string.shared_string_cancel);
			UiUtilities.setupDialogButton(false, saveButton, DialogButtonType.PRIMARY, R.string.shared_string_save);

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
						if (isChanged()) {
							if (saveNewProfile()) {
								profile = changedProfile;
								getActivity().onBackPressed();
							}
						}
					}
				}
			});
		}
		return view;
	}

	private boolean isChanged() {
		return !profile.equals(changedProfile);
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
		outState.putString(PROFILE_STRINGKEY_KEY, changedProfile.stringKey);
		outState.putInt(PROFILE_ICON_RES_KEY, changedProfile.iconRes);
		outState.putSerializable(PROFILE_COLOR_KEY, changedProfile.color);
		if (changedProfile.parent != null) {
			outState.putString(PROFILE_PARENT_KEY, changedProfile.parent.getStringKey());
		}
	}

	private void restoreState(Bundle savedInstanceState) {
		changedProfile.name = savedInstanceState.getString(PROFILE_NAME_KEY);
		changedProfile.stringKey = savedInstanceState.getString(PROFILE_STRINGKEY_KEY);
		changedProfile.iconRes = savedInstanceState.getInt(PROFILE_ICON_RES_KEY);
		changedProfile.color = (ApplicationMode.ProfileIconColors) savedInstanceState.getSerializable(PROFILE_COLOR_KEY);
		String parentStringKey = savedInstanceState.getString(PROFILE_PARENT_KEY);
		changedProfile.parent = ApplicationMode.valueOfStringKey(parentStringKey, null);
	}

	@Override
	protected void updateProfileButton() {
		View view = getView();
		if (view == null) {
			return;
		}
		View profileButton = view.findViewById(R.id.profile_button);
		if (profileButton != null) {
			int iconColor = ContextCompat.getColor(app, changedProfile.color.getColor(isNightMode()));
			AndroidUtils.setBackground(profileButton, UiUtilities.tintDrawable(ContextCompat.getDrawable(app,
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
			profileName.setImeOptions(EditorInfo.IME_ACTION_DONE);
			profileName.setText(changedProfile.name);
			profileName.requestFocus();
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
			baseProfileName = (EditText) holder.findViewById(R.id.master_profile_et);
			baseProfileName.setText(changedProfile.parent != null
					? changedProfile.parent.toHumanString(getContext())
					: getSelectedAppMode().toHumanString(getContext()));
			OsmandTextFieldBoxes baseProfileNameHint = (OsmandTextFieldBoxes) holder.findViewById(R.id.master_profile_otfb);
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
				View colorItem = createColorItemView(color, colorItems);
				colorItems.addView(colorItem, new FlowLayout.LayoutParams(0, 0));
				ImageView outlineCircle = colorItem.findViewById(R.id.outlineCircle);
				ImageView checkMark = colorItem.findViewById(R.id.checkMark);
				GradientDrawable gradientDrawable = (GradientDrawable) ContextCompat.getDrawable(app, R.drawable.circle_contour_bg_light);
				if (gradientDrawable != null) {
					gradientDrawable.setStroke(AndroidUtils.dpToPx(app, 2),
							UiUtilities.getColorWithAlpha(ContextCompat.getColor(app, color.getColor(isNightMode())), 0.3f));
					outlineCircle.setImageDrawable(gradientDrawable);
				}
				checkMark.setVisibility(View.GONE);
				outlineCircle.setVisibility(View.GONE);
			}
			updateColorSelector(changedProfile.color);
		} else if (ICON_ITEMS.equals(preference.getKey())) {
			iconItems = (FlowLayout) holder.findViewById(R.id.color_items);
			iconItems.removeAllViews();
			ArrayList<Integer> icons = ApplicationMode.ProfileIcons.getIcons();
			for (int iconRes : icons) {
				View iconItem = createIconItemView(iconRes, iconItems);
				iconItems.addView(iconItem, new FlowLayout.LayoutParams(0, 0));
				ImageView outlineCircle = iconItem.findViewById(R.id.outlineCircle);
				outlineCircle.setVisibility(View.GONE);
			}
			setIconNewColor(changedProfile.iconRes);
		}
	}

	private View createColorItemView(final ApplicationMode.ProfileIconColors colorRes, ViewGroup rootView) {
		FrameLayout colorItemView = (FrameLayout) UiUtilities.getInflater(getContext(), isNightMode())
				.inflate(R.layout.preference_circle_item, rootView, false);
		ImageView coloredCircle = colorItemView.findViewById(R.id.bckgroundCircle);
		coloredCircle.setImageDrawable(getPaintedIcon(R.drawable.circle_background_light,
				ContextCompat.getColor(app, colorRes.getColor(isNightMode()))));
		coloredCircle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (colorRes != changedProfile.color) {
					updateColorSelector(colorRes);
				}
			}
		});
		colorItemView.setTag(colorRes);
		return colorItemView;
	}

	private void updateColorSelector(ApplicationMode.ProfileIconColors color) {
		View colorItem = colorItems.findViewWithTag(changedProfile.color);
		colorItem.findViewById(R.id.outlineCircle).setVisibility(View.GONE);
		colorItem.findViewById(R.id.checkMark).setVisibility(View.GONE);
		colorItem = colorItems.findViewWithTag(color);
		colorItem.findViewById(R.id.outlineCircle).setVisibility(View.VISIBLE);
		colorItem.findViewById(R.id.checkMark).setVisibility(View.VISIBLE);
		changedProfile.color = color;
		if (iconItems != null) {
			setIconNewColor(changedProfile.iconRes);
		}
		updateProfileButton();
	}

	private View createIconItemView(final int iconRes, ViewGroup rootView) {
		FrameLayout iconItemView = (FrameLayout) UiUtilities.getInflater(getContext(), isNightMode())
				.inflate(R.layout.preference_circle_item, rootView, false);
		ImageView checkMark = iconItemView.findViewById(R.id.checkMark);
		checkMark.setImageDrawable(app.getUIUtilities().getIcon(iconRes, R.color.icon_color_default_light));
		ImageView coloredCircle = iconItemView.findViewById(R.id.bckgroundCircle);
		coloredCircle.setImageDrawable(getPaintedIcon(R.drawable.circle_background_light,
				UiUtilities.getColorWithAlpha(ContextCompat.getColor(app, R.color.icon_color_default_light), 0.1f)));
		coloredCircle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (iconRes != changedProfile.iconRes) {
					updateIconSelector(iconRes);
				}
			}
		});
		iconItemView.setTag(iconRes);
		return iconItemView;
	}

	private void updateIconSelector(int iconRes) {
		setIconNewColor(iconRes);
		View iconItem = iconItems.findViewWithTag(changedProfile.iconRes);
		iconItem.findViewById(R.id.outlineCircle).setVisibility(View.GONE);
		ImageView checkMark = iconItem.findViewById(R.id.checkMark);
		checkMark.setImageDrawable(app.getUIUtilities().getIcon(changedProfile.iconRes, R.color.icon_color_default_light));
		ImageView coloredCircle = iconItem.findViewById(R.id.bckgroundCircle);
		coloredCircle.setImageDrawable(getPaintedIcon(R.drawable.circle_background_light,
				UiUtilities.getColorWithAlpha(ContextCompat.getColor(app, R.color.icon_color_default_light), 0.1f)));
		changedProfile.iconRes = iconRes;
		updateProfileButton();
	}

	private void setIconNewColor(int iconRes) {
		int changedProfileColor = ContextCompat.getColor(app, changedProfile.color.getColor(
				app.getDaynightHelper().isNightModeForMapControls()));
		View iconItem = iconItems.findViewWithTag(iconRes);
		if (iconItem != null) {
			ImageView coloredCircle = iconItem.findViewById(R.id.bckgroundCircle);
			coloredCircle.setImageDrawable(getPaintedIcon(R.drawable.circle_background_light,
					UiUtilities.getColorWithAlpha(ContextCompat.getColor(app, changedProfile.color.getColor(isNightMode())), 0.1f)));
			ImageView outlineCircle = iconItem.findViewById(R.id.outlineCircle);
			GradientDrawable circleContourDrawable = (GradientDrawable) ContextCompat.getDrawable(app, R.drawable.circle_contour_bg_light);
			if (circleContourDrawable != null) {
				circleContourDrawable.setStroke(AndroidUtils.dpToPx(app, 2), changedProfileColor);
			}
			outlineCircle.setImageDrawable(circleContourDrawable);
			outlineCircle.setVisibility(View.VISIBLE);
			ImageView checkMark = iconItem.findViewById(R.id.checkMark);
			checkMark.setImageDrawable(app.getUIUtilities().getPaintedIcon(iconRes, changedProfileColor));
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
		changedProfile.parent = ApplicationMode.valueOfStringKey(key, ApplicationMode.DEFAULT);
	}

	private void setupBaseProfileView(String stringKey) {
		for (ApplicationMode am : ApplicationMode.getDefaultValues()) {
			if (am.getStringKey().equals(stringKey)) {
				baseProfileName.setText(Algorithms.capitalizeFirstLetter(am.toHumanString(app)));
			}
		}
	}

	private boolean saveNewProfile() {
		if (changedProfile.name.replace(" ", "").length() < 1) {
			if (getActivity() != null) {
				createWarningDialog(getActivity(),
						R.string.profile_alert_need_profile_name_title, R.string.profile_alert_need_profile_name_msg, R.string.shared_string_dismiss).show();
			}
			return false;
		}

		if (hasNameDuplicate()) {
			if (getActivity() != null) {
				AlertDialog.Builder duplicateNameWarning = createWarningDialog(getActivity(),
						R.string.profile_alert_duplicate_name_title, R.string.profile_alert_duplicate_name_msg, R.string.shared_string_dismiss);
				duplicateNameWarning.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						profileName.requestFocus();
					}
				});
				duplicateNameWarning.show();
			}
			return false;
		}

		ApplicationMode.ApplicationModeBuilder builder = ApplicationMode
				.createCustomMode(changedProfile.parent, changedProfile.name.trim(), changedProfile.stringKey)
				.icon(app, ApplicationMode.ProfileIcons.getResStringByResId(changedProfile.iconRes))
				.setRouteService(changedProfile.routeService)
				.setRoutingProfile(changedProfile.routingProfile)
				.setColor(changedProfile.color);
		ApplicationMode mode = ApplicationMode.saveProfile(builder, getMyApplication());
		if (!ApplicationMode.values(app).contains(mode)) {
			ApplicationMode.changeProfileAvailability(mode, true, getMyApplication());
		}
		return true;
	}

	private boolean hasNameDuplicate() {
		for (ApplicationMode m : ApplicationMode.allPossibleValues()) {
			if (m.toHumanString(app).equals(changedProfile.name) &&
					!m.getStringKey().equals(profile.stringKey)) {
				return true;
			}
		}
		return false;
	}

	public boolean isProfileAppearanceChanged(final MapActivity mapActivity) {
		if (isChanged()) {
			AlertDialog.Builder dismissDialog = createWarningDialog(getActivity(),
					R.string.shared_string_dismiss, R.string.exit_without_saving, R.string.shared_string_cancel);
			dismissDialog.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					changedProfile = profile;
					mapActivity.onBackPressed();
				}
			});
			dismissDialog.show();
			return true;
		} else {
			return false;
		}
	}

	private AlertDialog.Builder createWarningDialog(Activity activity, int title, int message, int negButton) {
		Context themedContext = UiUtilities.getThemedContext(activity, isNightMode());
		AlertDialog.Builder warningDialog = new AlertDialog.Builder(themedContext);
		warningDialog.setTitle(getString(title));
		warningDialog.setMessage(getString(message));
		warningDialog.setNegativeButton(negButton, null);
		return warningDialog;
	}

	public static boolean showInstance(FragmentActivity activity, SettingsScreenType screenType, @Nullable String appMode) {
		try {
			Fragment fragment = Fragment.instantiate(activity, screenType.fragmentName);
			Bundle args = new Bundle();
			if (appMode != null) {
				args.putString(BASE_PROFILE_FOR_NEW, appMode);
			}
			fragment.setArguments(args);
			activity.getSupportFragmentManager().beginTransaction()
					.replace(R.id.fragmentContainer, fragment, screenType.fragmentName)
					.addToBackStack(DRAWER_SETTINGS_ID + ".new")
					.commit();
			return true;
		} catch (Exception e) {
			LOG.error(e);
		}
		return false;
	}

	class ApplicationProfileObject {
		String stringKey;
		ApplicationMode parent = null;
		String name;
		ApplicationMode.ProfileIconColors color;
		int iconRes;
		String routingProfile;
		RouteProvider.RouteService routeService;

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			ApplicationProfileObject that = (ApplicationProfileObject) o;

			if (iconRes != that.iconRes) return false;
			if (stringKey != null ? !stringKey.equals(that.stringKey) : that.stringKey != null)
				return false;
			if (parent != null ? !parent.equals(that.parent) : that.parent != null) return false;
			if (name != null ? !name.equals(that.name) : that.name != null) return false;
			if (color != that.color) return false;
			if (routingProfile != null ? !routingProfile.equals(that.routingProfile) : that.routingProfile != null)
				return false;
			return routeService == that.routeService;
		}

		@Override
		public int hashCode() {
			int result = stringKey != null ? stringKey.hashCode() : 0;
			result = 31 * result + (parent != null ? parent.hashCode() : 0);
			result = 31 * result + (name != null ? name.hashCode() : 0);
			result = 31 * result + (color != null ? color.hashCode() : 0);
			result = 31 * result + iconRes;
			result = 31 * result + (routingProfile != null ? routingProfile.hashCode() : 0);
			result = 31 * result + (routeService != null ? routeService.hashCode() : 0);
			return result;
		}
	}
}

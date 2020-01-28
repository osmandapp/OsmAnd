package net.osmand.plus.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
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
import net.osmand.plus.profiles.LocationIcon;
import net.osmand.plus.profiles.NavigationIcon;
import net.osmand.plus.profiles.ProfileIconColors;
import net.osmand.plus.profiles.ProfileIcons;
import net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment;
import net.osmand.plus.routing.RouteProvider;
import net.osmand.plus.widgets.FlowLayout;
import net.osmand.plus.widgets.OsmandTextFieldBoxes;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collections;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SETTINGS_ID;
import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.DIALOG_TYPE;
import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.IS_PROFILE_IMPORTED_ARG;
import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.PROFILE_KEY_ARG;
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
	private static final String SELECT_LOCATION_ICON = "select_location_icon";
	private static final String LOCATION_ICON_ITEMS = "location_icon_items";
	private static final String SELECT_NAV_ICON = "select_nav_icon";
	private static final String NAV_ICON_ITEMS = "nav_icon_items";

	public static final String PROFILE_NAME_KEY = "profile_name_key";
	public static final String PROFILE_STRINGKEY_KEY = "profile_stringkey_key";
	public static final String PROFILE_ICON_RES_KEY = "profile_icon_res_key";
	public static final String PROFILE_COLOR_KEY = "profile_color_key";
	public static final String PROFILE_PARENT_KEY = "profile_parent_key";
	public static final String PROFILE_LOCATION_ICON_KEY = "profile_location_icon_key";
	public static final String PROFILE_NAVIGATION_ICON_KEY = "profile_navigation_icon_key";
	public static final String BASE_PROFILE_FOR_NEW = "base_profile_for_new";
	public static final String IS_BASE_PROFILE_IMPORTED = "is_base_profile_imported";
	private SelectProfileBottomSheetDialogFragment.SelectProfileListener parentProfileListener;
	private EditText baseProfileName;
	private ApplicationProfileObject profile;
	private ApplicationProfileObject changedProfile;
	private EditText profileName;
	private FlowLayout colorItems;
	private FlowLayout iconItems;
	private FlowLayout locationIconItems;
	private FlowLayout navIconItems;
	private OsmandTextFieldBoxes profileNameOtfb;
	private View saveButton;

	private boolean isBaseProfileImported;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		profile = new ApplicationProfileObject();
		ApplicationMode baseModeForNewProfile = null;
		if (getArguments() != null) {
			Bundle arguments = getArguments();
			String keyBaseProfileForNew = arguments.getString(BASE_PROFILE_FOR_NEW, null);
			baseModeForNewProfile = ApplicationMode.valueOfStringKey(keyBaseProfileForNew, null);
			isBaseProfileImported = arguments.getBoolean(IS_BASE_PROFILE_IMPORTED);
		}
		if (baseModeForNewProfile != null) {
			profile.stringKey = baseModeForNewProfile.getStringKey() + "_" + System.currentTimeMillis();
			profile.parent = baseModeForNewProfile;
			profile.name = baseModeForNewProfile.toHumanString(app);
			profile.color = baseModeForNewProfile.getIconColorInfo();
			profile.iconRes = baseModeForNewProfile.getIconRes();
			profile.routingProfile = baseModeForNewProfile.getRoutingProfile();
			profile.routeService = baseModeForNewProfile.getRouteService();
			profile.locationIcon = baseModeForNewProfile.getLocationIcon();
			profile.navigationIcon = baseModeForNewProfile.getNavigationIcon();
			onAppModeChanged(ApplicationMode.valueOfStringKey(baseModeForNewProfile.getStringKey(), null));
		} else {
			profile.stringKey = getSelectedAppMode().getStringKey();
			profile.parent = getSelectedAppMode().getParent();
			profile.name = getSelectedAppMode().toHumanString(getContext());
			profile.color = getSelectedAppMode().getIconColorInfo();
			profile.iconRes = getSelectedAppMode().getIconRes();
			profile.routingProfile = getSelectedAppMode().getRoutingProfile();
			profile.routeService = getSelectedAppMode().getRouteService();
			profile.locationIcon = getSelectedAppMode().getLocationIcon();
			profile.navigationIcon = getSelectedAppMode().getNavigationIcon();
		}
		changedProfile = new ApplicationProfileObject();
		if (savedInstanceState != null) {
			restoreState(savedInstanceState);
		} else {
			changedProfile.stringKey = profile.stringKey;
			changedProfile.parent = profile.parent;
			if (baseModeForNewProfile != null) {
				changedProfile.name = createNonDuplicateName(baseModeForNewProfile.toHumanString(app));
			} else {
				changedProfile.name = profile.name;
			}
			changedProfile.color = profile.color;
			changedProfile.iconRes = profile.iconRes;
			changedProfile.routingProfile = profile.routingProfile;
			changedProfile.routeService = profile.routeService;
			changedProfile.locationIcon = profile.locationIcon;
			changedProfile.navigationIcon = profile.navigationIcon;
		}
	}

	private String createNonDuplicateName(String oldName) {
		int suffix = 0;
		int i = oldName.length() - 1;
		do {
			try {
				if (oldName.charAt(i) == ' ' || oldName.charAt(i) == '-') {
					throw new NumberFormatException();
				}
				suffix = Integer.parseInt(oldName.substring(i));
			} catch (NumberFormatException e) {
				break;
			}
			i--;
		} while (i >= 0);
		String newName;
		String divider = suffix == 0 ? " " : "";
		do {
			suffix++;
			newName = oldName.substring(0, i + 1) + divider + suffix;
		}
		while (hasProfileWithName(newName));
		return newName;
	}

	private boolean hasProfileWithName(String newName) {
		for (ApplicationMode m : ApplicationMode.allPossibleValues()) {
			if (m.toHumanString(app).equals(newName)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void setupPreferences() {
		findPreference(SELECT_COLOR).setIconSpaceReserved(false);
		findPreference(SELECT_ICON).setIconSpaceReserved(false);
		findPreference(SELECT_LOCATION_ICON).setIconSpaceReserved(false);
		findPreference(SELECT_NAV_ICON).setIconSpaceReserved(false);
		if (getSelectedAppMode().equals(ApplicationMode.DEFAULT)) {
			findPreference(SELECT_ICON).setVisible(false);
			findPreference(ICON_ITEMS).setVisible(false);
		}
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
			saveButton = buttonsContainer.findViewById(R.id.right_bottom_button);

			saveButton.setVisibility(View.VISIBLE);
			buttonsContainer.findViewById(R.id.buttons_divider).setVisibility(View.VISIBLE);

			AndroidUtils.setBackground(getContext(), buttonsContainer, isNightMode(), R.color.list_background_color_light, R.color.list_background_color_dark);

			UiUtilities.setupDialogButton(isNightMode(), cancelButton, DialogButtonType.SECONDARY, R.string.shared_string_cancel);
			UiUtilities.setupDialogButton(isNightMode(), saveButton, DialogButtonType.PRIMARY, R.string.shared_string_save);

			cancelButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					goBackWithoutSaving();
				}
			});
			saveButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (getActivity() != null) {
						hideKeyboard();
						if (isChanged()) {
							boolean isNew = ApplicationMode.valueOfStringKey(changedProfile.stringKey, null) == null;
							if (saveProfile(isNew)) {
								profile = changedProfile;
								if (isNew) {
									ProfileAppearanceFragment.this.dismiss();
									BaseSettingsFragment.showInstance(getMapActivity(), SettingsScreenType.CONFIGURE_PROFILE,
											ApplicationMode.valueOfStringKey(changedProfile.stringKey, null));
								} else {
									getActivity().onBackPressed();
								}
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
		outState.putBoolean(IS_BASE_PROFILE_IMPORTED, isBaseProfileImported);
		outState.putSerializable(PROFILE_LOCATION_ICON_KEY, changedProfile.locationIcon);
		outState.putSerializable(PROFILE_NAVIGATION_ICON_KEY, changedProfile.navigationIcon);
	}

	private void restoreState(Bundle savedInstanceState) {
		changedProfile.name = savedInstanceState.getString(PROFILE_NAME_KEY);
		changedProfile.stringKey = savedInstanceState.getString(PROFILE_STRINGKEY_KEY);
		changedProfile.iconRes = savedInstanceState.getInt(PROFILE_ICON_RES_KEY);
		changedProfile.color = (ProfileIconColors) savedInstanceState.getSerializable(PROFILE_COLOR_KEY);
		String parentStringKey = savedInstanceState.getString(PROFILE_PARENT_KEY);
		changedProfile.parent = ApplicationMode.valueOfStringKey(parentStringKey, null);
		isBaseProfileImported = savedInstanceState.getBoolean(IS_BASE_PROFILE_IMPORTED);
		changedProfile.locationIcon = (LocationIcon) savedInstanceState.getSerializable(PROFILE_LOCATION_ICON_KEY);
		changedProfile.navigationIcon = (NavigationIcon) savedInstanceState.getSerializable(PROFILE_NAVIGATION_ICON_KEY);
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
	protected void onBindPreferenceViewHolder(Preference preference, PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);
		if (PROFILE_NAME.equals(preference.getKey())) {
			profileName = (EditText) holder.findViewById(R.id.profile_name_et);
			profileName.setImeOptions(EditorInfo.IME_ACTION_DONE);
			profileName.setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
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
					if (hasNameDuplicate()) {
						saveButton.setEnabled(false);
						profileNameOtfb.setError(app.getString(R.string.profile_alert_duplicate_name_msg), true);
					} else {
						saveButton.setEnabled(true);
					}
				}
			});
			if (getSelectedAppMode().equals(ApplicationMode.DEFAULT)) {
				profileName.setFocusableInTouchMode(false);
				profileName.setFocusable(false);
			} else {
				profileName.requestFocus();
			}
			profileNameOtfb = (OsmandTextFieldBoxes) holder.findViewById(R.id.profile_name_otfb);
		} else if (MASTER_PROFILE.equals(preference.getKey())) {
			baseProfileName = (EditText) holder.findViewById(R.id.master_profile_et);
			baseProfileName.setFocusable(false);
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
			for (ProfileIconColors color : ProfileIconColors.values()) {
				View colorItem = createColorItemView(color, colorItems);
				colorItems.addView(colorItem, new FlowLayout.LayoutParams(0, 0));

			}
			updateColorSelector(changedProfile.color);
		} else if (ICON_ITEMS.equals(preference.getKey())) {
			iconItems = (FlowLayout) holder.findViewById(R.id.color_items);
			iconItems.removeAllViews();
			ArrayList<Integer> icons = ProfileIcons.getIcons();
			for (int iconRes : icons) {
				View iconItem = createIconItemView(iconRes, iconItems);
				iconItems.addView(iconItem, new FlowLayout.LayoutParams(0, 0));
			}
			setIconNewColor(changedProfile.iconRes);
		} else if (LOCATION_ICON_ITEMS.equals(preference.getKey())) {
			locationIconItems = (FlowLayout) holder.findViewById(R.id.color_items);
			locationIconItems.removeAllViews();
			for (LocationIcon locationIcon : LocationIcon.values()) {
				View iconItemView = createLocationIconView(locationIcon, locationIconItems);
				locationIconItems.addView(iconItemView, new FlowLayout.LayoutParams(0, 0));
			}
			updateLocationIconSelector(changedProfile.locationIcon);
		} else if (NAV_ICON_ITEMS.equals(preference.getKey())) {
			navIconItems = (FlowLayout) holder.findViewById(R.id.color_items);
			navIconItems.removeAllViews();
			for (NavigationIcon navigationIcon : NavigationIcon.values()) {
				View iconItemView = createNavigationIconView(navigationIcon, navIconItems);
				navIconItems.addView(iconItemView, new FlowLayout.LayoutParams(0, 0));
			}
			updateNavigationIconSelector(changedProfile.navigationIcon);
		}
	}

	private View createColorItemView(final ProfileIconColors colorRes, ViewGroup rootView) {
		FrameLayout colorItemView = (FrameLayout) UiUtilities.getInflater(getContext(), isNightMode())
				.inflate(R.layout.preference_circle_item, rootView, false);
		ImageView coloredCircle = colorItemView.findViewById(R.id.backgroundCircle);
		AndroidUtils.setBackground(coloredCircle,
				UiUtilities.tintDrawable(ContextCompat.getDrawable(app, R.drawable.circle_background_light),
				ContextCompat.getColor(app, colorRes.getColor(isNightMode()))));
		coloredCircle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (colorRes != changedProfile.color) {
					updateColorSelector(colorRes);
					updatePreference(findPreference(MASTER_PROFILE));
					updatePreference(findPreference(LOCATION_ICON_ITEMS));
					updatePreference(findPreference(NAV_ICON_ITEMS));
				}
			}
		});

		ImageView outlineCircle = colorItemView.findViewById(R.id.outlineCircle);
		ImageView checkMark = colorItemView.findViewById(R.id.checkMark);
		GradientDrawable gradientDrawable = (GradientDrawable) ContextCompat.getDrawable(app, R.drawable.circle_contour_bg_light);
		if (gradientDrawable != null) {
			gradientDrawable.setStroke(AndroidUtils.dpToPx(app, 2),
					UiUtilities.getColorWithAlpha(ContextCompat.getColor(app, colorRes.getColor(isNightMode())), 0.3f));
			outlineCircle.setImageDrawable(gradientDrawable);
		}
		checkMark.setVisibility(View.GONE);
		outlineCircle.setVisibility(View.GONE);
		colorItemView.setTag(colorRes);
		return colorItemView;
	}

	private void updateColorSelector(ProfileIconColors color) {
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
		updateProfileNameAppearance();
		updateProfileButton();
	}

	private void updateProfileNameAppearance() {
		if (profileName != null) {
			if (profileName.isFocusable() && profileName.isFocusableInTouchMode()) {
				int selectedColor = ContextCompat.getColor(app, changedProfile.color.getColor(isNightMode()));
				profileNameOtfb.setPrimaryColor(selectedColor);
				profileName.getBackground().mutate().setColorFilter(selectedColor, PorterDuff.Mode.SRC_ATOP);
			}
		}
	}

	private View createIconItemView(final int iconRes, ViewGroup rootView) {
		FrameLayout iconItemView = (FrameLayout) UiUtilities.getInflater(getContext(), isNightMode())
				.inflate(R.layout.preference_circle_item, rootView, false);
		ImageView checkMark = iconItemView.findViewById(R.id.checkMark);
		checkMark.setImageDrawable(app.getUIUtilities().getIcon(iconRes, R.color.icon_color_default_light));
		ImageView coloredCircle = iconItemView.findViewById(R.id.backgroundCircle);
		AndroidUtils.setBackground(coloredCircle,
				UiUtilities.tintDrawable(ContextCompat.getDrawable(app, R.drawable.circle_background_light),
				UiUtilities.getColorWithAlpha(ContextCompat.getColor(app, R.color.icon_color_default_light), 0.1f)));
		coloredCircle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (iconRes != changedProfile.iconRes) {
					updateIconSelector(iconRes);
				}
			}
		});
		iconItemView.findViewById(R.id.outlineCircle).setVisibility(View.GONE);
		iconItemView.setTag(iconRes);
		return iconItemView;
	}

	private void updateIconSelector(int iconRes) {
		setIconNewColor(iconRes);
		View iconItem = iconItems.findViewWithTag(changedProfile.iconRes);
		iconItem.findViewById(R.id.outlineCircle).setVisibility(View.GONE);
		ImageView checkMark = iconItem.findViewById(R.id.checkMark);
		checkMark.setImageDrawable(app.getUIUtilities().getIcon(changedProfile.iconRes, R.color.icon_color_default_light));
		AndroidUtils.setBackground(iconItem.findViewById(R.id.backgroundCircle),
				UiUtilities.tintDrawable(ContextCompat.getDrawable(app, R.drawable.circle_background_light),
						UiUtilities.getColorWithAlpha(ContextCompat.getColor(app, R.color.icon_color_default_light), 0.1f)));
		changedProfile.iconRes = iconRes;
		updateProfileButton();
	}

	private View createLocationIconView(final LocationIcon locationIcon, ViewGroup rootView) {
		FrameLayout locationIconView = (FrameLayout) UiUtilities.getInflater(getContext(), isNightMode())
				.inflate(R.layout.preference_select_icon_button, rootView, false);
		int changedProfileColor = ContextCompat.getColor(app, changedProfile.color.getColor(
				app.getDaynightHelper().isNightModeForMapControls()));
		LayerDrawable locationIconDrawable = (LayerDrawable) app.getResources().getDrawable(locationIcon.getIconId());
		DrawableCompat.setTint(DrawableCompat.wrap(locationIconDrawable.getDrawable(1)), changedProfileColor);
		locationIconView.<ImageView>findViewById(R.id.icon).setImageDrawable(locationIconDrawable);
		ImageView headingIcon = locationIconView.findViewById(R.id.headingIcon);
		headingIcon.setImageDrawable(ContextCompat.getDrawable(app, locationIcon.getHeadingIconId()));
		headingIcon.setColorFilter(new PorterDuffColorFilter(changedProfileColor, PorterDuff.Mode.SRC_IN));
		ImageView coloredRect = locationIconView.findViewById(R.id.backgroundRect);
		AndroidUtils.setBackground(coloredRect,
				UiUtilities.tintDrawable(ContextCompat.getDrawable(app, R.drawable.bg_select_icon_button),
						UiUtilities.getColorWithAlpha(ContextCompat.getColor(app, R.color.icon_color_default_light), 0.1f)));
		coloredRect.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (locationIcon != changedProfile.locationIcon) {
					updateLocationIconSelector(locationIcon);
				}
			}
		});
		ImageView outlineRect = locationIconView.findViewById(R.id.outlineRect);
		GradientDrawable rectContourDrawable = (GradientDrawable) ContextCompat.getDrawable(app, R.drawable.bg_select_icon_button_outline);
		if (rectContourDrawable != null) {
			rectContourDrawable.setStroke(AndroidUtils.dpToPx(app, 2), changedProfileColor);
		}
		outlineRect.setImageDrawable(rectContourDrawable);
		outlineRect.setVisibility(View.GONE);
		locationIconView.setTag(locationIcon);
		return locationIconView;
	}

	private void updateLocationIconSelector(LocationIcon locationIcon) {
		View viewWithTag = locationIconItems.findViewWithTag(changedProfile.locationIcon);
		viewWithTag.findViewById(R.id.outlineRect).setVisibility(View.GONE);
		viewWithTag = locationIconItems.findViewWithTag(locationIcon);
		viewWithTag.findViewById(R.id.outlineRect).setVisibility(View.VISIBLE);
		changedProfile.locationIcon = locationIcon;
	}

	private View createNavigationIconView(final NavigationIcon navigationIcon, ViewGroup rootView) {
		FrameLayout navigationIconView = (FrameLayout) UiUtilities.getInflater(getContext(), isNightMode())
				.inflate(R.layout.preference_select_icon_button, rootView, false);
		LayerDrawable navigationIconDrawable = (LayerDrawable) app.getResources().getDrawable(navigationIcon.getIconId());
		DrawableCompat.setTint(DrawableCompat.wrap(navigationIconDrawable.getDrawable(1)),
				ContextCompat.getColor(app, changedProfile.color.getColor(app.getDaynightHelper().isNightModeForMapControls())));
		ImageView imageView = navigationIconView.findViewById(R.id.icon);
		imageView.setImageDrawable(navigationIconDrawable);
		Matrix matrix = new Matrix();
		imageView.setScaleType(ImageView.ScaleType.MATRIX);
		matrix.postRotate((float) -90, imageView.getDrawable().getIntrinsicWidth() / 2,
				imageView.getDrawable().getIntrinsicHeight() / 2);
		imageView.setImageMatrix(matrix);
		ImageView coloredRect = navigationIconView.findViewById(R.id.backgroundRect);
		AndroidUtils.setBackground(coloredRect,
				UiUtilities.tintDrawable(ContextCompat.getDrawable(app, R.drawable.bg_select_icon_button),
						UiUtilities.getColorWithAlpha(ContextCompat.getColor(app, R.color.icon_color_default_light), 0.1f)));
		coloredRect.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (navigationIcon != changedProfile.navigationIcon) {
					updateNavigationIconSelector(navigationIcon);
				}
			}
		});
		ImageView outlineRect = navigationIconView.findViewById(R.id.outlineRect);
		GradientDrawable rectContourDrawable = (GradientDrawable) ContextCompat.getDrawable(app, R.drawable.bg_select_icon_button_outline);
		int changedProfileColor = ContextCompat.getColor(app, changedProfile.color.getColor(
				app.getDaynightHelper().isNightModeForMapControls()));
		if (rectContourDrawable != null) {
			rectContourDrawable.setStroke(AndroidUtils.dpToPx(app, 2), changedProfileColor);
		}
		outlineRect.setImageDrawable(rectContourDrawable);
		outlineRect.setVisibility(View.GONE);
		navigationIconView.setTag(navigationIcon);
		return navigationIconView;
	}

	private void updateNavigationIconSelector(NavigationIcon navigationIcon) {
		View viewWithTag = navIconItems.findViewWithTag(changedProfile.navigationIcon);
		viewWithTag.findViewById(R.id.outlineRect).setVisibility(View.GONE);
		viewWithTag = navIconItems.findViewWithTag(navigationIcon);
		viewWithTag.findViewById(R.id.outlineRect).setVisibility(View.VISIBLE);
		changedProfile.navigationIcon = navigationIcon;
	}

	private void setIconNewColor(int iconRes) {
		int changedProfileColor = ContextCompat.getColor(app, changedProfile.color.getColor(
				app.getDaynightHelper().isNightModeForMapControls()));
		View iconItem = iconItems.findViewWithTag(iconRes);
		if (iconItem != null) {
			AndroidUtils.setBackground(iconItem.findViewById(R.id.backgroundCircle),
					UiUtilities.tintDrawable(ContextCompat.getDrawable(app, R.drawable.circle_background_light),
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
			AndroidUtils.hideSoftKeyboard(activity, cf);
		}
	}

	public SelectProfileBottomSheetDialogFragment.SelectProfileListener getParentProfileListener() {
		if (parentProfileListener == null) {
			parentProfileListener = new SelectProfileBottomSheetDialogFragment.SelectProfileListener() {
				@Override
				public void onSelectedType(Bundle args) {
					String profileKey = args.getString(PROFILE_KEY_ARG);
					boolean imported = args.getBoolean(IS_PROFILE_IMPORTED_ARG);
					updateParentProfile(profileKey, imported);
				}
			};
		}
		return parentProfileListener;
	}

	void updateParentProfile(String profileKey, boolean isBaseProfileImported) {
		deleteImportedProfile();
		setupBaseProfileView(profileKey);
		changedProfile.parent = ApplicationMode.valueOfStringKey(profileKey, ApplicationMode.DEFAULT);
		this.isBaseProfileImported = isBaseProfileImported;
	}

	private void setupBaseProfileView(String stringKey) {
		for (ApplicationMode am : ApplicationMode.getDefaultValues()) {
			if (am.getStringKey().equals(stringKey)) {
				baseProfileName.setText(Algorithms.capitalizeFirstLetter(am.toHumanString(app)));
			}
		}
	}

	private boolean saveProfile(boolean isNew) {
		if (changedProfile.name.replace(" ", "").length() < 1) {
			if (getActivity() != null) {
				createWarningDialog(getActivity(),
						R.string.profile_alert_need_profile_name_title, R.string.profile_alert_need_profile_name_msg, R.string.shared_string_dismiss).show();
			}
			return false;
		}
		if (isNew) {
			ApplicationMode.ApplicationModeBuilder builder = ApplicationMode
					.createCustomMode(changedProfile.parent, changedProfile.name.trim(), changedProfile.stringKey)
					.icon(app, ProfileIcons.getResStringByResId(changedProfile.iconRes))
					.setRouteService(changedProfile.routeService)
					.setRoutingProfile(changedProfile.routingProfile)
					.setColor(changedProfile.color)
					.locationIcon(changedProfile.locationIcon)
					.navigationIcon(changedProfile.navigationIcon);

			app.getSettings().copyPreferencesFromProfile(changedProfile.parent, builder.getApplicationMode());
			ApplicationMode mode = ApplicationMode.saveProfile(builder, app);
			if (!ApplicationMode.values(app).contains(mode)) {
				ApplicationMode.changeProfileAvailability(mode, true, app);
			}
		} else {
			ApplicationMode mode = ApplicationMode.valueOfStringKey(changedProfile.stringKey, null);
			mode.setParentAppMode(app, changedProfile.parent);
			mode.setUserProfileName(app, changedProfile.name.trim());
			mode.setIconResName(app, ProfileIcons.getResStringByResId(changedProfile.iconRes));
			mode.setRouteService(app, changedProfile.routeService);
			mode.setRoutingProfile(app, changedProfile.routingProfile);
			mode.setIconColor(app, changedProfile.color);
			mode.setLocationIcon(app, changedProfile.locationIcon);
			mode.setNavigationIcon(app, changedProfile.navigationIcon);
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
		hideKeyboard();
		if (isChanged()) {
			AlertDialog.Builder dismissDialog = createWarningDialog(getActivity(),
					R.string.shared_string_dismiss, R.string.exit_without_saving, R.string.shared_string_cancel);
			dismissDialog.setPositiveButton(R.string.shared_string_exit, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					changedProfile = profile;
					goBackWithoutSaving();
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

	private void goBackWithoutSaving() {
		deleteImportedProfile();
		if (getActivity() != null) {
			getActivity().onBackPressed();
		}
	}

	private void deleteImportedProfile() {
		if (isBaseProfileImported) {
			ApplicationMode appMode = ApplicationMode.valueOfStringKey(changedProfile.parent.getStringKey(), null);
			if (appMode != null) {
				ApplicationMode.deleteCustomModes(Collections.singletonList(appMode), app);
			}
		}
	}

	public static boolean showInstance(FragmentActivity activity, SettingsScreenType screenType, @Nullable String appMode, boolean imported) {
		try {
			Fragment fragment = Fragment.instantiate(activity, screenType.fragmentName);
			Bundle args = new Bundle();
			if (appMode != null) {
				args.putString(BASE_PROFILE_FOR_NEW, appMode);
				args.putBoolean(IS_BASE_PROFILE_IMPORTED, imported);
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
		ProfileIconColors color;
		int iconRes;
		String routingProfile;
		RouteProvider.RouteService routeService;
		NavigationIcon navigationIcon;
		LocationIcon locationIcon;

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
			if (routeService != that.routeService) return false;
			if (navigationIcon != that.navigationIcon) return false;
			return locationIcon == that.locationIcon;
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
			result = 31 * result + (navigationIcon != null ? navigationIcon.hashCode() : 0);
			result = 31 * result + (locationIcon != null ? locationIcon.hashCode() : 0);
			return result;
		}
	}
}

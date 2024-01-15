package net.osmand.plus.settings.fragments;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SETTINGS_ID;
import static net.osmand.plus.profiles.SelectProfileBottomSheet.PROFILES_LIST_UPDATED_ARG;
import static net.osmand.plus.profiles.SelectProfileBottomSheet.PROFILE_KEY_ARG;
import static net.osmand.plus.settings.backend.ApplicationMode.CUSTOM_MODE_KEY_SEPARATOR;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.profiles.LocationIcon;
import net.osmand.plus.profiles.NavigationIcon;
import net.osmand.plus.profiles.ProfileIconColors;
import net.osmand.plus.profiles.ProfileIcons;
import net.osmand.plus.profiles.SelectBaseProfileBottomSheet;
import net.osmand.plus.profiles.SelectProfileBottomSheet.OnSelectProfileCallback;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.routing.RouteService;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.backup.FileSettingsHelper.SettingsExportListener;
import net.osmand.plus.settings.backend.backup.items.ProfileSettingsItem;
import net.osmand.plus.track.cards.ColorsCard;
import net.osmand.plus.track.fragments.CustomColorBottomSheet.ColorPickerListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.FlowLayout;
import net.osmand.plus.widgets.OsmandTextFieldBoxes;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProfileAppearanceFragment extends BaseSettingsFragment implements OnSelectProfileCallback,
		CardListener, ColorPickerListener {

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

	private static final String PROFILE_NAME_KEY = "profile_name_key";
	private static final String PROFILE_STRINGKEY_KEY = "profile_stringkey_key";
	private static final String PROFILE_ICON_RES_KEY = "profile_icon_res_key";
	private static final String PROFILE_COLOR_KEY = "profile_color_key";
	private static final String PROFILE_CUSTOM_COLOR_KEY = "profile_custom_color_key";
	private static final String PROFILE_PARENT_KEY = "profile_parent_key";
	private static final String PROFILE_LOCATION_ICON_KEY = "profile_location_icon_key";
	private static final String PROFILE_NAVIGATION_ICON_KEY = "profile_navigation_icon_key";
	private static final String BASE_PROFILE_FOR_NEW = "base_profile_for_new";
	private static final String IS_BASE_PROFILE_IMPORTED = "is_base_profile_imported";
	private static final String IS_NEW_PROFILE_KEY = "is_new_profile_key";

	private SettingsExportListener exportListener;

	private ProgressDialog progress;

	private EditText baseProfileName;
	private ApplicationProfileObject profile;
	private ApplicationProfileObject changedProfile;
	private EditText profileName;
	private TextView colorName;
	private ColorsCard colorsCard;
	private FlowLayout iconItems;
	private FlowLayout locationIconItems;
	private FlowLayout navIconItems;
	private OsmandTextFieldBoxes profileNameOtfb;
	private DialogButton saveButton;

	private boolean isBaseProfileImported;
	private boolean isNewProfile;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
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
			setupAppProfileObjectFromAppMode(baseModeForNewProfile);
			profile.parent = baseModeForNewProfile;
			profile.stringKey = getUniqueStringKey(baseModeForNewProfile);
		} else {
			setupAppProfileObjectFromAppMode(getSelectedAppMode());
		}
		changedProfile = new ApplicationProfileObject();
		if (savedInstanceState != null) {
			restoreState(savedInstanceState);
		} else {
			changedProfile.stringKey = profile.stringKey;
			changedProfile.parent = profile.parent;
			if (baseModeForNewProfile != null) {
				changedProfile.name = createNonDuplicateName(baseModeForNewProfile.toHumanString());
			} else {
				changedProfile.name = profile.name;
			}
			changedProfile.color = profile.color;
			changedProfile.customColor = profile.customColor;
			changedProfile.iconRes = profile.iconRes;
			changedProfile.routingProfile = profile.routingProfile;
			changedProfile.routeService = profile.routeService;
			changedProfile.locationIcon = profile.locationIcon;
			changedProfile.navigationIcon = profile.navigationIcon;
			isNewProfile = ApplicationMode.valueOfStringKey(changedProfile.stringKey, null) == null;
		}
		requireMyActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			public void handleOnBackPressed() {
				showExitDialog();
			}
		});
	}

	public void setupAppProfileObjectFromAppMode(ApplicationMode baseModeForNewProfile) {
		profile.stringKey = baseModeForNewProfile.getStringKey();
		profile.parent = baseModeForNewProfile.getParent();
		profile.name = baseModeForNewProfile.toHumanString();
		profile.color = baseModeForNewProfile.getIconColorInfo();
		profile.customColor = baseModeForNewProfile.getCustomIconColor();
		profile.iconRes = baseModeForNewProfile.getIconRes();
		profile.routingProfile = baseModeForNewProfile.getRoutingProfile();
		profile.routeService = baseModeForNewProfile.getRouteService();
		profile.locationIcon = baseModeForNewProfile.getLocationIcon();
		profile.navigationIcon = baseModeForNewProfile.getNavigationIcon();
	}

	@Override
	protected void createToolbar(@NonNull LayoutInflater inflater, @NonNull View view) {
		super.createToolbar(inflater, view);
		if (isNewProfile) {
			TextView toolbarTitle = view.findViewById(R.id.toolbar_title);
			if (toolbarTitle != null) {
				toolbarTitle.setText(getString(R.string.new_profile));
			}
			TextView toolbarSubtitle = view.findViewById(R.id.toolbar_subtitle);
			if (toolbarSubtitle != null) {
				toolbarSubtitle.setVisibility(View.GONE);
			}
		}
	}

	private String createNonDuplicateName(String oldName) {
		return Algorithms.makeUniqueName(oldName, newName -> !hasProfileWithName(newName));
	}

	private boolean hasProfileWithName(String newName) {
		for (ApplicationMode m : ApplicationMode.allPossibleValues()) {
			if (m.toHumanString().equals(newName)) {
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
		if (getSelectedAppMode().equals(ApplicationMode.DEFAULT) && !isNewProfile) {
			findPreference(SELECT_ICON).setVisible(false);
			findPreference(ICON_ITEMS).setVisible(false);
		}
	}

	@SuppressLint("InlinedApi")
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			FrameLayout preferencesContainer = view.findViewById(android.R.id.list_container);
			LayoutInflater themedInflater = UiUtilities.getInflater(getContext(), isNightMode());
			View buttonsContainer = themedInflater.inflate(R.layout.bottom_buttons, preferencesContainer, false);

			preferencesContainer.addView(buttonsContainer);
			DialogButton cancelButton = buttonsContainer.findViewById(R.id.dismiss_button);
			saveButton = buttonsContainer.findViewById(R.id.right_bottom_button);

			saveButton.setVisibility(View.VISIBLE);
			buttonsContainer.findViewById(R.id.buttons_divider).setVisibility(View.VISIBLE);

			AndroidUtils.setBackground(getContext(), buttonsContainer, ColorUtilities.getListBgColorId(isNightMode()));

			cancelButton.setButtonType(DialogButtonType.SECONDARY);
			cancelButton.setTitleId(R.string.shared_string_cancel);
			saveButton.setButtonType(DialogButtonType.PRIMARY);
			saveButton.setTitleId(R.string.shared_string_save);

			cancelButton.setOnClickListener(v -> {
				goBackWithoutSaving();
			});
			saveButton.setOnClickListener(v -> {
				if (getActivity() != null) {
					hideKeyboard();
					if (isChanged() && checkProfileName()) {
						saveProfile();
					}
				}
			});
			getListView().addOnScrollListener(new RecyclerView.OnScrollListener() {
				@Override
				public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
					super.onScrollStateChanged(recyclerView, newState);
					if (newState != RecyclerView.SCROLL_STATE_IDLE) {
						hideKeyboard();
						if (profileName != null) {
							profileName.clearFocus();
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
	public void onSaveInstanceState(Bundle outState) {
		saveState(outState);
		super.onSaveInstanceState(outState);
	}

	private void saveState(Bundle outState) {
		outState.putString(PROFILE_NAME_KEY, changedProfile.name);
		outState.putString(PROFILE_STRINGKEY_KEY, changedProfile.stringKey);
		outState.putInt(PROFILE_ICON_RES_KEY, changedProfile.iconRes);
		outState.putSerializable(PROFILE_COLOR_KEY, changedProfile.color);
		outState.putSerializable(PROFILE_CUSTOM_COLOR_KEY, changedProfile.customColor);
		if (changedProfile.parent != null) {
			outState.putString(PROFILE_PARENT_KEY, changedProfile.parent.getStringKey());
		}
		outState.putBoolean(IS_NEW_PROFILE_KEY, isNewProfile);
		outState.putBoolean(IS_BASE_PROFILE_IMPORTED, isBaseProfileImported);
		outState.putSerializable(PROFILE_LOCATION_ICON_KEY, changedProfile.locationIcon);
		outState.putSerializable(PROFILE_NAVIGATION_ICON_KEY, changedProfile.navigationIcon);
	}

	private void restoreState(Bundle savedInstanceState) {
		changedProfile.name = savedInstanceState.getString(PROFILE_NAME_KEY);
		changedProfile.stringKey = savedInstanceState.getString(PROFILE_STRINGKEY_KEY);
		changedProfile.iconRes = savedInstanceState.getInt(PROFILE_ICON_RES_KEY);
		changedProfile.color = AndroidUtils.getSerializable(savedInstanceState, PROFILE_COLOR_KEY, ProfileIconColors.class);
		changedProfile.customColor = AndroidUtils.getSerializable(savedInstanceState, PROFILE_CUSTOM_COLOR_KEY, Integer.class);
		String parentStringKey = savedInstanceState.getString(PROFILE_PARENT_KEY);
		changedProfile.parent = ApplicationMode.valueOfStringKey(parentStringKey, null);
		isBaseProfileImported = savedInstanceState.getBoolean(IS_BASE_PROFILE_IMPORTED);
		changedProfile.locationIcon = AndroidUtils.getSerializable(savedInstanceState, PROFILE_LOCATION_ICON_KEY, LocationIcon.class);
		changedProfile.navigationIcon = AndroidUtils.getSerializable(savedInstanceState, PROFILE_NAVIGATION_ICON_KEY, NavigationIcon.class);
		isNewProfile = savedInstanceState.getBoolean(IS_NEW_PROFILE_KEY);
	}

	@Override
	protected void updateProfileButton() {
		View view = getView();
		if (view == null) {
			return;
		}
		View profileButton = view.findViewById(R.id.profile_button);
		if (profileButton != null) {
			int iconColor = changedProfile.getActualColor();
			AndroidUtils.setBackground(profileButton, UiUtilities.tintDrawable(AppCompatResources.getDrawable(app,
					R.drawable.circle_background_light), ColorUtilities.getColorWithAlpha(iconColor, 0.1f)));
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
			profileName.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					changedProfile.name = s.toString();
					if (nameIsEmpty()) {
						disableSaveButtonWithErrorMessage(app.getString(R.string.please_provide_profile_name_message));
					} else if (hasNameDuplicate()) {
						disableSaveButtonWithErrorMessage(app.getString(R.string.profile_alert_duplicate_name_msg));
					} else {
						saveButton.setEnabled(true);
					}
				}
			});
			profileName.setOnFocusChangeListener((v, hasFocus) -> {
				if (hasFocus) {
					profileName.setSelection(profileName.getText().length());
					AndroidUtils.showSoftKeyboard(getMyActivity(), profileName);
				}
			});
			if (getSelectedAppMode().equals(ApplicationMode.DEFAULT) && !isNewProfile) {
				profileName.setFocusableInTouchMode(false);
				profileName.setFocusable(false);
			}
			profileNameOtfb = (OsmandTextFieldBoxes) holder.findViewById(R.id.profile_name_otfb);
			updateProfileNameAppearance();
		} else if (MASTER_PROFILE.equals(preference.getKey())) {
			baseProfileName = (EditText) holder.findViewById(R.id.master_profile_et);
			baseProfileName.setFocusable(false);
			baseProfileName.setText(changedProfile.parent != null
					? changedProfile.parent.toHumanString()
					: getSelectedAppMode().toHumanString());
			OsmandTextFieldBoxes baseProfileNameHint = (OsmandTextFieldBoxes) holder.findViewById(R.id.master_profile_otfb);
			baseProfileNameHint.setLabelText(getString(R.string.profile_type_osmand_string));
			FrameLayout selectNavTypeBtn = (FrameLayout) holder.findViewById(R.id.select_nav_type_btn);
			selectNavTypeBtn.setOnClickListener(v -> {
				if (isNewProfile) {
					hideKeyboard();
					if (getActivity() != null) {
						String selected = changedProfile.parent != null ?
								changedProfile.parent.getStringKey() : null;
						SelectBaseProfileBottomSheet.showInstance(
								getActivity(), this,
								getSelectedAppMode(), selected, false);
					}
				}
			});
		} else if (SELECT_COLOR.equals(preference.getKey())) {
			colorName = holder.itemView.findViewById(R.id.summary);
			colorName.setTextColor(ContextCompat.getColor(app, R.color.preference_category_title));
		} else if (COLOR_ITEMS.equals(preference.getKey())) {
			createColorsCard(holder);
		} else if (ICON_ITEMS.equals(preference.getKey())) {
			iconItems = (FlowLayout) holder.findViewById(R.id.color_items);
			iconItems.removeAllViews();
			ArrayList<Integer> icons = ProfileIcons.getIcons();
			for (int iconRes : icons) {
				View iconItem = createIconItemView(iconRes, iconItems);
				int minimalPaddingBetweenIcon = app.getResources().getDimensionPixelSize(R.dimen.favorites_select_icon_button_right_padding);
				iconItems.addView(iconItem, new FlowLayout.LayoutParams(minimalPaddingBetweenIcon, 0));
				iconItems.setHorizontalAutoSpacing(true);
			}
			setIconColor(changedProfile.iconRes);
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

	@Override
	public void onResume() {
		super.onResume();
		checkSavingProfile();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (isNewProfile) {
			File file = FileUtils.getBackupFileForCustomAppMode(app, changedProfile.stringKey);
			boolean fileExporting = app.getFileSettingsHelper().isFileExporting(file);
			if (fileExporting) {
				app.getFileSettingsHelper().updateExportListener(file, null);
			}
		}
	}

	private void createColorsCard(PreferenceViewHolder holder) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		FlowLayout colorsCardContainer = (FlowLayout) holder.findViewById(R.id.color_items);
		colorsCardContainer.removeAllViews();

		int selectedColor = changedProfile.getActualColor();
		List<Integer> colors = new ArrayList<>();
		for (ProfileIconColors color : ProfileIconColors.values()) {
			colors.add(ContextCompat.getColor(app, color.getColor(isNightMode())));
		}
		colorsCard = new ColorsCard(mapActivity, getSelectedAppMode(), this, selectedColor,
				colors, app.getSettings().CUSTOM_ICON_COLORS, false);
		colorsCard.setListener(this);
		colorsCardContainer.addView(colorsCard.build(app));
		updateColorName();
	}

	private void updateProfileNameAppearance() {
		if (profileName != null) {
			if (profileName.isFocusable() && profileName.isFocusableInTouchMode()) {
				int selectedColor = changedProfile.getActualColor();
				profileNameOtfb.setPrimaryColor(selectedColor);
				profileName.getBackground().mutate().setColorFilter(selectedColor, PorterDuff.Mode.SRC_ATOP);
			}
		}
	}

	private View createIconItemView(int iconRes, ViewGroup rootView) {
		FrameLayout iconItemView = (FrameLayout) UiUtilities.getInflater(getContext(), isNightMode())
				.inflate(R.layout.preference_circle_item, rootView, false);
		ImageView checkMark = iconItemView.findViewById(R.id.checkMark);
		checkMark.setImageDrawable(app.getUIUtilities().getIcon(iconRes, R.color.icon_color_default_light));
		ImageView coloredCircle = iconItemView.findViewById(R.id.background);
		AndroidUtils.setBackground(coloredCircle,
				UiUtilities.tintDrawable(AppCompatResources.getDrawable(app, R.drawable.circle_background_light),
						ColorUtilities.getColorWithAlpha(ContextCompat.getColor(app, R.color.icon_color_default_light), 0.1f)));
		coloredCircle.setOnClickListener(v -> {
			if (iconRes != changedProfile.iconRes) {
				updateIconSelector(iconRes);
			}
		});
		iconItemView.findViewById(R.id.outline).setVisibility(View.GONE);
		iconItemView.setTag(iconRes);
		return iconItemView;
	}

	private void updateIconSelector(int iconRes) {
		updateIconColor(iconRes);
		View iconItem = iconItems.findViewWithTag(changedProfile.iconRes);
		iconItem.findViewById(R.id.outline).setVisibility(View.GONE);
		ImageView checkMark = iconItem.findViewById(R.id.checkMark);
		checkMark.setImageDrawable(app.getUIUtilities().getIcon(changedProfile.iconRes, R.color.icon_color_default_light));
		AndroidUtils.setBackground(iconItem.findViewById(R.id.background),
				UiUtilities.tintDrawable(AppCompatResources.getDrawable(app, R.drawable.circle_background_light),
						ColorUtilities.getColorWithAlpha(ContextCompat.getColor(app, R.color.icon_color_default_light), 0.1f)));
		changedProfile.iconRes = iconRes;
		updateProfileButton();
	}

	private View createLocationIconView(LocationIcon locationIcon, ViewGroup rootView) {
		FrameLayout locationIconView = (FrameLayout) UiUtilities.getInflater(getContext(), isNightMode())
				.inflate(R.layout.preference_select_icon_button, rootView, false);
		int changedProfileColor = changedProfile.getActualColor();
		LayerDrawable locationIconDrawable = (LayerDrawable) AppCompatResources.getDrawable(app, locationIcon.getIconId());
		if (locationIconDrawable != null) {
			DrawableCompat.setTint(DrawableCompat.wrap(locationIconDrawable.getDrawable(1)), changedProfileColor);
		}
		locationIconView.<ImageView>findViewById(R.id.icon).setImageDrawable(locationIconDrawable);
		ImageView headingIcon = locationIconView.findViewById(R.id.headingIcon);
		headingIcon.setImageDrawable(AppCompatResources.getDrawable(app, locationIcon.getHeadingIconId()));
		headingIcon.setColorFilter(new PorterDuffColorFilter(changedProfileColor, PorterDuff.Mode.SRC_IN));
		ImageView coloredRect = locationIconView.findViewById(R.id.backgroundRect);
		AndroidUtils.setBackground(coloredRect,
				UiUtilities.tintDrawable(AppCompatResources.getDrawable(app, R.drawable.bg_select_icon_button),
						ColorUtilities.getColorWithAlpha(ContextCompat.getColor(app, R.color.icon_color_default_light), 0.1f)));
		coloredRect.setOnClickListener(v -> {
			if (locationIcon != changedProfile.locationIcon) {
				setVerticalScrollBarEnabled(false);
				updateLocationIconSelector(locationIcon);
				setVerticalScrollBarEnabled(true);
			}
		});
		ImageView outlineRect = locationIconView.findViewById(R.id.outlineRect);
		GradientDrawable rectContourDrawable = (GradientDrawable) AppCompatResources.getDrawable(app, R.drawable.bg_select_icon_button_outline);
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

	private View createNavigationIconView(NavigationIcon navigationIcon, ViewGroup rootView) {
		FrameLayout navigationIconView = (FrameLayout) UiUtilities.getInflater(getContext(), isNightMode())
				.inflate(R.layout.preference_select_icon_button, rootView, false);
		LayerDrawable navigationIconDrawable = (LayerDrawable) AppCompatResources.getDrawable(app, navigationIcon.getIconId());
		if (navigationIconDrawable != null) {
			DrawableCompat.setTint(DrawableCompat.wrap(navigationIconDrawable.getDrawable(1)),
					changedProfile.getActualColor());
		}
		ImageView imageView = navigationIconView.findViewById(R.id.icon);
		imageView.setImageDrawable(navigationIconDrawable);
		Matrix matrix = new Matrix();
		imageView.setScaleType(ImageView.ScaleType.MATRIX);
		matrix.postRotate((float) -90, imageView.getDrawable().getIntrinsicWidth() / 2,
				imageView.getDrawable().getIntrinsicHeight() / 2);
		imageView.setImageMatrix(matrix);
		ImageView coloredRect = navigationIconView.findViewById(R.id.backgroundRect);
		AndroidUtils.setBackground(coloredRect,
				UiUtilities.tintDrawable(AppCompatResources.getDrawable(app, R.drawable.bg_select_icon_button),
						ColorUtilities.getColorWithAlpha(ContextCompat.getColor(app, R.color.icon_color_default_light), 0.1f)));
		coloredRect.setOnClickListener(v -> {
			if (navigationIcon != changedProfile.navigationIcon) {
				setVerticalScrollBarEnabled(false);
				updateNavigationIconSelector(navigationIcon);
				setVerticalScrollBarEnabled(true);
			}
		});
		ImageView outlineRect = navigationIconView.findViewById(R.id.outlineRect);
		GradientDrawable rectContourDrawable = (GradientDrawable) AppCompatResources.getDrawable(app, R.drawable.bg_select_icon_button_outline);
		int changedProfileColor = changedProfile.getActualColor();
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

	private void updateIconColor(int iconRes) {
		setVerticalScrollBarEnabled(false);
		setIconColor(iconRes);
		setVerticalScrollBarEnabled(true);
	}

	private void setIconColor(int iconRes) {
		int changedProfileColor = changedProfile.getActualColor();
		View iconItem = iconItems.findViewWithTag(iconRes);
		if (iconItem != null) {
			int newColor = changedProfile.getActualColor();
			AndroidUtils.setBackground(iconItem.findViewById(R.id.background),
					UiUtilities.tintDrawable(AppCompatResources.getDrawable(app, R.drawable.circle_background_light),
							ColorUtilities.getColorWithAlpha(newColor, 0.1f)));
			ImageView outlineCircle = iconItem.findViewById(R.id.outline);
			GradientDrawable circleContourDrawable = (GradientDrawable) AppCompatResources.getDrawable(app, R.drawable.circle_contour_bg_light);
			if (circleContourDrawable != null) {
				circleContourDrawable.setStroke(AndroidUtils.dpToPx(app, 2), changedProfileColor);
			}
			outlineCircle.setImageDrawable(circleContourDrawable);
			outlineCircle.setVisibility(View.VISIBLE);
			ImageView checkMark = iconItem.findViewById(R.id.checkMark);
			checkMark.setImageDrawable(app.getUIUtilities().getPaintedIcon(iconRes, changedProfileColor));
		}
	}

	private void setVerticalScrollBarEnabled(boolean enabled) {
		RecyclerView preferenceListView = getListView();
		if (enabled) {
			preferenceListView.post(() -> preferenceListView.setVerticalScrollBarEnabled(true));
		} else {
			preferenceListView.setVerticalScrollBarEnabled(false);
		}
	}

	private void hideKeyboard() {
		Activity activity = getActivity();
		if (activity != null) {
			View cf = activity.getCurrentFocus();
			AndroidUtils.hideSoftKeyboard(activity, cf);
		}
	}

	private SettingsExportListener getSettingsExportListener() {
		if (exportListener == null) {
			exportListener = new SettingsExportListener() {

				@Override
				public void onSettingsExportFinished(@NonNull File file, boolean succeed) {
					dismissProfileSavingDialog();
					if (succeed) {
						customProfileSaved();
					} else {
						app.showToastMessage(R.string.profile_backup_failed);
					}
				}

				@Override
				public void onSettingsExportProgressUpdate(int value) {

				}
			};
		}
		return exportListener;
	}

	private void updateParentProfile(String profileKey, boolean isBaseProfileImported) {
		deleteImportedProfile();
		setupBaseProfileView(profileKey);
		changedProfile.parent = ApplicationMode.valueOfStringKey(profileKey, ApplicationMode.DEFAULT);
		changedProfile.routingProfile = changedProfile.parent.getRoutingProfile();
		changedProfile.routeService = changedProfile.parent.getRouteService();
		this.isBaseProfileImported = isBaseProfileImported;
	}

	private void setupBaseProfileView(String stringKey) {
		ApplicationMode mode = ApplicationMode.valueOfStringKey(stringKey, ApplicationMode.DEFAULT);
		baseProfileName.setText(Algorithms.capitalizeFirstLetter(mode.toHumanString()));
	}

	private boolean checkProfileName() {
		if (Algorithms.isBlank(changedProfile.name)) {
			Activity activity = getActivity();
			if (activity != null) {
				createWarningDialog(activity, R.string.profile_alert_need_profile_name_title,
						R.string.profile_alert_need_profile_name_msg, R.string.shared_string_dismiss).show();
			}
			return false;
		}
		return true;
	}

	private void saveProfile() {
		profile = changedProfile;
		if (isNewProfile) {
			DialogInterface.OnShowListener showListener = dialog -> app.runInUIThread(() -> {
				ApplicationMode mode = saveNewProfile();
				saveProfileBackup(mode);
			});
			showNewProfileSavingDialog(showListener);
		} else {
			ApplicationMode mode = getSelectedAppMode();
			mode.setParentAppMode(changedProfile.parent);
			mode.setIconResName(ProfileIcons.getResStringByResId(changedProfile.iconRes));
			mode.setUserProfileName(changedProfile.name.trim());
			mode.setRoutingProfile(changedProfile.routingProfile);
			mode.setRouteService(changedProfile.routeService);
			mode.setIconColor(changedProfile.color);
			mode.updateCustomIconColor(changedProfile.customColor);
			mode.setLocationIcon(changedProfile.locationIcon);
			mode.setNavigationIcon(changedProfile.navigationIcon);

			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		}
	}

	private ApplicationMode saveNewProfile() {
		changedProfile.stringKey = getUniqueStringKey(changedProfile.parent);

		ApplicationMode.ApplicationModeBuilder builder = ApplicationMode
				.createCustomMode(changedProfile.parent, changedProfile.stringKey, app)
				.setIconResName(ProfileIcons.getResStringByResId(changedProfile.iconRes))
				.setUserProfileName(changedProfile.name.trim())
				.setRoutingProfile(changedProfile.routingProfile)
				.setRouteService(changedProfile.routeService)
				.setIconColor(changedProfile.color)
				.setCustomIconColor(changedProfile.customColor)
				.setLocationIcon(changedProfile.locationIcon)
				.setNavigationIcon(changedProfile.navigationIcon);

		app.getSettings().copyPreferencesFromProfile(changedProfile.parent, builder.getApplicationMode());
		ApplicationMode mode = ApplicationMode.saveProfile(builder, app);
		if (!ApplicationMode.values(app).contains(mode)) {
			ApplicationMode.changeProfileAvailability(mode, true, app);
		}
		return mode;
	}

	private void saveProfileBackup(ApplicationMode mode) {
		if (app != null) {
			File tempDir = app.getAppPath(IndexConstants.BACKUP_INDEX_DIR);
			if (!tempDir.exists()) {
				tempDir.mkdirs();
			}
			app.getFileSettingsHelper().exportSettings(tempDir, mode.getStringKey(),
					getSettingsExportListener(), true, new ProfileSettingsItem(app, mode));
		}
	}

	private void showNewProfileSavingDialog(@Nullable DialogInterface.OnShowListener showListener) {
		if (progress != null) {
			progress.dismiss();
		}
		progress = new ProgressDialog(getContext());
		progress.setMessage(getString(R.string.saving_new_profile));
		progress.setCancelable(false);
		progress.setOnShowListener(showListener);
		progress.show();
	}

	private void checkSavingProfile() {
		if (isNewProfile) {
			File file = FileUtils.getBackupFileForCustomAppMode(app, changedProfile.stringKey);
			boolean fileExporting = app.getFileSettingsHelper().isFileExporting(file);
			if (fileExporting) {
				showNewProfileSavingDialog(null);
				app.getFileSettingsHelper().updateExportListener(file, getSettingsExportListener());
			} else if (file.exists()) {
				dismissProfileSavingDialog();
				customProfileSaved();
			}
		}
	}

	private void dismissProfileSavingDialog() {
		FragmentActivity activity = getActivity();
		if (progress != null && AndroidUtils.isActivityNotDestroyed(activity)) {
			progress.dismiss();
		}
	}

	private void customProfileSaved() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			if (activity instanceof MapActivity) {
				((MapActivity) activity).updateApplicationModeSettings();
			}
			FragmentManager fragmentManager = activity.getSupportFragmentManager();
			if (!fragmentManager.isStateSaved()) {
				fragmentManager.popBackStack();
				BaseSettingsFragment.showInstance(activity, SettingsScreenType.CONFIGURE_PROFILE,
						ApplicationMode.valueOfStringKey(changedProfile.stringKey, null));
			}
		}
	}

	@NonNull
	private String getUniqueStringKey(@NonNull ApplicationMode mode) {
		return mode.getStringKey() + CUSTOM_MODE_KEY_SEPARATOR + System.currentTimeMillis();
	}

	private boolean hasNameDuplicate() {
		for (ApplicationMode mode : ApplicationMode.allPossibleValues()) {
			if (mode.toHumanString().trim().equals(changedProfile.name.trim()) &&
					!mode.getStringKey().trim().equals(profile.stringKey.trim())) {
				return true;
			}
		}
		return false;
	}

	private boolean nameIsEmpty() {
		return changedProfile.name.trim().isEmpty();
	}

	private void disableSaveButtonWithErrorMessage(String errorMessage) {
		saveButton.setEnabled(false);
		profileNameOtfb.setError(errorMessage, true);
	}

	public void showExitDialog() {
		hideKeyboard();
		if (isChanged()) {
			AlertDialog.Builder dismissDialog = createWarningDialog(getActivity(),
					R.string.shared_string_dismiss, R.string.exit_without_saving, R.string.shared_string_cancel);
			dismissDialog.setPositiveButton(R.string.shared_string_exit, (dialog, which) -> {
				changedProfile = profile;
				goBackWithoutSaving();
			});
			dismissDialog.show();
		} else {
			dismiss();
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

	private void updateColorName() {
		if (colorsCard == null || colorName == null) {
			return;
		}
		int selectedColor = colorsCard.getSelectedColor();
		if (colorsCard.isBaseColor(selectedColor)) {
			colorName.setText(changedProfile.getProfileColorByColorValue(selectedColor).getName());
		} else {
			colorName.setText(R.string.custom_color);
		}
	}

	@Override
	public void onProfileSelected(Bundle args) {
		String profileKey = args.getString(PROFILE_KEY_ARG);
		boolean imported = args.getBoolean(PROFILES_LIST_UPDATED_ARG);
		updateParentProfile(profileKey, imported);
	}

	@Override
	public void onCardLayoutNeeded(@NonNull BaseCard card) {
	}

	@Override
	public void onCardPressed(@NonNull BaseCard card) {
		if (card instanceof ColorsCard) {
			ColorsCard cardOfColors = (ColorsCard) card;
			int color = cardOfColors.getSelectedColor();

			if (color == changedProfile.getActualColor()) {
				return;
			}

			if (cardOfColors.isBaseColor(color)) {
				changedProfile.customColor = null;
				changedProfile.color = changedProfile.getProfileColorByColorValue(color);
			} else {
				changedProfile.customColor = cardOfColors.getSelectedColor();
				changedProfile.color = null;
			}

			if (iconItems != null) {
				updateIconColor(changedProfile.iconRes);
			}

			updateColorName();
			updateProfileNameAppearance();
			updateProfileButton();
			setVerticalScrollBarEnabled(false);
			updatePreference(findPreference(MASTER_PROFILE));
			updatePreference(findPreference(LOCATION_ICON_ITEMS));
			updatePreference(findPreference(NAV_ICON_ITEMS));
			setVerticalScrollBarEnabled(true);
		}
	}

	@Override
	public void onCardButtonPressed(@NonNull BaseCard card, int buttonIndex) {
	}

	@Override
	public void onColorSelected(Integer prevColor, int newColor) {
		colorsCard.onColorSelected(prevColor, newColor);
		this.onCardPressed(colorsCard);
	}

	public static boolean showInstance(@NonNull FragmentActivity activity,
	                                   @NonNull SettingsScreenType screenType,
	                                   @Nullable String appMode, boolean imported) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		String tag = screenType.fragmentName;
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			Fragment fragment = Fragment.instantiate(activity, tag);
			Bundle args = new Bundle();
			if (appMode != null) {
				args.putString(BASE_PROFILE_FOR_NEW, appMode);
				args.putBoolean(IS_BASE_PROFILE_IMPORTED, imported);
			}
			fragment.setArguments(args);
			fragmentManager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, tag)
					.addToBackStack(DRAWER_SETTINGS_ID)
					.commitAllowingStateLoss();
			return true;
		}
		return false;
	}

	class ApplicationProfileObject {
		String stringKey;
		ApplicationMode parent;
		String name;
		ProfileIconColors color;
		Integer customColor;
		int iconRes;
		String routingProfile;
		RouteService routeService;
		NavigationIcon navigationIcon;
		LocationIcon locationIcon;

		@ColorInt
		public int getActualColor() {
			return customColor != null ?
					customColor : ContextCompat.getColor(app, color.getColor(isNightMode()));
		}

		public ProfileIconColors getProfileColorByColorValue(int colorValue) {
			for (ProfileIconColors color : ProfileIconColors.values()) {
				if (ContextCompat.getColor(app, color.getColor(true)) == colorValue
						|| ContextCompat.getColor(app, color.getColor(false)) == colorValue) {
					return color;
				}
			}
			return ProfileIconColors.DEFAULT;
		}

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
			if (customColor != null ? !customColor.equals(that.customColor) : that.customColor != null)
				return false;
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
			result = 31 * result + (customColor != null ? customColor.hashCode() : 0);
			result = 31 * result + iconRes;
			result = 31 * result + (routingProfile != null ? routingProfile.hashCode() : 0);
			result = 31 * result + (routeService != null ? routeService.hashCode() : 0);
			result = 31 * result + (navigationIcon != null ? navigationIcon.hashCode() : 0);
			result = 31 * result + (locationIcon != null ? locationIcon.hashCode() : 0);
			return result;
		}
	}
}
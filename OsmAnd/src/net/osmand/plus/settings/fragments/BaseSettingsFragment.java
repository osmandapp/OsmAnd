package net.osmand.plus.settings.fragments;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SETTINGS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.SETTINGS_ID;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.*;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.ISupportInsets;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.IOsmAndFragment;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.profiles.SelectAppModesBottomSheetDialogFragment;
import net.osmand.plus.profiles.SelectAppModesBottomSheetDialogFragment.AppModeChangedListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.bottomsheets.BooleanPreferenceBottomSheet;
import net.osmand.plus.settings.bottomsheets.ChangeGeneralProfilesPrefBottomSheet;
import net.osmand.plus.settings.bottomsheets.CustomizableSingleSelectionBottomSheet;
import net.osmand.plus.settings.bottomsheets.EditTextPreferenceBottomSheet;
import net.osmand.plus.settings.bottomsheets.MultiSelectPreferencesBottomSheet;
import net.osmand.plus.settings.bottomsheets.SingleSelectPreferenceBottomSheet;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.MultiSelectBooleanPreference;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.InsetsUtils;
import net.osmand.plus.utils.InsetsUtils.InsetSide;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public abstract class BaseSettingsFragment extends PreferenceFragmentCompat implements IOsmAndFragment,
		OnPreferenceChangeListener, OnPreferenceClickListener, AppModeChangedListener, OnConfirmPreferenceChange, OnPreferenceChanged, ISupportInsets {

	private static final Log LOG = PlatformUtil.getLog(BaseSettingsFragment.class);

	public static final String OPEN_CONFIG_PROFILE = "openConfigProfile";
	public static final String OPEN_SETTINGS = "openSettings";
	public static final String OPEN_CONFIG_ON_MAP = "openConfigOnMap";
	public static final String MAP_CONFIG = "openMapConfigMenu";
	public static final String SCREEN_CONFIG = "screenConfig";

	protected OsmandApplication app;
	protected OsmandSettings settings;
	protected ApplicationMode appMode;
	protected UiUtilities iconsCache;
	protected boolean nightMode;

	private LayoutInflater themedInflater;
	protected OsmAndAppCustomization appCustomization;

	private SettingsScreenType currentScreenType;

	private int statusBarColor = -1;
	private boolean wasDrawerDisabled;

	private WindowInsetsCompat lastRootInsets = null;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		app = (OsmandApplication) requireActivity().getApplication();
		settings = app.getSettings();
		iconsCache = app.getUIUtilities();
		appCustomization = app.getAppCustomization();
		appMode = restoreAppMode(app, appMode, savedInstanceState, getArguments());
		super.onCreate(savedInstanceState);
		currentScreenType = getCurrentScreenType();
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		getPreferenceManager().setPreferenceDataStore(settings.getDataStore(getSelectedAppMode()));
	}

	@Override
	@SuppressLint("RestrictedApi")
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateTheme();
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			if (getPreferenceScreen() != null && currentScreenType != null) {
				PreferenceManager prefManager = getPreferenceManager();
				PreferenceScreen preferenceScreen = prefManager.inflateFromResource(prefManager.getContext(), currentScreenType.preferencesResId, null);
				if (prefManager.setPreferences(preferenceScreen)) {
					setupPreferences();
					registerPreferences(preferenceScreen);
				}
			} else {
				updateAllSettings();
			}
			createToolbar(inflater, view);
			setDivider(null);
			view.setBackgroundColor(ContextCompat.getColor(app, getBackgroundColorRes()));
			AndroidUtils.addStatusBarPadding21v(requireActionBarActivity(), view);
		}
		return view;
	}

	private boolean updateTheme() {
		ApplicationMode appMode = getSelectedAppMode();
		boolean nightMode = app.getDaynightHelper().isNightMode(appMode, ThemeUsageContext.APP);
		boolean changed = this.nightMode != nightMode;
		this.nightMode = nightMode;
		this.themedInflater = UiUtilities.getInflater(requireContext(), nightMode);
		return changed;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		updateToolbar();
		InsetsUtils.processInsets(this, view, null);
	}

	@Nullable
	@Override
	public Set<InsetSide> getRootInsetSides(){
		return EnumSet.of(InsetSide.TOP);
	}

	@Nullable
	@Override
	public List<Integer> getBottomContainersIds() {
		List<Integer> ids = new ArrayList<>();
		ids.add(R.id.bottom_buttons_container);
		return ids;
	}

	@Nullable
	@Override
	public List<Integer> getScrollableViewIds() {
		return null;
	}

	@Nullable
	@Override
	public List<Integer> getFabIds() {
		return null;
	}

	@Override
	public void onApplyInsets(@NonNull WindowInsetsCompat insets){

	}

	@Nullable
	@Override
	public WindowInsetsCompat getLastRootInsets() {
		return lastRootInsets;
	}

	@Override
	public void setLastRootInsets(@NonNull WindowInsetsCompat rootInsets) {
		lastRootInsets = rootInsets;
	}

	@Override
	public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(getActivity(), isNightMode());
		RecyclerView recyclerView = super.onCreateRecyclerView(themedInflater, parent, savedInstanceState);
		recyclerView.setPadding(0, 0, 0, AndroidUtils.dpToPx(app, 80));
		return recyclerView;
	}

	@SuppressLint("RestrictedApi")
	@Override
	protected RecyclerView.Adapter<PreferenceViewHolder> onCreateAdapter(PreferenceScreen preferenceScreen) {
		return new PreferenceGroupAdapter(preferenceScreen) {

			@Override
			public void onBindViewHolder(@NonNull PreferenceViewHolder holder, int position) {
				super.onBindViewHolder(holder, position);

				Preference preference = getItem(position);
				if (preference != null) {
					onBindPreferenceViewHolder(preference, holder);
				}
			}
		};
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		saveAppModeToBundle(appMode, outState);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onResume() {
		super.onResume();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			wasDrawerDisabled = mapActivity.isDrawerDisabled();
			if (!wasDrawerDisabled) {
				mapActivity.disableDrawer();
			}
		}
		updateStatusBar();
	}

	@Override
	public void onPause() {
		super.onPause();

		Activity activity = getActivity();
		if (activity != null) {
			if (!wasDrawerDisabled && activity instanceof MapActivity) {
				((MapActivity) activity).enableDrawer();
			}
			if (!(activity instanceof MapActivity) && statusBarColor != -1) {
				AndroidUiHelper.setStatusBarColor(activity, statusBarColor);
			}
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		if (getStatusBarColorId() != -1) {
			Activity activity = getActivity();
			if (activity instanceof MapActivity) {
				((MapActivity) activity).updateStatusBarColor();
				((MapActivity) activity).updateNavigationBarColor();
			}
		}
	}

	public void updateStatusBar() {
		Activity activity = getActivity();
		if (activity != null) {
			updateStatusBar(activity);
		}
	}

	protected void updateStatusBar(@NonNull Activity activity) {
		int colorId = getStatusBarColorId();
		if (colorId != -1) {
			if (activity instanceof MapActivity) {
				((MapActivity) activity).updateStatusBarColor();
			} else {
				statusBarColor = AndroidUiHelper.setStatusBarColor(activity, getColor(colorId));
			}
		}
	}

	@NonNull
	@Override
	public OsmandApplication getApp() {
		return app;
	}

	@NonNull
	@Override
	public LayoutInflater getThemedInflater() {
		return themedInflater;
	}

	@NonNull
	@Override
	public ThemeUsageContext getThemeUsageContext() {
		return ThemeUsageContext.APP;
	}

	@ColorRes
	public int getStatusBarColorId() {
		boolean nightMode = isNightMode();
		if (isProfileDependent()) {
			View view = getView();
			if (view != null && !nightMode) {
				AndroidUiHelper.setStatusBarContentColor(view, true);
			}
			return ColorUtilities.getListBgColorId(nightMode);
		} else {
			return ColorUtilities.getStatusBarColorId(nightMode);
		}
	}

	public boolean getContentStatusBarNightMode() {
		boolean nightMode = isNightMode();
		if (isProfileDependent()) {
			return nightMode;
		} else {
			return true;
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		return onConfirmPreferenceChange(preference.getKey(), newValue, getApplyQueryType());
	}

	@Override
	public final boolean onConfirmPreferenceChange(String prefId, Object newValue, ApplyQueryType applyQueryType) {
		if (applyQueryType != null && newValue instanceof Serializable) {
			if (applyQueryType == ApplyQueryType.SNACK_BAR) {
				applyPreferenceWithSnackBar(prefId, (Serializable) newValue);
				return true;
			} else if (applyQueryType == ApplyQueryType.BOTTOM_SHEET) {
				FragmentManager fragmentManager = getFragmentManager();
				if (fragmentManager != null) {
					ChangeGeneralProfilesPrefBottomSheet.showInstance(fragmentManager, prefId,
							(Serializable) newValue, this, false, getSelectedAppMode());
				}
				return false;
			} else if (applyQueryType == ApplyQueryType.NONE) {
				onApplyPreferenceChange(prefId, false, newValue);
				return true;
			}
		}
		return true;
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		return false;
	}

	public boolean isProfileDependent() {
		return currentScreenType != null && currentScreenType.profileDependent;
	}

	public ApplyQueryType getApplyQueryType() {
		return currentScreenType != null ? currentScreenType.applyQueryType : null;
	}

	protected void displayPreferenceDialog(@NonNull String prefKey) {
		Preference preference = findPreference(prefKey);
		if (preference != null) {
			onDisplayPreferenceDialog(preference);
		}
	}

	@Override
	public void onDisplayPreferenceDialog(Preference preference) {
		FragmentManager fragmentManager = getFragmentManager();
		if (fragmentManager == null) {
			return;
		}

		ApplicationMode appMode = getSelectedAppMode();
		if (preference instanceof ListPreferenceEx) {
			SingleSelectPreferenceBottomSheet.showInstance(fragmentManager, preference.getKey(), this, false, appMode, isProfileDependent(), false);
		} else if (preference instanceof SwitchPreferenceEx) {
			BooleanPreferenceBottomSheet.showInstance(fragmentManager, preference.getKey(), getApplyQueryType(), this, appMode, false, isProfileDependent());
		} else if (preference instanceof EditTextPreference) {
			EditTextPreferenceBottomSheet.showInstance(fragmentManager, preference.getKey(), this, false, appMode);
		} else if (preference instanceof MultiSelectBooleanPreference) {
			MultiSelectPreferencesBottomSheet.showInstance(fragmentManager, preference.getKey(), this, false, appMode, isProfileDependent());
		} else {
			super.onDisplayPreferenceDialog(preference);
		}
	}

	@Override
	public void onAppModeChanged(ApplicationMode appMode) {
		setAppMode(appMode);
		if (updateTheme()) {
			recreate();
		} else {
			getPreferenceManager().setPreferenceDataStore(settings.getDataStore(appMode));
			updateToolbar();
			updateAllSettings();
		}
	}

	@Override
	public void setAppMode(@NonNull ApplicationMode appMode) {
		this.appMode = appMode;
	}

	@NonNull
	@Override
	public ApplicationMode getAppMode() {
		return appMode;
	}

	public Bundle buildArguments() {
		return buildArguments(appMode.getStringKey());
	}

	public Bundle buildArguments(String appModeKey) {
		Bundle args = new Bundle();
		args.putString(APP_MODE_KEY, appModeKey);
		return args;
	}

	public void recreate() {
		FragmentActivity activity = getActivity();
		if (activity != null && currentScreenType != null) {
			Fragment fragment = Fragment.instantiate(activity, currentScreenType.fragmentName);
			fragment.setArguments(buildArguments());
			FragmentManager fm = activity.getSupportFragmentManager();
			fm.popBackStack();
			fm.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, fragment.getClass().getName())
					.addToBackStack(DRAWER_SETTINGS_ID)
					.commit();
		}
	}

	protected abstract void setupPreferences();

	protected void onBindPreferenceViewHolder(@NonNull Preference preference, @NonNull PreferenceViewHolder holder) {
		if (preference.isSelectable()) {
			View selectableView = holder.itemView.findViewById(R.id.selectable_list_item);
			if (selectableView != null) {
				Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, getActiveProfileColor(), 0.3f);
				AndroidUtils.setBackground(selectableView, drawable);
			}
		}
		TextView titleView = (TextView) holder.findViewById(android.R.id.title);
		if (titleView != null) {
			titleView.setSingleLine(false);
		}
		boolean enabled = preference.isEnabled();
		View cb = holder.itemView.findViewById(R.id.switchWidget);
		if (cb == null) {
			cb = holder.findViewById(android.R.id.checkbox);
		}
		if (cb instanceof CompoundButton) {
			if (isProfileDependent()) {
				int color = enabled ? getActiveProfileColor() : getDisabledTextColor();
				UiUtilities.setupCompoundButton(isNightMode(), color, (CompoundButton) cb);
			} else {
				UiUtilities.setupCompoundButton((CompoundButton) cb, isNightMode(), UiUtilities.CompoundButtonType.GLOBAL);
			}
		}
		if ((preference.isPersistent() || preference instanceof TwoStatePreference) && !(preference instanceof PreferenceCategory)) {
			if (titleView != null) {
				titleView.setTextColor(enabled ? getActiveTextColor() : getDisabledTextColor());
			}
			if (preference instanceof TwoStatePreference) {
				enabled = enabled & ((TwoStatePreference) preference).isChecked();
			}
			if (preference instanceof MultiSelectListPreference) {
				enabled = enabled & !((MultiSelectListPreference) preference).getValues().isEmpty();
			}
			ImageView imageView = (ImageView) holder.findViewById(android.R.id.icon);
			if (imageView != null) {
				imageView.setEnabled(enabled);
			}
		}
	}

	protected void updatePreference(Preference preference) {
		PreferenceGroupAdapter adapter = (PreferenceGroupAdapter) getListView().getAdapter();
		if (adapter != null) {
			adapter.onPreferenceChange(preference);
		}
	}

	protected void createToolbar(@NonNull LayoutInflater inflater, @NonNull View view) {
		AppBarLayout appBarLayout = view.findViewById(R.id.appbar);
		ViewCompat.setElevation(appBarLayout, 5.0f);

		View toolbarContainer = currentScreenType == null ? null :
				UiUtilities.getInflater(requireActivity(), isNightMode()).inflate(currentScreenType.toolbarResId, appBarLayout);

		TextView toolbarTitle = view.findViewById(R.id.toolbar_title);
		if (toolbarTitle != null) {
			toolbarTitle.setText(getPreferenceScreen().getTitle());
		}

		TextView toolbarSubtitle = view.findViewById(R.id.toolbar_subtitle);
		if (toolbarSubtitle != null) {
			toolbarSubtitle.setText(getSelectedAppMode().toHumanString());
		}

		View closeButton = view.findViewById(R.id.close_button);
		if (closeButton != null) {
			closeButton.setOnClickListener(v -> {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					mapActivity.onBackPressed();
				}
			});
			if (closeButton instanceof ImageView) {
				UiUtilities.rotateImageByLayoutDirection((ImageView) closeButton);
			}
		}

		View switchProfile = toolbarContainer == null ? null : toolbarContainer.findViewById(R.id.profile_button);
		if (switchProfile != null) {
			switchProfile.setContentDescription(getString(R.string.switch_profile));
			switchProfile.setOnClickListener(v -> {
				FragmentManager fragmentManager = getFragmentManager();
				if (fragmentManager != null) {
					SelectAppModesBottomSheetDialogFragment.showInstance(fragmentManager,
							BaseSettingsFragment.this, false, getSelectedAppMode(), false);
				}
			});
			switchProfile.setVisibility(View.GONE);
		}
	}

	protected void updateToolbar() {
		View view = getView();
		if (view == null) {
			return;
		}

		ApplicationMode selectedAppMode = getSelectedAppMode();
		int iconColor = getActiveProfileColor();

		ImageView profileIcon = view.findViewById(R.id.profile_icon);
		if (profileIcon != null) {
			int iconRes = selectedAppMode.getIconRes();
			profileIcon.setImageDrawable(getPaintedIcon(iconRes, iconColor));
		}
		TextView profileTitle = view.findViewById(R.id.profile_title);
		if (profileTitle != null) {
			String appName = selectedAppMode.toHumanString();
			profileTitle.setText(appName);
		}
		View toolbarDivider = view.findViewById(R.id.toolbar_divider);
		if (toolbarDivider != null) {
			toolbarDivider.setBackgroundColor(iconColor);
		}
		updateProfileButton();
	}

	protected void updateProfileButton() {
		View view = getView();
		if (view == null) {
			return;
		}

		View profileButton = view.findViewById(R.id.profile_button);
		if (profileButton != null && currentScreenType != null) {
			int toolbarRes = currentScreenType.toolbarResId;
			int iconColor = getActiveProfileColor();
			int bgColor = ColorUtilities.getColorWithAlpha(iconColor, 0.1f);
			int selectedColor = ColorUtilities.getColorWithAlpha(iconColor, 0.3f);

			int bgResId = 0;
			int selectableResId = 0;
			if (toolbarRes == R.layout.profile_preference_toolbar || toolbarRes == R.layout.profile_preference_toolbar_with_switch) {
				bgResId = R.drawable.circle_background_light;
				selectableResId = R.drawable.ripple_circle;
			} else if (toolbarRes == R.layout.profile_preference_toolbar_big) {
				bgResId = R.drawable.rectangle_rounded;
				selectableResId = R.drawable.ripple_rectangle_rounded;
			}
			Drawable bgDrawable = getPaintedIcon(bgResId, bgColor);
			Drawable selectable = getPaintedIcon(selectableResId, selectedColor);
			Drawable[] layers = {bgDrawable, selectable};
			AndroidUtils.setBackground(profileButton, new LayerDrawable(layers));
		}
	}

	protected void addOnPreferencesScreen(@NonNull Preference preference) {
		PreferenceScreen preferenceScreen = getPreferenceScreen();
		preferenceScreen.addPreference(preference);
	}

	private void updatePreferencesScreen() {
		if (getSelectedAppMode() != null && currentScreenType != null) {
			int resId = currentScreenType.preferencesResId;
			if (resId != -1) {
				addPreferencesFromResource(resId);
			}
			setupPreferences();
			registerPreferences(getPreferenceScreen());
		}
	}

	private void registerPreferences(PreferenceGroup preferenceGroup) {
		if (preferenceGroup != null) {
			for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
				Preference preference = preferenceGroup.getPreference(i);
				registerPreference(preference);
				if (preference instanceof PreferenceGroup) {
					registerPreferences((PreferenceGroup) preference);
				}
			}
		}
	}

	public void updateSetting(String prefId) {
		updateAllSettings();
	}

	public void onApplyPreferenceChange(String prefId, boolean applyToAllProfiles, Object newValue) {
		if (settings.getPreference(prefId) instanceof CommonPreference) {
			applyPreference(prefId, applyToAllProfiles, newValue);
		} else {
			Preference pref = findPreference(prefId);
			if (pref != null) {
				applyPreference(pref, applyToAllProfiles, newValue);
			}
		}
	}

	protected final void applyPreference(String prefId, boolean applyToAllProfiles, Object newValue) {
		if (applyToAllProfiles) {
			settings.setPreferenceForAllModes(prefId, newValue);
		} else {
			settings.setPreference(prefId, newValue, getSelectedAppMode());
		}
	}

	protected final void resetPreference(String prefId, boolean applyToAllProfiles) {
		if (applyToAllProfiles) {
			settings.resetPreferenceForAllModes(prefId);
		} else {
			settings.resetPreference(prefId, getSelectedAppMode());
		}
	}

	protected final void applyPreference(Preference pref, boolean applyToAllProfiles, Object newValue) {
		if (pref instanceof MultiSelectBooleanPreference msp) {
			Set<String> values = (Set<String>) newValue;
			String[] ids = msp.getPrefsIds();
			for (String id : ids) {
				applyPreference(id, applyToAllProfiles, values.contains(id));
			}
		}
	}

	public void updateAllSettings() {
		if (getContext() != null) {
			PreferenceScreen screen = getPreferenceScreen();
			if (screen != null) {
				screen.removeAll();
			}
			updatePreferencesScreen();
		}
	}

	public boolean shouldDismissOnChange() {
		return false;
	}

	public void dismiss() {
		callActivity(activity -> {
			FragmentManager fragmentManager = activity.getSupportFragmentManager();
			if (!fragmentManager.isStateSaved()) {
				fragmentManager.popBackStack();
			}
		});
	}

	protected void enableDisablePreferences(boolean enable) {
		PreferenceScreen screen = getPreferenceScreen();
		if (screen != null) {
			for (int i = 0; i < screen.getPreferenceCount(); i++) {
				screen.getPreference(i).setEnabled(enable);
			}
		}
	}

	private SettingsScreenType getCurrentScreenType() {
		String fragmentName = this.getClass().getName();
		for (SettingsScreenType type : SettingsScreenType.values()) {
			if (type.fragmentName.equals(fragmentName)) {
				return type;
			}
		}
		return null;
	}

	protected void setPreferenceIcon(@NonNull String prefId, @NonNull Drawable icon) {
		Preference preference = findPreference(prefId);
		if (preference != null) {
			preference.setIcon(icon);
		}
	}

	@ColorInt
	protected int getActiveProfileColor() {
		return isProfileDependent() ?
				getSelectedAppMode().getProfileColor(isNightMode()) :
				ColorUtilities.getActiveColor(app, nightMode);
	}

	@ColorRes
	protected int getActiveColorRes() {
		return ColorUtilities.getActiveColorId(isNightMode());
	}

	@ColorRes
	protected int getBackgroundColorRes() {
		return ColorUtilities.getListBgColorId(isNightMode());
	}

	@ColorInt
	protected int getActiveTextColor() {
		return ColorUtilities.getPrimaryTextColor(app, isNightMode());
	}

	@ColorInt
	protected int getDisabledTextColor() {
		return ColorUtilities.getSecondaryTextColor(app, isNightMode());
	}

	protected void registerPreference(@Nullable Preference preference) {
		if (preference != null) {
			preference.setOnPreferenceChangeListener(this);
			preference.setOnPreferenceClickListener(this);

			String prefId = preference.getKey();
			if (!Algorithms.isEmpty(prefId)) {
				boolean featureEnabled = appCustomization.isFeatureEnabled(SETTINGS_ID + prefId);
				preference.setVisible(featureEnabled && preference.isVisible());
			}
			if (preference instanceof ListPreference listPreference) {
				assert listPreference.getEntryValues().length == listPreference.getEntries().length;
			}
		}
	}

	public ApplicationMode getSelectedAppMode() {
		return appMode;
	}

	public boolean isNightMode() {
		return nightMode;
	}

	@NonNull
	@Override
	public UiUtilities getIconsCache() {
		return iconsCache;
	}

	protected Drawable getActiveIcon(@DrawableRes int id) {
		return getPaintedIcon(id, getActiveProfileColor());
	}

	protected Drawable getPersistentPrefIcon(@DrawableRes int iconId) {
		Drawable disabled = UiUtilities.createTintedDrawable(app, iconId, ContextCompat.getColor(app, R.color.icon_color_default_light));
		Drawable enabled = UiUtilities.createTintedDrawable(app, iconId, getActiveProfileColor());
		return getPersistentPrefIcon(enabled, disabled);
	}

	protected Drawable getPersistentPrefIcon(Drawable enabled, Drawable disabled) {
		return AndroidUtils.createEnabledStateListDrawable(disabled, enabled);
	}

	protected void showSingleSelectionDialog(@NonNull String processId,
	                                         @NonNull IDialogController controller) {
		callActivity(activity -> {
			DialogManager dialogManager = app.getDialogManager();
			dialogManager.register(processId, controller);
			ApplicationMode appMode = getSelectedAppMode();
			FragmentManager fm = activity.getSupportFragmentManager();
			CustomizableSingleSelectionBottomSheet.showInstance(fm, processId, appMode, false);
		});
	}

	public SwitchPreferenceCompat createSwitchPreference(OsmandPreference<Boolean> b, int title, int summary, int layoutId) {
		return createSwitchPreference(b, getString(title), getString(summary), layoutId);
	}

	public SwitchPreferenceCompat createSwitchPreference(OsmandPreference<Boolean> b, String title, String summary, int layoutId) {
		SwitchPreferenceCompat p = new SwitchPreferenceCompat(requireContext());
		p.setTitle(title);
		p.setKey(b.getId());
		p.setSummary(summary);
		p.setLayoutResource(layoutId);
		p.setIconSpaceReserved(true);
		return p;
	}

	public SwitchPreferenceEx createSwitchPreferenceEx(String prefId, int title, int layoutId) {
		return createSwitchPreferenceEx(prefId, getString(title), null, layoutId);
	}

	public SwitchPreferenceEx createSwitchPreferenceEx(String prefId, String title, String summary, int layoutId) {
		return createSwitchPreferenceEx(requireContext(), prefId, title, summary, layoutId);
	}

	public static SwitchPreferenceEx createSwitchPreferenceEx(@NonNull Context ctx, @NonNull String prefId,
	                                                          String title, String summary, int layoutId) {
		SwitchPreferenceEx p = new SwitchPreferenceEx(ctx);
		p.setKey(prefId);
		p.setTitle(title);
		p.setSummary(summary);
		p.setLayoutResource(layoutId);
		p.setIconSpaceReserved(true);
		return p;
	}

	public ListPreferenceEx createListPreferenceEx(String prefId, String[] names, Object[] values, int title, int layoutId) {
		return createListPreferenceEx(prefId, names, values, getString(title), layoutId);
	}

	public ListPreferenceEx createListPreferenceEx(String prefId, String[] names, Object[] values, String title, int layoutId) {
		return createListPreferenceEx(requireContext(), prefId, names, values, title, layoutId);
	}

	public static ListPreferenceEx createListPreferenceEx(@NonNull Context ctx, @NonNull String prefId,
	                                                      @NonNull String[] names, @NonNull Object[] values,
	                                                      String title, int layoutId) {
		ListPreferenceEx listPreference = new ListPreferenceEx(ctx);
		listPreference.setKey(prefId);
		listPreference.setTitle(title);
		listPreference.setDialogTitle(title);
		listPreference.setEntries(names);
		listPreference.setEntryValues(values);
		listPreference.setIconSpaceReserved(true);

		if (layoutId != 0) {
			listPreference.setLayoutResource(layoutId);
		}

		return listPreference;
	}

	public void addDividerPref() {
		getPreferenceScreen().addPreference(createDividerPref());
	}

	@NonNull
	public Preference createDividerPref() {
		Preference preference = new Preference(requireContext());
		preference.setSelectable(false);
		preference.setPersistent(false);
		preference.setLayoutResource(R.layout.simple_divider_item);
		return preference;
	}

	@NonNull
	protected <T extends Preference> T requirePreference(@NonNull CharSequence key) {
		T preference = findPreference(key);
		if (preference == null) {
			throw new IllegalArgumentException("Preference with key '" + key + "' not found.");
		}
		return preference;
	}

	public static boolean showInstance(@NonNull FragmentActivity activity, @NonNull SettingsScreenType screenType) {
		return showInstance(activity, screenType, null);
	}

	public static boolean showInstance(@NonNull FragmentActivity activity,
	                                   @NonNull SettingsScreenType screenType,
	                                   @Nullable ApplicationMode appMode) {
		return showInstance(activity, screenType, appMode, new Bundle(), null);
	}

	public static boolean showInstance(@NonNull FragmentActivity activity,
	                                   @NonNull SettingsScreenType screenType,
	                                   @Nullable ApplicationMode appMode,
	                                   @NonNull Bundle args,
	                                   @Nullable Fragment target) {
		try {
			FragmentManager fragmentManager = activity.getSupportFragmentManager();
			String tag = screenType.fragmentName;
			if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, tag)) {
				Fragment fragment = Fragment.instantiate(activity, tag);
				if (appMode != null) {
					args.putString(APP_MODE_KEY, appMode.getStringKey());
				}
				fragment.setArguments(args);
				fragment.setTargetFragment(target, 0);
				fragmentManager.beginTransaction()
						.replace(R.id.fragmentContainer, fragment, tag)
						.addToBackStack(DRAWER_SETTINGS_ID)
						.commitAllowingStateLoss();
				return true;
			}
		} catch (Exception e) {
			LOG.error(e);
		}
		return false;
	}

	void updateRouteInfoMenu() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapRouteInfoMenu().updateMenu();
		}
	}

	public void setupSpeedCamerasAlert() {
		Preference speedCamerasAlert = findPreference(settings.SPEED_CAMERAS_UNINSTALLED.getId());
		speedCamerasAlert.setIcon(getContentIcon(R.drawable.ic_action_alert));
		speedCamerasAlert.setVisible(!settings.SPEED_CAMERAS_UNINSTALLED.get());
	}

	public void setupPrefRoundedBg(PreferenceViewHolder holder) {
		View selectableView = holder.itemView.findViewById(R.id.selectable_list_item);
		if (selectableView != null) {
			int color = AndroidUtils.getColorFromAttr(holder.itemView.getContext(), R.attr.activity_background_color);
			int selectedColor = ColorUtilities.getColorWithAlpha(getActiveProfileColor(), 0.3f);

			Drawable bgDrawable = getPaintedIcon(R.drawable.rectangle_rounded, color);
			Drawable selectable = getPaintedIcon(R.drawable.ripple_rectangle_rounded, selectedColor);
			Drawable[] layers = {bgDrawable, selectable};
			AndroidUtils.setBackground(selectableView, new LayerDrawable(layers));
			LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) selectableView.getLayoutParams();
			params.setMargins(params.leftMargin, AndroidUtils.dpToPx(app, 6), params.rightMargin, params.bottomMargin);
		}
	}

	protected void applyPreferenceWithSnackBar(String prefId, Serializable newValue) {
		onApplyPreferenceChange(prefId, false, newValue);
		updateSetting(prefId);
		View containerView = getView();
		if (containerView != null) {
			String modeName = appMode.toHumanString();
			String text = app.getString(R.string.changes_applied_to_profile, modeName);
			SpannableString message = UiUtilities.createSpannableString(text, Typeface.BOLD, modeName);
			Snackbar snackbar = Snackbar.make(containerView, message, Snackbar.LENGTH_LONG)
					.setAction(R.string.apply_to_all_profiles, view -> onApplyPreferenceChange(prefId, true, newValue));
			UiUtilities.setupSnackbarVerticalLayout(snackbar);
			UiUtilities.setupSnackbar(snackbar, nightMode);
			snackbar.show();
		}
	}
}
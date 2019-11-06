package net.osmand.plus.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceGroupAdapter;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.activities.OsmandInAppPurchaseActivity;
import net.osmand.plus.profiles.SelectAppModesBottomSheetDialogFragment;
import net.osmand.plus.profiles.SelectAppModesBottomSheetDialogFragment.AppModeChangedListener;
import net.osmand.plus.settings.bottomsheets.BooleanPreferenceBottomSheet;
import net.osmand.plus.settings.bottomsheets.EditTextPreferenceBottomSheet;
import net.osmand.plus.settings.bottomsheets.MultiSelectPreferencesBottomSheet;
import net.osmand.plus.settings.bottomsheets.SingleSelectPreferenceBottomSheet;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.MultiSelectBooleanPreference;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SETTINGS_ID;

public abstract class BaseSettingsFragment extends PreferenceFragmentCompat implements OnPreferenceChangeListener,
		OnPreferenceClickListener, AppModeChangedListener {

	private static final Log LOG = PlatformUtil.getLog(BaseSettingsFragment.class);

	protected OsmandApplication app;
	protected OsmandSettings settings;
	protected UiUtilities iconsCache;

	protected int themeRes;

	private SettingsScreenType currentScreenType;

	private int statusBarColor = -1;
	private boolean nightMode;
	private boolean wasDrawerDisabled;

	public enum SettingsScreenType {

		MAIN_SETTINGS(MainSettingsFragment.class.getName(), false, R.xml.settings_main_screen, R.layout.global_preference_toolbar),
		GLOBAL_SETTINGS(GlobalSettingsFragment.class.getName(), false, R.xml.global_settings, R.layout.global_preference_toolbar),
		CONFIGURE_PROFILE(ConfigureProfileFragment.class.getName(), true, R.xml.configure_profile, R.layout.profile_preference_toolbar_big),
		PROXY_SETTINGS(ProxySettingsFragment.class.getName(), false, R.xml.proxy_preferences, R.layout.global_preferences_toolbar_with_switch),
		GENERAL_PROFILE(GeneralProfileSettingsFragment.class.getName(), true, R.xml.general_profile_settings, R.layout.profile_preference_toolbar_big),
		NAVIGATION(NavigationFragment.class.getName(), true, R.xml.navigation_settings_new, R.layout.profile_preference_toolbar_big),
		COORDINATES_FORMAT(CoordinatesFormatFragment.class.getName(), true, R.xml.coordinates_format, R.layout.profile_preference_toolbar),
		ROUTE_PARAMETERS(RouteParametersFragment.class.getName(), true, R.xml.route_parameters, R.layout.profile_preference_toolbar),
		SCREEN_ALERTS(ScreenAlertsFragment.class.getName(), true, R.xml.screen_alerts, R.layout.profile_preference_toolbar_with_switch),
		VOICE_ANNOUNCES(VoiceAnnouncesFragment.class.getName(), true, R.xml.voice_announces, R.layout.profile_preference_toolbar_with_switch),
		VEHICLE_PARAMETERS(VehicleParametersFragment.class.getName(), true, R.xml.vehicle_parameters, R.layout.profile_preference_toolbar),
		MAP_DURING_NAVIGATION(MapDuringNavigationFragment.class.getName(), true, R.xml.map_during_navigation, R.layout.profile_preference_toolbar),
		TURN_SCREEN_ON(TurnScreenOnFragment.class.getName(), true, R.xml.turn_screen_on, R.layout.profile_preference_toolbar_with_switch),
		DATA_STORAGE(DataStorageFragment.class.getName(), false, R.xml.data_storage, R.layout.global_preference_toolbar);

		public final String fragmentName;
		public final boolean profileDependent;
		public final int preferencesResId;
		public final int toolbarResId;

		SettingsScreenType(String fragmentName, boolean profileDependent, int preferencesResId, int toolbarResId) {
			this.fragmentName = fragmentName;
			this.profileDependent = profileDependent;
			this.preferencesResId = preferencesResId;
			this.toolbarResId = toolbarResId;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		app = requireMyApplication();
		settings = app.getSettings();
		super.onCreate(savedInstanceState);
		currentScreenType = getCurrentScreenType();
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		getPreferenceManager().setPreferenceDataStore(settings.getDataStore());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		nightMode = !settings.isLightContent();
		themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			AndroidUtils.addStatusBarPadding21v(getContext(), view);
			updateAllSettings();
			createToolbar(inflater, view);
			setDivider(null);
			view.setBackgroundColor(ContextCompat.getColor(app, getBackgroundColorRes()));
		}

		return view;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		updateToolbar();
	}

	@Override
	public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(getActivity(), nightMode);

		RecyclerView recyclerView = super.onCreateRecyclerView(themedInflater, parent, savedInstanceState);
		recyclerView.setPadding(0, 0, 0, AndroidUtils.dpToPx(app, 80));

		return recyclerView;
	}

	@SuppressLint("RestrictedApi")
	@Override
	protected RecyclerView.Adapter onCreateAdapter(PreferenceScreen preferenceScreen) {
		return new PreferenceGroupAdapter(preferenceScreen) {

			@Override
			public void onBindViewHolder(PreferenceViewHolder holder, int position) {
				super.onBindViewHolder(holder, position);

				Preference preference = getItem(position);
				if (preference != null) {
					onBindPreferenceViewHolder(preference, holder);
				}
			}
		};
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

		if (Build.VERSION.SDK_INT >= 21) {
			Activity activity = getActivity();
			if (activity != null) {
				int colorId = getStatusBarColorId();
				if (colorId != -1) {
					if (activity instanceof MapActivity) {
						((MapActivity) activity).updateStatusBarColor();
					} else {
						statusBarColor = activity.getWindow().getStatusBarColor();
						activity.getWindow().setStatusBarColor(ContextCompat.getColor(activity, colorId));
					}
				}
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		MapActivity mapActivity = getMapActivity();
		if (!wasDrawerDisabled && mapActivity != null) {
			mapActivity.enableDrawer();
		}

		if (Build.VERSION.SDK_INT >= 21) {
			Activity activity = getActivity();
			if (activity != null) {
				if (!(activity instanceof MapActivity) && statusBarColor != -1) {
					activity.getWindow().setStatusBarColor(statusBarColor);
				}
			}
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		if (Build.VERSION.SDK_INT >= 21 && getStatusBarColorId() != -1) {
			Activity activity = getActivity();
			if (activity instanceof MapActivity) {
				((MapActivity) activity).updateStatusBarColor();
			}
		}
	}

	@ColorRes
	public int getStatusBarColorId() {
		boolean nightMode = isNightMode();
		if (currentScreenType.profileDependent) {
			View view = getView();
			if (view != null && Build.VERSION.SDK_INT >= 23 && !nightMode) {
				view.setSystemUiVisibility(view.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
			}
			return nightMode ? R.color.list_background_color_dark : R.color.list_background_color_light;
		} else {
			return nightMode ? R.color.status_bar_color_dark : R.color.status_bar_color_light;
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		return true;
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		return false;
	}

	@Override
	public void onDisplayPreferenceDialog(Preference preference) {
		FragmentManager fragmentManager = getFragmentManager();
		if (fragmentManager == null) {
			return;
		}

		if (preference instanceof ListPreferenceEx) {
			SingleSelectPreferenceBottomSheet.showInstance(fragmentManager, preference.getKey(), this, false);
		} else if (preference instanceof SwitchPreferenceEx) {
			BooleanPreferenceBottomSheet.showInstance(fragmentManager, preference.getKey(), this, false);
		} else if (preference instanceof EditTextPreference) {
			EditTextPreferenceBottomSheet.showInstance(fragmentManager, preference.getKey(), this, false);
		} else if (preference instanceof MultiSelectBooleanPreference) {
			MultiSelectPreferencesBottomSheet.showInstance(fragmentManager, preference.getKey(), this, false);
		} else {
			super.onDisplayPreferenceDialog(preference);
		}
	}

	@Override
	public void onAppModeChanged() {
		updateToolbar();
		updateAllSettings();
	}

	protected abstract void setupPreferences();

	protected void onBindPreferenceViewHolder(Preference preference, PreferenceViewHolder holder) {
		if (preference.isSelectable()) {
			View selectableView = holder.itemView.findViewById(R.id.selectable_list_item);
			if (selectableView != null) {
				Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, getActiveProfileColor(), 0.3f);
				AndroidUtils.setBackground(selectableView, drawable);
			}
		}
	}

	@SuppressLint("RestrictedApi")
	protected void updatePreference(Preference preference) {
		PreferenceGroupAdapter adapter = (PreferenceGroupAdapter) getListView().getAdapter();
		adapter.onPreferenceChange(preference);
	}

	protected void createToolbar(LayoutInflater inflater, View view) {
		AppBarLayout appBarLayout = (AppBarLayout) view.findViewById(R.id.appbar);
		ViewCompat.setElevation(appBarLayout, 5.0f);

		View toolbarContainer = UiUtilities.getInflater(getActivity(), nightMode).inflate(currentScreenType.toolbarResId, appBarLayout);

		TextView toolbarTitle = (TextView) view.findViewById(R.id.toolbar_title);
		toolbarTitle.setText(getPreferenceScreen().getTitle());

		View closeButton = view.findViewById(R.id.close_button);
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					mapActivity.onBackPressed();
				}
			}
		});
		View switchProfile = toolbarContainer.findViewById(R.id.profile_button);
		if (switchProfile != null) {
			switchProfile.setContentDescription(getString(R.string.switch_profile));
			switchProfile.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					FragmentManager fragmentManager = getFragmentManager();
					if (fragmentManager != null) {
						SelectAppModesBottomSheetDialogFragment.showInstance(fragmentManager, BaseSettingsFragment.this);
					}
				}
			});
		}
	}

	protected void updateToolbar() {
		View view = getView();
		if (view == null) {
			return;
		}

		ApplicationMode selectedAppMode = getSelectedAppMode();
		int iconColor = getActiveProfileColor();

		ImageView profileIcon = (ImageView) view.findViewById(R.id.profile_icon);
		if (profileIcon != null) {
			int iconRes = selectedAppMode.getIconRes();
			profileIcon.setImageDrawable(getPaintedIcon(iconRes, iconColor));
		}
		TextView profileTitle = (TextView) view.findViewById(R.id.profile_title);
		if (profileTitle != null) {
			String appName = selectedAppMode.toHumanString(app);
			profileTitle.setText(appName);
		}
		View toolbarDivider = view.findViewById(R.id.toolbar_divider);
		if (toolbarDivider != null) {
			toolbarDivider.setBackgroundColor(iconColor);
		}
		updateProfileButton();
	}

	private void updateProfileButton() {
		View view = getView();
		if (view == null) {
			return;
		}

		View profileButton = view.findViewById(R.id.profile_button);
		if (profileButton != null) {
			int toolbarRes = currentScreenType.toolbarResId;
			int iconColor = getActiveProfileColor();
			int bgColor = UiUtilities.getColorWithAlpha(iconColor, 0.1f);
			int selectedColor = UiUtilities.getColorWithAlpha(iconColor, 0.3f);

			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
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
			} else {
				int bgResId = 0;
				if (toolbarRes == R.layout.profile_preference_toolbar || toolbarRes == R.layout.profile_preference_toolbar_with_switch) {
					bgResId = R.drawable.circle_background_light;
				} else if (toolbarRes == R.layout.profile_preference_toolbar_big) {
					bgResId = R.drawable.rectangle_rounded;
				}
				Drawable bgDrawable = getPaintedIcon(bgResId, bgColor);
				AndroidUtils.setBackground(profileButton, bgDrawable);
			}
		}
	}

	private void updatePreferencesScreen() {
		if (getSelectedAppMode() != null) {
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

	public void updateAllSettings() {
		PreferenceScreen screen = getPreferenceScreen();
		if (screen != null) {
			getPreferenceScreen().removeAll();
		}
		updatePreferencesScreen();
	}

	public boolean shouldDismissOnChange() {
		return false;
	}

	public void dismiss() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			activity.getSupportFragmentManager().popBackStack();
		}
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

	@ColorInt
	protected int getActiveProfileColor() {
		return ContextCompat.getColor(app, getActiveProfileColorRes());
	}

	@ColorRes
	protected int getActiveProfileColorRes() {
		return getSelectedAppMode().getIconColorInfo().getColor(isNightMode());
	}

	@ColorRes
	protected int getBackgroundColorRes() {
		return isNightMode() ? R.color.list_background_color_dark : R.color.list_background_color_light;
	}

	protected void registerPreference(Preference preference) {
		if (preference != null) {
			preference.setOnPreferenceChangeListener(this);
			preference.setOnPreferenceClickListener(this);

			if (preference instanceof ListPreference) {
				ListPreference listPreference = (ListPreference) preference;
				assert listPreference.getEntryValues().length == listPreference.getEntries().length;
			}
		}
	}

	public ApplicationMode getSelectedAppMode() {
		return settings.APPLICATION_MODE.get();
	}

	public boolean isNightMode() {
		return nightMode;
	}

	@Nullable
	public MapActivity getMapActivity() {
		FragmentActivity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		} else {
			return null;
		}
	}

	@Nullable
	protected OsmandApplication getMyApplication() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			return (OsmandApplication) activity.getApplication();
		} else {
			return null;
		}
	}

	@NonNull
	protected OsmandApplication requireMyApplication() {
		FragmentActivity activity = requireActivity();
		return (OsmandApplication) activity.getApplication();
	}

	@Nullable
	protected OsmandActionBarActivity getMyActivity() {
		return (OsmandActionBarActivity) getActivity();
	}

	@NonNull
	protected OsmandActionBarActivity requireMyActivity() {
		return (OsmandActionBarActivity) requireActivity();
	}

	@Nullable
	protected OsmandInAppPurchaseActivity getInAppPurchaseActivity() {
		Activity activity = getActivity();
		if (activity instanceof OsmandInAppPurchaseActivity) {
			return (OsmandInAppPurchaseActivity) getActivity();
		} else {
			return null;
		}
	}

	@Nullable
	protected UiUtilities getIconsCache() {
		OsmandApplication app = getMyApplication();
		if (iconsCache == null && app != null) {
			iconsCache = app.getUIUtilities();
		}
		return iconsCache;
	}

	@Nullable
	protected OsmandSettings getSettings() {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			return app.getSettings();
		} else {
			return null;
		}
	}

	@NonNull
	protected OsmandSettings requireSettings() {
		OsmandApplication app = requireMyApplication();
		return app.getSettings();
	}

	protected Drawable getIcon(@DrawableRes int id) {
		UiUtilities cache = getIconsCache();
		return cache != null ? cache.getIcon(id) : null;
	}

	protected Drawable getActiveIcon(@DrawableRes int id) {
		UiUtilities cache = getIconsCache();
		return cache != null ? cache.getIcon(id, getActiveProfileColorRes()) : null;
	}

	protected Drawable getIcon(@DrawableRes int id, @ColorRes int colorId) {
		UiUtilities cache = getIconsCache();
		return cache != null ? cache.getIcon(id, colorId) : null;
	}

	protected Drawable getContentIcon(@DrawableRes int id) {
		UiUtilities cache = getIconsCache();
		return cache != null ? cache.getThemedIcon(id) : null;
	}

	protected Drawable getPaintedIcon(@DrawableRes int id, @ColorInt int color) {
		UiUtilities cache = getIconsCache();
		return cache != null ? cache.getPaintedIcon(id, color) : null;
	}

	public SwitchPreferenceCompat createSwitchPreference(OsmandSettings.OsmandPreference<Boolean> b, int title, int summary, int layoutId) {
		return createSwitchPreference(b, getString(title), getString(summary), layoutId);
	}

	public SwitchPreferenceCompat createSwitchPreference(OsmandSettings.OsmandPreference<Boolean> b, String title, String summary, int layoutId) {
		SwitchPreferenceCompat p = new SwitchPreferenceCompat(getContext());
		p.setTitle(title);
		p.setKey(b.getId());
		p.setSummary(summary);
		p.setLayoutResource(layoutId);
		return p;
	}

	public SwitchPreferenceEx createSwitchPreferenceEx(String prefId, int title, int layoutId) {
		return createSwitchPreferenceEx(prefId, getString(title), null, layoutId);
	}

	public SwitchPreferenceEx createSwitchPreferenceEx(String prefId, String title, String summary, int layoutId) {
		SwitchPreferenceEx p = new SwitchPreferenceEx(getContext());
		p.setKey(prefId);
		p.setTitle(title);
		p.setSummary(summary);
		p.setLayoutResource(layoutId);
		return p;
	}

	public ListPreferenceEx createListPreferenceEx(String prefId, String[] names, Object[] values, int title, int layoutId) {
		return createListPreferenceEx(prefId, names, values, getString(title), layoutId);
	}

	public ListPreferenceEx createListPreferenceEx(String prefId, String[] names, Object[] values, String title, int layoutId) {
		ListPreferenceEx listPreference = new ListPreferenceEx(getContext());
		listPreference.setKey(prefId);
		listPreference.setTitle(title);
		listPreference.setDialogTitle(title);
		listPreference.setEntries(names);
		listPreference.setEntryValues(values);

		if (layoutId != 0) {
			listPreference.setLayoutResource(layoutId);
		}

		return listPreference;
	}

	public static String getAppModeDescription(Context ctx, ApplicationMode mode) {
		String description;
		if (mode.isCustomProfile()) {
			description = String.format(ctx.getString(R.string.profile_type_descr_string),
					Algorithms.capitalizeFirstLetterAndLowercase(mode.getParent().toHumanString(ctx)));
		} else {
			description = ctx.getString(R.string.profile_type_base_string);
		}

		return description;
	}

	public static boolean showInstance(FragmentActivity activity, SettingsScreenType screenType) {
		try {
			Fragment fragment = Fragment.instantiate(activity, screenType.fragmentName);

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
}
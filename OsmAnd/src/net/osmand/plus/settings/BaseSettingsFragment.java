package net.osmand.plus.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.XmlRes;
import android.support.design.widget.AppBarLayout;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceGroupAdapter;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
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
import net.osmand.plus.settings.bottomsheets.BooleanPreferenceBottomSheet;
import net.osmand.plus.settings.bottomsheets.EditTextPreferenceBottomSheet;
import net.osmand.plus.settings.bottomsheets.MultiSelectPreferencesBottomSheet;
import net.osmand.plus.settings.bottomsheets.SingleSelectPreferenceBottomSheet;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.MultiSelectBooleanPreference;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

import org.apache.commons.logging.Log;

public abstract class BaseSettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener, SelectAppModesBottomSheetDialogFragment.AppModeChangedListener {

	private final Log log = PlatformUtil.getLog(this.getClass());

	protected OsmandApplication app;
	protected OsmandSettings settings;
	protected UiUtilities iconsCache;

	private boolean nightMode;
	private boolean wasDrawerDisabled;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		app = requireMyApplication();
		settings = app.getSettings();
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		getPreferenceManager().setPreferenceDataStore(settings.getDataStore());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		nightMode = !settings.isLightContent();

		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			AndroidUtils.addStatusBarPadding21v(getContext(), view);
			createToolbar(inflater, view);
			setDivider(null);
			updateAllSettings();
		}

		return view;
	}

	@Override
	public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		Context themedContext = new ContextThemeWrapper(getActivity(), themeRes);
		LayoutInflater themedInflater = inflater.cloneInContext(themedContext);

		return super.onCreateRecyclerView(themedInflater, parent, savedInstanceState);
	}

	@SuppressLint("RestrictedApi")
	@Override
	protected RecyclerView.Adapter onCreateAdapter(PreferenceScreen preferenceScreen) {

		return new PreferenceGroupAdapter(preferenceScreen) {

			@Override
			public void onBindViewHolder(PreferenceViewHolder holder, int position) {
				super.onBindViewHolder(holder, position);

				if (BaseSettingsFragment.this.getClass().equals(ConfigureProfileFragment.class)) {
					View selectableView = holder.itemView.findViewById(R.id.selectable_list_item);
					if (selectableView != null) {
						Drawable selectableBg = selectableView.getBackground();

						int color = ContextCompat.getColor(app, getActiveProfileColor());
						int colorWithAlpha;
						if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP && selectableBg instanceof RippleDrawable) {
							colorWithAlpha = UiUtilities.getColorWithAlpha(color, 0.4f);
						}else {
							colorWithAlpha = UiUtilities.getColorWithAlpha(color, 0.2f);

						}
						if (selectableBg != null) {
							Drawable drawable = app.getUIUtilities().setRippleColor(selectableBg, colorWithAlpha);

							if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
								selectableView.setBackground(drawable);
							} else {
								selectableView.setBackgroundDrawable(drawable);
							}
							selectableView.invalidate();
						}
					}
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
	}

	@Override
	public void onPause() {
		super.onPause();
		MapActivity mapActivity = getMapActivity();
		if (!wasDrawerDisabled && mapActivity != null) {
			mapActivity.enableDrawer();
		}
	}

	private void createToolbar(LayoutInflater inflater, View view) {
		AppBarLayout appBarLayout = (AppBarLayout) view.findViewById(R.id.appbar);

		int toolbarRes = getToolbarResId();
		if (toolbarRes != -1) {
			Context activityContext = getActivity();

			final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
			final Context themedContext = new ContextThemeWrapper(activityContext, themeRes);
			LayoutInflater themedInflater = LayoutInflater.from(themedContext);

			View toolbarContainer = themedInflater.inflate(toolbarRes, null);
			appBarLayout.addView(toolbarContainer);

			view.findViewById(R.id.close_button).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					MapActivity mapActivity = getMapActivity();
					if (mapActivity != null) {
						mapActivity.onBackPressed();
					}
				}
			});
			View switchProfile = view.findViewById(R.id.switch_profile_button);
			if (switchProfile != null) {

				if (this.getClass().equals(ConfigureProfileFragment.class)) {
					int drawableId;
					if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
						drawableId = nightMode ? R.drawable.ripple_dark : R.drawable.ripple_light;
					} else {
						drawableId = nightMode ? R.drawable.btn_border_trans_dark : R.drawable.btn_border_trans_light;
					}

					int color = ContextCompat.getColor(app, getActiveProfileColor());
					int colorWithAlpha = UiUtilities.getColorWithAlpha(color, 0.40f);

					Drawable drawable = app.getUIUtilities().setRippleColor(drawableId, colorWithAlpha);

					if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
						switchProfile.setBackground(drawable);
					} else {
						switchProfile.setBackgroundDrawable(drawable);
					}
				} else {
					if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
						AndroidUtils.setBackground(app, switchProfile, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
					} else {
						AndroidUtils.setBackground(app, switchProfile, nightMode, R.drawable.btn_border_trans_light, R.drawable.btn_border_trans_dark);
					}
				}

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
			updateToolbar(view);
		}
	}

	private void updateToolbar(View view) {
		if (view == null) {
			return;
		}

		ApplicationMode selectedAppMode = getSelectedAppMode();
		int iconRes = selectedAppMode.getIconRes();
		int iconColor = selectedAppMode.getIconColorInfo().getColor(nightMode);
		String title = selectedAppMode.isCustomProfile() ? selectedAppMode.getCustomProfileName() : getResources().getString(selectedAppMode.getNameKeyResource());

		TextView toolbarTitle = (TextView) view.findViewById(R.id.toolbar_title);
		if (toolbarTitle != null) {
			toolbarTitle.setText(getToolbarTitle());
		}
		ImageView profileIcon = (ImageView) view.findViewById(R.id.profile_icon);
		if (profileIcon != null) {
			profileIcon.setImageDrawable(getIcon(iconRes, iconColor));
		}
		TextView profileTitle = (TextView) view.findViewById(R.id.profile_title);
		if (profileTitle != null) {
			profileTitle.setText(title);
		}
		TextView profileType = (TextView) view.findViewById(R.id.profile_type);
		if (profileType != null) {
			profileType.setVisibility(View.GONE);
		}
		View toolbarDivider = view.findViewById(R.id.toolbar_divider);
		if (toolbarDivider != null) {
			toolbarDivider.setBackgroundColor(ContextCompat.getColor(app, iconColor));
		}
		view.setBackgroundColor(ContextCompat.getColor(app, getBackgroundColor()));
	}

	protected abstract void setupPreferences();

	private void updatePreferencesScreen() {
		if (getSelectedAppMode() != null) {
			int resId = getPreferencesResId();
			if (resId != -1) {
				addPreferencesFromResource(getPreferencesResId());
				setupPreferences();
				registerPreferences();
			}
		}
	}

	private void registerPreferences() {
		PreferenceScreen screen = getPreferenceScreen();
		if (screen != null) {
			for (int i = 0; i < screen.getPreferenceCount(); i++) {
				Preference preference = screen.getPreference(i);
				registerPreference(preference);
			}
		}
	}

	public void updateAllSettings() {
		PreferenceScreen screen = getPreferenceScreen();
		if (screen != null) {
			getPreferenceScreen().removeAll();
		}
		updatePreferencesScreen();
		updateToolbar(getView());
	}

	@XmlRes
	protected int getPreferencesResId() {
		return -1;
	}

	@LayoutRes
	protected int getToolbarResId() {
		return -1;
	}

	protected String getToolbarTitle() {
		return getString(R.string.shared_string_settings);
	}

	@ColorRes
	protected int getStatusBarColorId() {
		View view = getView();
		if (view != null) {
			if (Build.VERSION.SDK_INT >= 23 && !isNightMode()) {
				view.setSystemUiVisibility(view.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
			}
			return isNightMode() ? R.color.list_background_color_dark : R.color.list_background_color_light;
		}
		return -1;
	}

	@ColorRes
	protected int getActiveProfileColor() {
		return getSelectedAppMode().getIconColorInfo().getColor(isNightMode());
	}

	@ColorRes
	protected int getBackgroundColor() {
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

	protected Drawable getIcon(@DrawableRes int id) {
		UiUtilities cache = getIconsCache();
		return cache != null ? cache.getIcon(id) : null;
	}

	protected Drawable getActiveIcon(@DrawableRes int id) {
		UiUtilities cache = getIconsCache();
		return cache != null ? cache.getIcon(id, getActiveProfileColor()) : null;
	}

	protected Drawable getIcon(@DrawableRes int id, @ColorRes int colorId) {
		UiUtilities cache = getIconsCache();
		return cache != null ? cache.getIcon(id, colorId) : null;
	}

	protected Drawable getContentIcon(@DrawableRes int id) {
		UiUtilities cache = getIconsCache();
		return cache != null ? cache.getThemedIcon(id) : null;
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

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		return true;
	}

	@Override
	public void onDisplayPreferenceDialog(Preference preference) {
		if (preference instanceof ListPreferenceEx) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				SingleSelectPreferenceBottomSheet.showInstance(fragmentManager, preference.getKey(), this);
			}
		} else if (preference instanceof SwitchPreferenceEx) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				BooleanPreferenceBottomSheet.showInstance(fragmentManager, preference.getKey(), this);
			}
		} else if (preference instanceof EditTextPreference) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				EditTextPreferenceBottomSheet.showInstance(fragmentManager, preference.getKey(), this);
			}
		} else if (preference instanceof MultiSelectBooleanPreference) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				MultiSelectPreferencesBottomSheet.showInstance(getFragmentManager(), preference.getKey(), this);
			}
		} else {
			super.onDisplayPreferenceDialog(preference);
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		return false;
	}

	@Override
	public void onAppModeChanged() {
		updateAllSettings();
	}

	public SwitchPreference createSwitchPreference(OsmandSettings.OsmandPreference<Boolean> b, int title, int summary, int layoutId) {
		return createSwitchPreference(b, getString(title), getString(summary), layoutId);
	}

	public SwitchPreference createSwitchPreference(OsmandSettings.OsmandPreference<Boolean> b, String title, String summary, int layoutId) {
		SwitchPreference p = new SwitchPreference(getContext());
		p.setTitle(title);
		p.setKey(b.getId());
		p.setSummary(summary);
		p.setLayoutResource(layoutId);
		return p;
	}

	public SwitchPreferenceEx createSwitchPreferenceEx(String prefId, int title, int layoutId) {
		return createSwitchPreferenceEx(prefId, getString(title), null, layoutId);
	}

	public SwitchPreferenceEx createSwitchPreferenceEx(String prefId, int title, int summary, int layoutId) {
		return createSwitchPreferenceEx(prefId, getString(title), getString(summary), layoutId);
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
}
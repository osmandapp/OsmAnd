package net.osmand.plus.settings;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.XmlRes;
import android.support.design.widget.AppBarLayout;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.activities.OsmandInAppPurchaseActivity;
import net.osmand.plus.profiles.AppProfileArrayAdapter;
import net.osmand.plus.profiles.ProfileDataObject;
import net.osmand.plus.views.SwitchFragmentPreference;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static net.osmand.plus.profiles.SettingsProfileFragment.PROFILE_STRING_KEY;

public abstract class BaseProfileSettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

	protected OsmandApplication app;
	protected OsmandSettings settings;
	protected UiUtilities iconsCache;
	protected List<ApplicationMode> modes = new ArrayList<ApplicationMode>();
	protected ApplicationMode selectedAppMode = null;

	private boolean nightMode;
	private boolean wasDrawerDisabled;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		app = requireMyApplication();
		settings = app.getSettings();
		nightMode = !settings.isLightContent();
		String modeName = "";
		if (getArguments() != null) {
			modeName = getArguments().getString(PROFILE_STRING_KEY, ApplicationMode.CAR.getStringKey());
		}
		selectedAppMode = ApplicationMode.valueOfStringKey(modeName, ApplicationMode.DEFAULT);
		settings.APPLICATION_MODE.set(selectedAppMode);
		super.onCreate(savedInstanceState);
		modes.clear();
		for (ApplicationMode a : ApplicationMode.values(app)) {
			if (a != ApplicationMode.DEFAULT) {
				modes.add(a);
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		if (view != null) {
			AndroidUtils.addStatusBarPadding21v(getContext(), view);
			createToolbar(inflater, view);
		}

		return view;
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
			View toolbarContainer = inflater.inflate(toolbarRes, null);
			appBarLayout.addView(toolbarContainer);

			view.findViewById(R.id.close_button).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					getActivity().getSupportFragmentManager().popBackStack();
				}
			});
			view.findViewById(R.id.switch_profile_button).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					selectAppModeDialog().show();
				}
			});
			updateToolbar(view);
		}
	}

	private void updateToolbar(View view) {
		if (view == null) {
			return;
		}

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
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		updatePreferencesScreen();
	}

	private void updatePreferencesScreen() {
		if (selectedAppMode != null) {
			String sharedPreferencesName = OsmandSettings.getSharedPreferencesName(selectedAppMode);
			getPreferenceManager().setSharedPreferencesName(sharedPreferencesName);
			int resId = getPreferenceResId();
			if (resId != -1) {
				addPreferencesFromResource(getPreferenceResId());
			}
			createUI();
		}
	}

	protected abstract void createUI();

	@XmlRes
	protected int getPreferenceResId() {
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
			if (Build.VERSION.SDK_INT >= 23 && !nightMode) {
				view.setSystemUiVisibility(view.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
			}
			return nightMode ? R.color.list_background_color_dark : R.color.list_background_color_light;
		}
		return -1;
	}

	protected Preference findAndRegisterPreference(String key) {
		Preference preference = getPreferenceScreen().findPreference(key);
		registerPreference(preference);
		return preference;
	}

	protected void registerPreference(Preference preference) {
		if (preference != null) {
			preference.setOnPreferenceChangeListener(this);
		}
	}

	public ApplicationMode getSelectedMode() {
		return selectedAppMode;
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

	protected Drawable getPaintedContentIcon(@DrawableRes int id, @ColorInt int color) {
		UiUtilities cache = getIconsCache();
		return cache != null ? cache.getPaintedIcon(id, color) : null;
	}

	protected Drawable getIcon(@DrawableRes int id, @ColorRes int colorId) {
		UiUtilities cache = getIconsCache();
		return cache != null ? cache.getIcon(id, colorId) : null;
	}

	protected Drawable getIcon(@DrawableRes int id) {
		UiUtilities cache = getIconsCache();
		return cache != null ? cache.getIcon(id) : null;
	}

	protected Drawable getContentIcon(@DrawableRes int id) {
		UiUtilities cache = getIconsCache();
		return cache != null ? cache.getThemedIcon(id) : null;
	}

	protected void setThemedDrawable(View view, @DrawableRes int iconId) {
		((ImageView) view).setImageDrawable(getContentIcon(iconId));
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

//	------------------------------------------------------------------------------------------------

	public SwitchPreference createSwitchPreference(OsmandSettings.OsmandPreference<Boolean> b, int title, int summary) {
		SwitchPreference p = new SwitchPreference(getContext());
		p.setTitle(title);
		p.setKey(b.getId());
		p.setSummary(summary);
		p.setOnPreferenceChangeListener(this);
		return p;
	}

	public SwitchPreference createSwitchFragmentPreference(OsmandSettings.OsmandPreference<Boolean> b, int title, int summary) {
		SwitchFragmentPreference p = new SwitchFragmentPreference(getContext());
		p.setTitle(title);
		p.setKey(b.getId());
		p.setSummary(summary);
		p.setOnPreferenceChangeListener(this);
		return p;
	}

	public SwitchPreference createSwitchPreference(OsmandSettings.OsmandPreference<Boolean> b) {
		SwitchPreference p = new SwitchPreference(getContext());
		p.setKey(b.getId());
		p.setOnPreferenceChangeListener(this);
		p.setSelectable(true);
		return p;
	}

	public <T> ListPreference createListPreference(OsmandSettings.OsmandPreference<T> b, String[] names, T[] values, String title, String summary) {
		ListPreference p = new ListPreference(getContext());
		p.setTitle(title);
		p.setKey(b.getId());
		p.setDialogTitle(title);
		p.setSummary(summary);
		p.setOnPreferenceChangeListener(this);
		prepareListPreference(b, names, values, p);
		return p;
	}

	private <T> void prepareListPreference(OsmandSettings.OsmandPreference<T> b, String[] names, T[] values, ListPreference p) {
		p.setOnPreferenceChangeListener(this);
		LinkedHashMap<String, Object> vals = new LinkedHashMap<String, Object>();
		assert names.length == values.length;
		for (int i = 0; i < names.length; i++) {
			vals.put(names[i], values[i]);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
			case android.R.id.home:
//				finish();
				return true;

		}
		return false;
	}

	protected AlertDialog.Builder selectAppModeDialog() {
		AlertDialog.Builder singleSelectDialogBuilder = new AlertDialog.Builder(getContext());
		singleSelectDialogBuilder.setTitle(R.string.profile_settings);

		final List<ProfileDataObject> activeModes = new ArrayList<>();
		for (ApplicationMode am : ApplicationMode.values(getMyApplication())) {
			boolean isSelected = false;
			if (am == selectedAppMode) {
				isSelected = true;
			}
			if (am != ApplicationMode.DEFAULT) {
				activeModes.add(new ProfileDataObject(
						am.toHumanString(getMyApplication()),
						getAppModeDescription(am),
						am.getStringKey(),
						am.getIconRes(),
						isSelected,
						am.getIconColorInfo()
				));
			}
		}

		final AppProfileArrayAdapter modeNames = new AppProfileArrayAdapter(
				getActivity(), R.layout.bottom_sheet_item_with_descr_and_radio_btn, activeModes, true);

		singleSelectDialogBuilder.setNegativeButton(R.string.shared_string_cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		singleSelectDialogBuilder.setAdapter(modeNames, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				selectedAppMode = modes.get(which);
				requireSettings().APPLICATION_MODE.set(selectedAppMode);
				updateAllSettings();
			}
		});
		return singleSelectDialogBuilder;
	}

	private String getAppModeDescription(ApplicationMode mode) {
		String descr;
		if (!mode.isCustomProfile()) {
			descr = getString(R.string.profile_type_base_string);
		} else {
			descr = String.format(getString(R.string.profile_type_descr_string),
					mode.getParent().toHumanString(getMyApplication()));
			if (mode.getRoutingProfile() != null && mode.getRoutingProfile().contains("/")) {
				descr = descr.concat(", " + mode.getRoutingProfile()
						.substring(0, mode.getRoutingProfile().indexOf("/")));
			}
		}
		return descr;
	}

	public void updateAllSettings() {
		updateToolbar(getView());
		getPreferenceScreen().removeAll();
		updatePreferencesScreen();
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String key = preference.getKey();
		return settings.setPreference(key, newValue);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		return false;
	}
}
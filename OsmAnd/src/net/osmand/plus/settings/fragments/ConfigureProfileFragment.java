package net.osmand.plus.settings.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceGroupAdapter;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.development.OsmandDevelopmentPlugin;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.openseamapsplugin.NauticalMapsPlugin;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet.CopyAppModePrefsListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsHelper.SettingsCollectListener;
import net.osmand.plus.settings.backend.backup.SettingsItem;
import net.osmand.plus.settings.bottomsheets.ResetProfilePrefsBottomSheet;
import net.osmand.plus.settings.bottomsheets.ResetProfilePrefsBottomSheet.ResetAppModePrefsListener;
import net.osmand.plus.skimapsplugin.SkiMapsPlugin;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static net.osmand.plus.UiUtilities.CompoundButtonType.TOOLBAR;

public class ConfigureProfileFragment extends BaseSettingsFragment implements CopyAppModePrefsListener, ResetAppModePrefsListener {

	public static final String TAG = ConfigureProfileFragment.class.getSimpleName();

	private static final Log LOG = PlatformUtil.getLog(ConfigureProfileFragment.class);

	private static final String PLUGIN_SETTINGS = "plugin_settings";
	private static final String SETTINGS_ACTIONS = "settings_actions";
	private static final String CONFIGURE_MAP = "configure_map";
	private static final String CONFIGURE_SCREEN = "configure_screen";
	private static final String COPY_PROFILE_SETTINGS = "copy_profile_settings";
	private static final String RESET_TO_DEFAULT = "reset_to_default";
	private static final String EXPORT_PROFILE = "export_profile";
	private static final String DELETE_PROFILE = "delete_profile";
	private static final String PROFILE_APPEARANCE = "profile_appearance";
	private static final String UI_CUSTOMIZATION = "ui_customization";

	@ColorRes
	protected int getBackgroundColorRes() {
		return isNightMode() ? R.color.activity_background_color_dark : R.color.activity_background_color_light;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		getListView().addItemDecoration(createDividerItemDecoration());

		return view;
	}

	@Override
	protected void createToolbar(LayoutInflater inflater, View view) {
		super.createToolbar(inflater, view);

		TextView toolbarTitle = view.findViewById(R.id.toolbar_title);
		toolbarTitle.setTypeface(FontCache.getRobotoMedium(view.getContext()));
		toolbarTitle.setText(getSelectedAppMode().toHumanString());
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			float letterSpacing = AndroidUtils.getFloatValueFromRes(view.getContext(), R.dimen.title_letter_spacing);
			toolbarTitle.setLetterSpacing(letterSpacing);
		}

		TextView toolbarSubtitle = view.findViewById(R.id.toolbar_subtitle);
		toolbarSubtitle.setText(R.string.configure_profile);
		toolbarSubtitle.setVisibility(View.VISIBLE);

		view.findViewById(R.id.toolbar_switch_container).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				ApplicationMode selectedMode = getSelectedAppMode();
				List<ApplicationMode> availableAppModes = ApplicationMode.values(getMyApplication());
				boolean isChecked = availableAppModes.contains(selectedMode);
				ApplicationMode.changeProfileAvailability(selectedMode, !isChecked, getMyApplication());
				updateToolbarSwitch();
			}
		});

		View switchProfile = view.findViewById(R.id.profile_button);
		if (switchProfile != null) {
			switchProfile.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onPause() {
		updateRouteInfoMenu();
		super.onPause();
	}

	private void updateToolbarSwitch() {
		View view = getView();
		if (view == null) {
			return;
		}
		boolean isChecked = ApplicationMode.values(getMyApplication()).contains(getSelectedAppMode());
		int color = isChecked ? getActiveProfileColor() : ContextCompat.getColor(app, R.color.preference_top_switch_off);
		View switchContainer = view.findViewById(R.id.toolbar_switch_container);
		AndroidUtils.setBackground(switchContainer, new ColorDrawable(color));

		SwitchCompat switchView = switchContainer.findViewById(R.id.switchWidget);
		switchView.setChecked(isChecked);
		UiUtilities.setupCompoundButton(switchView, isNightMode(), TOOLBAR);

		TextView title = switchContainer.findViewById(R.id.switchButtonText);
		title.setText(isChecked ? R.string.shared_string_on : R.string.shared_string_off);
	}

	@Override
	protected void updateToolbar() {
		super.updateToolbar();
		View view = getView();
		if (view != null) {
			updateToolbarSwitch();
			TextView toolbarTitle = view.findViewById(R.id.toolbar_title);
			toolbarTitle.setText(getSelectedAppMode().toHumanString());

			boolean visible = !getSelectedAppMode().equals(ApplicationMode.DEFAULT);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.switchWidget), visible);
		}
	}

	@Override
	public void copyAppModePrefs(ApplicationMode appMode) {
		if (appMode != null) {
			ApplicationMode selectedAppMode = getSelectedAppMode();
			app.getSettings().copyPreferencesFromProfile(appMode, selectedAppMode);
			updateCopiedOrResetPrefs();
		}
	}

	@Override
	public void resetAppModePrefs(ApplicationMode appMode) {
		if (appMode != null) {
			if (appMode.isCustomProfile()) {
				File file = getBackupFileForCustomMode(app, appMode.getStringKey());
				if (file.exists()) {
					restoreCustomModeFromFile(file);
				}
			} else {
				app.getSettings().resetPreferencesForProfile(appMode);
				app.showToastMessage(R.string.profile_prefs_reset_successful);
				updateCopiedOrResetPrefs();
			}
		}
	}

	private void restoreCustomModeFromFile(final File file) {
		app.getSettingsHelper().collectSettings(file, "", 1, new SettingsCollectListener() {
			@Override
			public void onSettingsCollectFinished(boolean succeed, boolean empty, @NonNull List<SettingsItem> items) {
				if (succeed) {
					for (SettingsItem item : items) {
						item.setShouldReplace(true);
					}
					importBackupSettingsItems(file, items);
				}
			}
		});
	}

	private void importBackupSettingsItems(File file, List<SettingsItem> items) {
		app.getSettingsHelper().importSettings(file, items, "", 1, new SettingsHelper.SettingsImportListener() {
			@Override
			public void onSettingsImportFinished(boolean succeed, boolean needRestart, @NonNull List<SettingsItem> items) {
				app.showToastMessage(R.string.profile_prefs_reset_successful);
				updateCopiedOrResetPrefs();
			}
		});
	}

	private void updateCopiedOrResetPrefs() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			app.getPoiFilters().loadSelectedPoiFilters();
			mapActivity.getMapLayers().getMapWidgetRegistry().updateVisibleWidgets();
			mapActivity.updateApplicationModeSettings();
			updateToolbar();
			updateAllSettings();
		}
	}

	private RecyclerView.ItemDecoration createDividerItemDecoration() {
		final Drawable dividerLight = new ColorDrawable(ContextCompat.getColor(app, R.color.list_background_color_light));
		final Drawable dividerDark = new ColorDrawable(ContextCompat.getColor(app, R.color.list_background_color_dark));
		final int pluginDividerHeight = AndroidUtils.dpToPx(app, 3);

		return new RecyclerView.ItemDecoration() {
			@Override
			public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
				int dividerLeft = parent.getPaddingLeft();
				int dividerRight = parent.getWidth() - parent.getPaddingRight();

				int childCount = parent.getChildCount();
				for (int i = 0; i < childCount - 1; i++) {
					View child = parent.getChildAt(i);

					if (shouldDrawDivider(child)) {
						RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

						int dividerTop = child.getBottom() + params.bottomMargin;
						int dividerBottom = dividerTop + pluginDividerHeight;

						Drawable divider = isNightMode() ? dividerDark : dividerLight;
						divider.setBounds(dividerLeft, dividerTop, dividerRight, dividerBottom);
						divider.draw(canvas);
					}
				}
			}

			@Override
			public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
				if (shouldDrawDivider(view)) {
					outRect.set(0, 0, 0, pluginDividerHeight);
				}
			}

			private boolean shouldDrawDivider(View view) {
				int position = getListView().getChildAdapterPosition(view);
				Preference pref = ((PreferenceGroupAdapter) getListView().getAdapter()).getItem(position);
				if (pref != null && pref.getParent() != null) {
					PreferenceGroup preferenceGroup = pref.getParent();
					return preferenceGroup.hasKey() && preferenceGroup.getKey().equals(PLUGIN_SETTINGS);
				}
				return false;
			}
		};
	}

	@Override
	protected void setupPreferences() {
		Preference generalSettings = findPreference("general_settings");
		generalSettings.setIcon(getContentIcon(R.drawable.ic_action_settings));

		setupNavigationSettingsPref();
		setupConfigureMapPref();
		setupConfigureScreenPref();
		setupProfileAppearancePref();
		setupUiCustomizationPref();

		PreferenceCategory pluginSettings = (PreferenceCategory) findPreference(PLUGIN_SETTINGS);
		setupOsmandPluginsPref(pluginSettings);

		PreferenceCategory settingsActions = (PreferenceCategory) findPreference(SETTINGS_ACTIONS);
		settingsActions.setIconSpaceReserved(false);

		setupCopyProfileSettingsPref();
		setupResetToDefaultPref();
		setupExportProfilePref();
		setupDeleteProfilePref();
	}

	private void setupNavigationSettingsPref() {
		Preference navigationSettings = findPreference("navigation_settings");
		navigationSettings.setIcon(getContentIcon(R.drawable.ic_action_gdirections_dark));
		navigationSettings.setVisible(!getSelectedAppMode().isDerivedRoutingFrom(ApplicationMode.DEFAULT));
	}

	private void setupConfigureMapPref() {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}
		Preference configureMap = findPreference(CONFIGURE_MAP);
		configureMap.setIcon(getContentIcon(R.drawable.ic_action_layers));

		Intent intent = new Intent(ctx, MapActivity.class);
		intent.putExtra(OPEN_CONFIG_ON_MAP, MAP_CONFIG);
		intent.putExtra(APP_MODE_KEY, getSelectedAppMode().getStringKey());
		configureMap.setIntent(intent);
	}

	private void setupConfigureScreenPref() {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}
		Preference configureMap = findPreference(CONFIGURE_SCREEN);
		configureMap.setIcon(getContentIcon(R.drawable.ic_configure_screen_dark));

		Intent intent = new Intent(ctx, MapActivity.class);
		intent.putExtra(OPEN_CONFIG_ON_MAP, SCREEN_CONFIG);
		intent.putExtra(APP_MODE_KEY, getSelectedAppMode().getStringKey());
		configureMap.setIntent(intent);
	}

	private void setupProfileAppearancePref() {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}
		Preference configureMap = findPreference(PROFILE_APPEARANCE);
		configureMap.setIcon(getContentIcon(getSelectedAppMode().getIconRes()));
		configureMap.setFragment(ProfileAppearanceFragment.TAG);
	}

	private void setupCopyProfileSettingsPref() {
		Preference copyProfilePrefs = findPreference(COPY_PROFILE_SETTINGS);
		copyProfilePrefs.setIcon(app.getUIUtilities().getIcon(R.drawable.ic_action_copy,
				isNightMode() ? R.color.active_color_primary_dark : R.color.active_color_primary_light));
	}

	private void setupResetToDefaultPref() {
		Preference resetToDefault = findPreference(RESET_TO_DEFAULT);
		ApplicationMode mode = getSelectedAppMode();
		if (mode.isCustomProfile() && !getBackupFileForCustomMode(app, mode.getStringKey()).exists()) {
			resetToDefault.setVisible(false);
		} else {
			OsmandDevelopmentPlugin plugin = OsmandPlugin.getEnabledPlugin(OsmandDevelopmentPlugin.class);
			if (plugin != null && mode.getParent() != null) {
				String baseProfile = "(" + mode.getParent().toHumanString() + ")";
				String title = getString(R.string.ltr_or_rtl_combine_via_space, getString(R.string.reset_to_default), baseProfile);
				resetToDefault.setTitle(title);
			}
			resetToDefault.setIcon(app.getUIUtilities().getIcon(R.drawable.ic_action_reset_to_default_dark,
					isNightMode() ? R.color.active_color_primary_dark : R.color.active_color_primary_light));
		}
	}

	private void setupExportProfilePref() {
		Preference exportProfile = findPreference(EXPORT_PROFILE);
		exportProfile.setIcon(app.getUIUtilities().getIcon(R.drawable.ic_action_app_configuration,
				isNightMode() ? R.color.active_color_primary_dark : R.color.active_color_primary_light));
	}

	private void setupDeleteProfilePref() {
		Preference deleteProfile = findPreference(DELETE_PROFILE);
		deleteProfile.setIcon(app.getUIUtilities().getIcon(R.drawable.ic_action_delete_dark,
				isNightMode() ? R.color.active_color_primary_dark : R.color.active_color_primary_light));
	}

	private void setupOsmandPluginsPref(PreferenceCategory preferenceCategory) {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}
		List<OsmandPlugin> plugins = OsmandPlugin.getVisiblePlugins();
		for (OsmandPlugin plugin : plugins) {
			if (plugin instanceof SkiMapsPlugin || plugin instanceof NauticalMapsPlugin || plugin.getSettingsScreenType() == null) {
				continue;
			}
			Preference preference = new Preference(ctx);
			preference.setPersistent(false);
			preference.setKey(plugin.getId());
			preference.setTitle(plugin.getName());
			preference.setSummary(plugin.getPrefsDescription());
			preference.setIcon(getContentIcon(plugin.getLogoResourceId()));
			preference.setLayoutResource(R.layout.preference_with_descr);
			preference.setFragment(plugin.getSettingsScreenType().fragmentName);

			preferenceCategory.addPreference(preference);
		}
	}

	private void setupUiCustomizationPref() {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}
		Preference uiCustomization = findPreference(UI_CUSTOMIZATION);
		if (uiCustomization != null) {
			uiCustomization.setIcon(getContentIcon(R.drawable.ic_action_ui_customization));
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String prefId = preference.getKey();

		if (CONFIGURE_MAP.equals(prefId) || CONFIGURE_SCREEN.equals(prefId)) {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				try {
					FragmentManager fragmentManager = activity.getSupportFragmentManager();
					if (fragmentManager != null) {
						ApplicationMode selectedMode = getSelectedAppMode();
						if (!ApplicationMode.values(app).contains(selectedMode)) {
							ApplicationMode.changeProfileAvailability(selectedMode, true, app);
						}
						settings.setApplicationMode(selectedMode);
						fragmentManager.beginTransaction()
								.remove(this)
								.addToBackStack(TAG)
								.commitAllowingStateLoss();
					}
				} catch (Exception e) {
					LOG.error(e);
				}
			}
		} else if (COPY_PROFILE_SETTINGS.equals(prefId)) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				SelectCopyAppModeBottomSheet.showInstance(fragmentManager, this, false, getSelectedAppMode());
			}
		} else if (RESET_TO_DEFAULT.equals(prefId)) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				ResetProfilePrefsBottomSheet.showInstance(fragmentManager, prefId, this, false, getSelectedAppMode());
			}
		} else if (EXPORT_PROFILE.equals(prefId)) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				ExportSettingsFragment.showInstance(fragmentManager, getSelectedAppMode(), false);
			}
		} else if (DELETE_PROFILE.equals(prefId)) {
			onDeleteProfileClick();
		} else if (UI_CUSTOMIZATION.equals(prefId)) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				ConfigureMenuRootFragment.showInstance(
						fragmentManager,
						this,
						getSelectedAppMode());
			}
		}
		return super.onPreferenceClick(preference);
	}

	private void onDeleteProfileClick() {
		final ApplicationMode profile = getSelectedAppMode();
		if (getActivity() != null) {
			if (profile.getParent() != null) {
				Context themedContext = UiUtilities.getThemedContext(getActivity(), isNightMode());
				AlertDialog.Builder bld = new AlertDialog.Builder(themedContext);
				bld.setTitle(R.string.profile_alert_delete_title);
				bld.setMessage(String
						.format(getString(R.string.profile_alert_delete_msg),
								profile.toHumanString()));
				bld.setPositiveButton(R.string.shared_string_delete,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								OsmandApplication app = getMyApplication();
								if (app != null) {
									ApplicationMode.deleteCustomModes(Collections.singletonList(profile), app);
								}

								if (getActivity() != null) {
									getActivity().onBackPressed();
								}
							}
						});
				bld.setNegativeButton(R.string.shared_string_dismiss, null);
				bld.show();
			} else {
				Toast.makeText(getActivity(), R.string.profile_alert_cant_delete_base,
						Toast.LENGTH_SHORT).show();
			}
		}
	}

	public static File getBackupFileForCustomMode(OsmandApplication app, String appModeKey) {
		String fileName = appModeKey + IndexConstants.OSMAND_SETTINGS_FILE_EXT;
		File backupDir = app.getAppPath(IndexConstants.BACKUP_INDEX_DIR);
		if (!backupDir.exists()) {
			backupDir.mkdirs();
		}

		return new File(backupDir, fileName);
	}
}
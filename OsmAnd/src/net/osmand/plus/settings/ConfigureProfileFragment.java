package net.osmand.plus.settings;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceGroupAdapter;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.aidl.OsmandAidlApi;
import net.osmand.aidl.ConnectedApp;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.SettingsHelper;
import net.osmand.plus.SettingsHelper.ProfileSettingsItem;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.PluginActivity;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.openseamapsplugin.NauticalMapsPlugin;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;
import net.osmand.plus.skimapsplugin.SkiMapsPlugin;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.List;

import static net.osmand.plus.profiles.EditProfileFragment.MAP_CONFIG;
import static net.osmand.plus.profiles.EditProfileFragment.OPEN_CONFIG_ON_MAP;
import static net.osmand.plus.profiles.EditProfileFragment.SCREEN_CONFIG;
import static net.osmand.plus.profiles.EditProfileFragment.SELECTED_ITEM;
import static net.osmand.plus.profiles.SettingsProfileFragment.PROFILE_STRING_KEY;

public class ConfigureProfileFragment extends BaseSettingsFragment {

	public static final String TAG = ConfigureProfileFragment.class.getSimpleName();

	private static final Log LOG = PlatformUtil.getLog(ConfigureProfileFragment.class);

	private static final String PLUGIN_SETTINGS = "plugin_settings";
	private static final String SETTINGS_ACTIONS = "settings_actions";
	private static final String CONFIGURE_MAP = "configure_map";
	private static final String CONFIGURE_SCREEN = "configure_screen";
	private static final String EXPORT_PROFILE = "export_profile";
	private static final String DELETE_PROFILE = "delete_profile";
	private static final String PROFILE_APPEARANCE = "profile_appearance";

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
		toolbarTitle.setText(getSelectedAppMode().toHumanString(getContext()));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			float letterSpacing = AndroidUtils.getFloatValueFromRes(view.getContext(), R.dimen.title_letter_spacing);
			toolbarTitle.setLetterSpacing(letterSpacing);
		}

		TextView toolbarSubtitle = view.findViewById(R.id.toolbar_subtitle);
		toolbarSubtitle.setText(R.string.configure_profile);
		toolbarSubtitle.setVisibility(View.VISIBLE);

		if (!getSelectedAppMode().equals(ApplicationMode.DEFAULT)) {
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
		} else {
			view.findViewById(R.id.switchWidget).setVisibility(View.GONE);
		}
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
		UiUtilities.setupCompoundButton(isNightMode(), getActiveProfileColor(), switchView);

		TextView title = switchContainer.findViewById(R.id.switchButtonText);
		title.setText(isChecked ? R.string.shared_string_on : R.string.shared_string_off);
	}

	@Override
	protected void updateToolbar() {
		super.updateToolbar();
		updateToolbarSwitch();
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

		PreferenceCategory pluginSettings = (PreferenceCategory) findPreference(PLUGIN_SETTINGS);
		pluginSettings.setIconSpaceReserved(false);

		setupConnectedAppsPref(pluginSettings);
		setupOsmandPluginsPref(pluginSettings);

		PreferenceCategory settingsActions = (PreferenceCategory) findPreference(SETTINGS_ACTIONS);
		settingsActions.setIconSpaceReserved(false);

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
		configureMap.setIcon(getContentIcon(R.drawable.ic_action_layers_dark));

		Intent intent = new Intent(ctx, MapActivity.class);
		intent.putExtra(OPEN_CONFIG_ON_MAP, MAP_CONFIG);
		intent.putExtra(SELECTED_ITEM, getSelectedAppMode().getStringKey());
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
		intent.putExtra(SELECTED_ITEM, getSelectedAppMode().getStringKey());
		configureMap.setIntent(intent);
	}

	private void setupProfileAppearancePref() {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}

		Preference configureMap = findPreference(PROFILE_APPEARANCE);
		if (!getSelectedAppMode().equals(ApplicationMode.DEFAULT)) {
			configureMap.setIcon(getContentIcon(getSelectedAppMode().getIconRes()));
			configureMap.setFragment(ProfileAppearanceFragment.TAG);
		} else {
			configureMap.setVisible(false);
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

	private void shareProfile(@NonNull File file, @NonNull ApplicationMode profile) {
		try {
			Context ctx = requireContext();
			final Intent sendIntent = new Intent();
			sendIntent.setAction(Intent.ACTION_SEND);
			sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.exported_osmand_profile, profile.toHumanString(ctx)));
			sendIntent.putExtra(Intent.EXTRA_STREAM, AndroidUtils.getUriForFile(getMyApplication(), file));
			sendIntent.setType("*/*");
			sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			startActivity(sendIntent);
		} catch (Exception e) {
			app.showToastMessage(R.string.export_profile_failed);
			LOG.error("Share profile error", e);
		}
	}

	private void setupConnectedAppsPref(PreferenceCategory preferenceCategory) {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}
		List<ConnectedApp> connectedApps = app.getAidlApi().getConnectedApps();
		for (ConnectedApp connectedApp : connectedApps) {
			SwitchPreferenceCompat preference = new SwitchPreferenceCompat(app);
			preference.setPersistent(false);
			preference.setKey(connectedApp.getPack());
			preference.setIcon(connectedApp.getIcon());
			preference.setTitle(connectedApp.getName());
			preference.setChecked(connectedApp.isEnabled());
			preference.setLayoutResource(R.layout.preference_switch);

			preferenceCategory.addPreference(preference);
		}
	}

	private void setupOsmandPluginsPref(PreferenceCategory preferenceCategory) {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}
		List<OsmandPlugin> plugins = OsmandPlugin.getVisiblePlugins();
		for (OsmandPlugin plugin : plugins) {
			if (plugin instanceof SkiMapsPlugin || plugin instanceof NauticalMapsPlugin) {
				continue;
			}
			SwitchPreferenceEx preference = new SwitchPreferenceEx(ctx);
			preference.setPersistent(false);
			preference.setKey(plugin.getId());
			preference.setTitle(plugin.getName());
			preference.setChecked(plugin.isActive());
			preference.setIcon(getPluginIcon(plugin));
			preference.setIntent(getPluginIntent(plugin));
			preference.setLayoutResource(R.layout.preference_dialog_and_switch);

			preferenceCategory.addPreference(preference);
		}
	}

	private Drawable getPluginIcon(OsmandPlugin plugin) {
		int iconResId = plugin.getLogoResourceId();
		return plugin.isActive() ? getActiveIcon(iconResId) : getIcon(iconResId, isNightMode() ? R.color.icon_color_secondary_dark : R.color.icon_color_secondary_light);
	}

	private Intent getPluginIntent(OsmandPlugin plugin) {
		Intent intent;
		final Class<? extends Activity> settingsActivity = plugin.getSettingsActivity();
		if (settingsActivity != null && !plugin.needsInstallation()) {
			intent = new Intent(getContext(), settingsActivity);
			intent.putExtra(PROFILE_STRING_KEY, getSelectedAppMode().getStringKey());
		} else {
			intent = new Intent(getContext(), PluginActivity.class);
			intent.putExtra(PluginActivity.EXTRA_PLUGIN_ID, plugin.getId());
		}
		return intent;
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
						settings.APPLICATION_MODE.set(selectedMode);
						fragmentManager.beginTransaction()
								.remove(this)
								.addToBackStack(TAG)
								.commitAllowingStateLoss();
					}
				} catch (Exception e) {
					LOG.error(e);
				}
			}
		} else if (EXPORT_PROFILE.equals(prefId)) {
			Context ctx = requireContext();
			final ApplicationMode profile = getSelectedAppMode();
			File tempDir = app.getAppPath(IndexConstants.TEMP_DIR);
			if (!tempDir.exists()) {
				tempDir.mkdirs();
			}
			String fileName = profile.toHumanString(ctx);
			app.getSettingsHelper().exportSettings(tempDir, fileName, new SettingsHelper.SettingsExportListener() {
				@Override
				public void onSettingsExportFinished(@NonNull File file, boolean succeed) {
					if (succeed) {
						shareProfile(file, profile);
					} else {
						app.showToastMessage(R.string.export_profile_failed);
					}
				}
			}, new ProfileSettingsItem(app.getSettings(), profile));
		} else if (DELETE_PROFILE.equals(prefId)) {
			onDeleteProfileClick();
		}
		return super.onPreferenceClick(preference);
	}

	void onDeleteProfileClick() {
		final ApplicationMode profile = getSelectedAppMode();
		if (getActivity() != null) {
			if (profile.getParent() != null) {
				Context themedContext = UiUtilities.getThemedContext(getActivity(), isNightMode());
				AlertDialog.Builder bld = new AlertDialog.Builder(themedContext);
				bld.setTitle(R.string.profile_alert_delete_title);
				bld.setMessage(String
						.format(getString(R.string.profile_alert_delete_msg),
								profile.getCustomProfileName()));
				bld.setPositiveButton(R.string.shared_string_delete,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								OsmandApplication app = getMyApplication();
								if (app != null) {
									ApplicationMode.deleteCustomMode(ApplicationMode.valueOfStringKey(profile.getStringKey(), ApplicationMode.DEFAULT), app);
									app.getSettings().APPLICATION_MODE.set(ApplicationMode.DEFAULT);
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

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String key = preference.getKey();

		OsmandPlugin plugin = OsmandPlugin.getPlugin(key);
		if (plugin != null) {
			if (newValue instanceof Boolean) {
				if ((plugin.isActive() || !plugin.needsInstallation())) {
					if (OsmandPlugin.enablePlugin(getActivity(), app, plugin, (Boolean) newValue)) {
						preference.setIcon(getPluginIcon(plugin));
						return true;
					}
				} else if (plugin.needsInstallation() && preference.getIntent() != null) {
					startActivity(preference.getIntent());
				}
			}
			return false;
		}

		OsmandAidlApi aidlApi = app.getAidlApi();
		ConnectedApp connectedApp = aidlApi.getConnectedApp(key);
		if (connectedApp != null) {
			return aidlApi.switchEnabled(connectedApp);
		}

		return super.onPreferenceChange(preference, newValue);
	}
}
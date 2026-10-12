package net.osmand.plus.settings.fragments;

import static net.osmand.plus.utils.UiUtilities.CompoundButtonType.TOOLBAR;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

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
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.backup.ui.BackupAuthorizationFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginInstalledBottomSheetDialog.PluginStateListener;
import net.osmand.plus.plugins.PluginsFragment;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet.CopyAppModePrefsListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ImportListener;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.settings.bottomsheets.ResetProfilePrefsBottomSheet;
import net.osmand.plus.settings.bottomsheets.ResetProfilePrefsBottomSheet.ResetAppModePrefsListener;
import net.osmand.plus.settings.fragments.configureitems.ConfigureMenuRootFragment;
import net.osmand.plus.settings.fragments.profileappearance.ProfileAppearanceFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.configure.dialogs.ConfigureScreenFragment;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class ConfigureProfileFragment extends BaseSettingsFragment implements CopyAppModePrefsListener,
		ResetAppModePrefsListener, PluginStateListener {

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
	private static final String FREE_FAVORITES_BACKUP_CARD = "free_favorites_backup_card";

	@ColorRes
	protected int getBackgroundColorRes() {
		return ColorUtilities.getActivityBgColorId(isNightMode());
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		getListView().addItemDecoration(createDividerItemDecoration());

		return view;
	}

	@Override
	protected void createToolbar(@NonNull LayoutInflater inflater, @NonNull View view) {
		super.createToolbar(inflater, view);

		TextView toolbarTitle = view.findViewById(R.id.toolbar_title);
		toolbarTitle.setTypeface(FontCache.getMediumFont());
		toolbarTitle.setText(getSelectedAppMode().toHumanString());
		float letterSpacing = AndroidUtils.getFloatValueFromRes(view.getContext(), R.dimen.title_letter_spacing);
		toolbarTitle.setLetterSpacing(letterSpacing);

		TextView toolbarSubtitle = view.findViewById(R.id.toolbar_subtitle);
		toolbarSubtitle.setText(R.string.configure_profile);
		toolbarSubtitle.setVisibility(View.VISIBLE);

		view.findViewById(R.id.toolbar_switch_container).setOnClickListener(view1 -> {
			ApplicationMode selectedMode = getSelectedAppMode();
			boolean isChecked = ApplicationMode.values(app).contains(selectedMode);
			ApplicationMode.changeProfileAvailability(selectedMode, !isChecked, getMyApplication());
			updateToolbarSwitch();
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
		boolean isChecked = ApplicationMode.values(app).contains(getSelectedAppMode());
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
	public void copyAppModePrefs(@NonNull ApplicationMode appMode) {
		ApplicationMode selectedAppMode = getSelectedAppMode();
		app.getSettings().copyPreferencesFromProfile(appMode, selectedAppMode);
		updateCopiedOrResetPrefs();
	}

	@Override
	public void resetAppModePrefs(ApplicationMode appMode) {
		if (appMode != null) {
			if (appMode.isCustomProfile()) {
				File file = FileUtils.getBackupFileForCustomAppMode(app, appMode.getStringKey());
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

	private void restoreCustomModeFromFile(@NonNull File file) {
		app.getFileSettingsHelper().collectSettings(file, "", 1, (succeed, empty, items) -> {
			if (succeed) {
				for (SettingsItem item : items) {
					item.setShouldReplace(true);
				}
				importBackupSettingsItems(file, items);
			}
		});
	}

	private void importBackupSettingsItems(File file, List<SettingsItem> items) {
		app.getFileSettingsHelper().importSettings(file, items, "", 1, new ImportListener() {
			@Override
			public void onImportFinished(boolean succeed, boolean needRestart, @NonNull List<SettingsItem> items) {
				app.showToastMessage(R.string.profile_prefs_reset_successful);
				updateCopiedOrResetPrefs();
			}
		});
	}

	private void updateCopiedOrResetPrefs() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.updateApplicationModeSettings();
			updateToolbar();
			updateAllSettings();
		}
	}

	private RecyclerView.ItemDecoration createDividerItemDecoration() {
		Drawable dividerLight = new ColorDrawable(ContextCompat.getColor(app, R.color.list_background_color_light));
		Drawable dividerDark = new ColorDrawable(ContextCompat.getColor(app, R.color.list_background_color_dark));
		int pluginDividerHeight = AndroidUtils.dpToPx(app, 3);

		return new RecyclerView.ItemDecoration() {
			@Override
			public void onDraw(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
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
			public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
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
	protected void onBindPreferenceViewHolder(@NonNull Preference preference, @NonNull PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);

		if (PLUGIN_SETTINGS.equals(preference.getKey())) {
			View noPluginsPart = holder.findViewById(R.id.no_plugins_part);
			boolean hasPlugins = PluginsHelper.getEnabledSettingsScreenPlugins().size() > 0;
			AndroidUiHelper.updateVisibility(noPluginsPart, !hasPlugins);

			DialogButton openPluginsButton = noPluginsPart.findViewById(R.id.open_plugins_button);
			if (!hasPlugins) {
				openPluginsButton.setOnClickListener(v -> {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						PluginsFragment.showInstance(activity.getSupportFragmentManager(), this);
					}
				});
			}
		} else if (FREE_FAVORITES_BACKUP_CARD.equals(preference.getKey())) {
			TextView title = (TextView) holder.findViewById(R.id.title);
			title.setText(R.string.free_settings_backup);
			ImageView closeBtn = (ImageView) holder.findViewById(R.id.btn_close);
			closeBtn.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_cancel, isNightMode()));
			ImageView icon = (ImageView) holder.findViewById(R.id.icon);
			icon.setImageResource(R.drawable.ic_action_settings_cloud_colored);
			closeBtn.setOnClickListener(v -> {
				preference.setVisible(false);
				app.getSettings().CONFIGURE_PROFILE_FREE_ACCOUNT_CARD_DISMISSED.set(true);
			});
			View getCloudBtnContainer = holder.findViewById(R.id.dismiss_button_container);
			getCloudBtnContainer.setOnClickListener(v -> {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					BackupAuthorizationFragment.showInstance(activity.getSupportFragmentManager());
				}
			});
		}
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
		setupOsmandPluginsPref();

		findPreference(SETTINGS_ACTIONS).setIconSpaceReserved(false);

		setupCopyProfileSettingsPref();
		setupResetToDefaultPref();
		setupExportProfilePref();
		setupDeleteProfilePref();
		setupFreeFavoritesBackupCard();
	}

	private void setupNavigationSettingsPref() {
		Preference navigationSettings = findPreference("navigation_settings");
		navigationSettings.setIcon(getContentIcon(R.drawable.ic_action_gdirections_dark));
		navigationSettings.setVisible(!getSelectedAppMode().isDerivedRoutingFrom(ApplicationMode.DEFAULT));
	}

	private void setupFreeFavoritesBackupCard() {
		boolean available = InAppPurchaseUtils.isBackupAvailable(app);
		boolean isRegistered = app.getBackupHelper().isRegistered();
		boolean shouldShowFreeBackupCard = !available && !isRegistered
				&& !app.getSettings().CONFIGURE_PROFILE_FREE_ACCOUNT_CARD_DISMISSED.get();
		Preference freeBackupCard = findPreference("free_favorites_backup_card");
		if (freeBackupCard != null) {
			freeBackupCard.setVisible(shouldShowFreeBackupCard);
		}
		Preference freeBackupCardDivider = findPreference("free_favorites_backup_card_divider");
		if (freeBackupCardDivider != null) {
			freeBackupCardDivider.setVisible(shouldShowFreeBackupCard);
		}
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
		Preference configureMap = findPreference(CONFIGURE_SCREEN);
		configureMap.setIcon(getContentIcon(R.drawable.ic_configure_screen_dark));
	}

	private void setupProfileAppearancePref() {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}
		Preference profileAppearance = findPreference(PROFILE_APPEARANCE);
		profileAppearance.setIcon(getContentIcon(getSelectedAppMode().getIconRes()));
		profileAppearance.setFragment(ProfileAppearanceFragment.TAG);
	}

	private void setupCopyProfileSettingsPref() {
		Preference copyProfilePrefs = findPreference(COPY_PROFILE_SETTINGS);
		copyProfilePrefs.setIcon(app.getUIUtilities().getIcon(R.drawable.ic_action_copy,
				ColorUtilities.getActiveColorId(isNightMode())));
	}

	private void setupResetToDefaultPref() {
		Preference resetToDefault = findPreference(RESET_TO_DEFAULT);
		ApplicationMode mode = getSelectedAppMode();
		if (mode.isCustomProfile() && !FileUtils.getBackupFileForCustomAppMode(app, mode.getStringKey()).exists()) {
			resetToDefault.setVisible(false);
		} else {
			OsmandDevelopmentPlugin plugin = PluginsHelper.getActivePlugin(OsmandDevelopmentPlugin.class);
			if (plugin != null && mode.getParent() != null) {
				String baseProfile = "(" + mode.getParent().toHumanString() + ")";
				String title = getString(R.string.ltr_or_rtl_combine_via_space, getString(R.string.reset_to_default), baseProfile);
				resetToDefault.setTitle(title);
			}
			resetToDefault.setIcon(app.getUIUtilities().getIcon(R.drawable.ic_action_reset_to_default_dark,
					ColorUtilities.getActiveColorId(isNightMode())));
		}
	}

	private void setupExportProfilePref() {
		Preference preference = findPreference(EXPORT_PROFILE);
		preference.setIcon(getIcon(R.drawable.ic_action_app_configuration, ColorUtilities.getActiveColorId(isNightMode())));
	}

	private void setupDeleteProfilePref() {
		Preference preference = findPreference(DELETE_PROFILE);
		preference.setIcon(getIcon(R.drawable.ic_action_delete_dark, ColorUtilities.getActiveColorId(isNightMode())));
	}

	private void setupOsmandPluginsPref() {
		Context context = requireContext();
		PreferenceCategory category = findPreference(PLUGIN_SETTINGS);
		List<OsmandPlugin> plugins = PluginsHelper.getEnabledSettingsScreenPlugins();
		if (plugins.size() != 0) {
			for (OsmandPlugin plugin : plugins) {
				Preference preference = new Preference(context);
				preference.setPersistent(false);
				preference.setKey(plugin.getId());
				preference.setTitle(plugin.getName());
				preference.setSummary(plugin.getPrefsDescription());
				preference.setIcon(getContentIcon(plugin.getLogoResourceId()));
				preference.setLayoutResource(R.layout.preference_with_descr);
				preference.setFragment(plugin.getSettingsScreenType().fragmentName);

				category.addPreference(preference);
			}
		}
	}

	private void setupUiCustomizationPref() {
		Preference uiCustomization = findPreference(UI_CUSTOMIZATION);
		if (uiCustomization != null) {
			uiCustomization.setIcon(getContentIcon(R.drawable.ic_action_ui_customization));
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		MapActivity mapActivity = getMapActivity();
		FragmentManager fragmentManager = getFragmentManager();
		if (mapActivity != null && fragmentManager != null) {
			String prefId = preference.getKey();
			ApplicationMode selectedMode = getSelectedAppMode();

			if (CONFIGURE_MAP.equals(prefId)) {
				sepAppModeToSelected();
				fragmentManager.beginTransaction()
						.remove(this)
						.addToBackStack(TAG)
						.commitAllowingStateLoss();
			} else if (CONFIGURE_SCREEN.equals(prefId)) {
				sepAppModeToSelected();
				ConfigureScreenFragment.showInstance(mapActivity);
			} else if (COPY_PROFILE_SETTINGS.equals(prefId)) {
				SelectCopyAppModeBottomSheet.showInstance(fragmentManager, this, selectedMode);
			} else if (RESET_TO_DEFAULT.equals(prefId)) {
				ResetProfilePrefsBottomSheet.showInstance(fragmentManager, getSelectedAppMode(), this);
			} else if (EXPORT_PROFILE.equals(prefId)) {
				ExportSettingsFragment.showInstance(fragmentManager, selectedMode, null, false);
			} else if (DELETE_PROFILE.equals(prefId)) {
				showDeleteModeConfirmation();
			} else if (UI_CUSTOMIZATION.equals(prefId)) {
				ConfigureMenuRootFragment.showInstance(fragmentManager, selectedMode, this);
			}
		}
		return super.onPreferenceClick(preference);
	}

	private void sepAppModeToSelected() {
		ApplicationMode selectedMode = getSelectedAppMode();
		if (!ApplicationMode.values(app).contains(selectedMode)) {
			ApplicationMode.changeProfileAvailability(selectedMode, true, app);
		}
		settings.setApplicationMode(selectedMode);
	}

	@Override
	public void onPluginStateChanged(@NonNull OsmandPlugin plugin) {
		if (plugin.getSettingsScreenType() != null) {
			updateAllSettings();
		}
	}

	private void showDeleteModeConfirmation() {
		ApplicationMode profile = getSelectedAppMode();
		FragmentActivity activity = getActivity();
		if (activity != null) {
			if (profile.isCustomProfile()) {
				AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(activity, isNightMode()));
				builder.setTitle(R.string.profile_alert_delete_title);
				builder.setMessage(String.format(getString(R.string.profile_alert_delete_msg), profile.toHumanString()));
				builder.setPositiveButton(R.string.shared_string_delete, (dialog, which) -> {
					ApplicationMode.deleteCustomModes(Collections.singletonList(profile), app);
					activity.onBackPressed();
				});
				builder.setNegativeButton(R.string.shared_string_dismiss, null);
				builder.show();
			} else {
				app.showShortToastMessage(R.string.profile_alert_cant_delete_base);
			}
		}
	}
}
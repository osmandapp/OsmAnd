package net.osmand.plus.settings.fragments.profileappearance;

import static net.osmand.plus.settings.backend.ApplicationMode.CUSTOM_MODE_KEY_SEPARATOR;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.card.color.palette.main.ColorsPaletteController;
import net.osmand.plus.card.color.palette.main.data.ColorsCollection;
import net.osmand.plus.card.color.palette.main.data.FileColorsCollection;
import net.osmand.plus.card.icon.CircleIconPaletteElements;
import net.osmand.plus.card.icon.IconsPaletteController;
import net.osmand.plus.card.icon.IconsPaletteElements;
import net.osmand.plus.helpers.Model3dHelper;
import net.osmand.plus.profiles.LocationIcon;
import net.osmand.plus.profiles.ProfileIcons;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.backup.FileSettingsHelper.SettingsExportListener;
import net.osmand.plus.settings.backend.backup.items.ProfileSettingsItem;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.enums.MarkerDisplayOption;
import net.osmand.plus.settings.fragments.ProfileOptionsDialogController;
import net.osmand.plus.settings.fragments.profileappearance.elements.LocationIconPaletteElements;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ProfileAppearanceController extends BaseDialogController {

	private static final String PROCESS_ID = "adjust_profile_appearance";

	private IProfileAppearanceScreen screen;
	private SettingsExportListener exportListener;

	private ApplicationProfileObject profile;
	private ApplicationProfileObject changedProfile;

	private ColorsPaletteController colorsCardController;
	private IconsPaletteController<Integer> profileIconCardController;
	private IconsPaletteController<String> restingIconCardController;
	private IconsPaletteController<String> navigationIconCardController;
	private ProfileOptionsDialogController profileOptionsDialogController;

	private final boolean isParentModeImported;
	private final boolean isNewProfile;

	public ProfileAppearanceController(@NonNull OsmandApplication app,
	                                   @NonNull IProfileAppearanceScreen screen,
	                                   @Nullable ApplicationMode parentMode,
	                                   boolean isParentModeImported) {
		super(app);
		bindScreen(screen);
		this.isParentModeImported = isParentModeImported;

		profile = new ApplicationProfileObject();
		if (parentMode != null) {
			profile.copyPropertiesFrom(parentMode);
			profile.parent = parentMode;
			profile.stringKey = createUniqueModeKey(parentMode);
		} else {
			profile.copyPropertiesFrom(screen.getSelectedAppMode());
		}

		changedProfile = new ApplicationProfileObject();
		changedProfile.stringKey = profile.stringKey;
		changedProfile.parent = profile.parent;
		if (parentMode != null) {
			changedProfile.name = createNonDuplicateName(parentMode.toHumanString());
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
		changedProfile.viewAngleVisibility = profile.viewAngleVisibility;
		changedProfile.locationRadiusVisibility = profile.locationRadiusVisibility;
		isNewProfile = ApplicationMode.valueOfStringKey(changedProfile.stringKey, null) == null;
	}

	public void bindScreen(@NonNull IProfileAppearanceScreen screen) {
		this.screen = screen;
		dialogManager.register(PROCESS_ID, screen);
	}

	@NonNull
	public CharSequence getToolbarTitle() {
		return isNewProfile ? getString(R.string.new_profile) : getString(R.string.profile_appearance);
	}

	@NonNull
	public String getProfileName() {
		return changedProfile.name;
	}

	public void setProfileName(@NonNull String name) {
		changedProfile.name = name;
	}

	@ColorInt
	public int getActualColor(boolean nightMode) {
		return changedProfile.getActualColor(app, nightMode);
	}

	public boolean isNotAllParamsEditable() {
		return Objects.equals(screen.getSelectedAppMode(), ApplicationMode.DEFAULT) && !isNewProfile;
	}

	@Nullable
	public ApplicationMode getChangedAppMode() {
		return ApplicationMode.valueOfStringKey(changedProfile.stringKey, null);
	}

	@NonNull
	public MarkerDisplayOption getViewAngleVisibility() {
		return changedProfile.viewAngleVisibility;
	}

	@NonNull
	public MarkerDisplayOption getLocationRadiusVisibility() {
		return changedProfile.locationRadiusVisibility;
	}

	public void askCloseScreen(@NonNull FragmentActivity activity) {
		screen.hideKeyboard();
		if (hasChanges()) {
			createWarningDialog(activity, R.string.shared_string_dismiss,
					R.string.exit_without_saving, R.string.shared_string_cancel)
					.setPositiveButton(R.string.shared_string_exit, (dialog, which) -> {
						changedProfile = profile;
						screen.goBackWithoutSaving();
					}).show();
		} else {
			closeScreen();
		}
	}

	private void closeScreen() {
		dialogManager.askDismissDialog(PROCESS_ID);
	}

	@NonNull
	private String createUniqueModeKey(@NonNull ApplicationMode mode) {
		return mode.getStringKey() + CUSTOM_MODE_KEY_SEPARATOR + System.currentTimeMillis();
	}

	private String createNonDuplicateName(@NonNull String oldName) {
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

	public void onSaveButtonClicked(@Nullable FragmentActivity activity) {
		if (activity != null) {
			screen.hideKeyboard();
			if (hasChanges() && checkProfileName(activity)) {
				getColorsCardController().refreshLastUsedTime();
				saveProfile();
			}
		}
	}

	public boolean hasChanges() {
		return !Objects.equals(profile, changedProfile);
	}

	private boolean checkProfileName(@NonNull FragmentActivity activity) {
		if (Algorithms.isBlank(changedProfile.name)) {
			createWarningDialog(activity,
					R.string.profile_alert_need_profile_name_title,
					R.string.profile_alert_need_profile_name_msg, R.string.shared_string_dismiss)
					.show();
			return false;
		}
		return true;
	}

	private AlertDialog.Builder createWarningDialog(Activity activity, int title, int message, int negButton) {
		Context themedContext = UiUtilities.getThemedContext(activity, isNightMode());
		AlertDialog.Builder warningDialog = new AlertDialog.Builder(themedContext);
		warningDialog.setTitle(getString(title));
		warningDialog.setMessage(getString(message));
		warningDialog.setNegativeButton(negButton, null);
		return warningDialog;
	}

	public void checkSavingProfile() {
		if (isNewProfile) {
			File file = FileUtils.getBackupFileForCustomAppMode(app, changedProfile.stringKey);
			boolean fileExporting = app.getFileSettingsHelper().isFileExporting(file);
			if (fileExporting) {
				screen.showNewProfileSavingDialog(null);
				app.getFileSettingsHelper().updateExportListener(file, getSettingsExportListener());
			} else if (file.exists()) {
				screen.dismissProfileSavingDialog();
				screen.customProfileSaved();
			}
		}
	}

	private void saveProfile() {
		profile = changedProfile;
		if (isNewProfile) {
			DialogInterface.OnShowListener showListener = dialog -> app.runInUIThread(() -> {
				ApplicationMode mode = saveNewProfile();
				saveProfileBackup(mode);
			});
			screen.showNewProfileSavingDialog(showListener);
		} else {
			ApplicationMode mode = screen.getSelectedAppMode();
			mode.setParentAppMode(changedProfile.parent);
			mode.setIconResName(ProfileIcons.getResStringByResId(changedProfile.iconRes));
			mode.setUserProfileName(changedProfile.name.trim());
			mode.setRoutingProfile(changedProfile.routingProfile);
			mode.setRouteService(changedProfile.routeService);
			mode.setIconColor(changedProfile.color);
			mode.updateCustomIconColor(changedProfile.customColor);
			mode.setLocationIcon(changedProfile.locationIcon);
			mode.setNavigationIcon(changedProfile.navigationIcon);
			mode.setViewAngleVisibility(changedProfile.viewAngleVisibility);
			mode.setLocationRadius(changedProfile.locationRadiusVisibility);
			screen.onAskDismissDialog(PROCESS_ID);
		}
	}

	private ApplicationMode saveNewProfile() {
		changedProfile.stringKey = createUniqueModeKey(changedProfile.parent);

		ApplicationMode.ApplicationModeBuilder builder = ApplicationMode
				.createCustomMode(changedProfile.parent, changedProfile.stringKey, app)
				.setIconResName(ProfileIcons.getResStringByResId(changedProfile.iconRes))
				.setUserProfileName(changedProfile.name.trim())
				.setRoutingProfile(changedProfile.routingProfile)
				.setRouteService(changedProfile.routeService)
				.setIconColor(changedProfile.color)
				.setCustomIconColor(changedProfile.customColor)
				.setLocationIcon(changedProfile.locationIcon)
				.setNavigationIcon(changedProfile.navigationIcon)
				.setViewAngle(changedProfile.viewAngleVisibility)
				.setLocationRadius(changedProfile.locationRadiusVisibility);

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

	public void deleteImportedMode() {
		if (isParentModeImported) {
			ApplicationMode appMode = ApplicationMode.valueOfStringKey(changedProfile.parent.getStringKey(), null);
			if (appMode != null) {
				ApplicationMode.deleteCustomModes(Collections.singletonList(appMode), app);
			}
		}
	}

	public boolean hasNameDuplicate() {
		for (ApplicationMode mode : ApplicationMode.allPossibleValues()) {
			if (mode.toHumanString().trim().equals(changedProfile.name.trim()) &&
					!mode.getStringKey().trim().equals(profile.stringKey.trim())) {
				return true;
			}
		}
		return false;
	}

	public boolean isNameEmpty() {
		return changedProfile.name.trim().isEmpty();
	}

	public void onScreenPause() {
		if (isNewProfile) {
			File file = FileUtils.getBackupFileForCustomAppMode(app, changedProfile.stringKey);
			boolean fileExporting = app.getFileSettingsHelper().isFileExporting(file);
			if (fileExporting) {
				app.getFileSettingsHelper().updateExportListener(file, null);
			}
		}
	}

	public void onScreenDestroy(@Nullable FragmentActivity activity) {
		if (activity != null && !activity.isChangingConfigurations()) {
			DialogManager manager = app.getDialogManager();
			manager.unregister(PROCESS_ID);
		}
	}

	@NonNull
	public ColorsPaletteController getColorsCardController() {
		if (colorsCardController == null) {
			boolean nightMode = isNightMode();
			int selectedColor = changedProfile.getActualColor(app, nightMode);
			ColorsCollection colorsCollection = new FileColorsCollection(app);
			colorsCardController = new ColorsPaletteController(app, colorsCollection, selectedColor) {
				@Override
				public void onAllColorsScreenClosed() {
					if (screen != null) {
						screen.updateStatusBar();
					}
				}

				@Override
				public int getControlsAccentColor(boolean nightMode) {
					if (selectedPaletteColor != null) {
						return selectedPaletteColor.getColor();
					}
					return super.getControlsAccentColor(nightMode);
				}

				@Override
				public boolean isAccentColorCanBeChanged() {
					return true;
				}
			};
			colorsCardController.setPaletteListener(paletteColor -> {
				int colorInt = paletteColor.getColor();
				changedProfile.color = changedProfile.getProfileColorByColorValue(app, colorInt);
				changedProfile.customColor = changedProfile.color == null ? colorInt : null;
				updateColorItems();
				screen.updateApplyButtonEnable();
			});
		}
		return colorsCardController;
	}

	private void updateColorItems() {
		screen.updateColorItems();
		for (IconsPaletteController<?> controller : collectIconsControllers()) {
			controller.askUpdateColoredPaletteElements();
		}
	}

	@NonNull
	private List<IconsPaletteController<?>> collectIconsControllers() {
		return Arrays.asList(getProfileIconCardController(), getRestingIconCardController(),
				getNavigationIconCardController());
	}

	@NonNull
	public IconsPaletteController<Integer> getProfileIconCardController() {
		if (profileIconCardController == null) {
			profileIconCardController = new ProfileIconsController<>(app, ProfileIcons.getIcons(), changedProfile.iconRes) {
				@Override
				protected IconsPaletteElements<Integer> createPaletteElements(@NonNull Context context, boolean nightMode) {
					return new CircleIconPaletteElements<>(context, nightMode) {
						@Override
						protected Drawable getIconDrawable(@NonNull Integer iconId, boolean isSelected) {
							return getContentIcon(iconId);
						}
					};
				}

				@Override
				public String getPaletteTitle() {
					return getString(R.string.profile_icon);
				}
			};
			profileIconCardController.setPaletteListener(icon -> {
				changedProfile.iconRes = icon;
				screen.updateApplyButtonEnable();
			});
		}
		return profileIconCardController;
	}

	@NonNull
	public IconsPaletteController<String> getRestingIconCardController() {
		if (restingIconCardController == null) {
			String iconName = LocationIcon.getActualIconName(changedProfile.locationIcon, false);
			restingIconCardController = new ProfileIconsController<>(app, listLocationIcons(), iconName) {
				@Override
				protected IconsPaletteElements<String> createPaletteElements(@NonNull Context context, boolean nightMode) {
					return new LocationIconPaletteElements(context, nightMode);
				}

				@Override
				public String getPaletteTitle() {
					return getString(R.string.resting_position_icon);
				}

				@Override
				public int getHorizontalIconsSpace() {
					return 0;
				}

				@Override
				public int getRecycleViewHorizontalPadding() {
					return getDimen(R.dimen.content_padding_half);
				}
			};
			restingIconCardController.setPaletteListener(icon -> {
				changedProfile.locationIcon = icon;
				screen.updateApplyButtonEnable();
			});
		}
		return restingIconCardController;
	}

	@NonNull
	public IconsPaletteController<String> getNavigationIconCardController() {
		if (navigationIconCardController == null) {
			String movementIconName = LocationIcon.getActualIconName(changedProfile.navigationIcon, true);
			navigationIconCardController = new ProfileIconsController<>(app, listNavigationIcons(), movementIconName) {
				@Override
				protected IconsPaletteElements<String> createPaletteElements(@NonNull Context context, boolean nightMode) {
					return new LocationIconPaletteElements(context, nightMode);
				}

				@Override
				public String getPaletteTitle() {
					return getString(R.string.navigation_position_icon);
				}

				@Override
				public int getHorizontalIconsSpace() {
					return 0;
				}

				@Override
				public int getRecycleViewHorizontalPadding() {
					return getDimen(R.dimen.content_padding_half);
				}
			};
			navigationIconCardController.setPaletteListener(icon -> {
				changedProfile.navigationIcon = icon;
				screen.updateApplyButtonEnable();
			});
		}
		return navigationIconCardController;
	}

	private SettingsExportListener getSettingsExportListener() {
		if (exportListener == null) {
			exportListener = (file, succeed) -> {
				screen.dismissProfileSavingDialog();
				if (succeed) {
					screen.customProfileSaved();
				} else {
					app.showToastMessage(R.string.profile_backup_failed);
				}
			};
		}
		return exportListener;
	}

	@NonNull
	public ProfileOptionsDialogController getProfileOptionController() {
		if (profileOptionsDialogController == null) {
			OsmandSettings settings = app.getSettings();
			profileOptionsDialogController = new ProfileOptionsDialogController(app) {
				@Override
				public void onItemSelected(@NonNull MarkerDisplayOption displayOption, @NonNull CommonPreference<MarkerDisplayOption> preference) {
					if (settings.VIEW_ANGLE_VISIBILITY.getId().equals(preference.getId())) {
						changedProfile.viewAngleVisibility = displayOption;
					} else if (settings.LOCATION_RADIUS_VISIBILITY.getId().equals(preference.getId())) {
						changedProfile.locationRadiusVisibility = displayOption;
					}
					screen.updateOptionsCard();
					screen.updateApplyButtonEnable();
				}

				@Override
				public MarkerDisplayOption getSelectedItem(@NonNull CommonPreference<MarkerDisplayOption> preference) {
					MarkerDisplayOption option;
					if (settings.VIEW_ANGLE_VISIBILITY.getId().equals(preference.getId())) {
						option = changedProfile.viewAngleVisibility;
					} else {
						option = changedProfile.locationRadiusVisibility;
					}
					return option;
				}
			};

		}
		return profileOptionsDialogController;
	}

	@NonNull
	private List<String> listLocationIcons() {
		List<String> locationIcons = new ArrayList<>();
		locationIcons.add(LocationIcon.STATIC_DEFAULT.name());
		locationIcons.add(LocationIcon.STATIC_CAR.name());
		locationIcons.add(LocationIcon.STATIC_BICYCLE.name());
		locationIcons.add(LocationIcon.MOVEMENT_DEFAULT.name());
		locationIcons.add(LocationIcon.MOVEMENT_NAUTICAL.name());
		locationIcons.add(LocationIcon.MOVEMENT_CAR.name());
		locationIcons.addAll(Model3dHelper.listModels(app));
		return locationIcons;
	}

	@NonNull
	List<String> listNavigationIcons() {
		List<String> navigationIcons = new ArrayList<>();
		navigationIcons.add(LocationIcon.MOVEMENT_DEFAULT.name());
		navigationIcons.add(LocationIcon.MOVEMENT_NAUTICAL.name());
		navigationIcons.add(LocationIcon.MOVEMENT_CAR.name());
		navigationIcons.add(LocationIcon.STATIC_DEFAULT.name());
		navigationIcons.add(LocationIcon.STATIC_CAR.name());
		navigationIcons.add(LocationIcon.STATIC_BICYCLE.name());
		navigationIcons.addAll(Model3dHelper.listModels(app));
		return navigationIcons;
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	private abstract class ProfileIconsController<IconData> extends IconsPaletteController<IconData> {

		private IconsPaletteElements<IconData> paletteElements;

		public ProfileIconsController(@NonNull OsmandApplication app,
		                              @NonNull List<IconData> icons,
		                              @Nullable IconData selectedIcon) {
			super(app, icons, selectedIcon);
		}

		@NonNull
		@Override
		public IconsPaletteElements<IconData> getPaletteElements(@NonNull Context context, boolean nightMode) {
			if (paletteElements == null) {
				paletteElements = createPaletteElements(context, nightMode);
			}
			return paletteElements;
		}

		protected abstract IconsPaletteElements<IconData> createPaletteElements(@NonNull Context context, boolean nightMode);

		@Override
		public void onAllIconsScreenClosed() {
			if (screen != null) {
				screen.updateStatusBar();
			}
		}

		@Override
		public int getControlsAccentColor(boolean nightMode) {
			return getColorsCardController().getControlsAccentColor(nightMode);
		}

		@Override
		public boolean isAccentColorCanBeChanged() {
			return true;
		}

	}

	@NonNull
	public static ProfileAppearanceController getInstance(@NonNull OsmandApplication app,
	                                                      @NonNull IProfileAppearanceScreen screen,
	                                                      @Nullable String parentModeKey,
	                                                      boolean imported) {
		DialogManager dialogManager = app.getDialogManager();
		ProfileAppearanceController controller = (ProfileAppearanceController) dialogManager.findController(PROCESS_ID);
		if (controller == null) {
			ApplicationMode parent = ApplicationMode.valueOfStringKey(parentModeKey, null);
			controller = new ProfileAppearanceController(app, screen, parent, imported);
			dialogManager.register(PROCESS_ID, controller);
		} else {
			controller.bindScreen(screen);
		}
		return controller;
	}
}

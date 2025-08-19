package net.osmand.plus.settings.fragments;

import static net.osmand.plus.settings.bottomsheets.DistanceDuringNavigationBottomSheet.*;
import static net.osmand.plus.settings.fragments.SettingsScreenType.EXTERNAL_INPUT_DEVICE;
import static net.osmand.plus.settings.fragments.SettingsScreenType.POSITION_ANIMATION;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatCheckedTextView;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;

import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.keyevent.InputDevicesHelper;
import net.osmand.plus.keyevent.devices.InputDeviceProfile;
import net.osmand.plus.routing.RouteService;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.bottomsheets.DistanceDuringNavigationBottomSheet;
import net.osmand.plus.settings.controllers.CompassModeDialogController;
import net.osmand.plus.settings.enums.AngularConstants;
import net.osmand.plus.settings.enums.DrivingRegion;
import net.osmand.plus.settings.enums.CompassMode;
import net.osmand.plus.settings.enums.ScreenOrientation;
import net.osmand.plus.settings.enums.TemperatureUnitsMode;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.settings.enums.VolumeUnit;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.router.GeneralRouter;
import net.osmand.shared.settings.enums.AltitudeMetrics;
import net.osmand.shared.settings.enums.MetricsConstants;
import net.osmand.shared.settings.enums.SpeedConstants;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GeneralProfileSettingsFragment extends BaseSettingsFragment {

	public static final String TAG = GeneralProfileSettingsFragment.class.getSimpleName();

	@Override
	protected void setupPreferences() {
		setupAppThemePref();
		setupRotateMapPref();
		setupMapScreenOrientationPref();
		setupTurnScreenOnPref();

		setupDrivingRegionPref();
		setupUnitsOfLengthPref();
		setupUnitsOfAltitudePref();
		setupCoordinatesFormatPref();
		setupAngularUnitsPref();
		setupSpeedSystemPref();
		setupUnitOfVolumePref();
		setupUnitOfTemperaturePref();
		setupPreciseDistanceNumbersPref();

		setupVolumeButtonsAsZoom();
		setupKalmanFilterPref();
		setupMagneticFieldSensorPref();
		setupMapEmptyStateAllowedPref();
		setupExternalInputDevicePref();
		setupPositionAnimation();
		setupTrackballForMovementsPref();

		updateDialogControllerCallbacks();
	}

	private void setupAppThemePref() {
		ListPreferenceEx appTheme =
				findPreference(settings.OSMAND_THEME.getId());

		ArrayList<String> entries = new ArrayList<>();
		entries.add(getString(R.string.dark_theme));
		entries.add(getString(R.string.light_theme));

		ArrayList<Integer> values = new ArrayList<>();
		values.add(OsmandSettings.OSMAND_DARK_THEME);
		values.add(OsmandSettings.OSMAND_LIGHT_THEME);

		if (settings.isSupportSystemTheme()) {
			entries.add(getString(R.string.system_default_theme));
			values.add(OsmandSettings.SYSTEM_DEFAULT_THEME);
		}

		String[] entriesStrings = new String[entries.size()];
		appTheme.setEntries(entries.toArray(entriesStrings));
		appTheme.setEntryValues(values.toArray());
		appTheme.setIcon(getOsmandThemeIcon());
	}

	private Drawable getOsmandThemeIcon() {
		int iconId;
		ApplicationMode mode = getSelectedAppMode();
		if (settings.isSystemThemeUsed(mode)) {
			iconId = R.drawable.ic_action_android;
		} else {
			boolean nightMode = app.getDaynightHelper().isNightMode(mode, ThemeUsageContext.APP);
			iconId = nightMode ? R.drawable.ic_action_moon : R.drawable.ic_action_sun;
		}
		return getActiveIcon(iconId);
	}

	private void setupRotateMapPref() {
		Preference uiPreference = findPreference(settings.ROTATE_MAP.getId());
		if (uiPreference != null) {
			ApplicationMode appMode = getSelectedAppMode();
			CompassMode compassMode = settings.getCompassMode(appMode);
			Drawable icon = getIcon(compassMode.getIconId(isNightMode()));
			uiPreference.setIcon(icon);
			uiPreference.setSummary(compassMode.getTitleId());
		}
	}

	private void setupMapScreenOrientationPref() {
		ListPreferenceEx preference = requirePreference(settings.MAP_SCREEN_ORIENTATION.getId());
		ScreenOrientation[] values = ScreenOrientation.values();
		String[] entries = new String[values.length];
		Integer[] entryValues = new Integer[values.length];

		for (int i = 0; i < values.length; i++) {
			ScreenOrientation orientation = values[i];
			entries[i] = getString(orientation.getTitleId());
			entryValues[i] = orientation.getValue();
		}

		preference.setEntries(entries);
		preference.setEntryValues(entryValues);
		preference.setIcon(getMapScreenOrientationIcon());
	}

	private void setupTurnScreenOnPref() {
		Preference screenControl = requirePreference("screen_control");
		screenControl.setIcon(getContentIcon(R.drawable.ic_action_turn_screen_on));
	}

	@NonNull
	private Drawable getMapScreenOrientationIcon() {
		int value = settings.MAP_SCREEN_ORIENTATION.getModeValue(getSelectedAppMode());
		ScreenOrientation orientation = ScreenOrientation.fromValue(value);
		return getActiveIcon(orientation.getIconId());
	}

	private void setupDrivingRegionPref() {
		ApplicationMode selectedMode = getSelectedAppMode();
		Preference defaultDrivingRegion = findPreference(settings.DRIVING_REGION.getId());
		defaultDrivingRegion.setIcon(getActiveIcon(R.drawable.ic_action_car_dark));
		defaultDrivingRegion.setSummary(getString(settings.DRIVING_REGION_AUTOMATIC.getModeValue(selectedMode) ? R.string.shared_string_automatic : settings.DRIVING_REGION.getModeValue(selectedMode).name));
	}

	private void setupUnitsOfLengthPref() {
		MetricsConstants[] metricsConstants = MetricsConstants.values();
		String[] entries = new String[metricsConstants.length];
		Integer[] entryValues = new Integer[metricsConstants.length];

		for (int i = 0; i < entries.length; i++) {
			entries[i] = metricsConstants[i].toHumanString();
			entryValues[i] = metricsConstants[i].ordinal();
		}

		ListPreferenceEx unitsOfLength = findPreference(settings.METRIC_SYSTEM.getId());
		unitsOfLength.setEntries(entries);
		unitsOfLength.setEntryValues(entryValues);
		unitsOfLength.setIcon(getActiveIcon(R.drawable.ic_action_units_length));
	}

	private void setupUnitsOfAltitudePref() {
		AltitudeMetrics[] altitudeMetrics = AltitudeMetrics.values();
		String[] entries = new String[altitudeMetrics.length];
		Integer[] entryValues = new Integer[altitudeMetrics.length];

		for (int i = 0; i < entries.length; i++) {
			entries[i] = Algorithms.capitalizeFirstLetter(altitudeMetrics[i].toHumanString());
			entryValues[i] = altitudeMetrics[i].ordinal();
		}

		ListPreferenceEx unitsOfAltitude = findPreference(settings.ALTITUDE_METRIC.getId());
		unitsOfAltitude.setEntries(entries);
		unitsOfAltitude.setEntryValues(entryValues);
		unitsOfAltitude.setDescription(R.string.altitude_metrics_description);
		unitsOfAltitude.setIcon(getActiveIcon(R.drawable.ic_action_units_altitude));
	}

	private void setupCoordinatesFormatPref() {
		Preference coordinatesFormat = findPreference(settings.COORDINATES_FORMAT.getId());
		coordinatesFormat.setIcon(getActiveIcon(R.drawable.ic_action_coordinates_widget));
		coordinatesFormat.setSummary(PointDescription.formatToHumanString(app, settings.COORDINATES_FORMAT.getModeValue(getSelectedAppMode())));
	}

	private void setupAngularUnitsPref() {
		AngularConstants[] ac = AngularConstants.values();
		String[] entries = new String[ac.length];
		Integer[] entryValues = new Integer[ac.length];

		for (int i = 0; i < entries.length; i++) {
			if (ac[i] == AngularConstants.DEGREES) {
				entries[i] = AngularConstants.DEGREES.toHumanString(app) + " 180";
				entryValues[i] = AngularConstants.DEGREES.ordinal();
			} else if (ac[i] == AngularConstants.DEGREES360) {
				entries[i] = AngularConstants.DEGREES.toHumanString(app) + " 360";
				entryValues[i] = AngularConstants.DEGREES360.ordinal();
			} else {
				entries[i] = ac[i].toHumanString(app);
				entryValues[i] = AngularConstants.MILLIRADS.ordinal();
			}
		}

		ListPreferenceEx angularUnits = findPreference(settings.ANGULAR_UNITS.getId());
		angularUnits.setEntries(entries);
		angularUnits.setEntryValues(entryValues);
		angularUnits.setIcon(getActiveIcon(R.drawable.ic_action_angular_unit));
	}

	private void setupSpeedSystemPref() {
		SpeedConstants[] speedConstants = SpeedConstants.values();
		String[] entries = new String[speedConstants.length];
		Integer[] entryValues = new Integer[speedConstants.length];

		for (int i = 0; i < entries.length; i++) {
			entries[i] = speedConstants[i].toHumanString();
			entryValues[i] = speedConstants[i].ordinal();
		}

		ListPreferenceEx speedSystem = findPreference(settings.SPEED_SYSTEM.getId());
		speedSystem.setEntries(entries);
		speedSystem.setEntryValues(entryValues);
		speedSystem.setDescription(R.string.default_speed_system_descr);
		speedSystem.setIcon(getActiveIcon(R.drawable.ic_action_speed));
	}

	private void setupUnitOfVolumePref() {
		boolean hidePref = false;
		ApplicationMode mode = getSelectedAppMode();
		RouteService routeService = mode.getRouteService();
		if (routeService == RouteService.OSMAND) {
			GeneralRouter router = app.getRouter(mode);
			if (router != null) {
				GeneralRouter.GeneralRouterProfile routerProfile = router.getProfile();
				hidePref = routerProfile == null
						|| routerProfile == GeneralRouter.GeneralRouterProfile.PEDESTRIAN
						|| routerProfile == GeneralRouter.GeneralRouterProfile.BICYCLE
						|| routerProfile == GeneralRouter.GeneralRouterProfile.HORSEBACKRIDING
						|| routerProfile == GeneralRouter.GeneralRouterProfile.SKI;
			}
		}
		ListPreferenceEx unitOfVolumePref = requirePreference(settings.UNIT_OF_VOLUME.getId());
		if (hidePref) {
			unitOfVolumePref.setVisible(false);
		} else {
			VolumeUnit[] unitValues = VolumeUnit.values();
			String[] entries = new String[unitValues.length];
			Integer[] entryValues = new Integer[unitValues.length];

			for (int i = 0; i < entries.length; i++) {
				entries[i] = unitValues[i].toHumanString(app);
				entryValues[i] = unitValues[i].ordinal();
			}

			unitOfVolumePref.setEntries(entries);
			unitOfVolumePref.setEntryValues(entryValues);
			unitOfVolumePref.setDescription(R.string.unit_of_volume_description);
			unitOfVolumePref.setIcon(getActiveIcon(R.drawable.ic_action_fuel_tank));
		}
	}

	private void setupUnitOfTemperaturePref() {
		ListPreferenceEx preference = requirePreference(settings.UNIT_OF_TEMPERATURE.getId());
		TemperatureUnitsMode[] unitValues = TemperatureUnitsMode.values();
		String[] entries = new String[unitValues.length];
		Integer[] entryValues = new Integer[unitValues.length];

		for (int i = 0; i < entries.length; i++) {
			entries[i] = unitValues[i].toHumanString(app);
			entryValues[i] = unitValues[i].ordinal();
		}
		preference.setEntries(entries);
		preference.setEntryValues(entryValues);
		preference.setDescription(R.string.unit_of_temperature_description);
		preference.setIcon(getActiveIcon(R.drawable.ic_action_thermometer));
	}

	private void setupPreciseDistanceNumbersPref() {
		ApplicationMode selectedMode = getSelectedAppMode();
		Preference preference = findPreference(settings.PRECISE_DISTANCE_NUMBERS.getId());
		DistanceDuringNavigationMode enabledMode = settings.PRECISE_DISTANCE_NUMBERS.getModeValue(selectedMode) ? DistanceDuringNavigationMode.PRECISE : DistanceDuringNavigationMode.ROUND_UP;
		preference.setSummary(getString(enabledMode.nameId));
		preference.setIcon(getActiveIcon(enabledMode.iconId));
	}

	private void setupVolumeButtonsAsZoom() {
		SwitchPreferenceEx volumeButtonsPref = findPreference(settings.USE_VOLUME_BUTTONS_AS_ZOOM.getId());
		volumeButtonsPref.setTitle(getString(R.string.use_volume_buttons_as_zoom));
		volumeButtonsPref.setDescription(getString(R.string.use_volume_buttons_as_zoom_descr));
		Drawable icon = getPersistentPrefIcon(R.drawable.ic_action_zoom_volume_buttons);
		volumeButtonsPref.setIcon(icon);
	}

	private void setupKalmanFilterPref() {
		SwitchPreferenceEx kalmanFilterPref = findPreference(settings.USE_KALMAN_FILTER_FOR_COMPASS.getId());
		kalmanFilterPref.setTitle(getString(R.string.use_kalman_filter_compass));
		kalmanFilterPref.setDescription(getString(R.string.use_kalman_filter_compass_descr));
	}

	private void setupMagneticFieldSensorPref() {
		SwitchPreferenceEx useMagneticSensorPref = findPreference(settings.USE_MAGNETIC_FIELD_SENSOR_COMPASS.getId());
		useMagneticSensorPref.setTitle(getString(R.string.use_magnetic_sensor));
		useMagneticSensorPref.setDescription(getString(R.string.use_magnetic_sensor_descr));
		useMagneticSensorPref.setVisible(app.getLocationProvider().hasOrientationSensor());
	}

	private void setupMapEmptyStateAllowedPref() {
		SwitchPreferenceEx mapEmptyStateAllowedPref = findPreference(settings.MAP_EMPTY_STATE_ALLOWED.getId());
		mapEmptyStateAllowedPref.setTitle(getString(R.string.tap_on_map_to_hide_interface));
		mapEmptyStateAllowedPref.setDescription(getString(R.string.tap_on_map_to_hide_interface_descr));
	}

	private void setupExternalInputDevicePref() {
		Preference uiPreference = findPreference(settings.EXTERNAL_INPUT_DEVICE.getId());
		if (uiPreference != null) {
			uiPreference.setSummary(getExternalInputDeviceSummary());
			uiPreference.setIcon(getExternalInputDeviceIcon());
		}
	}

	private void setupPositionAnimation() {
		Preference uiPreference = findPreference(settings.ANIMATE_MY_LOCATION.getId());

		if (uiPreference != null) {
			int interpolationValue = settings.LOCATION_INTERPOLATION_PERCENT.getModeValue(getSelectedAppMode());
			boolean animationEnabled = settings.ANIMATE_MY_LOCATION.getModeValue(getSelectedAppMode());

			String summary;
			Drawable icon;
			if (animationEnabled) {
				icon = getActiveIcon(R.drawable.ic_action_location_animation);
				String enabled = app.getString(R.string.shared_string_enabled);
				if (interpolationValue > 0) {
					String formattedInterpolateValue = OsmAndFormatter.getFormattedPredictionTime(app, interpolationValue);
					summary = app.getString(R.string.ltr_or_rtl_combine_via_bold_point, enabled, formattedInterpolateValue);
				} else {
					summary = enabled;
				}
			} else {
				icon = getContentIcon(R.drawable.ic_action_location_no_animation);
				summary = app.getString(R.string.shared_string_disabled);
			}
			uiPreference.setSummary(summary);
			uiPreference.setIcon(icon);
		}
	}

	private String getExternalInputDeviceSummary() {
		ApplicationMode appMode = getSelectedAppMode();
		InputDevicesHelper deviceHelper = app.getInputDeviceHelper();
		InputDeviceProfile inputDevice = deviceHelper.getCustomizationDevice(appMode);
		return inputDevice != null ? inputDevice.toHumanString(app) : getString(R.string.shared_string_disabled);
	}

	private Drawable getExternalInputDeviceIcon() {
		ApplicationMode appMode = getSelectedAppMode();
		InputDevicesHelper deviceHelper = app.getInputDeviceHelper();
		return deviceHelper.getCustomizationDevice(appMode) != null
				? getActiveIcon(R.drawable.ic_action_keyboard)
				: getContentIcon(R.drawable.ic_action_keyboard_disabled);
	}

	private void setupTrackballForMovementsPref() {
		SwitchPreferenceEx mapEmptyStateAllowedPref = findPreference(settings.USE_TRACKBALL_FOR_MOVEMENTS.getId());
		mapEmptyStateAllowedPref.setTitle(getString(R.string.use_trackball));
		mapEmptyStateAllowedPref.setDescription(getString(R.string.use_trackball_descr));

		int nav = getResources().getConfiguration().navigation;
		boolean visible = nav == Configuration.NAVIGATION_DPAD || nav == Configuration.NAVIGATION_TRACKBALL ||
				nav == Configuration.NAVIGATION_WHEEL || nav == Configuration.NAVIGATION_UNDEFINED;
		mapEmptyStateAllowedPref.setVisible(visible);
	}

	private void showDrivingRegionDialog() {
		Context themedContext = UiUtilities.getThemedContext(getActivity(), isNightMode());
		AlertDialog.Builder b = new AlertDialog.Builder(themedContext);

		b.setTitle(getString(R.string.driving_region));

		List<Object> drs = new ArrayList<>();
		drs.add(getString(R.string.shared_string_automatic));
		drs.addAll(Arrays.asList(DrivingRegion.values()));
		int sel = -1;
		ApplicationMode selectedMode = getSelectedAppMode();
		DrivingRegion selectedDrivingRegion = settings.DRIVING_REGION.getModeValue(selectedMode);
		if (settings.DRIVING_REGION_AUTOMATIC.getModeValue(selectedMode)) {
			sel = 0;
		}
		for (int i = 1; i < drs.size(); i++) {
			if (sel == -1 && drs.get(i) == selectedDrivingRegion) {
				sel = i;
				break;
			}
		}

		int selected = sel;
		ArrayAdapter<Object> singleChoiceAdapter =
				new ArrayAdapter<Object>(themedContext, R.layout.single_choice_description_item, R.id.text1, drs) {
					@NonNull
					@Override
					public View getView(int position, View convertView, @NonNull ViewGroup parent) {
						View v = convertView;
						if (v == null) {
							v = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_choice_description_item, parent, false);
						}
						Object item = getItem(position);
						AppCompatCheckedTextView title = v.findViewById(R.id.text1);
						TextView desc = v.findViewById(R.id.description);
						if (item instanceof DrivingRegion) {
							DrivingRegion drivingRegion = (DrivingRegion) item;
							title.setText(app.getString(drivingRegion.name));
							desc.setVisibility(View.VISIBLE);
							desc.setText(drivingRegion.getDescription(v.getContext()));
						} else if (item instanceof String) {
							title.setText((String) item);
							desc.setVisibility(View.GONE);
						}
						title.setChecked(position == selected);
						UiUtilities.setupCompoundButtonDrawable(app, isNightMode(), getActiveProfileColor(), title.getCheckMarkDrawable());
						return v;
					}
				};

		b.setAdapter(singleChoiceAdapter, (dialog, which) -> {
			onConfirmPreferenceChange(settings.DRIVING_REGION.getId(), drs.get(which), ApplyQueryType.BOTTOM_SHEET);
		});

		b.setNegativeButton(R.string.shared_string_cancel, null);
		b.show();
	}

	@Override
	public void onApplyPreferenceChange(String prefId, boolean applyToAllProfiles, Object newValue) {
		if (settings.DRIVING_REGION.getId().equals(prefId)) {
			ApplicationMode selectedMode = getSelectedAppMode();
			if (newValue instanceof String) {
				applyPreference(settings.DRIVING_REGION_AUTOMATIC.getId(), applyToAllProfiles, true);
				MapViewTrackingUtilities mapViewTrackingUtilities = app.getMapViewTrackingUtilities();
				if (mapViewTrackingUtilities != null) {
					mapViewTrackingUtilities.resetDrivingRegionUpdate();
				}
			} else if (newValue instanceof DrivingRegion) {
				applyPreference(settings.DRIVING_REGION_AUTOMATIC.getId(), applyToAllProfiles, false);
				if (applyToAllProfiles) {
					for (ApplicationMode appMode : ApplicationMode.allPossibleValues()) {
						settings.DRIVING_REGION.setModeValue(appMode, (DrivingRegion) newValue);
					}
				} else {
					settings.DRIVING_REGION.setModeValue(selectedMode, (DrivingRegion) newValue);
				}
			}
			updateAllSettings();
		} else {
			applyPreference(prefId, applyToAllProfiles, newValue);
			MapViewTrackingUtilities mapViewTrackingUtilities = app.getMapViewTrackingUtilities();
			if (mapViewTrackingUtilities != null) {
				mapViewTrackingUtilities.updateSettings();
			}
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String key = preference.getKey();
		ApplicationMode appMode = getSelectedAppMode();
		if (key.equals(settings.DRIVING_REGION.getId())) {
			showDrivingRegionDialog();
			return true;
		} else if (key.equals(settings.ROTATE_MAP.getId())) {
			CompassModeDialogController controller = new CompassModeDialogController(app, appMode);
			showSingleSelectionDialog(CompassModeDialogController.PROCESS_ID, controller);
			controller.setCallback(this);
			return true;
		} else if (key.equals(settings.EXTERNAL_INPUT_DEVICE.getId())) {
			BaseSettingsFragment.showInstance(requireActivity(), EXTERNAL_INPUT_DEVICE, appMode, new Bundle(), this);
			return true;
		} else if (key.equals(settings.ANIMATE_MY_LOCATION.getId())) {
			BaseSettingsFragment.showInstance(requireActivity(), POSITION_ANIMATION, appMode, new Bundle(), this);
			return true;
		} else if (key.equals(settings.PRECISE_DISTANCE_NUMBERS.getId())) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				DistanceDuringNavigationBottomSheet.showInstance(fragmentManager, preference.getKey(), this, getSelectedAppMode(), false);
			}
		}
		return super.onPreferenceClick(preference);
	}

	private void updateDialogControllerCallbacks() {
		IDialogController controller;
		DialogManager dialogManager = app.getDialogManager();

		controller = dialogManager.findController(CompassModeDialogController.PROCESS_ID);
		if (controller instanceof CompassModeDialogController) {
			((CompassModeDialogController) controller).setCallback(this);
		}
	}

	@Override
	public void onPreferenceChanged(@NonNull String prefId) {
		Preference preference = findPreference(prefId);
		if (preference != null) {
			if (settings.OSMAND_THEME.getId().equals(prefId)) {
				preference.setIcon(getOsmandThemeIcon());
			} else if (settings.MAP_SCREEN_ORIENTATION.getId().equals(prefId)) {
				preference.setIcon(getMapScreenOrientationIcon());
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (!requireActivity().isChangingConfigurations()) {
			app.getInputDeviceHelper().releaseCustomizationCollection();
		}
	}

	@Override
	public void updateSetting(String prefId) {
		if (settings.OSMAND_THEME.getId().equals(prefId)) {
			recreate();
			return;
		}
		super.updateSetting(prefId);
	}
}

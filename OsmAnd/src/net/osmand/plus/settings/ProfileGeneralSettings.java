package net.osmand.plus.settings;

import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.widget.AppCompatCheckedTextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.views.ListIntPreference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProfileGeneralSettings extends BaseSettingsFragment {

	public static final String TAG = "ProfileGeneralSettings";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		return view;
	}

	@Override
	protected int getPreferenceResId() {
		return R.xml.general_profile_settings;
	}

	@Override
	protected int getToolbarResId() {
		return R.layout.profile_preference_toolbar;
	}

	protected String getToolbarTitle() {
		return getString(R.string.general_settings_2);
	}

	protected void createUI() {
		PreferenceScreen screen = getPreferenceScreen();

		ListIntPreference appTheme = (ListIntPreference) findAndRegisterPreference(settings.OSMAND_THEME.getId());
		appTheme.setEntries(new String[]{getString(R.string.dark_theme), getString(R.string.light_theme)});
		appTheme.setEntryValues(new int[]{OsmandSettings.OSMAND_DARK_THEME, OsmandSettings.OSMAND_LIGHT_THEME});
		updateAppThemePref();

		ListIntPreference rotateMap = (ListIntPreference) findAndRegisterPreference(settings.ROTATE_MAP.getId());
		rotateMap.setEntries(new String[]{getString(R.string.rotate_map_none_opt), getString(R.string.rotate_map_bearing_opt), getString(R.string.rotate_map_compass_opt)});
		rotateMap.setEntryValues(new int[]{OsmandSettings.ROTATE_MAP_NONE, OsmandSettings.ROTATE_MAP_BEARING, OsmandSettings.ROTATE_MAP_COMPASS});
		updateRotateMapPref();

		ListIntPreference mapScreenOrientation = (ListIntPreference) findAndRegisterPreference(settings.MAP_SCREEN_ORIENTATION.getId());
		mapScreenOrientation.setEntries(new String[]{getString(R.string.map_orientation_portrait), getString(R.string.map_orientation_landscape), getString(R.string.map_orientation_default)});
		mapScreenOrientation.setEntryValues(new int[]{ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED});
		updateMapScreenOrientationPref();

		Preference defaultDrivingRegion = findAndRegisterPreference(settings.DRIVING_REGION.getId());
		defaultDrivingRegion.setIcon(getContentIcon(R.drawable.ic_action_car_dark));

		createUnitsOfLengthPref();
		createCoordinatesFormatPref();
		createAngularUnitsPref();
		createExternalInputDevicePref();
	}

	private void createUnitsOfLengthPref() {
		OsmandSettings.MetricsConstants[] mvls = OsmandSettings.MetricsConstants.values();
		String[] entries = new String[mvls.length];
		String[] entryValues = new String[mvls.length];
		for (int i = 0; i < entries.length; i++) {
			entries[i] = mvls[i].toHumanString(app);
			entryValues[i] = mvls[i].name();
		}

		ListPreference unitsOfLength = (ListPreference) findAndRegisterPreference(settings.METRIC_SYSTEM.getId());
		unitsOfLength.setEntries(entries);
		unitsOfLength.setEntryValues(entryValues);
		unitsOfLength.setIcon(getContentIcon(R.drawable.ic_action_ruler_unit));
	}

	private void createExternalInputDevicePref() {
		String[] entries = new String[]{
				getString(R.string.sett_no_ext_input),
				getString(R.string.sett_generic_ext_input),
				getString(R.string.sett_wunderlinq_ext_input),
				getString(R.string.sett_parrot_ext_input)};

		int[] entryValues = new int[]{
				OsmandSettings.NO_EXTERNAL_DEVICE,
				OsmandSettings.GENERIC_EXTERNAL_DEVICE,
				OsmandSettings.WUNDERLINQ_EXTERNAL_DEVICE,
				OsmandSettings.PARROT_EXTERNAL_DEVICE};

		ListIntPreference externalInputDevice = (ListIntPreference) findAndRegisterPreference(settings.EXTERNAL_INPUT_DEVICE.getId());
		externalInputDevice.setEntries(entries);
		externalInputDevice.setEntryValues(entryValues);

		int index = settings.EXTERNAL_INPUT_DEVICE.get();
		externalInputDevice.setSummary(entries[index]);
	}

	private void createCoordinatesFormatPref() {
		int[] cvls = new int[5];
		cvls[0] = PointDescription.FORMAT_DEGREES;
		cvls[1] = PointDescription.FORMAT_MINUTES;
		cvls[2] = PointDescription.FORMAT_SECONDS;
		cvls[3] = PointDescription.UTM_FORMAT;
		cvls[4] = PointDescription.OLC_FORMAT;
		String[] entries = new String[5];
		entries[0] = PointDescription.formatToHumanString(getContext(), PointDescription.FORMAT_DEGREES);
		entries[1] = PointDescription.formatToHumanString(getContext(), PointDescription.FORMAT_MINUTES);
		entries[2] = PointDescription.formatToHumanString(getContext(), PointDescription.FORMAT_SECONDS);
		entries[3] = PointDescription.formatToHumanString(getContext(), PointDescription.UTM_FORMAT);
		entries[4] = PointDescription.formatToHumanString(getContext(), PointDescription.OLC_FORMAT);

		ListIntPreference coordinatesFormat = (ListIntPreference) findAndRegisterPreference(settings.COORDINATES_FORMAT.getId());
		coordinatesFormat.setEntries(entries);
		coordinatesFormat.setEntryValues(cvls);
		coordinatesFormat.setIcon(getContentIcon(R.drawable.ic_action_coordinates_widget));
	}

	private void createAngularUnitsPref() {
		OsmandSettings.AngularConstants[] ac = OsmandSettings.AngularConstants.values();
		String[] entries = new String[ac.length];
		String[] entryValues = new String[ac.length];
		for (int i = 0; i < entries.length; i++) {
			if (ac[i] == OsmandSettings.AngularConstants.DEGREES) {
				entries[i] = OsmandSettings.AngularConstants.DEGREES.toHumanString(app) + " 180";
				entryValues[i] = OsmandSettings.AngularConstants.DEGREES.name();
			} else if (ac[i] == OsmandSettings.AngularConstants.DEGREES360) {
				entries[i] = OsmandSettings.AngularConstants.DEGREES.toHumanString(app) + " 360";
				entryValues[i] = OsmandSettings.AngularConstants.DEGREES360.name();
			} else {
				entries[i] = ac[i].toHumanString(app);
				entryValues[i] = OsmandSettings.AngularConstants.MILLIRADS.name();
			}
		}

		ListPreference angularUnits = (ListPreference) findAndRegisterPreference(settings.ANGULAR_UNITS.getId());
		angularUnits.setEntries(entries);
		angularUnits.setEntryValues(entryValues);
		angularUnits.setIcon(getContentIcon(R.drawable.ic_action_angular_unit));
	}

	private void updateAppThemePref() {
		ListIntPreference appTheme = (ListIntPreference) findAndRegisterPreference(settings.OSMAND_THEME.getId());
		int iconRes = settings.isLightContent() ? R.drawable.ic_action_sun : R.drawable.ic_action_moon;
		appTheme.setIcon(getContentIcon(iconRes));
	}

	private void updateRotateMapPref() {
		ListIntPreference rotateMap = (ListIntPreference) findAndRegisterPreference(settings.ROTATE_MAP.getId());
		int iconRes;
		if (settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_NONE) {
			iconRes = R.drawable.ic_action_direction_north;
		} else if (settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_BEARING) {
			iconRes = R.drawable.ic_action_direction_movement;
		} else {
			iconRes = R.drawable.ic_action_direction_compass;
		}
		rotateMap.setIcon(getContentIcon(iconRes));
	}

	private void updateMapScreenOrientationPref() {
		ListIntPreference mapScreenOrientation = (ListIntPreference) findAndRegisterPreference(settings.MAP_SCREEN_ORIENTATION.getId());
		int iconRes;
		if (settings.MAP_SCREEN_ORIENTATION.get() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
			iconRes = R.drawable.ic_action_phone_portrait_orientation;
		} else if (settings.MAP_SCREEN_ORIENTATION.get() == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
			iconRes = R.drawable.ic_action_phone_landscape_orientation;
		} else {
			iconRes = R.drawable.ic_action_phone_device_orientation;
		}
		mapScreenOrientation.setIcon(getContentIcon(iconRes));
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		boolean changed = super.onPreferenceChange(preference, newValue);
		if (changed) {
			String key = preference.getKey();
			if (key.equals(settings.OSMAND_THEME.getId())) {
				updateAppThemePref();
			} else if (key.equals(settings.ROTATE_MAP.getId())) {
				updateRotateMapPref();
			} else if (key.equals(settings.MAP_SCREEN_ORIENTATION.getId())) {
				updateMapScreenOrientationPref();
			}
		}

		return changed;
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference.getKey().equals(settings.DRIVING_REGION.getId())) {
			showDrivingRegionDialog();
			return true;
		}
		return false;
	}

	private void showDrivingRegionDialog() {
		final AlertDialog.Builder b = new AlertDialog.Builder(getActivity());

		b.setTitle(getString(R.string.driving_region));

		final List<OsmandSettings.DrivingRegion> drs = new ArrayList<>();
		drs.add(null);
		drs.addAll(Arrays.asList(OsmandSettings.DrivingRegion.values()));
		int sel = -1;
		OsmandSettings.DrivingRegion selectedDrivingRegion = settings.DRIVING_REGION.get();
		if (settings.DRIVING_REGION_AUTOMATIC.get()) {
			sel = 0;
		}
		for (int i = 1; i < drs.size(); i++) {
			if (sel == -1 && drs.get(i) == selectedDrivingRegion) {
				sel = i;
				break;
			}
		}

		final int selected = sel;
		final ArrayAdapter<OsmandSettings.DrivingRegion> singleChoiceAdapter =
				new ArrayAdapter<OsmandSettings.DrivingRegion>(getActivity(), R.layout.single_choice_description_item, R.id.text1, drs) {
					@NonNull
					@Override
					public View getView(int position, View convertView, @NonNull ViewGroup parent) {
						View v = convertView;
						if (v == null) {
							LayoutInflater inflater = getActivity().getLayoutInflater();
							v = inflater.inflate(R.layout.single_choice_description_item, parent, false);
						}
						OsmandSettings.DrivingRegion item = getItem(position);
						AppCompatCheckedTextView title = (AppCompatCheckedTextView) v.findViewById(R.id.text1);
						TextView desc = (TextView) v.findViewById(R.id.description);
						if (item != null) {
							title.setText(getString(item.name));
							desc.setVisibility(View.VISIBLE);
							desc.setText(item.getDescription(v.getContext()));
						} else {
							title.setText(getString(R.string.driving_region_automatic));
							desc.setVisibility(View.GONE);
						}
						title.setChecked(position == selected);
						return v;
					}
				};

		b.setAdapter(singleChoiceAdapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (drs.get(which) == null) {
					settings.DRIVING_REGION_AUTOMATIC.set(true);
					MapViewTrackingUtilities mapViewTrackingUtilities = getMyApplication().getMapViewTrackingUtilities();
					if (mapViewTrackingUtilities != null) {
						mapViewTrackingUtilities.resetDrivingRegionUpdate();
					}
				} else {
					settings.DRIVING_REGION_AUTOMATIC.set(false);
					settings.DRIVING_REGION.set(drs.get(which));
				}
				updateAllSettings();
			}
		});

		b.setNegativeButton(R.string.shared_string_cancel, null);
		b.show();
	}

	public static boolean showInstance(FragmentManager fragmentManager) {
		try {
			ProfileGeneralSettings profileGeneralSettingsSettings = new ProfileGeneralSettings();
			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, profileGeneralSettingsSettings, ProfileGeneralSettings.TAG)
					.addToBackStack(ProfileGeneralSettings.TAG)
					.commitAllowingStateLoss();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
package net.osmand.plus.settings;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.views.ListIntPreference;

public class ProfileGeneralSettings extends BaseProfileSettingsFragment {

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

		ListIntPreference rotateMap = (ListIntPreference) findAndRegisterPreference(settings.ROTATE_MAP.getId());
		rotateMap.setEntries(new String[]{getString(R.string.rotate_map_none_opt), getString(R.string.rotate_map_bearing_opt), getString(R.string.rotate_map_compass_opt)});
		rotateMap.setEntryValues(new int[]{OsmandSettings.ROTATE_MAP_NONE, OsmandSettings.ROTATE_MAP_BEARING, OsmandSettings.ROTATE_MAP_COMPASS});

		ListIntPreference mapScreenOrientation = (ListIntPreference) findAndRegisterPreference(settings.MAP_SCREEN_ORIENTATION.getId());
		mapScreenOrientation.setEntries(new String[]{getString(R.string.map_orientation_portrait), getString(R.string.map_orientation_landscape), getString(R.string.map_orientation_default)});
		mapScreenOrientation.setEntryValues(new int[]{ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED});




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
	}

	@Override
	public void onDisplayPreferenceDialog(Preference preference) {
		String key = preference.getKey();

		super.onDisplayPreferenceDialog(preference);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String key = preference.getKey();

		return super.onPreferenceClick(preference);
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
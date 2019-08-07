package net.osmand.plus.settings;

import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.app.FragmentManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.views.ListFloatPreference;
import net.osmand.plus.views.ListIntPreference;

public class SettingsMapDuringNavigationFragment extends SettingsBaseProfileDependentFragment {

	public static final String TAG = "SettingsMapDuringNavigationFragment";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		return view;
	}

	@Override
	protected int getPreferenceResId() {
		return R.xml.map_during_navigation;
	}

	@Override
	protected int getToolbarResId() {
		return R.layout.profile_preference_toolbar;
	}

	protected String getToolbarTitle() {
		return getString(R.string.map_during_navigation);
	}

	protected void createUI() {
		PreferenceScreen screen = getPreferenceScreen();

		int[] intValues = new int[]{0, 5, 10, 15, 20, 25, 30, 45, 60, 90};
		String[] entries = new String[intValues.length];
		entries[0] = getString(R.string.shared_string_never);
		for (int i = 1; i < intValues.length; i++) {
			entries[i] = intValues[i] + " " + getString(R.string.int_seconds);
		}
		ListIntPreference autoFollowRoute = (ListIntPreference) screen.findPreference(settings.AUTO_FOLLOW_ROUTE.getId());
		autoFollowRoute.setEntries(entries);
		autoFollowRoute.setEntryValues(intValues);

		Preference autoZoomMap = screen.findPreference(settings.AUTO_ZOOM_MAP.getId());
		autoZoomMap.setOnPreferenceClickListener(this);

		SwitchPreference snapToRoad = (SwitchPreference) screen.findPreference(settings.SNAP_TO_ROAD.getId());

		String[] speedNamesPos;
		float[] speedLimitsPos;
		if (settings.METRIC_SYSTEM.get() == OsmandSettings.MetricsConstants.KILOMETERS_AND_METERS) {
			speedLimitsPos = new float[]{0f, 5f, 7f, 10f, 15f, 20f};
			speedNamesPos = new String[speedLimitsPos.length];
			for (int i = 0; i < speedLimitsPos.length; i++) {
				speedNamesPos[i] = (int) speedLimitsPos[i] + " " + getString(R.string.km_h);
			}
		} else {
			Float[] speedLimitsMiles = new Float[]{-7f, -5f, -3f, 0f, 3f, 5f, 7f, 10f, 15f};
			speedLimitsPos = new float[]{0f, 3f, 5f, 7f, 10f, 15f};

			String[] speedNames = new String[speedLimitsMiles.length];
			for (int i = 0; i < speedNames.length; i++) {
				speedNames[i] = speedLimitsMiles[i].intValue() + " " + getString(R.string.mile_per_hour);
			}
			speedNamesPos = new String[speedLimitsPos.length];
			for (int i = 0; i < speedNamesPos.length; i++) {
				speedNamesPos[i] = speedLimitsMiles[i].intValue() + " " + getString(R.string.mile_per_hour);
			}
		}

		ListFloatPreference switchMapDirectionToCompass = (ListFloatPreference) screen.findPreference(settings.SWITCH_MAP_DIRECTION_TO_COMPASS.getId());
		switchMapDirectionToCompass.setEntries(speedNamesPos);
		switchMapDirectionToCompass.setEntryValues(speedLimitsPos);
	}

	@Override
	public void onDisplayPreferenceDialog(Preference preference) {
		String key = preference.getKey();
		if (key != null && key.equals(settings.AUTO_ZOOM_MAP.getId())) {
			Toast.makeText(getContext(), "onDisplayPreferenceDialog AUTO_ZOOM_MAP", Toast.LENGTH_LONG).show();
		}
		super.onDisplayPreferenceDialog(preference);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String key = preference.getKey();
		if (key != null && key.equals(settings.AUTO_ZOOM_MAP.getId())) {
			Toast.makeText(getContext(), "onPreferenceClick AUTO_ZOOM_MAP", Toast.LENGTH_LONG).show();
		}
		return super.onPreferenceClick(preference);
	}

	public static boolean showInstance(FragmentManager fragmentManager) {
		try {
			SettingsMapDuringNavigationFragment settingsNavigationFragment = new SettingsMapDuringNavigationFragment();
			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, settingsNavigationFragment, SettingsMapDuringNavigationFragment.TAG)
					.addToBackStack(SettingsMapDuringNavigationFragment.TAG)
					.commitAllowingStateLoss();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
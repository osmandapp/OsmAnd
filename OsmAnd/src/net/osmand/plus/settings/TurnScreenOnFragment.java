package net.osmand.plus.settings;

import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.app.FragmentManager;
import android.support.v7.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.R;
import net.osmand.plus.views.ListIntPreference;

public class TurnScreenOnFragment extends BaseProfileSettingsFragment {

	public static final String TAG = "TurnScreenOnFragment";

	@Override
	protected int getPreferenceResId() {
		return R.xml.turn_screen_on;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		return view;
	}

	@Override
	protected int getToolbarResId() {
		return R.layout.profile_preference_toolbar;
	}

	protected String getToolbarTitle() {
		return getString(R.string.turn_screen_on);
	}

	@Override
	protected void createUI() {
		PreferenceScreen screen = getPreferenceScreen();

		SwitchPreference turnScreenOn = (SwitchPreference) findAndRegisterPreference(settings.TURN_SCREEN_ON.getId());

		int[] screenPowerSaveValues = new int[]{0, 5, 10, 15, 20, 30, 45, 60};
		String[] screenPowerSaveNames = new String[screenPowerSaveValues.length];
		screenPowerSaveNames[0] = getString(R.string.shared_string_never);
		for (int i = 1; i < screenPowerSaveValues.length; i++) {
			screenPowerSaveNames[i] = screenPowerSaveValues[i] + " " + getString(R.string.int_seconds);
		}

		ListIntPreference turnScreenOnTime = (ListIntPreference) findAndRegisterPreference(settings.TURN_SCREEN_ON_TIME_INT.getId());
		turnScreenOnTime.setEntries(screenPowerSaveNames);
		turnScreenOnTime.setEntryValues(screenPowerSaveValues);

		SwitchPreference turnScreenOnSensor = (SwitchPreference) findAndRegisterPreference(settings.TURN_SCREEN_ON_SENSOR.getId());
	}

	public static boolean showInstance(FragmentManager fragmentManager) {
		try {
			TurnScreenOnFragment settingsNavigationFragment = new TurnScreenOnFragment();
			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, settingsNavigationFragment, TurnScreenOnFragment.TAG)
					.addToBackStack(TurnScreenOnFragment.TAG)
					.commitAllowingStateLoss();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
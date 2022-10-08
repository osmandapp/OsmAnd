package net.osmand.plus.plugins.antplus;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.antplus.devices.CommonDevice;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.util.Algorithms;

import java.util.List;

public class AntPlusSettingsFragment extends BaseSettingsFragment {

	@Override
	protected void setupPreferences() {
		AntPlusPlugin plugin = PluginsHelper.getPlugin(AntPlusPlugin.class);
		Preference developmentInfo = findPreference("antplus_info");
		developmentInfo.setIcon(getContentIcon(R.drawable.ic_action_info_dark));

		List<CommonDevice<?>> commonDevices = plugin.getDevices();
		if (!Algorithms.isEmpty(commonDevices)) {
			setupCommonDevicesPrefs(commonDevices);
		}
	}

	private void setupCommonDevicesPrefs(@NonNull List<CommonDevice<?>> commonDevices) {
		PreferenceScreen screen = getPreferenceScreen();
		for (CommonDevice device : commonDevices) {
			WidgetType widgetType = device.getDeviceWidgetType();

			PreferenceCategory category = new PreferenceCategory(screen.getContext());
			category.setTitle(widgetType.titleId);
			category.setLayoutResource(R.layout.preference_category_with_descr);
			category.setIconSpaceReserved(false);
			screen.addPreference(category);

			String deviceEnabledPrefId = device.getDeviceEnabledPref().getId();
			SwitchPreferenceEx deviceEnabledPref = createSwitchPreferenceEx(deviceEnabledPrefId, R.string.ant_read_data, R.layout.preference_with_descr_dialog_and_switch);
			deviceEnabledPref.setSummaryOn(R.string.shared_string_enabled);
			deviceEnabledPref.setSummaryOff(R.string.shared_string_disabled);
			deviceEnabledPref.setIconSpaceReserved(false);
			screen.addPreference(deviceEnabledPref);

			String writePrefId = device.getWriteGpxPref().getId();
			SwitchPreferenceEx deviceWritePref = createSwitchPreferenceEx(writePrefId, R.string.ant_write_to_gpx, R.layout.preference_with_descr_dialog_and_switch);
			deviceWritePref.setSummaryOn(R.string.shared_string_enabled);
			deviceWritePref.setSummaryOff(R.string.shared_string_disabled);
			deviceWritePref.setIconSpaceReserved(false);
			screen.addPreference(deviceWritePref);
		}
	}
}

package net.osmand.plus.settings;

import android.graphics.drawable.ColorDrawable;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;

public class ScreenAlertsFragment extends BaseSettingsFragment {

	public static final String TAG = "ScreenAlertsFragment";

	@Override
	protected int getPreferencesResId() {
		return R.xml.screen_alerts;
	}

	@Override
	protected int getToolbarResId() {
		return R.layout.profile_preference_toolbar;
	}

	@Override
	protected int getToolbarTitle() {
		return R.string.screen_alerts;
	}

	@Override
	protected void setupPreferences() {
		Preference showRoutingAlarmsInfo = findPreference("show_routing_alarms_info");
		SwitchPreference showTrafficWarnings = (SwitchPreference) findPreference(settings.SHOW_TRAFFIC_WARNINGS.getId());
		SwitchPreference showPedestrian = (SwitchPreference) findPreference(settings.SHOW_PEDESTRIAN.getId());
		SwitchPreference showCameras = (SwitchPreference) findPreference(settings.SHOW_CAMERAS.getId());
		SwitchPreference showTunnels = (SwitchPreference) findPreference(settings.SHOW_TUNNELS.getId());

		showRoutingAlarmsInfo.setIcon(getContentIcon(R.drawable.ic_action_info_dark));
		showTrafficWarnings.setIcon(getIcon(R.drawable.list_warnings_traffic_calming));
		showPedestrian.setIcon(getIcon(R.drawable.list_warnings_pedestrian));
		showCameras.setIcon(getIcon(R.drawable.list_warnings_speed_camera));
		showTunnels.setIcon(getIcon(R.drawable.list_warnings_tunnel));
	}

	@Override
	protected void onBindPreferenceViewHolder(Preference preference, PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);

		if (settings.SHOW_ROUTING_ALARMS.getId().equals(preference.getKey())) {
			boolean checked = ((SwitchPreference) preference).isChecked();
			int color = checked ? getActiveProfileColor() : ContextCompat.getColor(app, R.color.preference_top_switch_off);

			AndroidUtils.setBackground(holder.itemView, new ColorDrawable(color));
		}
	}
}
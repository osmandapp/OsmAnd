package net.osmand.plus.settings;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.widget.ImageView;

import net.osmand.plus.R;

public class ScreenAlertsFragment extends BaseSettingsFragment {

	public static final String TAG = "ScreenAlertsFragment";

	private static final String SHOW_ROUTING_ALARMS_INFO = "show_routing_alarms_info";
	private static final String SCREEN_ALERTS_IMAGE = "screen_alerts_image";

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
		Preference showRoutingAlarmsInfo = findPreference(SHOW_ROUTING_ALARMS_INFO);
		SwitchPreferenceCompat showTrafficWarnings = (SwitchPreferenceCompat) findPreference(settings.SHOW_TRAFFIC_WARNINGS.getId());
		SwitchPreferenceCompat showPedestrian = (SwitchPreferenceCompat) findPreference(settings.SHOW_PEDESTRIAN.getId());
		SwitchPreferenceCompat showCameras = (SwitchPreferenceCompat) findPreference(settings.SHOW_CAMERAS.getId());
		SwitchPreferenceCompat showTunnels = (SwitchPreferenceCompat) findPreference(settings.SHOW_TUNNELS.getId());

		showRoutingAlarmsInfo.setIcon(getContentIcon(R.drawable.ic_action_info_dark));
		showTrafficWarnings.setIcon(getIcon(R.drawable.list_warnings_traffic_calming));
		showPedestrian.setIcon(getIcon(R.drawable.list_warnings_pedestrian));
		showCameras.setIcon(getIcon(R.drawable.list_warnings_speed_camera));
		showTunnels.setIcon(getIcon(R.drawable.list_warnings_tunnel));

		setupScreenAlertsImage();
	}

	@Override
	protected void onBindPreferenceViewHolder(Preference preference, PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);

		String key = preference.getKey();
		if (settings.SHOW_ROUTING_ALARMS.getId().equals(key)) {
			boolean checked = ((SwitchPreferenceCompat) preference).isChecked();
			int color = checked ? getActiveProfileColor() : ContextCompat.getColor(app, R.color.preference_top_switch_off);

			holder.itemView.setBackgroundColor(color);
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			if (SHOW_ROUTING_ALARMS_INFO.equals(key)) {
				int colorRes = isNightMode() ? R.color.activity_background_color_dark : R.color.activity_background_color_light;
				holder.itemView.setBackgroundColor(ContextCompat.getColor(app, colorRes));
			} else if (SCREEN_ALERTS_IMAGE.equals(key)) {
				ImageView deviceImage = (ImageView) holder.itemView.findViewById(R.id.device_image);
				ImageView warningIcon = (ImageView) holder.itemView.findViewById(R.id.warning_icon);

				deviceImage.setImageDrawable(getDeviceImage());
				warningIcon.setImageDrawable(getWarningIcon());
			}
		}
	}

	@SuppressLint("RestrictedApi")
	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			Preference routeParametersImage = findPreference(SCREEN_ALERTS_IMAGE);
			updatePreference(routeParametersImage);
		}

		return super.onPreferenceClick(preference);
	}

	private void setupScreenAlertsImage() {
		Preference routeParametersImage = findPreference(SCREEN_ALERTS_IMAGE);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
			routeParametersImage.setVisible(false);
		}
	}

	private Drawable getDeviceImage() {
		int bgResId = isNightMode() ? R.drawable.img_settings_device_bottom_dark : R.drawable.img_settings_device_bottom_light;
		return app.getUIUtilities().getLayeredIcon(bgResId, R.drawable.img_settings_sreen_route_alerts);
	}

	private Drawable getWarningIcon() {
		boolean americanSigns = settings.DRIVING_REGION.get().americanSigns;
		if (settings.SHOW_TRAFFIC_WARNINGS.get()) {
			return getIcon(americanSigns ? R.drawable.warnings_traffic_calming_us : R.drawable.warnings_traffic_calming);
		} else if (settings.SHOW_PEDESTRIAN.get()) {
			return getIcon(americanSigns ? R.drawable.warnings_pedestrian_us : R.drawable.warnings_pedestrian);
		} else if (settings.SHOW_CAMERAS.get()) {
			return getIcon(R.drawable.warnings_speed_camera);
		} else if (settings.SHOW_TUNNELS.get()) {
			return getIcon(americanSigns ? R.drawable.warnings_tunnel_us : R.drawable.warnings_tunnel);
		}

		return null;
	}
}
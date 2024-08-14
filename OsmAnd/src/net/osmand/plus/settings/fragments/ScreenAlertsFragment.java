package net.osmand.plus.settings.fragments;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreferenceCompat;

import net.osmand.plus.settings.enums.DrivingRegion;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.dialogs.SpeedCamerasBottomSheet;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;


import static net.osmand.plus.utils.UiUtilities.CompoundButtonType.TOOLBAR;

public class ScreenAlertsFragment extends BaseSettingsFragment {

	public static final String TAG = ScreenAlertsFragment.class.getSimpleName();

	private static final String SHOW_ROUTING_ALARMS_INFO = "show_routing_alarms_info";
	private static final String SCREEN_ALERTS_IMAGE = "screen_alerts_image";

	@Override
	protected void setupPreferences() {
		Preference showRoutingAlarmsInfo = findPreference(SHOW_ROUTING_ALARMS_INFO);
		SwitchPreferenceCompat showTrafficWarnings = findPreference(settings.SHOW_TRAFFIC_WARNINGS.getId());
		SwitchPreferenceCompat showSpeedLimitWarnings = findPreference(settings.SHOW_SPEED_LIMIT_WARNINGS.getId());
		SwitchPreferenceCompat showPedestrian = findPreference(settings.SHOW_PEDESTRIAN.getId());
		SwitchPreferenceCompat showTunnels = findPreference(settings.SHOW_TUNNELS.getId());

		showRoutingAlarmsInfo.setIcon(getContentIcon(R.drawable.ic_action_info_dark));
		showTrafficWarnings.setIcon(getIcon(R.drawable.list_warnings_traffic_calming));
		int speedLimitIcon = getSpeedLimitIcon();
		showSpeedLimitWarnings.setIcon(speedLimitIcon);
		showPedestrian.setIcon(getIcon(R.drawable.list_warnings_pedestrian));
		showTunnels.setIcon(getIcon(R.drawable.list_warnings_tunnel));

		setupScreenAlertsImage();
		setupShowCamerasPref();
		setupSpeedCamerasAlert();
		enableDisablePreferences(settings.SHOW_ROUTING_ALARMS.getModeValue(getSelectedAppMode()));
	}

	private int getSpeedLimitIcon() {
		int speedLimitIcon;
		if (isUsaRegion()) {
			speedLimitIcon = R.drawable.list_warnings_speed_limit_us;
		} else if (isCanadaRegion()) {
			speedLimitIcon = R.drawable.list_warnings_speed_limit_ca;
		} else {
			speedLimitIcon = R.drawable.list_warnings_limit;
		}
		return speedLimitIcon;
	}

	private boolean isUsaRegion() {
		ApplicationMode mode = app.getSettings().getApplicationMode();
		return settings.DRIVING_REGION.getModeValue(mode) == DrivingRegion.US;
	}

	private boolean isCanadaRegion() {
		ApplicationMode mode = app.getSettings().getApplicationMode();
		return settings.DRIVING_REGION.getModeValue(mode) == DrivingRegion.CANADA;
	}

	@Override
	protected void createToolbar(@NonNull LayoutInflater inflater, @NonNull View view) {
		super.createToolbar(inflater, view);

		view.findViewById(R.id.toolbar_switch_container).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				ApplicationMode selectedMode = getSelectedAppMode();
				boolean checked = !settings.SHOW_ROUTING_ALARMS.getModeValue(selectedMode);
				onConfirmPreferenceChange(
						settings.SHOW_ROUTING_ALARMS.getId(), checked, ApplyQueryType.SNACK_BAR);
				updateToolbarSwitch();
				enableDisablePreferences(checked);
			}
		});
	}

	@Override
	protected void updateToolbar() {
		super.updateToolbar();
		updateToolbarSwitch();
	}

	private void updateToolbarSwitch() {
		View view = getView();
		if (view == null) {
			return;
		}
		boolean checked = settings.SHOW_ROUTING_ALARMS.getModeValue(getSelectedAppMode());

		int color = checked ? getActiveProfileColor() : ContextCompat.getColor(app, R.color.preference_top_switch_off);
		View switchContainer = view.findViewById(R.id.toolbar_switch_container);
		AndroidUtils.setBackground(switchContainer, new ColorDrawable(color));

		SwitchCompat switchView = switchContainer.findViewById(R.id.switchWidget);
		switchView.setChecked(checked);
		UiUtilities.setupCompoundButton(switchView, isNightMode(), TOOLBAR);

		TextView title = switchContainer.findViewById(R.id.switchButtonText);
		title.setText(checked ? R.string.shared_string_on : R.string.shared_string_off);
	}

	@Override
	protected void onBindPreferenceViewHolder(@NonNull Preference preference, @NonNull PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);

		String key = preference.getKey();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			if (SHOW_ROUTING_ALARMS_INFO.equals(key)) {
				holder.itemView.setBackgroundColor(ColorUtilities.getActivityBgColor(app, isNightMode()));
			} else if (SCREEN_ALERTS_IMAGE.equals(key)) {
				ImageView deviceImage = holder.itemView.findViewById(R.id.device_image);
				ImageView warningIcon = holder.itemView.findViewById(R.id.warning_icon);

				deviceImage.setImageDrawable(getDeviceImage());
				warningIcon.setImageDrawable(getWarningIcon());
			} else if (settings.SPEED_CAMERAS_UNINSTALLED.getId().equals(key)) {
				setupPrefRoundedBg(holder);
			}
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			Preference routeParametersImage = findPreference(SCREEN_ALERTS_IMAGE);
			updatePreference(routeParametersImage);
		}
		if (settings.SPEED_CAMERAS_UNINSTALLED.getId().equals(preference.getKey())) {
			SpeedCamerasBottomSheet.showInstance(requireActivity().getSupportFragmentManager(), this);
		}
		return super.onPreferenceClick(preference);
	}

	@Override
	public void onPreferenceChanged(@NonNull String prefId) {
		if (prefId.equals(settings.SPEED_CAMERAS_UNINSTALLED.getId())) {
			setupShowCamerasPref();
			setupSpeedCamerasAlert();
		}
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
		ApplicationMode selectedMode = getSelectedAppMode();
		boolean americanSigns = settings.DRIVING_REGION.getModeValue(selectedMode).isAmericanTypeSigns();
		if (settings.SHOW_TRAFFIC_WARNINGS.getModeValue(selectedMode)) {
			return getIcon(americanSigns ? R.drawable.warnings_traffic_calming_us : R.drawable.warnings_traffic_calming);
		} else if (settings.SHOW_PEDESTRIAN.getModeValue(selectedMode)) {
			return getIcon(americanSigns ? R.drawable.warnings_pedestrian_us : R.drawable.warnings_pedestrian);
		} else if (settings.SHOW_CAMERAS.getModeValue(selectedMode) && !settings.SPEED_CAMERAS_UNINSTALLED.get()) {
			return getIcon(R.drawable.warnings_speed_camera);
		} else if (settings.SHOW_TUNNELS.getModeValue(selectedMode)) {
			return getIcon(americanSigns ? R.drawable.warnings_tunnel_us : R.drawable.warnings_tunnel);
		}

		return null;
	}

	private void setupShowCamerasPref() {
		SwitchPreferenceCompat showCameras = findPreference(settings.SHOW_CAMERAS.getId());
		showCameras.setIcon(getIcon(R.drawable.list_warnings_speed_camera));
		showCameras.setVisible(!settings.SPEED_CAMERAS_UNINSTALLED.get());
	}
}
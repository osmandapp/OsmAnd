package net.osmand.plus.settings.fragments;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreferenceCompat;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.dialogs.SpeedCamerasBottomSheet;
import net.osmand.plus.dialogs.SpeedCamerasBottomSheet.OnSpeedCamerasUninstallListener;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;

import java.lang.ref.WeakReference;

import static net.osmand.plus.UiUtilities.CompoundButtonType.TOOLBAR;

public class ScreenAlertsFragment extends BaseSettingsFragment implements OnSpeedCamerasUninstallListener {

	public static final String TAG = ScreenAlertsFragment.class.getSimpleName();

	private static final String SHOW_ROUTING_ALARMS_INFO = "show_routing_alarms_info";
	private static final String SCREEN_ALERTS_IMAGE = "screen_alerts_image";
	private static final String SHOW_CAMERAS = "show_cameras";
	private SwitchPreferenceCompat showCameras;

	@Override
	protected void setupPreferences() {
		Preference showRoutingAlarmsInfo = findPreference(SHOW_ROUTING_ALARMS_INFO);
		SwitchPreferenceCompat showTrafficWarnings = (SwitchPreferenceCompat) findPreference(settings.SHOW_TRAFFIC_WARNINGS.getId());
		SwitchPreferenceCompat showPedestrian = (SwitchPreferenceCompat) findPreference(settings.SHOW_PEDESTRIAN.getId());
		SwitchPreferenceCompat showTunnels = (SwitchPreferenceCompat) findPreference(settings.SHOW_TUNNELS.getId());
		showCameras = (SwitchPreferenceCompat) findPreference(settings.SHOW_CAMERAS.getId());

		showRoutingAlarmsInfo.setIcon(getContentIcon(R.drawable.ic_action_info_dark));
		showTrafficWarnings.setIcon(getIcon(R.drawable.list_warnings_traffic_calming));
		showPedestrian.setIcon(getIcon(R.drawable.list_warnings_pedestrian));
		showTunnels.setIcon(getIcon(R.drawable.list_warnings_tunnel));
		showCameras.setIcon(getIcon(R.drawable.list_warnings_speed_camera));

		setupScreenAlertsImage();
		enableDisablePreferences(settings.SHOW_ROUTING_ALARMS.getModeValue(getSelectedAppMode()));
		if (settings.SPEED_CAMERAS_UNINSTALLED.get()) {
			onSpeedCamerasUninstalled();
		}
	}

	@Override
	protected void createToolbar(LayoutInflater inflater, View view) {
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

		SwitchCompat switchView = (SwitchCompat) switchContainer.findViewById(R.id.switchWidget);
		switchView.setChecked(checked);
		UiUtilities.setupCompoundButton(switchView, isNightMode(), TOOLBAR);

		TextView title = switchContainer.findViewById(R.id.switchButtonText);
		title.setText(checked ? R.string.shared_string_on : R.string.shared_string_off);
	}

	@Override
	protected void onBindPreferenceViewHolder(Preference preference, PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);

		String key = preference.getKey();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			if (SHOW_ROUTING_ALARMS_INFO.equals(key)) {
				int colorRes = isNightMode() ? R.color.activity_background_color_dark : R.color.activity_background_color_light;
				holder.itemView.setBackgroundColor(ContextCompat.getColor(app, colorRes));
			} else if (SCREEN_ALERTS_IMAGE.equals(key)) {
				ImageView deviceImage = (ImageView) holder.itemView.findViewById(R.id.device_image);
				ImageView warningIcon = (ImageView) holder.itemView.findViewById(R.id.warning_icon);

				deviceImage.setImageDrawable(getDeviceImage());
				warningIcon.setImageDrawable(getWarningIcon());
			} else if (SHOW_CAMERAS.equals(key)) {
				setupSpeedCamerasAlert(app, requireMyActivity(), holder, this, isNightMode());
			}
		}
	}

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
		ApplicationMode selectedMode = getSelectedAppMode();
		boolean americanSigns = settings.DRIVING_REGION.getModeValue(selectedMode).isAmericanTypeSigns();
		if (settings.SHOW_TRAFFIC_WARNINGS.getModeValue(selectedMode)) {
			return getIcon(americanSigns ? R.drawable.warnings_traffic_calming_us : R.drawable.warnings_traffic_calming);
		} else if (settings.SHOW_PEDESTRIAN.getModeValue(selectedMode)) {
			return getIcon(americanSigns ? R.drawable.warnings_pedestrian_us : R.drawable.warnings_pedestrian);
		} else if (settings.SHOW_CAMERAS.getModeValue(selectedMode)) {
			return getIcon(R.drawable.warnings_speed_camera);
		} else if (settings.SHOW_TUNNELS.getModeValue(selectedMode)) {
			return getIcon(americanSigns ? R.drawable.warnings_tunnel_us : R.drawable.warnings_tunnel);
		}

		return null;
	}

	public static void setupSpeedCamerasAlert(@NonNull OsmandApplication app,
											  @NonNull FragmentActivity activity,
											  @NonNull PreferenceViewHolder holder,
											  @Nullable Fragment targetFragment,
											  boolean nightMode) {
		ImageView alertIcon = (ImageView) holder.itemView.findViewById(R.id.alert_icon);
		TextView alertTitle = (TextView) holder.itemView.findViewById(R.id.alert_title);
		TextView alertSubTitle = (TextView) holder.itemView.findViewById(R.id.alert_subtitle);
		LinearLayout alertBg = (LinearLayout) holder.itemView.findViewById(R.id.alert_bg);

		alertBg.setBackgroundDrawable(UiUtilities.getRoundedBackgroundDrawable(
				app,
				nightMode ? R.color.activity_background_color_dark : R.color.activity_background_color_light,
				6));
		alertIcon.setImageDrawable(app.getUIUtilities().getIcon(
				R.drawable.ic_action_alert,
				nightMode ? R.color.icon_color_default_dark : R.color.icon_color_default_light));
		alertTitle.setText(R.string.speed_cameras_alert);
		alertTitle.setTypeface(FontCache.getRobotoMedium(app));
		alertSubTitle.setText(R.string.read_more);
		alertSubTitle.setTypeface(FontCache.getRobotoMedium(app));
		alertSubTitle.setTextColor(nightMode
				? app.getResources().getColor(R.color.active_color_primary_dark)
				: app.getResources().getColor(R.color.active_color_primary_light));
		final WeakReference<FragmentActivity> weakActivity = new WeakReference<>(activity);
		final WeakReference<Fragment> weakFragment = new WeakReference<>(targetFragment);
		alertSubTitle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				FragmentActivity a = weakActivity.get();
				if (a != null) {
					SpeedCamerasBottomSheet.showInstance(a.getSupportFragmentManager(), weakFragment.get());
				}
			}
		});
	}

	@Override
	public void onSpeedCamerasUninstalled() {
		if (showCameras != null) {
			showCameras.setVisible(false);
		}
	}
}
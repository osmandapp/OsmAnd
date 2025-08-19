package net.osmand.plus.views.mapwidgets.configure.settings;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.externalsensors.DeviceType;
import net.osmand.plus.plugins.externalsensors.ExternalSensorsPlugin;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorTextWidget;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;
import net.osmand.plus.plugins.externalsensors.dialogs.SelectExternalDeviceFragment;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.enums.ExternalDeviceShowMode;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;

public class SensorWidgetSettingFragment extends BaseSimpleWidgetInfoFragment implements SelectExternalDeviceFragment.SelectDeviceListener {

	private static final String SHOW_DATA_MODE = "show_data_mode";

	private SensorTextWidget sensorWidget;
	protected ExternalSensorsPlugin plugin;

	@Nullable
	private OsmandPreference<ExternalDeviceShowMode> showModePreference;

	private String sourceDeviceId;
	private int selectedShowMode;
	private TextView showModeDescription;

	private AppCompatImageView deviceIcon;
	private AppCompatImageView showIcon;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		plugin = PluginsHelper.getPlugin(ExternalSensorsPlugin.class);
	}

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);
		if (widgetInfo == null) {
			widgetInfo = widgetRegistry.getWidgetInfoById(widgetId);
		}
		if (widgetInfo != null) {
			sensorWidget = ((SensorTextWidget) widgetInfo.widget);
			showModePreference = sensorWidget.getPreference();
			setupDataSource();
		} else {
			dismiss();
		}
		selectedShowMode = bundle.getInt(SHOW_DATA_MODE, showModePreference != null ?
				showModePreference.getModeValue(appMode).ordinal() : ExternalDeviceShowMode.SENSOR_DATA.ordinal());
	}

	@Override
	public void selectNewDevice(@Nullable String deviceId, @NonNull SensorWidgetDataFieldType requestedWidgetDataFieldType) {
		sourceDeviceId = deviceId;
		updateSourceDeviceUI();
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		inflate(R.layout.sensor_widget_settings_fragment, container);
		deviceIcon = view.findViewById(R.id.device_icon);
		showIcon = view.findViewById(R.id.show_icon);
		showModeDescription = container.findViewById(R.id.show);
		view.findViewById(R.id.widget_source_card).setOnClickListener((v) ->
				SelectExternalDeviceFragment.showInstance(requireActivity().getSupportFragmentManager(),
						this, sensorWidget.getFieldType(), getSourceDeviceId(), false));
		View showSourceButton = view.findViewById(R.id.widget_show_card);
		showSourceButton.setOnClickListener(v -> showShowModeDialog());
		AndroidUiHelper.updateVisibility(showSourceButton, true);
		updateSourceDeviceUI();
		updateShowModeDescription();
	}

	@Override
	public void onResume() {
		super.onResume();
		updateShowModeDescription();
	}

	private void showShowModeDialog() {
		CharSequence[] items = new CharSequence[ExternalDeviceShowMode.values().length];
		for (int i = 0; i < ExternalDeviceShowMode.values().length; i++) {
			items[i] = getString(ExternalDeviceShowMode.values()[i].getTitleId());
		}
		AlertDialogData dialogData = new AlertDialogData(requireActivity(), nightMode)
				.setTitle(R.string.shared_string_mode)
				.setControlsColor(ColorUtilities.getActiveColor(app, nightMode));

		CustomAlert.showSingleSelection(dialogData, items, selectedShowMode, v -> {
			selectedShowMode = (int) v.getTag();
			updateShowModeDescription();
		});
	}

	private void updateShowModeDescription() {
		showModeDescription.setText(getString(ExternalDeviceShowMode.values()[selectedShowMode].getTitleId()));
		SensorWidgetDataFieldType fieldType = sensorWidget.getFieldType();
		int iconId = selectedShowMode == ExternalDeviceShowMode.SENSOR_DATA.ordinal() ? fieldType.disconnectedIconId : fieldType.disconnectedBatteryIconId;
		showIcon.setImageDrawable(getContentIcon(iconId));
	}

	@NonNull
	private String getSourceDeviceId() {
		return sourceDeviceId == null ? "" : sourceDeviceId;
	}

	private void setupDataSource() {
		if (sensorWidget != null) {
			sourceDeviceId = sensorWidget.getDeviceId(appMode);
		}
	}

	private void updateSourceDeviceUI() {
		AbstractDevice<?> sourceDevice = plugin.getDevice(sourceDeviceId);
		if (sourceDevice == null) {
			deviceIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_sensor));
			AbstractDevice<?> connectedDevice = null;
			if (sensorWidget != null) {
				if (sensorWidget.getWidgetDevice() != null) {
					connectedDevice = sensorWidget.getWidgetDevice();
				} else {
					connectedDevice = plugin.getAnyDevice(sensorWidget.getFieldType());
				}
			}
			String prompt;
			if (connectedDevice != null) {
				prompt = String.format(getString(R.string.any_connected_with_device), connectedDevice.getName());
			} else {
				prompt = getString(R.string.any_connected);
			}
			((TextView) view.findViewById(R.id.device_name)).setText(prompt);
		} else {
			((TextView) view.findViewById(R.id.device_name)).setText(sourceDevice.getName());
			DeviceType deviceType = sourceDevice.getDeviceType();
			deviceIcon.setImageResource(nightMode ? deviceType.nightIconId : deviceType.dayIconId);
		}
	}

	@Override
	protected void applySettings() {
		super.applySettings();
		sensorWidget.setDeviceId(getSourceDeviceId());
		if (showModePreference != null) {
			showModePreference.setModeValue(appMode, ExternalDeviceShowMode.values()[selectedShowMode]);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(SHOW_DATA_MODE, selectedShowMode);
	}
}
package net.osmand.plus.plugins.externalsensors.dialogs;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.externalsensors.adapters.DevicesForWidgetAdapter;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice.DeviceListener;
import net.osmand.plus.plugins.externalsensors.devices.DeviceConnectionResult;
import net.osmand.plus.plugins.externalsensors.devices.sensors.AbstractSensor;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.configure.settings.SensorWidgetSettingFragment;

import java.util.ArrayList;
import java.util.List;

public class SelectExternalDeviceFragment extends ExternalDevicesBaseFragment implements DevicesForWidgetAdapter.SelectDeviceListener,
		DeviceListener {

	public static final String TAG = SelectExternalDeviceFragment.class.getSimpleName();
	public static final String WIDGET_TYPE_KEY = "WIDGET_TYPE_KEY";
	public static final String SELECTED_DEVICE_ID_KEY = "SELECTED_DEVICE_ID_KEY";
	protected View emptyView;
	protected View contentView;
	protected RecyclerView devicesList;
	private DevicesForWidgetAdapter devicesListAdapter;
	private AppBarLayout appBar;
	private View noBluetoothCard;
	private String selectedDeviceId;
	private SensorWidgetDataFieldType widgetDataFieldType;

	@Override
	protected int getLayoutId() {
		return R.layout.fragment_select_external_sensors;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle arguments = getArguments();
		if (arguments != null) {
			int widgetFieldTypeOrdinal = arguments.getInt(WIDGET_TYPE_KEY);
			widgetDataFieldType = SensorWidgetDataFieldType.values()[widgetFieldTypeOrdinal];
			selectedDeviceId = arguments.getString(SELECTED_DEVICE_ID_KEY);
		}
	}

	@Override
	protected void setupUI(@NonNull View view) {
		super.setupUI(view);
		emptyView = view.findViewById(R.id.empty_view);
		contentView = view.findViewById(R.id.devices_content);
		devicesList = view.findViewById(R.id.connected_devices_list);
		appBar = view.findViewById(R.id.appbar);
		noBluetoothCard = view.findViewById(R.id.no_bluetooth_card);

		setupPairSensorButton(view.findViewById(R.id.pair_btn_empty));
		setupPairSensorButton(view.findViewById(R.id.pair_btn_additional));
		setupOpenBtSettingsButton(view.findViewById(R.id.bt_settings_button_container));
		devicesListAdapter = new DevicesForWidgetAdapter(app, nightMode, this);
		devicesListAdapter.setDeviceId(selectedDeviceId);
		devicesList.setAdapter(devicesListAdapter);
	}

	private void setupPairSensorButton(@NonNull View view) {
		View dismissButton = view.findViewById(R.id.dismiss_button);
		int buttonTextId = R.string.ant_plus_pair_new_sensor;
		ViewGroup.LayoutParams layoutParams = dismissButton.getLayoutParams();
		UiUtilities.setupDialogButton(nightMode, dismissButton, UiUtilities.DialogButtonType.SECONDARY, buttonTextId);
		layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
		dismissButton.setLayoutParams(layoutParams);
		view.requestLayout();
		dismissButton.setOnClickListener(v -> showPairNewSensorBottomSheet());
		AndroidUiHelper.updateVisibility(dismissButton, true);
	}

	private void setupOpenBtSettingsButton(@NonNull View view) {
		View dismissButton = view.findViewById(R.id.dismiss_button);
		int buttonTextId = R.string.ant_plus_open_settings;
		ViewGroup.LayoutParams layoutParams = dismissButton.getLayoutParams();
		UiUtilities.setupDialogButton(nightMode, dismissButton, UiUtilities.DialogButtonType.SECONDARY, buttonTextId);
		layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
		dismissButton.setLayoutParams(layoutParams);
		view.requestLayout();
		dismissButton.setOnClickListener(v -> {
			Intent intentOpenBluetoothSettings = new Intent();
			intentOpenBluetoothSettings.setAction(Settings.ACTION_BLUETOOTH_SETTINGS);
			startActivity(intentOpenBluetoothSettings);
		});
		AndroidUiHelper.updateVisibility(dismissButton, true);
	}

	private void showPairNewSensorBottomSheet() {
		PairNewDeviceBottomSheet.showInstance(requireActivity().getSupportFragmentManager());
	}

	@Override
	protected void setupToolbar(@NonNull View view) {
		super.setupToolbar(view);
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setOnMenuItemClickListener(item -> {
			if (item.getItemId() == R.id.action_add) {
				showPairNewSensorBottomSheet();
			}
			return false;
		});
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		contentView = null;
		emptyView = null;
		devicesList = null;
		appBar = null;
		noBluetoothCard = null;
	}

	@Override
	public void onResume() {
		super.onResume();
		noBluetoothCard.setVisibility(plugin.isBlueToothEnabled() ? View.GONE : View.VISIBLE);
		updatePairedSensorsListeners(true);
		updatePairedSensorsList();
	}

	@Override
	public void onPause() {
		super.onPause();
		updatePairedSensorsListeners(false);
	}

	private void updatePairedSensorsListeners(boolean add) {
		List<AbstractDevice<?>> devices = plugin.getPairedDevices();
		for (AbstractDevice<?> device : devices) {
			if (add) {
				device.addListener(this);
			} else {
				device.removeListener(this);
			}
		}
	}

	private void updatePairedSensorsList() {
		List<AbstractDevice<?>> devices = plugin.getPairedDevices();
		ArrayList<AbstractDevice<?>> filteredDevices = new ArrayList<>();
		for (AbstractDevice<?> device : devices) {
			if (isDeviceForWidgetFieldType(device)) {
				filteredDevices.add(device);
			}
		}
		if (devices.isEmpty()) {
			emptyView.setVisibility(View.VISIBLE);
			contentView.setVisibility(View.GONE);
		} else {
			app.runInUIThread(() -> {
				appBar.setExpanded(false);
				devicesListAdapter.setItems(filteredDevices);
				contentView.setVisibility(View.VISIBLE);
				emptyView.setVisibility(View.GONE);
			});
		}
	}

	private boolean isDeviceForWidgetFieldType(AbstractDevice<?> device) {
		for (AbstractSensor sensor : device.getSensors()) {
			for (SensorWidgetDataFieldType type :
					sensor.getSupportedWidgetDataFieldTypes()) {
				if (type == widgetDataFieldType) {
					return true;
				}
			}
		}
		return false;
	}

	public static void showInstance(@NonNull FragmentManager manager,
	                                @NonNull Fragment targetFragment,
	                                @NonNull SensorWidgetDataFieldType fieldType,
	                                @Nullable String selectedDeviceId) {
		if (!(targetFragment instanceof SelectDeviceListener)) {
			throw new IllegalArgumentException("targetFragment should implement SelectDeviceListener interface");
		}
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			SelectExternalDeviceFragment fragment = new SelectExternalDeviceFragment();
			Bundle arguments = new Bundle();
			arguments.putInt(WIDGET_TYPE_KEY, fieldType.ordinal());
			arguments.putString(SELECTED_DEVICE_ID_KEY, selectedDeviceId);
			fragment.setTargetFragment(targetFragment, 0);
			fragment.setArguments(arguments);
			fragment.setRetainInstance(true);
			fragment.show(manager, TAG);
		}
	}

	@Override
	public void onDeviceConnect(@NonNull AbstractDevice<?> device, @NonNull DeviceConnectionResult result, @Nullable String error) {
		app.runInUIThread(this::updatePairedSensorsList);
	}

	@Override
	public void onDeviceDisconnect(@NonNull AbstractDevice<?> device) {
		app.runInUIThread(this::updatePairedSensorsList);
	}

	@Override
	public void onSensorData(@NonNull AbstractSensor sensor, @NonNull SensorData data) {
	}

	public void onDeviceSelected(@Nullable AbstractDevice<?> device) {
		if (getTargetFragment() instanceof SelectDeviceListener) {
			((SelectDeviceListener) getTargetFragment()).selectNewDevice(device, widgetDataFieldType);
			dismiss();
		}
	}

	public interface SelectDeviceListener {
		void selectNewDevice(AbstractDevice<?> device, SensorWidgetDataFieldType requestedWidgetDataFieldType);
	}
}
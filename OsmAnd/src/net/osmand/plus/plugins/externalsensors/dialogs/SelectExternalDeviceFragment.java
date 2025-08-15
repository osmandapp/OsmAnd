package net.osmand.plus.plugins.externalsensors.dialogs;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
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
import net.osmand.plus.plugins.externalsensors.adapters.DevicesForWidgetAdapter.SelectDeviceListener;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice.DeviceListener;
import net.osmand.plus.plugins.externalsensors.devices.DeviceConnectionResult;
import net.osmand.plus.plugins.externalsensors.devices.sensors.AbstractSensor;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

import java.util.ArrayList;
import java.util.List;

public class SelectExternalDeviceFragment extends ExternalDevicesBaseFragment implements SelectDeviceListener, DeviceListener {

	private static final String TAG = SelectExternalDeviceFragment.class.getSimpleName();
	private static final String WIDGET_TYPE_KEY = "WIDGET_TYPE_KEY";
	private static final String SELECTED_DEVICE_ID_KEY = "SELECTED_DEVICE_ID_KEY";
	private static final String WITH_NONE_VARIANT_KEY = "WITH_NONE_VARIANT_KEY";

	private View contentView;
	private RecyclerView devicesList;
	private DevicesForWidgetAdapter devicesListAdapter;
	private AppBarLayout appBar;
	private View noBluetoothCard;
	private View stateNoBluetoothView;

	private String selectedDeviceId;
	private SensorWidgetDataFieldType widgetDataFieldType;
	private boolean withNoneVariant;
	private States currentState = States.CONTENT;

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
			withNoneVariant = arguments.getBoolean(WITH_NONE_VARIANT_KEY);
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		updateCurrentStateView();
		return view;
	}

	@Override
	protected void setupUI(@NonNull View view) {
		super.setupUI(view);
		contentView = view.findViewById(R.id.devices_content);
		devicesList = view.findViewById(R.id.connected_devices_list);
		appBar = view.findViewById(R.id.appbar);
		noBluetoothCard = view.findViewById(R.id.no_bluetooth_card);

		setupPairSensorButton(view.findViewById(R.id.pair_btn_additional));
		setupOpenBtSettingsButton(view.findViewById(R.id.bt_settings_button_container));
		setupNoBluetoothView(view);
		devicesListAdapter = new DevicesForWidgetAdapter(app, nightMode, this, widgetDataFieldType, withNoneVariant);
		devicesListAdapter.setDeviceId(selectedDeviceId);
		devicesList.setAdapter(devicesListAdapter);
	}

	private void setupPairSensorButton(@NonNull View view) {
		DialogButton dismissButton = view.findViewById(R.id.dismiss_button);
		ViewGroup.LayoutParams layoutParams = dismissButton.getLayoutParams();
		dismissButton.setButtonType(DialogButtonType.SECONDARY);
		dismissButton.setTitleId(R.string.ant_plus_pair_new_sensor);
		layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
		dismissButton.setLayoutParams(layoutParams);
		view.requestLayout();
		dismissButton.setOnClickListener(v -> showPairNewSensorBottomSheet());
		AndroidUiHelper.updateVisibility(dismissButton, true);
	}

	private void setupNoBluetoothView(View parentView) {
		stateNoBluetoothView = parentView.findViewById(R.id.state_no_bluetooth);
		DialogButton openSettingButton = stateNoBluetoothView.findViewById(R.id.dismiss_button);
		openSettingButton.setButtonType(DialogButtonType.SECONDARY);
		openSettingButton.setTitleId(R.string.ant_plus_open_settings);
		openSettingButton.setOnClickListener(v -> {
					Intent intentOpenBluetoothSettings = new Intent();
					intentOpenBluetoothSettings.setAction(Settings.ACTION_BLUETOOTH_SETTINGS);
					startActivity(intentOpenBluetoothSettings);
				}
		);
		AndroidUiHelper.updateVisibility(openSettingButton, true);
	}

	private void setupOpenBtSettingsButton(@NonNull View view) {
		DialogButton dismissButton = view.findViewById(R.id.dismiss_button);
		dismissButton.setButtonType(DialogButtonType.SECONDARY);
		dismissButton.setTitleId(R.string.ant_plus_open_settings);
		ViewGroup.LayoutParams layoutParams = dismissButton.getLayoutParams();
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

	private void updateCurrentStateView() {
		AndroidUiHelper.updateVisibility(
				stateNoBluetoothView,
				currentState == States.NO_BLUETOOTH
		);
		AndroidUiHelper.updateVisibility(
				contentView,
				currentState == States.CONTENT
		);
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
		devicesList = null;
		appBar = null;
		noBluetoothCard = null;
	}

	@Override
	public void onResume() {
		super.onResume();
		noBluetoothCard.setVisibility(AndroidUtils.isBluetoothEnabled(requireActivity()) ? View.GONE : View.VISIBLE);
		updatePairedSensorsListeners(true);
		updatePairedSensorsList();
		updateCurrentStateView();
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
		List<AbstractDevice<?>> filteredDevices = plugin.getPairedDevicesByWidgetType(widgetDataFieldType);
		if (AndroidUtils.isBluetoothEnabled(requireActivity())) {
			currentState = States.CONTENT;
			app.runInUIThread(() -> {
				appBar.setExpanded(false);
				devicesListAdapter.setItems(new ArrayList<>(filteredDevices));
			});
		} else {
			currentState = States.NO_BLUETOOTH;
		}
		updateCurrentStateView();
	}

	@Override
	public void onDeviceConnecting(@NonNull AbstractDevice<?> device) {
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

	public void onDeviceSelected(@Nullable String deviceId) {
		if (getTargetFragment() instanceof SelectDeviceListener) {
			((SelectDeviceListener) getTargetFragment()).selectNewDevice(deviceId, widgetDataFieldType);
			requireActivity().onBackPressed();
		}
	}

	public static void showInstance(@NonNull FragmentManager manager,
	                                @NonNull Fragment targetFragment,
	                                @NonNull SensorWidgetDataFieldType fieldType,
	                                @Nullable String selectedDeviceId,
	                                boolean withNoneVariant) {
		if (!(targetFragment instanceof SelectDeviceListener)) {
			throw new IllegalArgumentException("targetFragment should implement SelectDeviceListener interface");
		}
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			SelectExternalDeviceFragment fragment = new SelectExternalDeviceFragment();
			Bundle arguments = new Bundle();
			arguments.putInt(WIDGET_TYPE_KEY, fieldType.ordinal());
			arguments.putString(SELECTED_DEVICE_ID_KEY, selectedDeviceId);
			arguments.putBoolean(WITH_NONE_VARIANT_KEY, withNoneVariant);
			fragment.setTargetFragment(targetFragment, 0);
			fragment.setArguments(arguments);
			fragment.setRetainInstance(true);
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
		}
	}

	enum States {
		NO_BLUETOOTH, CONTENT
	}

	public interface SelectDeviceListener {
		void selectNewDevice(@Nullable String deviceId, @NonNull SensorWidgetDataFieldType requestedWidgetDataFieldType);
	}
}
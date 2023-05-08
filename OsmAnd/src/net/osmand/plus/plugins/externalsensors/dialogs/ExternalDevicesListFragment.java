package net.osmand.plus.plugins.externalsensors.dialogs;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;

import net.osmand.CallbackWithObject;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.externalsensors.adapters.PairedDevicesAdapter;
import net.osmand.plus.plugins.externalsensors.adapters.PairedDevicesAdapter.FoundDevicesMenuListener;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice.DeviceListener;
import net.osmand.plus.plugins.externalsensors.devices.DeviceConnectionResult;
import net.osmand.plus.plugins.externalsensors.devices.sensors.AbstractSensor;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData;
import net.osmand.plus.plugins.externalsensors.dialogs.EditDeviceNameDialog.OnSaveSensorNameCallback;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class ExternalDevicesListFragment extends ExternalDevicesBaseFragment implements FoundDevicesMenuListener,
		DeviceListener, OnSaveSensorNameCallback {

	public static final String TAG = ExternalDevicesListFragment.class.getSimpleName();
	protected View emptyView;
	protected View contentView;
	protected View connectedPrompt;
	protected View disconnectedPrompt;
	protected RecyclerView connectedList;
	protected RecyclerView disconnectedList;
	private PairedDevicesAdapter connectedListAdapter;
	private PairedDevicesAdapter disconnectedListAdapter;
	private AppBarLayout appBar;
	private View noBluetoothCard;

	@Override
	protected int getLayoutId() {
		return R.layout.fragment_ant_plus_sensors_list;
	}

	@Override
	protected void setupUI(@NonNull View view) {
		super.setupUI(view);
		emptyView = view.findViewById(R.id.empty_view);
		contentView = view.findViewById(R.id.devices_content);
		connectedList = view.findViewById(R.id.connected_devices_list);
		disconnectedList = view.findViewById(R.id.disconnected_devices_list);
		connectedPrompt = view.findViewById(R.id.connected_prompt);
		disconnectedPrompt = view.findViewById(R.id.disconnected_prompt);
		appBar = view.findViewById(R.id.appbar);
		noBluetoothCard = view.findViewById(R.id.no_bluetooth_card);

		ImageView sensorIcon = view.findViewById(R.id.sensor_icon);
		sensorIcon.setBackgroundResource(nightMode ? R.drawable.img_help_sensors_night : R.drawable.img_help_sensors_day);
		TextView learnMore = view.findViewById(R.id.learn_more_button);
		String docsLinkText = app.getString(R.string.learn_more_about_sensors_link);
		UiUtilities.setupClickableText(app, learnMore, docsLinkText, docsLinkText, nightMode, new CallbackWithObject<Void>() {
			@Override
			public boolean processResult(Void unused) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					String docsUrl = getString(R.string.docs_external_sensors);
					intent.setData(Uri.parse(docsUrl));
					AndroidUtils.startActivityIfSafe(activity, intent);
				}
				return false;
			}
		});
		setupPairSensorButton(view.findViewById(R.id.pair_btn_empty));
		setupPairSensorButton(view.findViewById(R.id.pair_btn_additional));
		setupOpenBtSettingsButton(view.findViewById(R.id.bt_settings_button_container));
		connectedListAdapter = new PairedDevicesAdapter(app, nightMode, this);
		disconnectedListAdapter = new PairedDevicesAdapter(app, nightMode, this);
		connectedList.setAdapter(connectedListAdapter);
		disconnectedList.setAdapter(disconnectedListAdapter);
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
		dismiss();
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
		connectedList = null;
		disconnectedList = null;
		connectedPrompt = null;
		disconnectedPrompt = null;
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
		List<AbstractDevice<?>> connectedDevices = new ArrayList<>();
		List<AbstractDevice<?>> disconnectedDevices = new ArrayList<>();
		for (AbstractDevice<?> device : devices) {
			if (device.isConnected()) {
				connectedDevices.add(device);
			} else {
				disconnectedDevices.add(device);
			}
		}
		if (devices.isEmpty()) {
			emptyView.setVisibility(View.VISIBLE);
			contentView.setVisibility(View.GONE);
			requireActivity().runOnUiThread(() -> {
				appBar.setExpanded(false);
			});
		} else {
			requireActivity().runOnUiThread(() -> {
				appBar.setExpanded(false);
				connectedListAdapter.setItems(connectedDevices);
				disconnectedListAdapter.setItems(disconnectedDevices);
				contentView.setVisibility(View.VISIBLE);
				emptyView.setVisibility(View.GONE);
				connectedPrompt.setVisibility(connectedDevices.size() > 0 ? View.VISIBLE : View.GONE);
				disconnectedPrompt.setVisibility(disconnectedDevices.size() > 0 ? View.VISIBLE : View.GONE);
			});
		}
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			ExternalDevicesListFragment fragment = new ExternalDevicesListFragment();
			fragment.setRetainInstance(true);
			fragment.show(manager, TAG);
		}
	}

	@Override
	public void onDisconnect(@NonNull AbstractDevice<?> device) {
		device.disconnect();
	}

	@Override
	public void onConnect(@NonNull AbstractDevice<?> device) {
		device.connect(app, getActivity());
	}

	@Override
	public void onSettings(@NonNull AbstractDevice<?> device) {
		ExternalDeviceDetailsFragment.Companion.showInstance(
				requireActivity().getSupportFragmentManager(), device);
	}

	@Override
	public void onRename(@NonNull AbstractDevice<?> device) {
		EditDeviceNameDialog.showInstance(requireActivity(), this, device);
	}

	@Override
	public void onForget(@NonNull AbstractDevice<?> device) {
		ForgetDeviceDialog fragment = new ForgetDeviceDialog();
		Bundle args = new Bundle();
		args.putString(ForgetDeviceDialog.DEVICE_ID_KEY, device.getDeviceId());
		fragment.setArguments(args);
		fragment.setTargetFragment(this, 0);
		fragment.show(requireActivity().getSupportFragmentManager(), ForgetDeviceDialog.TAG);
	}

	public void onForgetSensorConfirmed(@NonNull AbstractDevice<?> device) {
		plugin.unpairDevice(device);
		updatePairedSensorsList();
	}

	@Override
	public void changeSensorName(@NonNull String sensorId, @NonNull String newName) {
		plugin.changeDeviceName(sensorId, newName);
		updatePairedSensorsList();
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
}
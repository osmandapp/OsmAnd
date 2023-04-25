package net.osmand.plus.plugins.antplus.dialogs;

import android.content.Intent;
import android.net.Uri;
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
import net.osmand.plus.plugins.antplus.BleConnectionStateListener;
import net.osmand.plus.plugins.antplus.ExternalDevice;
import net.osmand.plus.plugins.antplus.adapters.PairedDevicesAdapter;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;

public class AntPlusSensorsListFragment extends AntPlusBaseFragment implements PairedDevicesAdapter.FoundDevicesMenuListener, BleConnectionStateListener, EditDeviceNameDialog.OnSaveDeviceNameCallback {

	public static final String TAG = AntPlusSensorsListFragment.class.getSimpleName();
	protected View emptyView;
	protected View contentView;
	protected View connectedPrompt;
	protected View disconnectedPrompt;
	protected RecyclerView connectedList;
	protected RecyclerView disconnectedList;
	private PairedDevicesAdapter connectedListAdapter;
	private PairedDevicesAdapter disconnectedListAdapter;
	private AppBarLayout appbar;
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
		appbar = view.findViewById(R.id.appbar);
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
		PairNewSensorBottomSheet.showInstance(requireActivity().getSupportFragmentManager());
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
		appbar = null;
		noBluetoothCard = null;
	}

	@Override
	public void onResume() {
		super.onResume();
		noBluetoothCard.setVisibility(plugin.isBlueToothEnabled() ? View.GONE : View.VISIBLE);
		plugin.addBleDeviceConnectionStateListener(this);
		updatePairedDevicesList();
	}

	@Override
	public void onPause() {
		super.onPause();
		plugin.removeBleDeviceConnectionStateListener(this);
	}

	private void updatePairedDevicesList() {
		ArrayList<ExternalDevice> devicesList = plugin.getPairedDevices();
		ArrayList<ExternalDevice> connectedDevicesList = new ArrayList<>();
		ArrayList<ExternalDevice> disconnectedDevicesList = new ArrayList<>();
		for (ExternalDevice device : devicesList) {
			if (plugin.isDeviceConnected(device)) {
				device.setIsConnected(true);
				connectedDevicesList.add(device);
			} else {
				device.setIsConnected(false);
				disconnectedDevicesList.add(device);
			}
		}
		if (devicesList.isEmpty()) {
			emptyView.setVisibility(View.VISIBLE);
			contentView.setVisibility(View.GONE);
			requireActivity().runOnUiThread(() -> {
				appbar.setExpanded(false);
			});
		} else {
			requireActivity().runOnUiThread(() -> {
				appbar.setExpanded(false);
				connectedListAdapter.setItems(connectedDevicesList);
				disconnectedListAdapter.setItems(disconnectedDevicesList);
				contentView.setVisibility(View.VISIBLE);
				emptyView.setVisibility(View.GONE);
				connectedPrompt.setVisibility(connectedDevicesList.size() > 0 ? View.VISIBLE : View.GONE);
				disconnectedPrompt.setVisibility(disconnectedDevicesList.size() > 0 ? View.VISIBLE : View.GONE);
			});
		}
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			AntPlusSensorsListFragment fragment = new AntPlusSensorsListFragment();
			fragment.setRetainInstance(true);
			fragment.show(manager, TAG);
		}
	}

	@Override
	public void onDisconnect(ExternalDevice device) {
		plugin.disconnectDevice(device);
	}

	@Override
	public void onConnect(ExternalDevice device) {
		plugin.connectDevice(device);
	}

	@Override
	public void onSettings(ExternalDevice device) {
		ExternalDeviceDetailsFragment.Companion.showInstance(getActivity().getSupportFragmentManager(), device);
	}

	@Override
	public void onRename(ExternalDevice device) {
		EditDeviceNameDialog.showInstance(requireActivity(), device.getName(), this, device.getAddress());
	}

	@Override
	public void onForget(ExternalDevice device) {
		ForgetDeviceDialog fragment = new ForgetDeviceDialog();
		fragment.setDeviceAddress(device.getAddress());
		fragment.setTargetFragment(this, 0);
		fragment.show(requireActivity().getSupportFragmentManager(), ForgetDeviceDialog.TAG);
	}

	public void onForgetDeviceConfirmed(@NonNull String address) {
		plugin.unpairDevice(address);
		updatePairedDevicesList();
	}

	@Override
	public void onStateChanged(@Nullable String address, int newState) {
		updatePairedDevicesList();
	}

	@Override
	public void changeDeviceName(@NonNull String newName, @NonNull String deviceAddress) {
		plugin.changeDeviceName(deviceAddress, newName);
		updatePairedDevicesList();
	}
}
package net.osmand.plus.plugins.externalsensors.dialogs;

import static net.osmand.plus.plugins.externalsensors.devices.sensors.DeviceChangeableProperty.NAME;

import android.content.Intent;
import android.provider.Settings;
import android.text.SpannableString;
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

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.externalsensors.adapters.PairedDevicesAdapter;
import net.osmand.plus.plugins.externalsensors.adapters.PairedDevicesAdapter.FoundDevicesMenuListener;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice.DeviceListener;
import net.osmand.plus.plugins.externalsensors.devices.DeviceConnectionResult;
import net.osmand.plus.plugins.externalsensors.devices.sensors.AbstractSensor;
import net.osmand.plus.plugins.externalsensors.devices.sensors.DeviceChangeableProperty;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData;
import net.osmand.plus.plugins.externalsensors.dialogs.EditDevicePropertyDialog.OnSaveSensorPropertyCallback;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.InsetsUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

import java.util.ArrayList;
import java.util.List;

public class ExternalDevicesListFragment extends ExternalDevicesBaseFragment implements FoundDevicesMenuListener,
		DeviceListener, OnSaveSensorPropertyCallback, ForgetDeviceDialog.ForgetDeviceListener {

	public static final String TAG = ExternalDevicesListFragment.class.getSimpleName();
	protected View dividerBeforeButton;
	protected View dividerBetweenDeviceGroups;
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
		dividerBeforeButton = view.findViewById(R.id.pair_btn_additional_divider);
		dividerBetweenDeviceGroups = view.findViewById(R.id.divider_between_device_groups);

		ImageView sensorIcon = view.findViewById(R.id.sensor_icon);
		sensorIcon.setBackgroundResource(nightMode ? R.drawable.bg_empty_external_device_list_icon_night : R.drawable.bg_empty_external_device_list_icon_day);
		sensorIcon.setImageResource(nightMode ? R.drawable.img_help_sensors_night : R.drawable.img_help_sensors_day);

		String docsLinkText = app.getString(R.string.learn_more_about_sensors_link);
		SpannableString spannable = UiUtilities.createClickableSpannable(docsLinkText, docsLinkText, unused -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				AndroidUtils.openUrl(activity, R.string.docs_external_sensors, nightMode);
			}
			return false;
		});
		TextView learnMore = view.findViewById(R.id.learn_more_button);
		UiUtilities.setupClickableText(learnMore, spannable, nightMode);

		if(!InsetsUtils.isEdgeToEdgeSupported()){
			Toolbar toolbar = view.findViewById(R.id.toolbar);
			toolbar.setFitsSystemWindows(true);
			view.setFitsSystemWindows(false);
		}

		setupPairSensorButton(view.findViewById(R.id.pair_btn_empty));
		setupPairSensorButton(view.findViewById(R.id.pair_btn_additional));
		setupOpenBtSettingsButton(view.findViewById(R.id.bt_settings_button_container));
		connectedListAdapter = new PairedDevicesAdapter(app, nightMode, this);
		disconnectedListAdapter = new PairedDevicesAdapter(app, nightMode, this);
		connectedList.setAdapter(connectedListAdapter);
		disconnectedList.setAdapter(disconnectedListAdapter);
	}

	@Nullable
	@Override
	public List<Integer> getCollapsingAppBarLayoutId() {
		List<Integer> ids = new ArrayList<>();
		ids.add(R.id.appbar);
		return ids;
	}

	private void setupPairSensorButton(@NonNull View view) {
		DialogButton dismissButton = view.findViewById(R.id.dismiss_button);
		dismissButton.setButtonType(DialogButtonType.SECONDARY);
		dismissButton.setTitleId(R.string.ant_plus_pair_new_sensor);
		ViewGroup.LayoutParams layoutParams = dismissButton.getLayoutParams();
		layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
		dismissButton.setLayoutParams(layoutParams);
		view.requestLayout();
		dismissButton.setOnClickListener(v -> showPairNewSensorBottomSheet());
		AndroidUiHelper.updateVisibility(dismissButton, true);
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
		noBluetoothCard.setVisibility(AndroidUtils.isBluetoothEnabled(requireActivity()) ? View.GONE : View.VISIBLE);
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
			app.runInUIThread(() -> {
				appBar.setExpanded(true, false);
			});
		} else {
			app.runInUIThread(() -> {
				appBar.setExpanded(false, false);
				connectedListAdapter.setItems(new ArrayList<>(connectedDevices));
				disconnectedListAdapter.setItems(new ArrayList<>(disconnectedDevices));
				contentView.setVisibility(View.VISIBLE);
				emptyView.setVisibility(View.GONE);
				boolean hasConnectedDevices = connectedDevices.size() > 0;
				boolean hasDisConnectedDevices = disconnectedDevices.size() > 0;
				connectedPrompt.setVisibility(hasConnectedDevices ? View.VISIBLE : View.GONE);
				disconnectedPrompt.setVisibility(hasDisConnectedDevices ? View.VISIBLE : View.GONE);
				dividerBetweenDeviceGroups.setVisibility(hasConnectedDevices && hasDisConnectedDevices ? View.VISIBLE : View.GONE);
			});
		}
	}

	@Override
	public void onDisconnect(@NonNull AbstractDevice<?> device) {
		plugin.disconnectDevice(device);
	}

	@Override
	public void onConnect(@NonNull AbstractDevice<?> device) {
		plugin.connectDevice(getActivity(), device);
	}

	@Override
	public void onSettings(@NonNull AbstractDevice<?> device) {
		ExternalDeviceDetailsFragment.Companion.showInstance(
				requireActivity().getSupportFragmentManager(), device);
	}

	@Override
	public void onRename(@NonNull AbstractDevice<?> device) {
		EditDevicePropertyDialog.showInstance(requireActivity(), this, device, NAME);
	}

	@Override
	public void onForget(@NonNull AbstractDevice<?> device) {
		ForgetDeviceDialog.Companion.showInstance(requireActivity().getSupportFragmentManager(), this, device.getDeviceId());
	}

	@Override
	public void onForgetSensorConfirmed(@NonNull AbstractDevice<?> device) {
		plugin.unpairDevice(device);
		updatePairedSensorsList();
	}

	@Override
	public void changeSensorPropertyValue(@NonNull String sensorId, @NonNull DeviceChangeableProperty property, @NonNull String newName) {
		if (property == NAME) {
			plugin.changeDeviceName(sensorId, newName);
			updatePairedSensorsList();
		}
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

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			ExternalDevicesListFragment fragment = new ExternalDevicesListFragment();
			fragment.setRetainInstance(true);
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
		}
	}
}
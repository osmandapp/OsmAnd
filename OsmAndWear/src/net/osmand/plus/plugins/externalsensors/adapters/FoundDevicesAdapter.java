package net.osmand.plus.plugins.externalsensors.adapters;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.externalsensors.DeviceType;
import net.osmand.plus.plugins.externalsensors.ExternalSensorsPlugin;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.ble.BLEAbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.AbstractSensor;
import net.osmand.plus.plugins.externalsensors.viewholders.FoundDeviceViewHolder;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class FoundDevicesAdapter extends RecyclerView.Adapter<FoundDeviceViewHolder> {

	protected final OsmandApplication app;
	protected final ExternalSensorsPlugin plugin;
	protected final boolean nightMode;
	protected List<Object> items = new ArrayList<>();
	protected DeviceClickListener deviceClickListener;
	protected UiUtilities uiUtils;

	public FoundDevicesAdapter(@NonNull OsmandApplication app, boolean nightMode, DeviceClickListener deviceClickListener) {
		this.app = app;
		this.plugin = PluginsHelper.getPlugin(ExternalSensorsPlugin.class);
		this.nightMode = nightMode;
		this.deviceClickListener = deviceClickListener;
		uiUtils = app.getUIUtilities();
	}

	@NonNull
	@Override
	public FoundDeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = UiUtilities.getInflater(parent.getContext(), nightMode);
		View view = inflater.inflate(R.layout.item_external_device, parent, false);
		return new FoundDeviceViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull FoundDeviceViewHolder holder, int position) {
		holder.menuIcon.setVisibility(View.VISIBLE);
		AbstractDevice<?> device = (AbstractDevice<?>) items.get(position);
		DeviceType deviceType = device.getDeviceType();
		holder.name.setText(plugin.getDeviceName(device));
		holder.icon.setImageResource(device.isConnected() ? (nightMode ? deviceType.nightIconId : deviceType.dayIconId) : deviceType.disconnectedIconId);
		int rssi = device.getRssi();
		Drawable signalLevelIcon;
		UiUtilities uiUtils = app.getUIUtilities();
		if (!device.isConnected()) {
			signalLevelIcon = uiUtils.getIcon(R.drawable.ic_action_signal_not_found, nightMode);
		} else if (rssi > -50) {
			signalLevelIcon = uiUtils.getIcon(R.drawable.ic_action_signal_high);
		} else if (rssi > -70) {
			signalLevelIcon = uiUtils.getIcon(R.drawable.ic_action_signal_middle);
		} else {
			signalLevelIcon = uiUtils.getIcon(R.drawable.ic_action_signal_low);
		}
		holder.description.setVisibility(View.VISIBLE);
		boolean isBle = device instanceof BLEAbstractDevice;
		String bleTextMarker = app.getString(R.string.external_device_ble);
		String antTextMarker = app.getString(R.string.external_device_ant);
		int connectedTextId;
		if (device.isConnected()) {
			connectedTextId = R.string.external_device_connected;
		} else {
			connectedTextId = R.string.external_device_disconnected;
		}
		holder.description.setText(app.getString(
				R.string.ltr_or_rtl_combine_via_comma,
				app.getString(connectedTextId),
				isBle ? bleTextMarker : antTextMarker
		));
		holder.description.setCompoundDrawablesRelativeWithIntrinsicBounds(signalLevelIcon, null, null, null);
		holder.description.setGravity(Gravity.CENTER_VERTICAL);
		holder.itemView.setOnClickListener((v) -> {
			if (deviceClickListener != null) {
				deviceClickListener.onDeviceClicked(device);
			}
		});
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	@SuppressLint("NotifyDataSetChanged")
	public void setItems(@NonNull List<Object> items) {
		this.items = items;
		notifyDataSetChanged();
	}

	public interface DeviceClickListener {
		void onDeviceClicked(@NonNull AbstractDevice<?> device);
	}

}
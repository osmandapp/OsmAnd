package net.osmand.plus.plugins.antplus.adapters;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.antplus.ExternalDevice;
import net.osmand.plus.plugins.antplus.devices.DeviceType;
import net.osmand.plus.plugins.antplus.viewholders.FoundDeviceViewHolder;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.WidgetType;

import java.util.ArrayList;

public class FoundDevicesAdapter extends RecyclerView.Adapter<FoundDeviceViewHolder> {

	private final OsmandApplication app;
	private final boolean nightMode;
	private final ArrayList<ExternalDevice> items = new ArrayList<>();
	private DeviceClickListener deviceClickListener;

	public FoundDevicesAdapter(@NonNull OsmandApplication app, ArrayList<ExternalDevice> items, boolean nightMode, DeviceClickListener deviceClickListener) {
		this.app = app;
		this.items.addAll(items);
		this.nightMode = nightMode;
		this.deviceClickListener = deviceClickListener;
	}

	@NonNull
	@Override
	public FoundDeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = UiUtilities.getInflater(parent.getContext(), nightMode);
		View view = inflater.inflate(R.layout.item_with_desc_dividers, parent, false);
		return new FoundDeviceViewHolder(view, nightMode);
	}

	@Override
	public void onBindViewHolder(@NonNull FoundDeviceViewHolder holder, int position) {
		ExternalDevice device = items.get(position);
		holder.name.setText(device.getName());
		DeviceType deviceType = device.getDeviceType();
		if (deviceType != null) {
			WidgetType widgetType = deviceType.getWidgetType();
			holder.icon.setImageResource(nightMode ? widgetType.nightIconId : widgetType.dayIconId);
		}
		int rssi = device.getRssi();
		Drawable signalLevelIcon;
		if (rssi > -50) {
			signalLevelIcon = app.getDrawable(R.drawable.ic_action_signal_high);
		} else if (rssi > -70) {
			signalLevelIcon = app.getDrawable(R.drawable.ic_action_signal_middle);
		} else {
			signalLevelIcon = app.getDrawable(R.drawable.ic_action_signal_low);
		}
		holder.description.setVisibility(View.VISIBLE);
		boolean isBle = device.getConnectionType() == ExternalDevice.DeviceConnectionType.BLE;
		String bleTextMarker = app.getString(R.string.external_device_ble);
		String antTextMarker = app.getString(R.string.external_device_ant);
		holder.description.setText(String.format(app.getString(R.string.bluetooth_disconnected), isBle ? bleTextMarker : antTextMarker));
		holder.description.setCompoundDrawablesWithIntrinsicBounds(signalLevelIcon, null, null, null);
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

	public void setItems(ArrayList<ExternalDevice> items) {
		this.items.clear();
		this.items.addAll(items);
		notifyDataSetChanged();
	}

	public interface DeviceClickListener {
		void onDeviceClicked(@NonNull ExternalDevice device);
	}

}
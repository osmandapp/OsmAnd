package net.osmand.plus.plugins.externalsensors.adapters;

import static net.osmand.plus.plugins.externalsensors.ExternalSensorsPlugin.ANY_CONNECTED_DEVICE_WRITE_SENSOR_DATA_TO_TRACK_KEY;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;
import net.osmand.plus.plugins.externalsensors.viewholders.FoundDeviceViewHolder;
import net.osmand.util.Algorithms;

public class DevicesForWidgetAdapter extends FoundDevicesAdapter {

	private static final int NONE_ITEM_TYPE = 0;
	private static final int ANY_CONNECTED_ITEM_TYPE = 1;
	private static final int DEVICE_ITEM_TYPE = 2;

	private final SelectDeviceListener deviceClickListener;
	private final SensorWidgetDataFieldType widgetDataFieldType;
	private final boolean withNoneVariant;

	private String selectedDeviceId;
	private int selectedPosition = -1;

	public DevicesForWidgetAdapter(@NonNull OsmandApplication app, boolean nightMode,
	                               @NonNull SelectDeviceListener deviceClickListener,
	                               @NonNull SensorWidgetDataFieldType widgetDataFieldType,
	                               boolean withNoneVariant) {
		super(app, nightMode, null);
		this.deviceClickListener = deviceClickListener;
		this.widgetDataFieldType = widgetDataFieldType;
		this.withNoneVariant = withNoneVariant;
	}

	public void setDeviceId(@Nullable String selectedDeviceId) {
		this.selectedDeviceId = selectedDeviceId;
	}

	@Override
	public void onBindViewHolder(@NonNull FoundDeviceViewHolder holder, int position) {
		int itemType = getItemViewType(position);
		boolean anyConnected = ANY_CONNECTED_DEVICE_WRITE_SENSOR_DATA_TO_TRACK_KEY.equals(selectedDeviceId);
		if (itemType == NONE_ITEM_TYPE) {
			holder.selectionMark.setChecked(Algorithms.isEmpty(selectedDeviceId)
					|| !anyConnected && plugin.getDevice(selectedDeviceId) == null);
			holder.itemView.setOnClickListener(v -> onItemClicked(holder, null));
			holder.description.setVisibility(View.GONE);
			holder.name.setText(R.string.shared_string_none);
			holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_action_track_disabled, nightMode));
		} else if (itemType == ANY_CONNECTED_ITEM_TYPE) {
			holder.selectionMark.setChecked(anyConnected);
			holder.itemView.setOnClickListener(v -> onItemClicked(holder, ANY_CONNECTED_DEVICE_WRITE_SENSOR_DATA_TO_TRACK_KEY));
			holder.description.setVisibility(View.GONE);
			holder.name.setText(R.string.any_connected);
			holder.icon.setImageDrawable(uiUtils.getIcon(widgetDataFieldType.getIconId(), nightMode));
		} else if (itemType == DEVICE_ITEM_TYPE) {
			super.onBindViewHolder(holder, position - getNonDeviceItemsCount());
			holder.description.setVisibility(View.VISIBLE);
			holder.menuIcon.setVisibility(View.GONE);
			AbstractDevice<?> device = items.get(position - getNonDeviceItemsCount());
			holder.selectionMark.setChecked(Algorithms.stringsEqual(selectedDeviceId, device.getDeviceId()));
			holder.itemView.setOnClickListener(v -> onItemClicked(holder, device.getDeviceId()));
		}
		if (holder.selectionMark.isChecked()) {
			setSelectedPosition(position);
		}
		holder.selectionMark.setVisibility(View.VISIBLE);
	}

	@Override
	public int getItemViewType(int position) {
		if (position == 0) {
			return withNoneVariant ? NONE_ITEM_TYPE : ANY_CONNECTED_ITEM_TYPE;
		} else if (position == 1) {
			return withNoneVariant ? ANY_CONNECTED_ITEM_TYPE : DEVICE_ITEM_TYPE;
		} else {
			return DEVICE_ITEM_TYPE;
		}
	}

	private void setSelectedPosition(int position) {
		selectedPosition = position;
	}

	private void onItemClicked(@NonNull FoundDeviceViewHolder holder, @Nullable String deviceId) {
		holder.selectionMark.setChecked(true);
		notifyItemChanged(selectedPosition);
		deviceClickListener.onDeviceSelected(deviceId);
	}

	@Override
	public int getItemCount() {
		return super.getItemCount() + getNonDeviceItemsCount();
	}

	public interface SelectDeviceListener {
		void onDeviceSelected(@Nullable String deviceId);
	}

	private int getNonDeviceItemsCount() {
		return withNoneVariant ? 2 : 1;
	}
}
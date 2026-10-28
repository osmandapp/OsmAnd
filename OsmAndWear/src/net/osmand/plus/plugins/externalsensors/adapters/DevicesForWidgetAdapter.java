package net.osmand.plus.plugins.externalsensors.adapters;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;
import net.osmand.plus.plugins.externalsensors.viewholders.FoundDeviceViewHolder;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

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
		boolean anyConnected = plugin.isAnyConnectedDeviceId(selectedDeviceId);
		if (itemType == NONE_ITEM_TYPE) {
			holder.selectionMark.setChecked(Algorithms.isEmpty(selectedDeviceId)
					|| !anyConnected && plugin.getDevice(selectedDeviceId) == null);
			holder.itemView.setOnClickListener(v -> onItemClicked(holder, null));
			holder.description.setVisibility(View.GONE);
			holder.name.setText(R.string.shared_string_none);
			holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_action_track_disabled, nightMode));
		} else if (itemType == ANY_CONNECTED_ITEM_TYPE) {
			holder.selectionMark.setChecked(anyConnected);
			holder.itemView.setOnClickListener(v -> onItemClicked(holder, plugin.getAnyConnectedDeviceId()));
			holder.description.setVisibility(View.GONE);
			holder.name.setText(R.string.any_connected);
			holder.icon.setImageDrawable(uiUtils.getIcon(widgetDataFieldType.getIconId(), nightMode));
		} else if (itemType == DEVICE_ITEM_TYPE) {
			super.onBindViewHolder(holder, position);
			holder.description.setVisibility(View.VISIBLE);
			holder.menuIcon.setVisibility(View.GONE);
			AbstractDevice<?> device = (AbstractDevice<?>) items.get(position);
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
		Object object = items.get(position);
		if (object instanceof AbstractDevice) {
			return DEVICE_ITEM_TYPE;
		} else if (object instanceof Integer) {
			int item = (Integer) object;
			if (NONE_ITEM_TYPE == item) {
				return NONE_ITEM_TYPE;
			} else if (ANY_CONNECTED_ITEM_TYPE == item) {
				return ANY_CONNECTED_ITEM_TYPE;
			}
		}
		throw new IllegalArgumentException("Unsupported view type");
	}

	private void setSelectedPosition(int position) {
		selectedPosition = position;
	}

	private void onItemClicked(@NonNull FoundDeviceViewHolder holder, @Nullable String deviceId) {
		holder.selectionMark.setChecked(true);
		notifyItemChanged(selectedPosition);
		deviceClickListener.onDeviceSelected(deviceId);
	}

	public interface SelectDeviceListener {
		void onDeviceSelected(@Nullable String deviceId);
	}

	@Override
	public void setItems(@NonNull List<Object> items) {
		ArrayList<Object> newItems = new ArrayList<>();
		if (withNoneVariant) {
			newItems.add(NONE_ITEM_TYPE);
		}
		newItems.add(ANY_CONNECTED_ITEM_TYPE);
		newItems.addAll(items);
		super.setItems(newItems);
	}
}
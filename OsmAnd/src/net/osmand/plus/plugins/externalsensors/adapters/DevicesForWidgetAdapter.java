package net.osmand.plus.plugins.externalsensors.adapters;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.externalsensors.ExternalSensorsPlugin;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice;
import net.osmand.plus.plugins.externalsensors.viewholders.FoundDeviceViewHolder;
import net.osmand.util.Algorithms;

public class DevicesForWidgetAdapter extends FoundDevicesAdapter {

	private final SelectDeviceListener deviceClickListener;
	private String selectedDeviceId;
	private int selectedPosition = -1;

	public DevicesForWidgetAdapter(@NonNull OsmandApplication app, boolean nightMode,
	                               @NonNull SelectDeviceListener deviceClickListener) {
		super(app, nightMode, null);
		this.deviceClickListener = deviceClickListener;
	}

	public void setDeviceId(@Nullable String selectedDeviceId) {
		this.selectedDeviceId = selectedDeviceId;
	}

	@Override
	public void onBindViewHolder(@NonNull FoundDeviceViewHolder holder, int position) {
		if (position == 0) {
			holder.selectionMark.setChecked(Algorithms.isEmpty(selectedDeviceId) || ExternalSensorsPlugin.DENY_WRITE_SENSOR_DATA_TO_TRACK_KEY.equals(selectedDeviceId));
			holder.itemView.setOnClickListener(v -> onItemClicked(holder, null));
			holder.description.setVisibility(View.GONE);
			holder.name.setText(R.string.shared_string_none);
			holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_action_sensor_off, nightMode));
		} else {
			super.onBindViewHolder(holder, position - 1);
			holder.description.setVisibility(View.VISIBLE);
			holder.menuIcon.setVisibility(View.GONE);
			AbstractDevice<?> device = items.get(position - 1);
			holder.selectionMark.setChecked(Algorithms.stringsEqual(selectedDeviceId, device.getDeviceId()));
			holder.itemView.setOnClickListener(v -> onItemClicked(holder, device));
		}
		if (holder.selectionMark.isChecked()) {
			setSelectedPosition(position);
		}
		holder.selectionMark.setVisibility(View.VISIBLE);
	}

	private void setSelectedPosition(int position) {
		selectedPosition = position;
	}

	private void onItemClicked(@NonNull FoundDeviceViewHolder holder, @Nullable AbstractDevice<?> device) {
		holder.selectionMark.setChecked(true);
		notifyItemChanged(selectedPosition);
		deviceClickListener.onDeviceSelected(device);
	}

	@Override
	public int getItemCount() {
		return super.getItemCount() + 1;
	}

	public interface SelectDeviceListener {
		void onDeviceSelected(@Nullable AbstractDevice<?> device);
	}

}
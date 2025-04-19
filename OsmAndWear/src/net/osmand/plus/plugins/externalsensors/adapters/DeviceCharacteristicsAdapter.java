package net.osmand.plus.plugins.externalsensors.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorDataField;
import net.osmand.plus.plugins.externalsensors.viewholders.DeviceCharacteristicsViewHolder;
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class DeviceCharacteristicsAdapter extends RecyclerView.Adapter<DeviceCharacteristicsViewHolder> {

	private final OsmandApplication app;
	private final boolean nightMode;
	private List<SensorDataField> items = new ArrayList<>();

	public DeviceCharacteristicsAdapter(@NonNull OsmandApplication app, boolean nightMode) {
		this.app = app;
		this.nightMode = nightMode;
	}

	@NonNull
	@Override
	public DeviceCharacteristicsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = UiUtilities.getInflater(parent.getContext(), nightMode);
		View view = inflater.inflate(R.layout.device_characteristic_item, parent, false);
		return new DeviceCharacteristicsViewHolder(view, nightMode);
	}

	@Override
	public void onBindViewHolder(@NonNull DeviceCharacteristicsViewHolder holder, int position) {
		SensorDataField field = items.get(position);
		holder.name.setText(app.getString(field.getNameId()));
		AndroidUiHelper.updateVisibility(holder.divider, position != items.size() - 1);
		FormattedValue formattedValue = field.getFormattedValue(app);
		if (formattedValue != null) {
			if (!Algorithms.isEmpty(formattedValue.unit)) {
				holder.value.setText(app.getString(R.string.ltr_or_rtl_combine_via_space, formattedValue.value, formattedValue.unit));
			} else {
				holder.value.setText(formattedValue.value);
			}
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	@SuppressLint("NotifyDataSetChanged")
	public void setItems(@NonNull List<SensorDataField> items) {
		this.items = items;
		notifyDataSetChanged();
	}
}
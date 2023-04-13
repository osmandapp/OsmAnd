package net.osmand.plus.plugins.antplus.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.antplus.models.CharacteristicDataField;
import net.osmand.plus.plugins.antplus.viewholders.DeviceCharacteristicsViewHolder;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;

public class DeviceCharacteristicsAdapter extends RecyclerView.Adapter<DeviceCharacteristicsViewHolder> {

	private final OsmandApplication app;
	private final boolean nightMode;
	private final ArrayList<CharacteristicDataField> items = new ArrayList<>();

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
		CharacteristicDataField field = items.get(position);
		holder.name.setText(app.getString(field.getNameId()));
		String valueUnit = "";
		if (field.getUnitNameId() != -1) {
			valueUnit = app.getString(field.getUnitNameId());
		}
		holder.value.setText(field.getValue() + valueUnit);
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	public void setItems(ArrayList<CharacteristicDataField> items) {
		this.items.clear();
		this.items.addAll(items);
		notifyDataSetChanged();
	}
}
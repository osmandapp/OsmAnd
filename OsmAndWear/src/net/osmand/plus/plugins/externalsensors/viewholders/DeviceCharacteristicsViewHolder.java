package net.osmand.plus.plugins.externalsensors.viewholders;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;

public class DeviceCharacteristicsViewHolder extends RecyclerView.ViewHolder {

	public final TextView name;
	public final View divider;
	public final TextView value;
	public final boolean nightMode;

	public DeviceCharacteristicsViewHolder(@NonNull View view, boolean nightMode) {
		super(view);
		this.nightMode = nightMode;
		name = view.findViewById(R.id.title);
		value = view.findViewById(R.id.value);
		divider = view.findViewById(R.id.divider);
	}
}
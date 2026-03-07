package net.osmand.plus.plugins.externalsensors.viewholders;

import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;

public class FoundDeviceViewHolder extends RecyclerView.ViewHolder {

	public final TextView name;
	public final TextView description;
	public final ImageView icon;
	public final ImageView menuIcon;
	public final RadioButton selectionMark;
	public final View divider;

	public FoundDeviceViewHolder(@NonNull View view) {
		super(view);
		name = view.findViewById(R.id.title);
		description = view.findViewById(R.id.description);
		icon = view.findViewById(R.id.icon);
		menuIcon = view.findViewById(R.id.menu_icon);
		selectionMark = view.findViewById(R.id.selection_mark);
		divider = view.findViewById(R.id.short_divider);
	}
}

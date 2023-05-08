package net.osmand.plus.plugins.externalsensors.viewholders;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public class FoundDeviceViewHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;
	public final TextView name;
	public final TextView description;
	public final ImageView icon;
	public final ImageView menuIcon;
	public final boolean nightMode;

	public FoundDeviceViewHolder(@NonNull View view, boolean nightMode) {
		super(view);
		this.nightMode = nightMode;
		app = (OsmandApplication) itemView.getContext().getApplicationContext().getApplicationContext();
		name = view.findViewById(R.id.title);
		description = view.findViewById(R.id.description);
		icon = view.findViewById(R.id.icon);
		menuIcon = view.findViewById(R.id.menu_icon);
	}
}

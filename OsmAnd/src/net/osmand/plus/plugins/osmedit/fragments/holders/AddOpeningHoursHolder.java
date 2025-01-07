package net.osmand.plus.plugins.osmedit.fragments.holders;

import static net.osmand.plus.plugins.osmedit.fragments.EditPoiContentAdapter.TYPE_ADD_OPENING_HOURS;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.plugins.osmedit.fragments.EditPoiContentAdapter.EditPoiListener;

public class AddOpeningHoursHolder extends RecyclerView.ViewHolder {

	private final View button;

	public AddOpeningHoursHolder(@NonNull View itemView) {
		super(itemView);
		button = itemView.findViewById(R.id.addOpeningHoursButton);
	}

	public void bindView(EditPoiListener editPoiListener) {
		button.setOnClickListener(v -> editPoiListener.onAddNewItem(getAdapterPosition(), TYPE_ADD_OPENING_HOURS));
	}
}
package net.osmand.plus.plugins.osmedit.fragments.holders;

import static net.osmand.plus.plugins.osmedit.fragments.EditPoiContentAdapter.TYPE_ADD_TAG;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.plugins.osmedit.fragments.EditPoiContentAdapter;

public class AddItemHolder extends RecyclerView.ViewHolder {

	private final View addTagButton;

	public AddItemHolder(@NonNull View itemView) {
		super(itemView);
		this.addTagButton = itemView.findViewById(R.id.addTagButton);
	}

	public void bindView(EditPoiContentAdapter.EditPoiListener editPoiListener) {
		addTagButton.setOnClickListener(v -> editPoiListener.onAddNewItem(getAdapterPosition(), TYPE_ADD_TAG));
	}
}
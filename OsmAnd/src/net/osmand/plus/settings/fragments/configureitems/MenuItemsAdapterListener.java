package net.osmand.plus.settings.fragments.configureitems;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

public interface MenuItemsAdapterListener {

	void onDragStarted(@NonNull ViewHolder holder);

	void onDragOrSwipeEnded(@NonNull ViewHolder holder);

	void onItemMoved(@Nullable String id, int position);

	void onButtonClicked(int view);
}

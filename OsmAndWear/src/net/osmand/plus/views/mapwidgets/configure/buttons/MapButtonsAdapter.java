package net.osmand.plus.views.mapwidgets.configure.buttons;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class MapButtonsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private final List<MapButtonState> items = new ArrayList<>();

	private final ItemClickListener listener;
	private final LayoutInflater themedInflater;
	private final boolean nightMode;

	public MapButtonsAdapter(@NonNull Context context, @Nullable ItemClickListener listener, boolean nightMode) {
		this.listener = listener;
		this.nightMode = nightMode;
		this.themedInflater = UiUtilities.getInflater(context, nightMode);
	}

	public void setItems(@NonNull List<MapButtonState> items) {
		this.items.clear();
		this.items.addAll(items);

		notifyDataSetChanged();
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = themedInflater.inflate(R.layout.configure_screen_list_item, parent, false);
		return new MapButtonViewHolder(view, nightMode);
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		if (holder instanceof MapButtonViewHolder) {
			MapButtonState item = items.get(position);
			boolean lastItem = position == getItemCount() - 1;

			MapButtonViewHolder viewHolder = (MapButtonViewHolder) holder;
			viewHolder.bindView(item, lastItem);
			viewHolder.itemView.setOnClickListener(v -> {
				int adapterPosition = holder.getAdapterPosition();
				if (listener != null && adapterPosition != RecyclerView.NO_POSITION) {
					listener.onItemClick(items.get(adapterPosition));
				}
			});
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	public interface ItemClickListener {

		void onItemClick(@NonNull MapButtonState buttonState);

	}
}
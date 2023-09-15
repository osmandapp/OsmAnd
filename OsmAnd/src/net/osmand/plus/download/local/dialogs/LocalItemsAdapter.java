package net.osmand.plus.download.local.dialogs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.dialogs.viewholders.LocalItemHolder;
import net.osmand.plus.download.local.dialogs.viewholders.MemoryViewHolder;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class LocalItemsAdapter extends RecyclerView.Adapter<ViewHolder> {

	private static final int LIST_ITEM_TYPE = 0;
	private static final int MEMORY_USAGE_TYPE = 1;


	private final List<Object> items = new ArrayList<>();

	private final LayoutInflater themedInflater;
	@Nullable
	private final LocalItemListener listener;
	private final boolean nightMode;
	private boolean selectionMode;

	public LocalItemsAdapter(@NonNull Context context, @Nullable LocalItemListener listener, boolean nightMode) {
		themedInflater = UiUtilities.getInflater(context, nightMode);
		this.listener = listener;
		this.nightMode = nightMode;
	}

	public void setItems(@NonNull List<Object> items) {
		this.items.clear();
		this.items.addAll(items);

		notifyDataSetChanged();
	}

	public void setSelectionMode(boolean selectionMode) {
		this.selectionMode = selectionMode;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		switch (viewType) {
			case LIST_ITEM_TYPE:
				View itemView = themedInflater.inflate(R.layout.local_list_item, parent, false);
				return new LocalItemHolder(itemView, listener, nightMode);
			case MEMORY_USAGE_TYPE:
				itemView = themedInflater.inflate(R.layout.local_memory_card, parent, false);
				return new MemoryViewHolder(itemView, false, nightMode);
			default:
				throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		if (holder instanceof MemoryViewHolder) {
			MemoryInfo memoryInfo = (MemoryInfo) items.get(position);

			MemoryViewHolder viewHolder = (MemoryViewHolder) holder;
			viewHolder.bindView(memoryInfo);
		} else if (holder instanceof LocalItemHolder) {
			LocalItem item = (LocalItem) items.get(position);
			boolean lastItem = position == getItemCount() - 1;

			LocalItemHolder viewHolder = (LocalItemHolder) holder;
			viewHolder.bindView(item, selectionMode, lastItem);
		}
	}

	@Override
	public int getItemViewType(int position) {
		Object object = items.get(position);
		if (object instanceof LocalItem) {
			return LIST_ITEM_TYPE;
		} else if (object instanceof MemoryInfo) {
			return MEMORY_USAGE_TYPE;
		}
		throw new IllegalArgumentException("Unsupported view type");
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	public int getItemPosition(@NonNull Object object) {
		return items.indexOf(object);
	}

	public void updateItem(@NonNull Object object) {
		int index = getItemPosition(object);
		if (index != -1) {
			notifyItemChanged(index);
		}
	}

	public interface LocalItemListener {

		boolean isItemSelected(@NonNull LocalItem item);

		void onItemSelected(@NonNull LocalItem item);

		void onItemOptionsSelected(@NonNull LocalItem item, @NonNull View view);
	}
}

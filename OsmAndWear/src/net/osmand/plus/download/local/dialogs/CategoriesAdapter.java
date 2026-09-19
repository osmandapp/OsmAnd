package net.osmand.plus.download.local.dialogs;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalCategory;
import net.osmand.plus.download.local.LocalGroup;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.download.local.dialogs.viewholders.CategoryViewHolder;
import net.osmand.plus.download.local.dialogs.viewholders.GroupViewHolder;
import net.osmand.plus.download.local.dialogs.viewholders.MemoryViewHolder;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class CategoriesAdapter extends RecyclerView.Adapter<ViewHolder> {

	private static final int ITEM_TYPE = 0;
	private static final int HEADER_TYPE = 1;
	private static final int MEMORY_USAGE_TYPE = 2;

	private final List<Object> items = new ArrayList<>();

	@Nullable
	private final LocalTypeListener listener;
	private final boolean nightMode;

	private MemoryInfo memoryInfo;

	public CategoriesAdapter(@Nullable LocalTypeListener listener, boolean nightMode) {
		this.listener = listener;
		this.nightMode = nightMode;
	}

	public void setItems(@NonNull List<Object> items, @NonNull MemoryInfo memoryInfo) {
		this.items.clear();
		this.items.addAll(items);
		this.memoryInfo = memoryInfo;

		notifyDataSetChanged();
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = UiUtilities.getInflater(parent.getContext(), nightMode);
		switch (viewType) {
			case ITEM_TYPE:
				View itemView = inflater.inflate(R.layout.local_group_item, parent, false);
				return new GroupViewHolder(itemView, nightMode);
			case HEADER_TYPE:
				itemView = inflater.inflate(R.layout.changes_list_header_item, parent, false);
				return new CategoryViewHolder(itemView);
			case MEMORY_USAGE_TYPE:
				itemView = inflater.inflate(R.layout.local_memory_card, parent, false);
				return new MemoryViewHolder(itemView, true, nightMode);
			default:
				throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		if (holder instanceof MemoryViewHolder) {
			MemoryInfo memoryInfo = (MemoryInfo) items.get(position);
			MemoryViewHolder viewHolder = (MemoryViewHolder) holder;
			viewHolder.bindView(memoryInfo, false);
		} else if (holder instanceof CategoryViewHolder) {
			LocalCategory category = (LocalCategory) items.get(position);
			CategoryViewHolder viewHolder = (CategoryViewHolder) holder;
			viewHolder.bindView(category);
		} else if (holder instanceof GroupViewHolder) {
			LocalGroup group = (LocalGroup) items.get(position);
			boolean lastItem = position == getItemCount() - 1;

			GroupViewHolder viewHolder = (GroupViewHolder) holder;
			viewHolder.bindView(group, memoryInfo, listener, lastItem);
		}
	}

	@Override
	public int getItemViewType(int position) {
		Object object = items.get(position);
		if (object instanceof LocalCategory) {
			return HEADER_TYPE;
		} else if (object instanceof LocalGroup) {
			return ITEM_TYPE;
		} else if (object instanceof MemoryInfo) {
			return MEMORY_USAGE_TYPE;
		}
		throw new IllegalArgumentException("Unsupported view type");
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	@Nullable
	public LocalGroup getLocalGroup(@Nullable LocalItemType type) {
		for (Object object : items) {
			if (object instanceof LocalGroup) {
				LocalGroup group = (LocalGroup) object;
				if (group.getType() == type) {
					return group;
				}
			}
		}
		return null;
	}

	public interface LocalTypeListener {
		void onGroupSelected(@NonNull LocalGroup group);
	}
}
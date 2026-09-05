package net.osmand.plus.download.local.dialogs;

import static net.osmand.plus.download.local.dialogs.LocalItemsAdapter.LIST_ITEM_TYPE;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.local.BaseLocalItem;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.dialogs.LocalItemsAdapter.LocalItemListener;
import net.osmand.plus.download.local.dialogs.viewholders.LocalItemHolder;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class LocalSearchAdapter extends RecyclerView.Adapter<ViewHolder> implements Filterable {

	private final List<BaseLocalItem> items = new ArrayList<>();

	private final LocalSearchFilter filter;
	private final LocalItemListener listener;
	private final LayoutInflater themedInflater;
	private final boolean nightMode;

	public LocalSearchAdapter(@NonNull OsmandApplication app, @NonNull LocalItemListener listener, boolean nightMode) {
		this.listener = listener;
		this.nightMode = nightMode;
		this.filter = new LocalSearchFilter(app, result -> {
			this.items.clear();
			this.items.addAll(result);

			notifyDataSetChanged();
			return true;
		});
		themedInflater = UiUtilities.getInflater(app, nightMode);
	}

	public void setItems(@NonNull List<BaseLocalItem> items) {
		this.items.clear();
		this.items.addAll(items);
		filter.setItems(items);

		notifyDataSetChanged();
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		switch (viewType) {
			case LIST_ITEM_TYPE:
				View itemView = themedInflater.inflate(R.layout.local_list_item, parent, false);
				return new LocalItemHolder(itemView, listener, nightMode);
			default:
				throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		if (holder instanceof LocalItemHolder) {
			BaseLocalItem item = items.get(position);
			boolean lastItem = position == getItemCount() - 1;

			LocalItemHolder viewHolder = (LocalItemHolder) holder;
			viewHolder.bindView(item, false, lastItem, false);
		}
	}

	@Override
	public int getItemViewType(int position) {
		return LIST_ITEM_TYPE;
	}

	@Override
	public Filter getFilter() {
		return filter;
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	public int getItemPosition(@NonNull LocalItem item) {
		return items.indexOf(item);
	}

	public void updateItem(@NonNull LocalItem item) {
		int index = getItemPosition(item);
		if (index != -1) {
			notifyItemChanged(index);
		}
	}
}
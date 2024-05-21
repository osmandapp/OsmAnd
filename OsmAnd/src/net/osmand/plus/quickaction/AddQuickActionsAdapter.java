package net.osmand.plus.quickaction;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddQuickActionsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
	private final OsmandApplication app;
	private final Map<QuickActionType, List<QuickActionType>> quickActionsMap = new HashMap<>();
	private final List<QuickActionType> items = new ArrayList<>();
	private boolean searchMode = false;
	private String filterQuery;

	private final ItemClickListener listener;
	private final LayoutInflater themedInflater;
	private final boolean nightMode;

	public AddQuickActionsAdapter(@NonNull OsmandApplication app, @NonNull Context context, @Nullable ItemClickListener listener, boolean nightMode) {
		this.app = app;
		this.listener = listener;
		this.nightMode = nightMode;
		this.themedInflater = UiUtilities.getInflater(context, nightMode);
	}

	public void setMap(@NonNull Map<QuickActionType, List<QuickActionType>> quickActionsMap) {
		this.quickActionsMap.clear();
		this.quickActionsMap.putAll(quickActionsMap);
		setItemsFromMap();
	}

	public void setItems(@NonNull List<QuickActionType> items) {
		this.items.clear();
		this.items.addAll(items);
		this.items.sort((o1, o2) -> app.getMapButtonsHelper().compareNames(o1.getFullName(app), o2.getFullName(app)));
	}

	private void setItemsFromMap() {
		items.clear();
		if (searchMode) {
			for (List<QuickActionType> typeActions : quickActionsMap.values()) {
				if (Algorithms.isEmpty(filterQuery)) {
					items.addAll(typeActions);
				} else {
					for (QuickActionType action : typeActions) {
						if (action.getFullName(app).toLowerCase().contains(filterQuery.toLowerCase())) {
							items.add(action);
						}
					}
				}
			}
			items.sort((o1, o2) -> app.getMapButtonsHelper().compareNames(o1.getFullName(app), o2.getFullName(app)));
		} else {
			quickActionsMap.keySet()
					.stream()
					.sorted((o1, o2) -> app.getMapButtonsHelper().compareNames(o1.getFullName(app), o2.getFullName(app)))
					.forEach(items::add);
		}
	}

	public void filter(@Nullable String query) {
		this.filterQuery = query;
		setItemsFromMap();
		notifyDataSetChanged();
	}

	public String getSearchQuery() {
		return filterQuery;
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = themedInflater.inflate(R.layout.configure_screen_list_item, parent, false);
		return new QuickActionViewHolder(view, nightMode);
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		if (holder instanceof QuickActionViewHolder) {
			QuickActionType item = items.get(position);
			boolean lastItem = position == getItemCount() - 1;
			int descriptionCount = 0;
			if (!searchMode && item.getId() == 0) {
				List<QuickActionType> typeActions = quickActionsMap.get(item);
				if (typeActions != null) {
					descriptionCount = typeActions.size();
				}
			}
			QuickActionViewHolder viewHolder = (QuickActionViewHolder) holder;
			viewHolder.bindView(item, descriptionCount, lastItem);
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

	public void setSearchMode(boolean searchMode) {
		this.searchMode = searchMode;
		setItemsFromMap();
		notifyDataSetChanged();
	}

	public interface ItemClickListener {
		void onItemClick(@NonNull QuickActionType quickActionType);
	}
}
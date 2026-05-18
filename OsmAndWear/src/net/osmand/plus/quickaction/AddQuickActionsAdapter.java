package net.osmand.plus.quickaction;

import static net.osmand.plus.quickaction.QuickActionListFragment.*;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddQuickActionsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	public static final int DEFAULT_MODE = 0;
	public static final int SEARCH_MODE = 1;
	public static final int CATEGORY_MODE = 2;

	@IntDef(value = {DEFAULT_MODE, SEARCH_MODE, CATEGORY_MODE})

	@Retention(RetentionPolicy.SOURCE)
	public @interface QuickActionAdapterMode {
	}

	private final OsmandApplication app;
	private final Map<QuickActionType, List<QuickActionType>> quickActionsMap = new HashMap<>();
	private final List<ListItem> items = new ArrayList<>();
	private String filterQuery;

	private final ItemClickListener listener;
	private final LayoutInflater themedInflater;
	private final boolean nightMode;

	@QuickActionAdapterMode
	private int mode;

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

	public void setItems(@NonNull List<QuickActionType> actionItems) {
		this.items.clear();
		actionItems.sort((o1, o2) -> app.getMapButtonsHelper().compareNames(o1.getFullName(app), o2.getFullName(app)));
		if (!actionItems.isEmpty()) {
			items.add(new ListItem(ItemType.LIST_DIVIDER));
		}
		fillItems(actionItems);
	}

	public void setAdapterMode(@QuickActionAdapterMode int mode){
		this.mode = mode;
	}

	private void fillItems(@NonNull List<QuickActionType> typeActions) {
		for (QuickActionType type : typeActions) {
			items.add(new ListItem(ItemType.ACTION, type));
		}
	}

	private void setItemsFromMap() {
		items.clear();
		if (mode == SEARCH_MODE) {
			List<QuickActionType> sortedActions = new ArrayList<>();
			for (List<QuickActionType> typeActions : quickActionsMap.values()) {
				if (Algorithms.isEmpty(filterQuery)) {
					sortedActions.addAll(typeActions);
				} else {
					for (QuickActionType action : typeActions) {
						if (action.getFullName(app).toLowerCase().contains(filterQuery.toLowerCase())) {
							sortedActions.add(action);
						}
					}
				}
			}
			sortedActions.sort((o1, o2) -> app.getMapButtonsHelper().compareNames(o1.getFullName(app), o2.getFullName(app)));
			fillItems(sortedActions);
		} else {
			quickActionsMap.keySet()
					.stream()
					.sorted((o1, o2) -> app.getMapButtonsHelper().compareNames(o1.getFullName(app), o2.getFullName(app)))
					.forEach(quickActionType -> items.add(new ListItem(ItemType.ACTION, quickActionType)));
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
		ItemType type = viewType < ItemType.values().length ? ItemType.values()[viewType] : ItemType.LIST_DIVIDER;
		View itemView;
		switch (type) {
			case ACTION:
				View view = themedInflater.inflate(R.layout.configure_screen_list_item, parent, false);
				return new QuickActionViewHolder(view, nightMode);
			case LIST_DIVIDER:
				itemView = themedInflater.inflate(R.layout.list_item_divider, parent, false);
				return new ListDivider(itemView);
			default:
				throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@Override
	public int getItemViewType(int position) {
		ListItem item = items.get(position);
		return item.type.ordinal();
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		if (holder instanceof QuickActionViewHolder) {
			QuickActionType item = (QuickActionType) items.get(position).value;
			boolean lastItem = position == getItemCount() - 1;
			int descriptionCount = 0;
			if (mode == DEFAULT_MODE && item.getId() == 0) {
				List<QuickActionType> typeActions = quickActionsMap.get(item);
				if (typeActions != null) {
					descriptionCount = typeActions.size();
				}
			}
			QuickActionViewHolder viewHolder = (QuickActionViewHolder) holder;
			viewHolder.bindView(item, descriptionCount, lastItem, mode);
			viewHolder.itemView.setOnClickListener(v -> {
				int adapterPosition = holder.getAdapterPosition();
				if (listener != null && adapterPosition != RecyclerView.NO_POSITION) {
					listener.onItemClick((QuickActionType) items.get(adapterPosition).value);
				}
			});
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	public void setSearchMode(boolean searchMode) {
		mode = searchMode ? SEARCH_MODE : DEFAULT_MODE;
		setItemsFromMap();
		notifyDataSetChanged();
	}

	private static class ListDivider extends RecyclerView.ViewHolder {

		public ListDivider(View itemView) {
			super(itemView);
		}
	}

	public interface ItemClickListener {
		void onItemClick(@NonNull QuickActionType quickActionType);
	}
}
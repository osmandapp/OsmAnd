package net.osmand.plus.backup.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.backup.ui.ChangesFragment.ChangesTabType;
import net.osmand.plus.backup.ui.status.EmptyStateChangesViewHolder;
import net.osmand.plus.backup.ui.status.ListHeaderViewHolder;
import net.osmand.plus.backup.ui.status.SyncStatusViewHolder;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class ChangesAdapter extends RecyclerView.Adapter<ViewHolder> {
	public static final int SYNC_STATUS_HEADER_TYPE = 0;
	public static final int LIST_HEADER_TYPE = 1;
	public static final int LIST_ITEM_TYPE = 2;
	public static final int EMPTY_STATE_TYPE = 3;

	private final List<Object> items = new ArrayList<>();
	private final List<SettingsItem> changesList;
	private final boolean nightMode;
	private final MapActivity mapActivity;
	private final ChangesTabType tabType;

	ChangesAdapter(@NonNull MapActivity mapActivity, List<SettingsItem> changesList, boolean nightMode, ChangesTabType tabType) {
		this.changesList = changesList;
		this.nightMode = nightMode;
		this.mapActivity = mapActivity;
		this.tabType = tabType;
		updateItems();
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = UiUtilities.getInflater(parent.getContext(), nightMode);
		switch (viewType) {
			case SYNC_STATUS_HEADER_TYPE:
				View itemView = inflater.inflate(R.layout.sync_status_item, parent, false);
				return new SyncStatusViewHolder(itemView);
			case EMPTY_STATE_TYPE:
				itemView = inflater.inflate(R.layout.empty_state_cloud_changes, parent, false);
				return new EmptyStateChangesViewHolder(itemView);
			case LIST_HEADER_TYPE:
				itemView = inflater.inflate(R.layout.changes_list_header_item, parent, false);
				return new ListHeaderViewHolder(itemView);
			default:
				throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@Override
	public int getItemViewType(int position) {
		Object obj = items.get(position);
		if (Algorithms.objectEquals(obj, SYNC_STATUS_HEADER_TYPE)) {
			return SYNC_STATUS_HEADER_TYPE;
		} else if (Algorithms.objectEquals(obj, EMPTY_STATE_TYPE)) {
			return EMPTY_STATE_TYPE;
		} else if (Algorithms.objectEquals(obj, LIST_HEADER_TYPE)) {
			return LIST_HEADER_TYPE;
		}/* else if (obj instanceof ?) {
			return LIST_ITEM_TYPE;
		}*/ else {
			throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		if (holder instanceof SyncStatusViewHolder) {
			SyncStatusViewHolder viewHolder = (SyncStatusViewHolder) holder;
			viewHolder.bindView();
		} else if (holder instanceof EmptyStateChangesViewHolder) {
			EmptyStateChangesViewHolder viewHolder = (EmptyStateChangesViewHolder) holder;
			viewHolder.bindView();
		} else if (holder instanceof ListHeaderViewHolder) {
			ListHeaderViewHolder viewHolder = (ListHeaderViewHolder) holder;
			viewHolder.bindView(tabType, changesList.size());
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	public void updateItems() {
		items.clear();

		items.add(SYNC_STATUS_HEADER_TYPE);

		if (Algorithms.isEmpty(changesList)) {
			items.add(EMPTY_STATE_TYPE);
		} else {
			items.add(LIST_HEADER_TYPE);
			//items.addAll(changesList);
		}

		notifyDataSetChanged();
	}
}

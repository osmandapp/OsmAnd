package net.osmand.plus.backup.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.SyncBackupTask.OnBackupSyncListener;
import net.osmand.plus.backup.ui.ChangesFragment.RecentChangesType;
import net.osmand.plus.backup.ui.ChangesTabFragment.CloudChangeItem;
import net.osmand.plus.backup.ui.status.EmptyStateViewHolder;
import net.osmand.plus.backup.ui.status.HeaderViewHolder;
import net.osmand.plus.backup.ui.status.ItemViewHolder;
import net.osmand.plus.backup.ui.status.StatusViewHolder;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class ChangesAdapter extends RecyclerView.Adapter<ViewHolder> implements OnBackupSyncListener {

	public static final int BACKUP_STATUS_TYPE = 0;
	public static final int LIST_HEADER_TYPE = 1;
	public static final int LIST_ITEM_TYPE = 2;
	public static final int EMPTY_STATE_TYPE = 3;

	private final OsmandApplication app;

	private final List<Object> items = new ArrayList<>();
	private List<CloudChangeItem> cloudChangeItems = new ArrayList<>();

	private final ChangesTabFragment fragment;
	private final RecentChangesType tabType;
	private final boolean nightMode;

	ChangesAdapter(@NonNull OsmandApplication app, @NonNull ChangesTabFragment fragment, boolean nightMode) {
		this.app = app;
		this.fragment = fragment;
		this.tabType = fragment.getChangesTabType();
		this.nightMode = nightMode;
	}

	public void setCloudChangeItems(@NonNull List<CloudChangeItem> changeItems) {
		this.cloudChangeItems = changeItems;

		items.clear();
		items.add(BACKUP_STATUS_TYPE);

		if (changeItems.isEmpty()) {
			if (!app.getBackupHelper().isBackupPreparing()) {
				items.add(EMPTY_STATE_TYPE);
			}
		} else {
			items.add(LIST_HEADER_TYPE);
			items.addAll(changeItems);
		}
		notifyDataSetChanged();
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = UiUtilities.getInflater(parent.getContext(), nightMode);
		switch (viewType) {
			case BACKUP_STATUS_TYPE:
				View itemView = inflater.inflate(R.layout.backup_status_header, parent, false);
				return new StatusViewHolder(itemView, nightMode);
			case EMPTY_STATE_TYPE:
				itemView = inflater.inflate(R.layout.cloud_empty_state_card, parent, false);
				return new EmptyStateViewHolder(itemView, tabType);
			case LIST_HEADER_TYPE:
				itemView = inflater.inflate(R.layout.changes_list_header_item, parent, false);
				return new HeaderViewHolder(itemView);
			case LIST_ITEM_TYPE:
				itemView = inflater.inflate(R.layout.cloud_change_item, parent, false);
				return new ItemViewHolder(itemView, nightMode);
			default:
				throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@Override
	public int getItemViewType(int position) {
		Object object = items.get(position);
		if (Algorithms.objectEquals(object, BACKUP_STATUS_TYPE)) {
			return BACKUP_STATUS_TYPE;
		} else if (Algorithms.objectEquals(object, EMPTY_STATE_TYPE)) {
			return EMPTY_STATE_TYPE;
		} else if (Algorithms.objectEquals(object, LIST_HEADER_TYPE)) {
			return LIST_HEADER_TYPE;
		} else if (object instanceof CloudChangeItem) {
			return LIST_ITEM_TYPE;
		} else {
			throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		if (holder instanceof StatusViewHolder) {
			StatusViewHolder viewHolder = (StatusViewHolder) holder;
			viewHolder.bindView();
		} else if (holder instanceof EmptyStateViewHolder) {
			EmptyStateViewHolder viewHolder = (EmptyStateViewHolder) holder;
			viewHolder.bindView(nightMode);
		} else if (holder instanceof HeaderViewHolder) {
			HeaderViewHolder viewHolder = (HeaderViewHolder) holder;
			viewHolder.bindView(tabType, cloudChangeItems.size());
		} else if (holder instanceof ItemViewHolder) {
			CloudChangeItem item = (CloudChangeItem) items.get(position);
			boolean lastItem = position == getItemCount() - 1;
			ItemViewHolder viewHolder = (ItemViewHolder) holder;
			viewHolder.bindView(item, fragment, lastItem);
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	@Override
	public void onBackupSyncStarted() {
		notifyDataSetChanged();
	}

	@Override
	public void onBackupProgressUpdate(int progress) {
		notifyItemChanged(items.indexOf(BACKUP_STATUS_TYPE));
	}

	@Override
	public void onBackupItemStarted(@NonNull String type, @NonNull String fileName, int work) {
		CloudChangeItem changeItem = getChangeItem(type, fileName);
		if (changeItem != null) {
			notifyItemChanged(items.indexOf(changeItem));
		}
	}

	@Override
	public void onBackupItemProgress(@NonNull String type, @NonNull String fileName, int value) {
		CloudChangeItem changeItem = getChangeItem(type, fileName);
		if (changeItem != null) {
			notifyItemChanged(items.indexOf(changeItem));
		}
	}

	@Override
	public void onBackupItemFinished(@NonNull String type, @NonNull String fileName) {
		CloudChangeItem changeItem = getChangeItem(type, fileName);
		if (changeItem != null) {
			changeItem.synced = true;
			notifyItemChanged(items.indexOf(changeItem));
		}
	}

	@Nullable
	private CloudChangeItem getChangeItem(@NonNull String type, @NonNull String fileName) {
		for (CloudChangeItem item : cloudChangeItems) {
			if (Algorithms.stringsEqual(item.fileName, fileName)
					&& item.settingsItem.getType() == SettingsItemType.fromName(type)) {
				return item;
			}
		}
		return null;
	}
}
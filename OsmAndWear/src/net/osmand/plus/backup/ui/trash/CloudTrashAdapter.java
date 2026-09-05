package net.osmand.plus.backup.ui.trash;

import static net.osmand.plus.backup.ui.ChangesAdapter.BACKUP_STATUS_TYPE;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.R;
import net.osmand.plus.backup.SyncBackupTask.OnBackupSyncListener;
import net.osmand.plus.backup.ui.status.StatusViewHolder;
import net.osmand.plus.backup.ui.trash.viewholder.EmptyBannerViewHolder;
import net.osmand.plus.backup.ui.trash.viewholder.HeaderViewHolder;
import net.osmand.plus.backup.ui.trash.viewholder.ItemViewHolder;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class CloudTrashAdapter extends RecyclerView.Adapter<ViewHolder> implements OnBackupSyncListener {

	public static final int HEADER_TYPE = 1;
	public static final int TRASH_ITEM_TYPE = 2;
	public static final int EMPTY_BANNER_TYPE = 3;

	private final List<Object> items = new ArrayList<>();
	private final LayoutInflater themedInflater;
	private final CloudTrashController controller;
	private final boolean nightMode;

	public CloudTrashAdapter(@NonNull Context context, @NonNull CloudTrashController controller, boolean nightMode) {
		setHasStableIds(true);
		this.controller = controller;
		this.nightMode = nightMode;
		this.themedInflater = UiUtilities.getInflater(context, nightMode);
	}

	public void setItems(@NonNull List<Object> items) {
		this.items.clear();
		this.items.addAll(items);

		notifyDataSetChanged();
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		switch (viewType) {
			case BACKUP_STATUS_TYPE:
				return new StatusViewHolder(themedInflater.inflate(R.layout.backup_status_header, parent, false), nightMode);
			case HEADER_TYPE:
				return new HeaderViewHolder(themedInflater.inflate(R.layout.list_item_header_56dp, parent, false));
			case TRASH_ITEM_TYPE:
				View view = themedInflater.inflate(R.layout.list_item_trash_item, parent, false);
				return new ItemViewHolder(view, controller, nightMode);
			case EMPTY_BANNER_TYPE:
				return new EmptyBannerViewHolder(themedInflater.inflate(R.layout.card_cloud_trash_empty_banner, parent, false));
			default:
				throw new IllegalArgumentException("Unsupported view type " + viewType);
		}
	}


	@Override
	public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
		Object object = items.get(position);

		if (viewHolder instanceof StatusViewHolder) {
			((StatusViewHolder) viewHolder).bindView();
		} else if (viewHolder instanceof HeaderViewHolder) {
			HeaderViewHolder holder = (HeaderViewHolder) viewHolder;
			holder.bindView((TrashGroup) object);
		} else if (viewHolder instanceof ItemViewHolder) {
			TrashItem trashItem = (TrashItem) object;
			boolean lastItem = position == getItemCount() - 1;
			boolean hideDivider = !lastItem && items.get(position + 1) instanceof TrashGroup;

			ItemViewHolder holder = (ItemViewHolder) viewHolder;
			holder.bindView(trashItem, lastItem, hideDivider);
		}
	}

	@Override
	public int getItemViewType(int position) {
		Object object = items.get(position);
		if (object instanceof TrashGroup) {
			return HEADER_TYPE;
		} else if (object instanceof TrashItem) {
			return TRASH_ITEM_TYPE;
		} else if (object instanceof Integer) {
			return (int) object;
		}
		throw new IllegalArgumentException("Unsupported view type " + object);
	}

	@Override
	public long getItemId(int position) {
		return items.get(position).hashCode();
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
		TrashItem item = getChangeItem(type, fileName);
		if (item != null) {
			notifyItemChanged(items.indexOf(item));
		}
	}

	@Override
	public void onBackupItemProgress(@NonNull String type, @NonNull String fileName, int value) {
		TrashItem item = getChangeItem(type, fileName);
		if (item != null) {
			notifyItemChanged(items.indexOf(item));
		}
	}

	@Override
	public void onBackupItemFinished(@NonNull String type, @NonNull String fileName) {
		TrashItem item = getChangeItem(type, fileName);
		if (item != null) {
			item.synced = true;
			notifyItemChanged(items.indexOf(item));
		}
	}

	@Nullable
	private TrashItem getChangeItem(@NonNull String type, @NonNull String fileName) {
		for (Object object : items) {
			if (object instanceof TrashItem) {
				TrashItem trashItem = (TrashItem) object;
				SettingsItem settingsItem = trashItem.getSettingsItem();

				if (settingsItem != null && SettingsItemType.fromName(type) == settingsItem.getType()
						&& Algorithms.stringsEqual(settingsItem.getFileName(), fileName)) {
					return trashItem;
				}
			}
		}
		return null;
	}
}

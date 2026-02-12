package net.osmand.plus.myplaces.favorites.dialogs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.data.FavouritePoint;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.configmap.tracks.viewholders.EmptyTracksViewHolder;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.dialogs.SortFavoriteViewHolder.SortFavoriteListener;
import net.osmand.plus.settings.enums.FavoriteListSortMode;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UpdateLocationUtils;
import net.osmand.plus.utils.UpdateLocationUtils.UpdateLocationViewCache;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FavoriteFoldersAdapter extends RecyclerView.Adapter<ViewHolder> {

	public static final Object SELECTION_MODE = new Object();
	public static final Object SELECTION_TOGGLE = new Object();

	private final UpdateLocationViewCache cache;

	public static final int TYPE_SORT_FAVORITE = 0;
	public static final int TYPE_FOLDER = 1;
	public static final int TYPE_FAVORITE = 2;
	public static final int TYPE_EMPTY_FOLDER = 3;
	public static final int TYPE_EMPTY_FOLDERS = 4;
	public static final int TYPE_FOLDER_STATS = 5;
	public static final int TYPE_EMPTY_FAVORITES = 6;
	public static final int TYPE_EMPTY_SEARCH = 7;


	private final OsmandApplication app;
	private final UpdateLocationViewCache locationViewCache;
	private final List<Object> items = new ArrayList<>();

	@Nullable
	private SortFavoriteListener sortListener;
	private final boolean nightMode;
	private FavoriteListSortMode sortMode;
	private boolean selectionMode;

	private final FavoriteAdapterListener listener;

	public FavoriteFoldersAdapter(@NonNull Context context, boolean nightMode, FavoriteAdapterListener listener) {
		this.app = (OsmandApplication) context.getApplicationContext();
		this.nightMode = nightMode;
		this.listener = listener;
		locationViewCache = UpdateLocationUtils.getUpdateLocationViewCache(context);
		locationViewCache.arrowResId = R.drawable.ic_direction_arrow;
		locationViewCache.arrowColor = ColorUtilities.getActiveIconColorId(nightMode);
		sortMode = FavoriteListSortMode.NAME_ASCENDING;
		cache = UpdateLocationUtils.getUpdateLocationViewCache(context);

		setHasStableIds(true);
	}

	public void setItems(@NonNull List<Object> items) {
		this.items.clear();
		this.items.addAll(items);
		notifyDataSetChanged();
	}

	public void setSortMode(@NonNull FavoriteListSortMode sortMode) {
		this.sortMode = sortMode;
	}

	public void setSelectionMode(boolean selectionMode) {
		this.selectionMode = selectionMode;
		notifyItemRangeChanged(0, getItemCount(), SELECTION_MODE);
	}

	public void setSortFavoriteListener(@Nullable SortFavoriteListener sortListener) {
		this.sortListener = sortListener;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = UiUtilities.getInflater(parent.getContext(), nightMode);
		switch (viewType) {
			case TYPE_FAVORITE:
				View view = inflater.inflate(R.layout.track_list_item, parent, false);
				return new FavoriteViewHolder(view, nightMode);
			case TYPE_FOLDER:
				view = inflater.inflate(R.layout.track_list_item, parent, false);
				return new FavoriteFolderViewHolder(view, null, nightMode, selectionMode);
			case TYPE_SORT_FAVORITE:
				view = inflater.inflate(R.layout.sort_type_view, parent, false);
				return new SortFavoriteViewHolder(view, sortListener, nightMode);
			case TYPE_EMPTY_FOLDER:
				view = inflater.inflate(R.layout.track_folder_empty_state, parent, false);
				return new FavoriteEmptyFolderVHolder(view, listener);
			case TYPE_EMPTY_FOLDERS:
				view = inflater.inflate(R.layout.track_folder_empty_state, parent, false);
				return new FavoriteEmptyFoldersVHolder(view, listener);
			case TYPE_FOLDER_STATS:
				view = inflater.inflate(R.layout.folder_stats_item, parent, false);
				return new FavoriteStatsViewHolder(app, view);
			case TYPE_EMPTY_SEARCH:
				view = inflater.inflate(R.layout.track_folder_empty_state, parent, false);
				return new FavoriteSearchEmptyStateVHolder(view, listener);
			case TYPE_EMPTY_FAVORITES:
				view = inflater.inflate(R.layout.track_folder_empty_state, parent, false);
				return new EmptyTracksViewHolder(view, null);
			default:
				throw new IllegalArgumentException("Unsupported view type " + viewType);
		}
	}

	@Override
	public int getItemViewType(int position) {
		Object object = items.get(position);
		if (object instanceof FavouritePoint) {
			return TYPE_FAVORITE;
		} else if (object instanceof FavoriteGroup) {
			return TYPE_FOLDER;
		} else if (object instanceof FavoriteFolderAnalysis) {
			return TYPE_FOLDER_STATS;
		} else if (object instanceof Integer) {
			int item = (Integer) object;
			if (TYPE_SORT_FAVORITE == item) {
				return TYPE_SORT_FAVORITE;
			} else if (TYPE_EMPTY_FOLDER == item) {
				return TYPE_EMPTY_FOLDER;
			} else if (TYPE_EMPTY_FOLDERS == item) {
				return TYPE_EMPTY_FOLDERS;
			} else if (TYPE_EMPTY_FAVORITES == item) {
				return TYPE_EMPTY_FAVORITES;
			} else if (TYPE_EMPTY_SEARCH == item) {
				return TYPE_EMPTY_SEARCH;
			}
		}
		throw new IllegalArgumentException(String.valueOf(object));
	}

	private boolean isLastItem(int position) {
		int itemCount = getItemCount();
		boolean isStatsLastItem = items.get(itemCount - 1) instanceof FavoriteFolderAnalysis;
		int offset = (isStatsLastItem && itemCount >= 2) ? 2 : 1;
		return position == itemCount - offset;
	}

	private boolean isLastPinnedFolder(int position) {
		Object cur = items.get(position);
		if (!(cur instanceof FavoriteGroup currentGroup)) {
			return false;
		}
		if (!currentGroup.isPinned()) {
			return false;
		}

		for (int i = position + 1; i < items.size(); i++) {
			Object o = items.get(i);
			if (o instanceof FavoriteGroup g) {
				if (g.isPinned()) {
					return false;
				} else {
					break;
				}
			}
		}
		return true;
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		boolean lastItem = isLastItem(position);
		boolean lastPinned = isLastPinnedFolder(position);

		if (holder instanceof SortFavoriteViewHolder viewHolder) {
			viewHolder.bindView(hasTrackItems());
		} else if (holder instanceof FavoriteViewHolder viewHolder) {
			FavouritePoint favouritePoint = (FavouritePoint) items.get(position);
			viewHolder.bindView(sortMode, favouritePoint, !lastItem, selectionMode, cache, listener);
		} else if (holder instanceof FavoriteFolderViewHolder viewHolder) {
			FavoriteGroup favFolder = (FavoriteGroup) items.get(position);
			viewHolder.bindView(favFolder, !lastPinned && !lastItem, lastPinned && !lastItem, listener);
		} else if (holder instanceof FavoriteStatsViewHolder viewHolder) {
			FavoriteFolderAnalysis folderAnalysis = (FavoriteFolderAnalysis) items.get(position);
			viewHolder.bindView(folderAnalysis);
		} else if (holder instanceof FavoriteEmptyFolderVHolder viewHolder) {
			viewHolder.bindView();
		} else if (holder instanceof FavoriteEmptyFoldersVHolder viewHolder) {
			viewHolder.bindView();
		} else if (holder instanceof EmptyTracksViewHolder viewHolder) {
			viewHolder.bindView();
		} else if (holder instanceof FavoriteSearchEmptyStateVHolder viewHolder) {
			viewHolder.bindView();
		}
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position,
	                             @NonNull List<Object> payloads) {
		if (!payloads.isEmpty()) {
			for (Object p : payloads) {
				if (p == SELECTION_MODE) {
					if (holder instanceof FavoriteViewHolder viewHolder) {
						FavouritePoint favouritePoint = (FavouritePoint) items.get(position);
						viewHolder.bindSelectionMode(selectionMode, listener, favouritePoint);
					} else if (holder instanceof FavoriteFolderViewHolder viewHolder) {
						FavoriteGroup trackFolder = (FavoriteGroup) items.get(position);
						viewHolder.bindSelectionMode(selectionMode, listener, trackFolder);
					}
					return;
				} else if (p == SELECTION_TOGGLE) {
					if (holder instanceof FavoriteViewHolder viewHolder) {
						FavouritePoint favouritePoint = (FavouritePoint) items.get(position);
						viewHolder.bindSelectionToggle(selectionMode, listener, favouritePoint);
					} else if (holder instanceof FavoriteFolderViewHolder viewHolder) {
						FavoriteGroup trackFolder = (FavoriteGroup) items.get(position);
						viewHolder.bindSelectionToggle(selectionMode, listener, trackFolder);
					}
					return;
				}
			}
		} else {
			onBindViewHolder(holder, position);
		}
	}

	@Override
	public long getItemId(int position) {
		Object object = items.get(position);

		if (object instanceof Integer integer) {
			return integer.longValue();
		} else {
			if (object instanceof FavouritePoint favouritePoint) {
				return favouritePoint.hashCode();
			} else if (object instanceof FavoriteGroup favoriteGroup) {
				return favoriteGroup.hashCode();
			} else if (object instanceof FavoriteFolderAnalysis) {
				return TYPE_FOLDER_STATS;
			}
		}
		return super.getItemId(position);
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

	public void onItemsSelected(@NonNull Set<?> items) {
		for (Object item : items) {
			updateItem(item);
		}
	}

	private boolean hasTrackItems() {
		for (Object o : items) {
			if (o instanceof FavoriteGroup || o instanceof FavouritePoint) {
				return true;
			}
		}
		return false;
	}

	public void selectItem(Object object) {
		getItemPosition(object);
		notifyItemChanged(getItemPosition(object), SELECTION_TOGGLE);
	}

	public void updateSelectionAllItems() {
		notifyItemRangeChanged(0, getItemCount(), SELECTION_TOGGLE);
	}

	public interface FavoriteAdapterListener {
		boolean isItemSelected(@NonNull Object object);

		void onItemSingleClick(@NonNull Object object);

		void onItemLongClick(@NonNull Object object);

		void onActionButtonClick(@NonNull Object object, @NonNull View anchor);

		void onEmptyStateClick();
	}
}

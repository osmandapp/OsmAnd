package net.osmand.plus.exploreplaces;

import static net.osmand.plus.search.listitems.QuickSearchListItemType.SEARCH_RESULT;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.data.Amenity;
import net.osmand.plus.R;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.search.NearbyPlacesAdapter.NearbyItemClickListener;
import net.osmand.plus.search.SearchResultViewHolder;
import net.osmand.plus.search.WikiItemViewHolder;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.plus.search.listitems.QuickSearchWikiItem;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UpdateLocationUtils;
import net.osmand.plus.utils.UpdateLocationUtils.UpdateLocationViewCache;

import java.util.ArrayList;
import java.util.List;

public class ExplorePlacesAdapter extends RecyclerView.Adapter<ViewHolder> {

	private static final int POI_TYPE = 0;
	private static final int WIKI_TYPE = 1;

	private final UpdateLocationViewCache locationViewCache;
	private final NearbyItemClickListener itemClickListener;
	private final boolean nightMode;
	private List<QuickSearchListItem> items = new ArrayList<>();

	@Nullable
	private PoiUIFilter poiUIFilter;

	public ExplorePlacesAdapter(@NonNull Context context, @Nullable PoiUIFilter poiUIFilter,
			@Nullable NearbyItemClickListener itemClickListener, boolean nightMode) {
		this.nightMode = nightMode;
		this.poiUIFilter = poiUIFilter;
		this.itemClickListener = itemClickListener;
		this.locationViewCache = UpdateLocationUtils.getUpdateLocationViewCache(context);
	}

	public void setItems(@NonNull List<QuickSearchListItem> items) {
		this.items = items;
		notifyDataSetChanged();
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = UiUtilities.getInflater(parent.getContext(), nightMode);
		return switch (viewType) {
			case WIKI_TYPE -> {
				View view = inflater.inflate(R.layout.search_nearby_item_vertical, parent, false);
				yield new WikiItemViewHolder(view, locationViewCache, nightMode);
			}
			case POI_TYPE -> {
				View view = inflater.inflate(R.layout.search_list_item_full, parent, false);
				yield new SearchResultViewHolder(view, locationViewCache, nightMode);
			}
			default -> throw new IllegalArgumentException("Unsupported view type");
		};

	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		if (holder instanceof WikiItemViewHolder viewHolder) {
			QuickSearchWikiItem item = (QuickSearchWikiItem) items.get(position);
			viewHolder.bindItem(item, poiUIFilter, false);

			viewHolder.itemView.setOnClickListener(v -> {
				if (itemClickListener != null) {
					itemClickListener.onNearbyItemClicked(item.getAmenity());
				}
			});
		} else if (holder instanceof SearchResultViewHolder viewHolder) {
			QuickSearchListItem item = items.get(position);
			viewHolder.bindItem(item, false);

			viewHolder.itemView.setOnClickListener(v -> {
				if (itemClickListener != null && item.getSearchResult().object instanceof Amenity amenity) {
					itemClickListener.onNearbyItemClicked(amenity);
				}
			});
		}
	}

	public void setPoiUIFilter(@Nullable PoiUIFilter filter) {
		poiUIFilter = filter;
		notifyDataSetChanged();
	}

	@Override
	public int getItemViewType(int position) {
		QuickSearchListItem item = items.get(position);
		if (item instanceof QuickSearchWikiItem) {
			return WIKI_TYPE;
		} else if (item.getType() == SEARCH_RESULT && item.getSearchResult().object instanceof Amenity) {
			return POI_TYPE;
		}
		throw new IllegalArgumentException("Unsupported view type " + item);
	}

	@Override
	public int getItemCount() {
		return items.size();
	}
}
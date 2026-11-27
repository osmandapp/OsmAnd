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
import net.osmand.data.MapObject;
import net.osmand.plus.R;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.search.MapObjectViewHolder;
import net.osmand.plus.search.SearchResultViewHolder;
import net.osmand.plus.search.WikiItemViewHolder;
import net.osmand.plus.search.dialogs.QuickSearchListAdapter;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.plus.search.listitems.QuickSearchWikiItem;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UpdateLocationUtils;
import net.osmand.plus.utils.UpdateLocationUtils.UpdateLocationViewCache;
import net.osmand.search.core.SearchResult;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ExplorePlacesAdapter extends RecyclerView.Adapter<ViewHolder> {

	private static final int POI_TYPE = 0;
	private static final int WIKI_TYPE = 1;
	private static final int CITY_TYPE = 2;
	private static final int MAP_OBJECT_TYPE = 3;

	private static final Object LOCATION_UPDATE_PAYLOAD = new Object();
	private static final Object TIME_UPDATE_PAYLOAD = new Object();
	private static final long TIME_UPDATE_INTERVAL_MS = 60_000;

	private final UpdateLocationViewCache locationViewCache;
	private final ExploreItemClickListener itemClickListener;
	@NonNull
	private final Calendar calendar = Calendar.getInstance();
	private final boolean nightMode;
	private List<QuickSearchListItem> items = new ArrayList<>();
	private long lastTimeUpdate;

	@Nullable
	private PoiUIFilter poiUIFilter;

	public ExplorePlacesAdapter(@NonNull Context context, @Nullable PoiUIFilter poiUIFilter,
			@Nullable ExploreItemClickListener itemClickListener, boolean nightMode) {
		this.nightMode = nightMode;
		this.poiUIFilter = poiUIFilter;
		this.itemClickListener = itemClickListener;
		this.locationViewCache = UpdateLocationUtils.getUpdateLocationViewCache(context);
	}

	public void setItems(@NonNull List<QuickSearchListItem> items) {
		this.items = items;
		notifyDataSetChanged();
	}

	public void updateLocationAllItems() {
		Object payload = LOCATION_UPDATE_PAYLOAD;
		long now = System.currentTimeMillis();
		if (now - lastTimeUpdate >= TIME_UPDATE_INTERVAL_MS) {
			// opening hours depend on the current time, so bind the rows in full once a minute
			lastTimeUpdate = now;
			payload = TIME_UPDATE_PAYLOAD;
		}
		notifyItemRangeChanged(0, getItemCount(), payload);
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = UiUtilities.getInflater(parent.getContext(), nightMode);
		return switch (viewType) {
			case CITY_TYPE, MAP_OBJECT_TYPE -> {
				View view = inflater.inflate(R.layout.search_list_item_administrative, parent, false);
				yield new MapObjectViewHolder(view, locationViewCache);
			}
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
		QuickSearchListItem item = items.get(position);
		holder.itemView.setOnClickListener(v -> {
			if (itemClickListener != null) {
				itemClickListener.onExploreItemClicked(item.getSearchResult());
			}
		});
		if (holder instanceof MapObjectViewHolder viewHolder) {
			viewHolder.setNightMode(nightMode);
			viewHolder.bindItem(item, false);
		} else if (holder instanceof WikiItemViewHolder viewHolder) {
			QuickSearchWikiItem wikiItem = (QuickSearchWikiItem) item;
			viewHolder.bindItem(wikiItem, false);
		} else if (holder instanceof SearchResultViewHolder viewHolder) {
			SearchResultViewHolder.bindPOISearchResult(holder.itemView, item, nightMode, calendar);
		}
		QuickSearchListAdapter.updateCompass(holder.itemView, item, locationViewCache, false);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
		if (payloads.contains(LOCATION_UPDATE_PAYLOAD) && !payloads.contains(TIME_UPDATE_PAYLOAD)) {
			QuickSearchListItem item = items.get(position);
			QuickSearchListAdapter.updateCompass(holder.itemView, item, locationViewCache, false);
		} else {
			super.onBindViewHolder(holder, position, payloads);
		}
	}

	public void setPoiUIFilter(@Nullable PoiUIFilter filter) {
		poiUIFilter = filter;
		notifyDataSetChanged();
	}

	@Override
	public int getItemViewType(int position) {
		QuickSearchListItem item = items.get(position);
		Amenity amenity = null;
		if (item.getSearchResult().object instanceof Amenity) {
			amenity = (Amenity) item.getSearchResult().object;
		}
		if (amenity != null && amenity.getType().isAdministrative()) {
			return CITY_TYPE;
		} else if (item instanceof QuickSearchWikiItem) {
			return WIKI_TYPE;
		} else if (item.getType() == SEARCH_RESULT && amenity != null) {
			return POI_TYPE;
		} else if (item.getType() == SEARCH_RESULT && item.getSearchResult().object instanceof MapObject) {
			return MAP_OBJECT_TYPE;
		}
		throw new IllegalArgumentException("Unsupported view type " + item);
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	public interface ExploreItemClickListener {

		void onExploreItemClicked(@NonNull SearchResult searchResult);
	}
}
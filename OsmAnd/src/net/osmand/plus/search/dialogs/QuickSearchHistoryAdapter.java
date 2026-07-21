package net.osmand.plus.search.dialogs;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.search.SearchResultViewHolder;
import net.osmand.plus.search.history.HistoryEntry;
import net.osmand.plus.search.listitems.QuickSearchDisabledHistoryItem;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UpdateLocationUtils;
import net.osmand.plus.utils.UpdateLocationUtils.UpdateLocationViewCache;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchResult;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class QuickSearchHistoryAdapter extends ArrayAdapter<QuickSearchHistoryAdapter.Item> {

	private static final int TYPE_HEADER = 0;
	private static final int TYPE_RESULT = 1;
	private static final int TYPE_DISABLED_HISTORY = 2;

	private final OsmandApplication app;
	private final LayoutInflater inflater;
	private final boolean nightMode;
	private final Calendar calendar = Calendar.getInstance();
	private final UpdateLocationViewCache locationViewCache;

	private final List<Item> items = new ArrayList<>();
	private boolean useMapCenter;
	private boolean showDestinationDate;

	public QuickSearchHistoryAdapter(@NonNull OsmandApplication app, @NonNull FragmentActivity activity,
			boolean nightMode) {
		super(activity, R.layout.search_list_item);
		this.app = app;
		this.nightMode = nightMode;
		inflater = UiUtilities.getInflater(activity, nightMode);
		locationViewCache = UpdateLocationUtils.getUpdateLocationViewCache(activity);
	}

	public void setUseMapCenter(boolean useMapCenter) {
		this.useMapCenter = useMapCenter;
		notifyDataSetChanged();
	}

	public void setShowDestinationDate(boolean showDestinationDate) {
		this.showDestinationDate = showDestinationDate;
		notifyDataSetChanged();
	}

	public void setItems(@NonNull List<Item> items) {
		this.items.clear();
		this.items.addAll(items);
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return items.size();
	}

	@Nullable
	@Override
	public Item getItem(int position) {
		return items.get(position);
	}

	@Override
	public int getViewTypeCount() {
		return 3;
	}

	@Override
	public int getItemViewType(int position) {
		Item item = getItem(position);
		if (item != null && item.headerTitle != null) {
			return TYPE_HEADER;
		}
		QuickSearchListItem listItem = item != null ? item.getListItem() : null;
		return listItem instanceof QuickSearchDisabledHistoryItem ? TYPE_DISABLED_HISTORY : TYPE_RESULT;
	}

	@Override
	public boolean isEnabled(int position) {
		return getItemViewType(position) == TYPE_RESULT;
	}

	@NonNull
	@Override
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		Item item = getItem(position);
		if (item == null) {
			return new View(parent.getContext());
		}
		if (item.headerTitle != null) {
			View view = getView(convertView, R.layout.quick_search_history_section_header);
			View header = view.findViewById(R.id.header);
			header.setBackgroundResource(R.drawable.bg_list_card_top_round);
			TextView title = view.findViewById(R.id.title);
			title.setText(item.headerTitle);
			return view;
		}
		QuickSearchListItem listItem = item.getListItem();
		if (listItem instanceof QuickSearchDisabledHistoryItem disabledHistoryItem) {
			return bindDisabledHistoryItem(convertView, disabledHistoryItem);
		}
		return listItem != null ? bindResultItem(position, convertView, listItem) : new View(parent.getContext());
	}

	@NonNull
	private View bindDisabledHistoryItem(@Nullable View convertView,
	                                     @NonNull QuickSearchDisabledHistoryItem disabledHistoryItem) {
		View view = getView(convertView, R.layout.quick_search_disabled_history_card);
		TextView settingsButtonDescr = view.findViewById(R.id.settings_button);
		View settingsButton = view.findViewById(R.id.settings_button_container);
		settingsButton.setOnClickListener(disabledHistoryItem.getOnClickListener());
		settingsButtonDescr.setOnClickListener(disabledHistoryItem.getOnClickListener());
		return view;
	}

	@NonNull
	private View bindResultItem(int position, @Nullable View convertView, @NonNull QuickSearchListItem listItem) {
		SearchResult searchResult = listItem.getSearchResult();
		LinearLayout view;
		if (searchResult != null && searchResult.objectType == ObjectType.GPX_TRACK) {
			view = getView(convertView, R.layout.search_gpx_list_item);
			QuickSearchListAdapter.bindGpxTrack(view, listItem, (GPXInfo) searchResult.relatedObject);
		} else if (searchResult != null && searchResult.objectType == ObjectType.POI) {
			view = getView(convertView, R.layout.search_list_item_full);
			SearchResultViewHolder.bindPOISearchResult(view, listItem, nightMode, calendar);
		} else if (listItem.isDestinationHistoryItem()) {
			view = getView(convertView, R.layout.search_list_item_full);
			SearchResultViewHolder.bindFullSearchResult(view, listItem);
			bindDestinationDate(view, listItem);
		} else if (listItem.isLegacyHistoryItem()) {
			view = getView(convertView, R.layout.search_legacy_history_list_item);
			SearchResultViewHolder.bindSearchResult(view, listItem, calendar);
		} else {
			view = getView(convertView, R.layout.search_list_item);
			SearchResultViewHolder.bindSearchResult(view, listItem, calendar);
		}
		if (view.findViewById(R.id.compass_layout) != null) {
			QuickSearchListAdapter.updateCompass(view, listItem, locationViewCache, useMapCenter);
		}
		setupBackground(position, view);
		updateDivider(position, view);
		return view;
	}

	private void bindDestinationDate(@NonNull View view, @NonNull QuickSearchListItem listItem) {
		SearchResult searchResult = listItem.getSearchResult();
		if (!showDestinationDate || searchResult == null || !(searchResult.object instanceof HistoryEntry entry)) {
			return;
		}
		if (entry.getLastAccessTime() <= 0) {
			return;
		}
		String date = OsmAndFormatter.getFormattedDate(app, entry.getLastAccessTime());
		TextView subtitle = view.findViewById(R.id.subtitle);
		if (subtitle != null && !Algorithms.isEmpty(date)) {
			String description = app.getString(R.string.ltr_or_rtl_combine_via_bold_point,
					listItem.getTypeName(), date);
			subtitle.setText(description);
			subtitle.setVisibility(View.VISIBLE);
		}
	}

	private void setupBackground(int position, @NonNull View view) {
		boolean first = position == 0;
		boolean last = isLastResultInGroup(position);
		if (first && last) {
			view.setBackgroundResource(R.drawable.bg_list_card_round);
		} else if (first) {
			view.setBackgroundResource(R.drawable.bg_list_card_top_round);
		} else if (last) {
			view.setBackgroundResource(R.drawable.bg_list_card_bottom_round);
		} else {
			view.setBackgroundColor(ColorUtilities.getListBgColor(app, nightMode));
		}
	}

	private void updateDivider(int position, @NonNull View view) {
		View divider = view.findViewById(R.id.divider);
		if (divider != null) {
			boolean last = isLastResultInGroup(position);
			divider.setVisibility(last ? View.GONE : View.VISIBLE);
		}
	}

	private boolean isLastResultInGroup(int position) {
		return position == getCount() - 1 || getItemViewType(position + 1) == TYPE_HEADER;
	}

	@SuppressWarnings("unchecked")
	@NonNull
	private <T extends View> T getView(@Nullable View convertView, int layoutId) {
		if (convertView == null || !Algorithms.objectEquals(convertView.getTag(), layoutId)) {
			convertView = inflater.inflate(layoutId, null);
			convertView.setTag(layoutId);
		}
		return (T) convertView;
	}

	public static Item header(@NonNull String title) {
		return new Item(title, null);
	}

	public static Item result(@NonNull QuickSearchListItem item) {
		return new Item(null, item);
	}

	public static Item disabledHistory(@NonNull QuickSearchDisabledHistoryItem item) {
		return new Item(null, item);
	}

	public static class Item {
		@Nullable
		private final String headerTitle;
		@Nullable
		private final QuickSearchListItem listItem;

		private Item(@Nullable String headerTitle, @Nullable QuickSearchListItem listItem) {
			this.headerTitle = headerTitle;
			this.listItem = listItem;
		}

		@Nullable
		public QuickSearchListItem getListItem() {
			return listItem;
		}
	}
}

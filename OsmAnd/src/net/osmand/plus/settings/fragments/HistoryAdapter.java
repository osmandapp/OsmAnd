package net.osmand.plus.settings.fragments;

import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.TargetPoint;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.routepreparationmenu.cards.PreviousRouteCard;
import net.osmand.plus.search.SearchResultViewHolder;
import net.osmand.plus.search.dialogs.QuickSearchListAdapter;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UpdateLocationUtils;
import net.osmand.plus.utils.UpdateLocationUtils.UpdateLocationViewCache;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchResult;

import java.text.SimpleDateFormat;
import java.util.*;

public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private static final int HEADER_TYPE = 1;
	private static final int MARKER_TYPE = 2;
	private static final int SEARCH_TYPE = 3;
	private static final int TRACK_TYPE = 4;
	private static final int TARGET_POINT_TYPE = 5;

	private static final int TODAY_HEADER = 56;
	private static final int YESTERDAY_HEADER = 57;
	private static final int LAST_SEVEN_DAYS_HEADER = 58;
	private static final int THIS_YEAR_HEADER = 59;
	public static final int PREVIOUS_ROUTE_HEADER = 60;

	private final OsmandApplication app;
	private final UiUtilities uiUtilities;
	private final UpdateLocationViewCache locationViewCache;

	private List<Object> items = new ArrayList<>();
	private Set<?> selectedItems = new HashSet<>();
	private Map<Integer, List<?>> itemsGroups = new HashMap<>();

	private final LayoutInflater themedInflater;
	private final OnItemSelectedListener listener;
	private final int activeColorId;
	private final int defaultColorId;
	private final boolean nightMode;

	public HistoryAdapter(@NonNull MapActivity activity, @Nullable OnItemSelectedListener listener,
			boolean nightMode) {
		this.app = activity.getApp();
		this.listener = listener;
		this.nightMode = nightMode;
		activeColorId = ColorUtilities.getActiveColorId(nightMode);
		defaultColorId = ColorUtilities.getDefaultIconColorId(nightMode);
		uiUtilities = app.getUIUtilities();
		themedInflater = UiUtilities.getInflater(activity, nightMode);
		locationViewCache = UpdateLocationUtils.getUpdateLocationViewCache(activity);
	}

	public void updateSettingsItems(@NonNull List<Object> items,
			@NonNull Map<Integer, List<?>> markerGroups,
			@NonNull Set<?> selectedItems) {
		this.items = items;
		this.itemsGroups = markerGroups;
		this.selectedItems = selectedItems;
		notifyDataSetChanged();
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
		switch (viewType) {
			case HEADER_TYPE: {
				return new HeaderViewHolder(themedInflater.inflate(R.layout.history_preference_header, viewGroup, false));
			}
			case MARKER_TYPE: {
				return new MarkerViewHolder(themedInflater.inflate(R.layout.history_preference_item, viewGroup, false));
			}
			case SEARCH_TYPE: {
				return new SearchItemViewHolder(themedInflater.inflate(R.layout.history_preference_item, viewGroup, false));
			}
			case TARGET_POINT_TYPE: {
				return new TargetPointViewHolder(themedInflater.inflate(R.layout.history_preference_item, viewGroup, false));
			}
			case TRACK_TYPE: {
				return new TrackViewHolder(themedInflater.inflate(R.layout.history_gpx_item, viewGroup, false));
			}
			default:
				throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		if (holder instanceof HeaderViewHolder) {
			Integer dateHeader = (Integer) getItem(position);
			bindHeaderItem((HeaderViewHolder) holder, dateHeader, position);
		} else if (holder instanceof TrackViewHolder) {
			SearchResult searchResult = (SearchResult) getItem(position);
			bindTrackItem((TrackViewHolder) holder, searchResult, position);
		} else if (holder instanceof HistoryItemViewHolder) {
			Object item = getItem(position);
			HistoryItemViewHolder viewHolder = (HistoryItemViewHolder) holder;

			boolean selected = selectedItems.contains(item);
			viewHolder.compoundButton.setChecked(selected);
			viewHolder.itemView.setOnClickListener(v -> {
				boolean checked = !viewHolder.compoundButton.isChecked();
				if (listener != null) {
					listener.onItemSelected(item, checked);
				}
				notifyDataSetChanged();
			});
			if (holder instanceof SearchItemViewHolder) {
				SearchResult searchResult = (SearchResult) getItem(position);
				QuickSearchListItem listItem = new QuickSearchListItem(app, searchResult);
				SearchResultViewHolder.bindSearchResult((LinearLayout) viewHolder.itemView, listItem);

				int iconColor = ContextCompat.getColor(app, selected ? activeColorId : defaultColorId);
				viewHolder.icon.setImageDrawable(UiUtilities.tintDrawable(listItem.getIcon(), iconColor));
				updateCompassVisibility(viewHolder.compassView, searchResult.location);
			} else if (holder instanceof TargetPointViewHolder) {
				TargetPoint targetPoint = (TargetPoint) getItem(position);
				int colorId = selected ? activeColorId : defaultColorId;
				viewHolder.title.setText(PreviousRouteCard.getPointName(app, targetPoint));
				viewHolder.icon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_marker_dark, colorId));
				updateCompassVisibility(viewHolder.compassView, targetPoint.getLatLon());
			} else if (holder instanceof MarkerViewHolder) {
				MapMarker mapMarker = (MapMarker) getItem(position);
				int colorId = selected ? MapMarker.getColorId(mapMarker.colorIndex) : defaultColorId;
				viewHolder.title.setText(mapMarker.getName(app));
				viewHolder.icon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_flag, colorId));
				updateCompassVisibility(viewHolder.compassView, mapMarker.point);
			}
			boolean lastItem = position == getItemCount() - 1;
			AndroidUiHelper.updateVisibility(viewHolder.divider, lastItem);
			UiUtilities.setupCompoundButton(nightMode, ContextCompat.getColor(app, activeColorId), viewHolder.compoundButton);
		}
	}

	public void bindHeaderItem(HeaderViewHolder viewHolder, Integer dateHeader, int position) {
		viewHolder.title.setText(getDateForHeader(app, dateHeader));
		viewHolder.compoundButton.setChecked(selectedItems.containsAll(itemsGroups.get(dateHeader)));

		viewHolder.itemView.setOnClickListener(v -> {
			List<?> items = itemsGroups.get(dateHeader);
			if (items != null) {
				boolean selected = !viewHolder.compoundButton.isChecked();
				if (listener != null) {
					listener.onCategorySelected(new ArrayList<>(items), selected);
				}
				notifyDataSetChanged();
			}
		});
		AndroidUiHelper.updateVisibility(viewHolder.divider, position > 0);
		AndroidUiHelper.updateVisibility(viewHolder.shadowDivider, position == 0);
		UiUtilities.setupCompoundButton(nightMode, ContextCompat.getColor(app, activeColorId), viewHolder.compoundButton);
	}

	public void bindTrackItem(TrackViewHolder viewHolder, SearchResult searchResult, int position) {
		GPXInfo gpxInfo = (GPXInfo) searchResult.relatedObject;
		QuickSearchListItem listItem = new QuickSearchListItem(app, searchResult);
		QuickSearchListAdapter.bindGpxTrack(viewHolder.itemView, listItem, gpxInfo);

		boolean selected = selectedItems.contains(searchResult);
		viewHolder.compoundButton.setChecked(selected);
		viewHolder.itemView.setOnClickListener(v -> {
			boolean checked = !viewHolder.compoundButton.isChecked();
			if (listener != null) {
				listener.onItemSelected(searchResult, checked);
			}
			notifyDataSetChanged();
		});
		int iconColor = ContextCompat.getColor(app, selected ? activeColorId : defaultColorId);
		viewHolder.icon.setImageDrawable(UiUtilities.tintDrawable(listItem.getIcon(), iconColor));

		boolean lastItem = position == getItemCount() - 1;
		AndroidUiHelper.updateVisibility(viewHolder.divider, lastItem);
		UiUtilities.setupCompoundButton(nightMode, ContextCompat.getColor(app, activeColorId), viewHolder.compoundButton);
	}

	@Override
	public int getItemViewType(int position) {
		Object item = items.get(position);
		if (item instanceof Integer) {
			return HEADER_TYPE;
		} else if (item instanceof MapMarker) {
			return MARKER_TYPE;
		} else if (item instanceof TargetPoint) {
			return TARGET_POINT_TYPE;
		} else if (item instanceof SearchResult) {
			SearchResult searchResult = (SearchResult) item;
			return searchResult.objectType == ObjectType.GPX_TRACK ? TRACK_TYPE : SEARCH_TYPE;
		} else {
			throw new IllegalArgumentException("Unsupported view type");
		}
	}

	private void updateCompassVisibility(@NonNull View compassView, @Nullable LatLon location) {
		ImageView direction = compassView.findViewById(R.id.direction);
		TextView distanceText = compassView.findViewById(R.id.distance);

		boolean showCompass = location != null;
		if (showCompass) {
			UpdateLocationUtils.updateLocationView(app, locationViewCache, direction, distanceText, location);
		}
		AndroidUiHelper.updateVisibility(compassView, showCompass);
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	public Object getItem(int position) {
		return items.get(position);
	}

	interface OnItemSelectedListener {

		void onItemSelected(Object item, boolean selected);

		void onCategorySelected(List<Object> type, boolean selected);

	}

	public static <T> void createHistoryGroups(List<Pair<Long, T>> pairs,
			Map<Integer, List<T>> groups, List<Object> items) {
		int previousHeader = -1;
		int monthsDisplayed = 0;

		Calendar currentDateCalendar = Calendar.getInstance();
		currentDateCalendar.setTimeInMillis(System.currentTimeMillis());
		int currentDay = currentDateCalendar.get(Calendar.DAY_OF_YEAR);
		int currentMonth = currentDateCalendar.get(Calendar.MONTH);
		int currentYear = currentDateCalendar.get(Calendar.YEAR);
		Calendar markerCalendar = Calendar.getInstance();
		for (int i = 0; i < pairs.size(); i++) {
			Pair<Long, T> pair = pairs.get(i);

			markerCalendar.setTimeInMillis(pair.first);
			int markerDay = markerCalendar.get(Calendar.DAY_OF_YEAR);
			int markerMonth = markerCalendar.get(Calendar.MONTH);
			int markerYear = markerCalendar.get(Calendar.YEAR);
			if (markerYear == currentYear) {
				if (markerDay == currentDay && previousHeader != TODAY_HEADER) {
					items.add(TODAY_HEADER);
					previousHeader = TODAY_HEADER;
				} else if (markerDay == currentDay - 1 && previousHeader != YESTERDAY_HEADER) {
					items.add(YESTERDAY_HEADER);
					previousHeader = YESTERDAY_HEADER;
				} else if (currentDay - markerDay >= 2 && currentDay - markerDay <= 8 && previousHeader != LAST_SEVEN_DAYS_HEADER) {
					items.add(LAST_SEVEN_DAYS_HEADER);
					previousHeader = LAST_SEVEN_DAYS_HEADER;
				} else if (currentDay - markerDay > 8 && monthsDisplayed < 3 && previousHeader != markerMonth) {
					items.add(markerMonth);
					previousHeader = markerMonth;
					monthsDisplayed++;
				} else if (currentMonth - markerMonth >= 4 && previousHeader != markerMonth && previousHeader != THIS_YEAR_HEADER) {
					items.add(THIS_YEAR_HEADER);
					previousHeader = THIS_YEAR_HEADER;
				}
			} else if (previousHeader != markerYear) {
				items.add(markerYear);
				previousHeader = markerYear;
			}
			addMarkerToGroup(groups, previousHeader, pair.second);
			items.add(pair.second);
		}
	}

	private static <T> void addMarkerToGroup(Map<Integer, List<T>> markerGroups,
			Integer groupHeader, T marker) {
		List<T> group = markerGroups.get(groupHeader);
		if (group != null) {
			group.add(marker);
		} else {
			group = new ArrayList<>();
			group.add(marker);
			markerGroups.put(groupHeader, group);
		}
	}

	public static String getDateForHeader(@NonNull OsmandApplication app, int header) {
		if (header == TODAY_HEADER) {
			return app.getString(R.string.today);
		} else if (header == YESTERDAY_HEADER) {
			return app.getString(R.string.yesterday);
		} else if (header == LAST_SEVEN_DAYS_HEADER) {
			return app.getString(R.string.last_seven_days);
		} else if (header == THIS_YEAR_HEADER) {
			return app.getString(R.string.this_year);
		} else if (header == PREVIOUS_ROUTE_HEADER) {
			return app.getString(R.string.previous_route);
		} else if (header / 100 == 0) {
			return getMonth(header);
		} else {
			return String.valueOf(header);
		}
	}

	public static String getMonth(int month) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("LLLL", Locale.getDefault());
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.MONTH, month);
		String monthStr = dateFormat.format(calendar.getTime());
		if (monthStr.length() > 1) {
			monthStr = Character.toUpperCase(monthStr.charAt(0)) + monthStr.substring(1);
		}
		return monthStr;
	}

	private static class HeaderViewHolder extends RecyclerView.ViewHolder {

		final TextView title;
		final View divider;
		final View shadowDivider;
		final CompoundButton compoundButton;

		public HeaderViewHolder(View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			divider = itemView.findViewById(R.id.divider);
			shadowDivider = itemView.findViewById(R.id.shadow_divider);
			compoundButton = itemView.findViewById(R.id.toggle_item);
		}
	}

	private abstract static class HistoryItemViewHolder extends RecyclerView.ViewHolder {

		final TextView title;
		final ImageView icon;
		final CompoundButton compoundButton;
		final View divider;
		final View compassView;

		public HistoryItemViewHolder(@NonNull View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			icon = itemView.findViewById(R.id.imageView);
			divider = itemView.findViewById(R.id.divider);
			compoundButton = itemView.findViewById(R.id.toggle_item);
			compassView = itemView.findViewById(R.id.compass_layout);
		}
	}

	private static class SearchItemViewHolder extends HistoryItemViewHolder {

		public SearchItemViewHolder(@NonNull View itemView) {
			super(itemView);
		}
	}

	private static class MarkerViewHolder extends HistoryItemViewHolder {

		public MarkerViewHolder(@NonNull View itemView) {
			super(itemView);
		}
	}

	private static class TargetPointViewHolder extends HistoryItemViewHolder {

		public TargetPointViewHolder(View itemView) {
			super(itemView);
		}
	}

	private static class TrackViewHolder extends RecyclerView.ViewHolder {

		final TextView title;
		final ImageView icon;
		final View divider;
		final CompoundButton compoundButton;

		public TrackViewHolder(@NonNull View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			icon = itemView.findViewById(R.id.icon);
			divider = itemView.findViewById(R.id.divider);
			compoundButton = itemView.findViewById(R.id.toggle_item);
		}
	}
}

package net.osmand.plus.mapmarkers.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapMarkersHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private static final int HEADER_TYPE = 1;
	private static final int MARKER_TYPE = 2;

	private static final int TODAY_HEADER = 56;
	private static final int YESTERDAY_HEADER = 57;
	private static final int LAST_SEVEN_DAYS_HEADER = 58;
	private static final int THIS_YEAR_HEADER = 59;

	private OsmandApplication app;
	private List<Object> items = new ArrayList<>();
	private Map<Integer, List<MapMarker>> markerGroups = new HashMap<>();
	private MapMarkersHistoryAdapterListener listener;
	private Snackbar snackbar;
	private boolean night;

	public MapMarkersHistoryAdapter(OsmandApplication app) {
		this.app = app;
		night = !app.getSettings().isLightContent();
		createHeaders();
	}

	public void createHeaders() {
		items = new ArrayList<>();
		markerGroups = new HashMap<>();
		List<MapMarker> markersHistory = app.getMapMarkersHelper().getMapMarkersHistory();
		int previousHeader = -1;
		int monthsDisplayed = 0;

		Calendar currentDateCalendar = Calendar.getInstance();
		currentDateCalendar.setTimeInMillis(System.currentTimeMillis());
		int currentDay = currentDateCalendar.get(Calendar.DAY_OF_YEAR);
		int currentMonth = currentDateCalendar.get(Calendar.MONTH);
		int currentYear = currentDateCalendar.get(Calendar.YEAR);
		Calendar markerCalendar = Calendar.getInstance();
		for (int i = 0; i < markersHistory.size(); i++) {
			MapMarker marker = markersHistory.get(i);
			markerCalendar.setTimeInMillis(marker.visitedDate);
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
			addMarkerToGroup(previousHeader, marker);
			items.add(marker);
		}
	}

	private void addMarkerToGroup(Integer groupHeader, MapMarker marker) {
		List<MapMarker> group = markerGroups.get(groupHeader);
		if (group != null) {
			group.add(marker);
		} else {
			group = new ArrayList<>();
			group.add(marker);
			markerGroups.put(groupHeader, group);
		}
	}

	public void setAdapterListener(MapMarkersHistoryAdapterListener listener) {
		this.listener = listener;
	}

	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
		if (viewType == MARKER_TYPE) {
			View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.map_marker_item_new, viewGroup, false);
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					listener.onItemClick(view);
				}
			});
			return new MapMarkerItemViewHolder(view);
		} else if (viewType == HEADER_TYPE) {
			View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.map_marker_item_header, viewGroup, false);
			return new MapMarkerHeaderViewHolder(view);
		} else {
			throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@Override
	public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
		UiUtilities iconsCache = app.getUIUtilities();
		if (holder instanceof MapMarkerItemViewHolder) {
			final MapMarkerItemViewHolder itemViewHolder = (MapMarkerItemViewHolder) holder;
			final MapMarker marker = (MapMarker) getItem(position);
			itemViewHolder.iconReorder.setVisibility(View.GONE);

			int color = night ? R.color.icon_color_default_dark : R.color.icon_color_default_light;
			int actionIconColor = night ? R.color.icon_color_primary_dark : R.color.icon_color_primary_light;
			itemViewHolder.icon.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_flag, color));

			itemViewHolder.title.setText(marker.getName(app));

			String desc = app.getString(R.string.passed, OsmAndFormatter.getFormattedDate(app, marker.visitedDate));
			String markerGroupName = marker.groupName;
			if (markerGroupName != null) {
				if (markerGroupName.isEmpty()) {
					markerGroupName = app.getString(R.string.shared_string_favorites);
				}
				desc += " • " + markerGroupName;
			}
			itemViewHolder.description.setText(desc);

			itemViewHolder.optionsBtn.setBackgroundDrawable(AppCompatResources.getDrawable(app, night ? R.drawable.marker_circle_background_dark_with_inset : R.drawable.marker_circle_background_light_with_inset));
			itemViewHolder.optionsBtn.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_reset_to_default_dark, actionIconColor));
			itemViewHolder.optionsBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					int position = itemViewHolder.getAdapterPosition();
					if (position < 0) {
						return;
					}
					app.getMapMarkersHelper().restoreMarkerFromHistory(marker, 0);

					snackbar = Snackbar.make(itemViewHolder.itemView, app.getString(R.string.marker_moved_to_active), Snackbar.LENGTH_LONG)
							.setAction(R.string.shared_string_undo, new View.OnClickListener() {
								@Override
								public void onClick(View view) {
									app.getMapMarkersHelper().moveMapMarkerToHistory(marker);
								}
							});
					UiUtilities.setupSnackbar(snackbar, night);
					snackbar.show();
				}
			});
			itemViewHolder.flagIconLeftSpace.setVisibility(View.VISIBLE);
			itemViewHolder.iconDirection.setVisibility(View.GONE);
			itemViewHolder.leftPointSpace.setVisibility(View.GONE);
			itemViewHolder.rightPointSpace.setVisibility(View.GONE);
			boolean lastItem = position == getItemCount() - 1;
			if ((getItemCount() > position + 1 && getItemViewType(position + 1) == HEADER_TYPE) || lastItem) {
				itemViewHolder.divider.setVisibility(View.GONE);
			} else {
				itemViewHolder.divider.setBackgroundColor(ContextCompat.getColor(app, night ? R.color.app_bar_color_dark : R.color.divider_color_light));
				itemViewHolder.divider.setVisibility(View.VISIBLE);
			}
			itemViewHolder.bottomShadow.setVisibility(lastItem ? View.VISIBLE : View.GONE);
		} else if (holder instanceof MapMarkerHeaderViewHolder) {
			final MapMarkerHeaderViewHolder dateViewHolder = (MapMarkerHeaderViewHolder) holder;
			final Integer dateHeader = (Integer) getItem(position);
			String dateString;
			if (dateHeader == TODAY_HEADER) {
				dateString = app.getString(R.string.today);
			} else if (dateHeader == YESTERDAY_HEADER) {
				dateString = app.getString(R.string.yesterday);
			} else if (dateHeader == LAST_SEVEN_DAYS_HEADER) {
				dateString = app.getString(R.string.last_seven_days);
			} else if (dateHeader == THIS_YEAR_HEADER) {
				dateString = app.getString(R.string.this_year);
			} else if (dateHeader / 100 == 0) {
				dateString = getMonth(dateHeader);
			} else {
				dateString = String.valueOf(dateHeader);
			}
			dateViewHolder.disableGroupSwitch.setVisibility(View.GONE);
			dateViewHolder.title.setText(dateString);
			dateViewHolder.clearButton.setVisibility(View.VISIBLE);
			dateViewHolder.articleDescription.setVisibility(View.GONE);

			dateViewHolder.clearButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final List<MapMarker> group = markerGroups.get(dateHeader);
					if (group == null) {
						return;
					}
					for (MapMarker marker : group) {
						app.getMapMarkersHelper().removeMarker((MapMarker) marker);
					}
					snackbar = Snackbar.make(holder.itemView, app.getString(R.string.n_items_removed), Snackbar.LENGTH_LONG)
							.setAction(R.string.shared_string_undo, new View.OnClickListener() {
								@Override
								public void onClick(View view) {
									for (MapMarker marker : group) {
										app.getMapMarkersHelper().addMarker(marker);
									}
								}
							});
					UiUtilities.setupSnackbar(snackbar, night);
					snackbar.show();
				}
			});
		}
	}

	public void hideSnackbar() {
		if (snackbar != null && snackbar.isShown()) {
			snackbar.dismiss();
		}
	}

	@Override
	public int getItemViewType(int position) {
		Object item = items.get(position);
		if (item instanceof MapMarker) {
			return MARKER_TYPE;
		} else if (item instanceof Integer) {
			return HEADER_TYPE;
		} else {
			throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	public Object getItem(int position) {
		return items.get(position);
	}

	private String getMonth(int month) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("LLLL", Locale.getDefault());
		Date date = new Date();
		date.setMonth(month);
		String monthStr = dateFormat.format(date);
		if (monthStr.length() > 1) {
			monthStr = Character.toUpperCase(monthStr.charAt(0)) + monthStr.substring(1);
		}
		return monthStr;
	}

	public interface MapMarkersHistoryAdapterListener {

		void onItemClick(View view);
	}
}

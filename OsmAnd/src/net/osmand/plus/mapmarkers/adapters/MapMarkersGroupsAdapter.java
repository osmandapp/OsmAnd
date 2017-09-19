package net.osmand.plus.mapmarkers.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.data.LatLon;
import net.osmand.plus.IconsCache;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapmarkers.MapMarkersGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MapMarkersGroupsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private static final int HEADER_TYPE = 1;
	private static final int MARKER_TYPE = 2;
	private static final int SHOW_HIDE_HISTORY_TYPE = 3;

	private static final int TODAY_HEADER = 56;
	private static final int YESTERDAY_HEADER = 57;
	private static final int LAST_SEVEN_DAYS_HEADER = 58;
	private static final int THIS_YEAR_HEADER = 59;

	private OsmandApplication app;
    private List<Object> items = new ArrayList<>();
	private List<MapMarkersGroup> groups;
    private boolean night;
    private int screenOrientation;
    private LatLon location;
    private Float heading;
    private boolean useCenter;

    public MapMarkersGroupsAdapter(MapActivity mapActivity) {
		this.app = mapActivity.getMyApplication();
        night = !mapActivity.getMyApplication().getSettings().isLightContent();
		createDisplayGroups();
    }

    public void createDisplayGroups() {
		items.clear();

		groups = app.getMapMarkersHelper().getMapMarkersGroups();

		for (int i = 0; i < groups.size(); i++) {
			MapMarkersGroup group = groups.get(i);
			String markerGroupName = group.getName();
			if (markerGroupName == null) {
				int previousDateHeader = -1;
				int monthsDisplayed = 0;

				Calendar currentDateCalendar = Calendar.getInstance();
				currentDateCalendar.setTimeInMillis(System.currentTimeMillis());
				int currentDay = currentDateCalendar.get(Calendar.DAY_OF_YEAR);
				int currentMonth = currentDateCalendar.get(Calendar.MONTH);
				int currentYear = currentDateCalendar.get(Calendar.YEAR);
				Calendar markerCalendar = Calendar.getInstance();
				List<MapMarker> groupMarkers = group.getMapMarkers();
				for (int j = 0; j < groupMarkers.size(); j++) {
					MapMarker marker = groupMarkers.get(j);
					markerCalendar.setTimeInMillis(marker.creationDate);
					int markerDay = markerCalendar.get(Calendar.DAY_OF_YEAR);
					int markerMonth = markerCalendar.get(Calendar.MONTH);
					int markerYear = markerCalendar.get(Calendar.YEAR);
					if (markerYear == currentYear) {
						if (markerDay == currentDay && previousDateHeader != TODAY_HEADER) {
							items.add(TODAY_HEADER);
							previousDateHeader = TODAY_HEADER;
						} else if (markerDay == currentDay - 1 && previousDateHeader != YESTERDAY_HEADER) {
							items.add(YESTERDAY_HEADER);
							previousDateHeader = YESTERDAY_HEADER;
						} else if (currentDay - markerDay >= 2 && currentDay - markerDay <= 8 && previousDateHeader != LAST_SEVEN_DAYS_HEADER) {
							items.add(LAST_SEVEN_DAYS_HEADER);
							previousDateHeader = LAST_SEVEN_DAYS_HEADER;
						} else if (currentDay - markerDay > 8 && monthsDisplayed < 3 && previousDateHeader != markerMonth) {
							items.add(markerMonth);
							previousDateHeader = markerMonth;
							monthsDisplayed += 1;
						} else if (currentMonth - markerMonth >= 4 && previousDateHeader != THIS_YEAR_HEADER) {
							items.add(THIS_YEAR_HEADER);
							previousDateHeader = THIS_YEAR_HEADER;
						}
					} else if (previousDateHeader != markerYear) {
						items.add(markerYear);
						previousDateHeader = markerYear;
					}
					items.add(marker);
				}
			} else {
				if (markerGroupName.equals("")) {
					markerGroupName = app.getString(R.string.shared_string_favorites);
				}
				items.add(markerGroupName);
				items.addAll(group.getMapMarkers());
				if (group.getHistoryMarkers().size() > 0) {
					MapMarkersGroup.ShowHideHistoryButton showHideHistoryButton = new MapMarkersGroup.ShowHideHistoryButton();
					showHideHistoryButton.setShowHistory(false);
					showHideHistoryButton.setGroup(group);
					group.setShowHideHistoryButton(showHideHistoryButton);
					items.add(showHideHistoryButton);
				}
			}
		}
	}

    public void setLocation(LatLon location) {
        this.location = location;
    }

    public void setHeading(Float heading) {
        this.heading = heading;
    }

    public void setUseCenter(boolean useCenter) {
        this.useCenter = useCenter;
    }

    public void setScreenOrientation(int screenOrientation) {
        this.screenOrientation = screenOrientation;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
		if (viewType == MARKER_TYPE) {
			View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.map_marker_item_new, viewGroup, false);
			return new MapMarkerItemViewHolder(view);
		} else if (viewType == HEADER_TYPE) {
			View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.map_marker_item_header, viewGroup, false);
			return new MapMarkerHeaderViewHolder(view);
		} else if (viewType == SHOW_HIDE_HISTORY_TYPE) {
			View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.map_marker_item_show_hide_history, viewGroup, false);
			return new MapMarkersShowHideHistoryViewHolder(view);
		} else {
			throw new IllegalArgumentException("Unsupported view type");
		}
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
		IconsCache iconsCache = app.getIconsCache();
		if (holder instanceof MapMarkerItemViewHolder) {
			final MapMarkerItemViewHolder itemViewHolder = (MapMarkerItemViewHolder) holder;
			final MapMarker marker = (MapMarker) getItem(position);
			final boolean markerInHistory = marker.history;

			int color = MapMarker.getColorId(marker.colorIndex);
			itemViewHolder.icon.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_flag_dark, color));

			itemViewHolder.title.setText(marker.getName(app));

			itemViewHolder.description.setText(app.getString(R.string.passed, new SimpleDateFormat("MMM dd", Locale.getDefault()).format(new Date(marker.visitedDate))));

			itemViewHolder.optionsBtn.setImageDrawable(iconsCache.getThemedIcon(markerInHistory ? R.drawable.ic_action_reset_to_default_dark : R.drawable.ic_action_marker_passed));
			itemViewHolder.optionsBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					int position = itemViewHolder.getAdapterPosition();
					if (position < 0) {
						return;
					}
					if (markerInHistory) {
						app.getMapMarkersHelper().restoreMarkerFromHistory(marker, 0);
						if (marker.groupName != null) {
							MapMarkersGroup group = getMapMarkerGroupByName(marker.groupName);
							if (group != null) {
								restoreMarkerFromHistoryInGroup(group, marker);
								MapMarkersGroup.ShowHideHistoryButton showHideHistoryButton = group.getShowHideHistoryButton();
								if (showHideHistoryButton != null) {
									if (group.getHistoryMarkers().size() == 0) {
										items.remove(showHideHistoryButton);
										group.setShowHideHistoryButton(null);
									}
								}
							}
						}
					} else {
						app.getMapMarkersHelper().moveMapMarkerToHistory(marker);
						if (marker.groupName != null) {
							MapMarkersGroup group = getMapMarkerGroupByName(marker.groupName);
							if (group != null) {
								moveMarkerToHistoryInGroup(group, marker);
								MapMarkersGroup.ShowHideHistoryButton showHideHistoryButton = group.getShowHideHistoryButton();
								if (showHideHistoryButton == null) {
									items.remove(marker);
									showHideHistoryButton = new MapMarkersGroup.ShowHideHistoryButton();
									showHideHistoryButton.setShowHistory(false);
									showHideHistoryButton.setGroup(group);
									int index = getLastDisplayItemIndexOfGroup(group);
									if (index != -1) {
										items.add(index + 1, showHideHistoryButton);
										group.setShowHideHistoryButton(showHideHistoryButton);
									}
								} else if (!showHideHistoryButton.isShowHistory()) {
									items.remove(marker);
								}
							}
						}
					}
					notifyDataSetChanged();
				}
			});
			itemViewHolder.iconReorder.setVisibility(View.GONE);
			itemViewHolder.flagIconLeftSpace.setVisibility(View.VISIBLE);
			itemViewHolder.iconDirection.setVisibility(View.GONE);
			itemViewHolder.leftPointSpace.setVisibility(View.GONE);
			itemViewHolder.rightPointSpace.setVisibility(View.GONE);
			if (position == getItemCount() - 1) {
				itemViewHolder.bottomShadow.setVisibility(View.VISIBLE);
				itemViewHolder.divider.setVisibility(View.GONE);
			} else {
				itemViewHolder.bottomShadow.setVisibility(View.GONE);
				itemViewHolder.divider.setVisibility(View.VISIBLE);
			}
		} else if (holder instanceof MapMarkerHeaderViewHolder) {
			final MapMarkerHeaderViewHolder headerViewHolder = (MapMarkerHeaderViewHolder) holder;
			final Object header = getItem(position);
			String headerString;
			if (header instanceof Integer) {
				Integer dateHeader = (Integer) header;
				if (dateHeader == TODAY_HEADER) {
					headerString = app.getString(R.string.today);
				} else if (dateHeader == YESTERDAY_HEADER) {
					headerString = app.getString(R.string.yesterday);
				} else if (dateHeader == LAST_SEVEN_DAYS_HEADER) {
					headerString = app.getString(R.string.last_seven_days);
				} else if (dateHeader == THIS_YEAR_HEADER) {
					headerString = app.getString(R.string.this_year);
				} else if (dateHeader / 100 == 0) {
					headerString = getMonth(dateHeader);
				} else {
					headerString = String.valueOf(dateHeader);
				}
			} else if (header instanceof String) {
				headerString = (String) header;
			} else {
				throw new IllegalArgumentException("Unsupported header");
			}
			headerViewHolder.date.setText(headerString);
		} else if (holder instanceof MapMarkersShowHideHistoryViewHolder) {
			final MapMarkersShowHideHistoryViewHolder showHideHistoryViewHolder = (MapMarkersShowHideHistoryViewHolder) holder;
			final MapMarkersGroup.ShowHideHistoryButton showHideHistoryButton = (MapMarkersGroup.ShowHideHistoryButton) getItem(position);
			final boolean showHistory = showHideHistoryButton.isShowHistory();
			showHideHistoryViewHolder.title.setText(app.getString(showHistory ? R.string.hide_passed : R.string.show_passed));
			showHideHistoryViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					List<MapMarker> historyMarkers = showHideHistoryButton.getGroup().getHistoryMarkers();
					int pos = holder.getAdapterPosition();
					if (showHistory) {
						showHideHistoryButton.setShowHistory(false);
						items.removeAll(historyMarkers);
					} else {
						showHideHistoryButton.setShowHistory(true);
						items.addAll(pos, historyMarkers);
					}
					notifyDataSetChanged();
				}
			});
		}
    }

	@Override
	public int getItemViewType(int position) {
		Object item = items.get(position);
		if (item instanceof MapMarker) {
			return MARKER_TYPE;
		} else if (item instanceof String || item instanceof Integer) {
			return HEADER_TYPE;
		} else if (item instanceof MapMarkersGroup.ShowHideHistoryButton) {
			return SHOW_HIDE_HISTORY_TYPE;
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
		return dateFormat.format(date);
	}

	private MapMarkersGroup getMapMarkerGroupByName(String name) {
		for (MapMarkersGroup group : groups) {
			if (group.getName() != null && group.getName().equals(name)) {
				return group;
			}
		}
		return null;
	}

	private void moveMarkerToHistoryInGroup(MapMarkersGroup group, MapMarker marker) {
		group.getMapMarkers().remove(marker);
		group.getHistoryMarkers().add(marker);
	}

	private void restoreMarkerFromHistoryInGroup(MapMarkersGroup group, MapMarker marker) {
		group.getHistoryMarkers().remove(marker);
		group.getMapMarkers().add(marker);
	}

	private int getLastDisplayItemIndexOfGroup(MapMarkersGroup group) {
		List<MapMarker> markers = group.getMapMarkers();
		int index = -1;
		for (MapMarker marker : markers) {
			int markerIndex = items.indexOf(marker);
			if (markerIndex > index) {
				index = markerIndex;
			}
		}
		return index;
	}
}

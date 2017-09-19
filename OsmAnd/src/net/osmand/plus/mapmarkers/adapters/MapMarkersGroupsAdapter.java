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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MapMarkersGroupsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private static final int HEADER_TYPE = 1;
	private static final int MARKER_TYPE = 2;

	private static final int TODAY_HEADER = 56;
	private static final int YESTERDAY_HEADER = 57;
	private static final int LAST_SEVEN_DAYS_HEADER = 58;
	private static final int THIS_YEAR_HEADER = 59;

	private OsmandApplication app;
    private List<Object> items = new ArrayList<>();
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

		List<MapMarker> markersHistory = app.getMapMarkersHelper().getMarkersSortedByGroup();

		int previousDateHeader = -1;
		String previousNameHeader = "";
		int monthsDisplayed = 0;

		Calendar currentDateCalendar = Calendar.getInstance();
		currentDateCalendar.setTimeInMillis(System.currentTimeMillis());
		int currentDay = currentDateCalendar.get(Calendar.DAY_OF_YEAR);
		int currentMonth = currentDateCalendar.get(Calendar.MONTH);
		int currentYear = currentDateCalendar.get(Calendar.YEAR);
		Calendar markerCalendar = Calendar.getInstance();
		for (int i = 0; i < markersHistory.size(); i++) {
			MapMarker marker = markersHistory.get(i);
			markerCalendar.setTimeInMillis(marker.creationDate);
			int markerDay = markerCalendar.get(Calendar.DAY_OF_YEAR);
			int markerMonth = markerCalendar.get(Calendar.MONTH);
			int markerYear = markerCalendar.get(Calendar.YEAR);
			String markerGroupName = marker.groupName;
			if (markerGroupName != null && markerGroupName.equals("")) {
				markerGroupName = app.getString(R.string.shared_string_favorites);
			}
			if (markerGroupName == null && markerYear == currentYear) {
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
			} else if (markerGroupName == null && previousDateHeader != markerYear) {
				items.add(markerYear);
				previousDateHeader = markerYear;
			} else if (markerGroupName != null && !previousNameHeader.equals(markerGroupName)) {
				items.add(markerGroupName);
				previousNameHeader = markerGroupName;
			}
			items.add(marker);
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
		} else {
			throw new IllegalArgumentException("Unsupported view type");
		}
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
		IconsCache iconsCache = app.getIconsCache();
		if (holder instanceof MapMarkerItemViewHolder) {
			final MapMarkerItemViewHolder itemViewHolder = (MapMarkerItemViewHolder) holder;
			final MapMarker marker = (MapMarker) getItem(position);
			itemViewHolder.iconReorder.setVisibility(View.GONE);

			int color = MapMarker.getColorId(marker.colorIndex);
			itemViewHolder.icon.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_flag_dark, color));

			itemViewHolder.title.setText(marker.getName(app));

			itemViewHolder.description.setText(app.getString(R.string.passed, new SimpleDateFormat("MMM dd", Locale.getDefault()).format(new Date(marker.visitedDate))));

			itemViewHolder.optionsBtn.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_reset_to_default_dark));
			itemViewHolder.optionsBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
//					int position = itemViewHolder.getAdapterPosition();
//					if (position < 0) {
//						return;
//					}
//					app.getMapMarkersHelper().restoreMarkerFromHistory(marker, 0);
//					notifyItemRemoved(position);
				}
			});
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
			final MapMarkerHeaderViewHolder dateViewHolder = (MapMarkerHeaderViewHolder) holder;
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
			dateViewHolder.date.setText(headerString);
		}
    }

	@Override
	public int getItemViewType(int position) {
		Object item = items.get(position);
		if (item instanceof MapMarker) {
			return MARKER_TYPE;
		} else if (item instanceof String || item instanceof Integer) {
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
		return dateFormat.format(date);
	}
}

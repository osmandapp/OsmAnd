package net.osmand.plus.mapmarkers.adapters;

import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.data.LatLon;
import net.osmand.plus.IconsCache;
import net.osmand.plus.MapMarkersHelper.GroupHeader;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.MapMarkersHelper.MapMarkersGroup;
import net.osmand.plus.MapMarkersHelper.ShowHideHistoryButton;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashLocationFragment;

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

	private MapActivity mapActivity;
	private OsmandApplication app;
	private List<Object> items = new ArrayList<>();
	private boolean night;
	private int screenOrientation;
	private LatLon location;
	private Float heading;
	private boolean useCenter;
	private boolean showDirectionEnabled;
	private List<MapMarker> showDirectionMarkers;
	private Snackbar snackbar;

	private MapMarkersGroupsAdapterListener listener;

	public void setListener(MapMarkersGroupsAdapterListener listener) {
		this.listener = listener;
	}

	public MapMarkersGroupsAdapter(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		app = mapActivity.getMyApplication();
		night = !mapActivity.getMyApplication().getSettings().isLightContent();
		updateShowDirectionMarkers();
		createDisplayGroups();
	}

	public void updateShowDirectionMarkers() {
		showDirectionEnabled = app.getSettings().MAP_MARKERS_MODE.get() != OsmandSettings.MapMarkersMode.NONE;
		List<MapMarker> mapMarkers = app.getMapMarkersHelper().getMapMarkers();
		int markersCount = mapMarkers.size();
		showDirectionMarkers = new ArrayList<>(mapMarkers.subList(0, getToIndex(markersCount)));
	}

	private int getToIndex(int markersCount) {
		if (markersCount > 0) {
			if (markersCount > 1 && app.getSettings().DISPLAYED_MARKERS_WIDGETS_COUNT.get() == 2) {
				return 2;
			}
			return 1;
		}
		return 0;
	}

	public void createDisplayGroups() {
		items = new ArrayList<>();
		app.getMapMarkersHelper().updateGroups();
		List<MapMarkersGroup> groups = app.getMapMarkersHelper().getMapMarkersGroups();
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
				List<MapMarker> groupMarkers = group.getActiveMarkers();
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
				GroupHeader header = group.getGroupHeader();
				items.add(header);
				populateAdapterWithGroupMarkers(group, getItemCount());
			}
		}
	}

	private void populateAdapterWithGroupMarkers(MapMarkersGroup group, int position) {
		if (position != RecyclerView.NO_POSITION) {
			ShowHideHistoryButton showHideHistoryButton = group.getShowHideHistoryButton();
			if (!group.isDisabled()) {
				List<Object> objectsToAdd = new ArrayList<>();
				if (showHideHistoryButton != null && showHideHistoryButton.isShowHistory()) {
					objectsToAdd.addAll(group.getMarkers());
				} else {
					objectsToAdd.addAll(group.getActiveMarkers());
				}
				if (showHideHistoryButton != null) {
					objectsToAdd.add(showHideHistoryButton);
				}
				items.addAll(position, objectsToAdd);
			} else {
				items.removeAll(group.getActiveMarkers());
				if (showHideHistoryButton != null) {
					items.remove(showHideHistoryButton);
				}
			}
		}
	}

	public int getGroupHeaderPosition(String groupId) {
		int pos = -1;
		MapMarkersGroup group = app.getMapMarkersHelper().getMapMarkerGroupByKey(groupId);
		if (group != null) {
			pos = items.indexOf(group.getGroupHeader());
		}
		return pos;
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

			int color;
			if (marker.history) {
				color = R.color.icon_color_light;
			} else {
				color = MapMarker.getColorId(marker.colorIndex);
			}
			ImageView markerImageViewToUpdate;
			int drawableResToUpdate;
			final boolean markerToHighlight = showDirectionMarkers.contains(marker);
			if (showDirectionEnabled && markerToHighlight) {
				itemViewHolder.iconDirection.setVisibility(View.GONE);

				itemViewHolder.icon.setImageDrawable(iconsCache.getIcon(R.drawable.ic_arrow_marker_diretion, color));
				itemViewHolder.mainLayout.setBackgroundColor(ContextCompat.getColor(mapActivity, night ? R.color.list_divider_dark : R.color.markers_top_bar_background));
				itemViewHolder.title.setTextColor(ContextCompat.getColor(mapActivity, R.color.color_white));
				itemViewHolder.divider.setBackgroundColor(ContextCompat.getColor(mapActivity, R.color.map_markers_on_map_divider_color));
				itemViewHolder.optionsBtn.setBackgroundDrawable(mapActivity.getResources().getDrawable(R.drawable.marker_circle_background_on_map_with_inset));
				itemViewHolder.optionsBtn.setImageDrawable(iconsCache.getIcon(markerInHistory ? R.drawable.ic_action_reset_to_default_dark : R.drawable.ic_action_marker_passed, R.color.color_white));
				itemViewHolder.description.setTextColor(ContextCompat.getColor(mapActivity, R.color.map_markers_on_map_color));

				drawableResToUpdate = R.drawable.ic_arrow_marker_diretion;
				markerImageViewToUpdate = itemViewHolder.icon;
			} else {
				itemViewHolder.iconDirection.setVisibility(View.VISIBLE);

				itemViewHolder.icon.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_flag_dark, color));
				itemViewHolder.mainLayout.setBackgroundColor(ContextCompat.getColor(mapActivity, night ? R.color.bg_color_dark : R.color.bg_color_light));
				itemViewHolder.title.setTextColor(ContextCompat.getColor(mapActivity, night ? R.color.color_white : R.color.color_black));
				itemViewHolder.divider.setBackgroundColor(ContextCompat.getColor(mapActivity, night ? R.color.actionbar_dark_color : R.color.dashboard_divider_light));
				itemViewHolder.optionsBtn.setBackgroundDrawable(mapActivity.getResources().getDrawable(night ? R.drawable.marker_circle_background_dark_with_inset : R.drawable.marker_circle_background_light_with_inset));
				itemViewHolder.optionsBtn.setImageDrawable(iconsCache.getThemedIcon(markerInHistory ? R.drawable.ic_action_reset_to_default_dark : R.drawable.ic_action_marker_passed));
				itemViewHolder.description.setTextColor(ContextCompat.getColor(mapActivity, night ? R.color.dash_search_icon_dark : R.color.icon_color));

				drawableResToUpdate = R.drawable.ic_direction_arrow;
				markerImageViewToUpdate = itemViewHolder.iconDirection;
			}

			itemViewHolder.title.setText(marker.getName(app));

			boolean noGroup = marker.groupName == null;
			boolean createdEarly = false;
			if (noGroup && !markerInHistory) {
				Calendar currentDateCalendar = Calendar.getInstance();
				currentDateCalendar.setTimeInMillis(System.currentTimeMillis());
				int currentDay = currentDateCalendar.get(Calendar.DAY_OF_YEAR);
				int currentYear = currentDateCalendar.get(Calendar.YEAR);
				Calendar markerCalendar = Calendar.getInstance();
				markerCalendar.setTimeInMillis(System.currentTimeMillis());
				int markerDay = markerCalendar.get(Calendar.DAY_OF_YEAR);
				int markerYear = markerCalendar.get(Calendar.YEAR);
				createdEarly = currentDay - markerDay >= 2 || currentYear != markerYear;
			}
			if (markerInHistory || createdEarly) {
				itemViewHolder.point.setVisibility(View.VISIBLE);
				itemViewHolder.description.setVisibility(View.VISIBLE);
				Date date;
				if (markerInHistory) {
					date = new Date(marker.visitedDate);
				} else {
					date = new Date(marker.creationDate);
				}
				String month = new SimpleDateFormat("MMM", Locale.getDefault()).format(date);
				if (month.length() > 1) {
					month = Character.toUpperCase(month.charAt(0)) + month.substring(1);
				}
				month = month.replaceAll("\\.", "");
				String day = new SimpleDateFormat("d", Locale.getDefault()).format(date);
				itemViewHolder.description.setText(app.getString(R.string.passed, month + " " + day));
			} else {
				itemViewHolder.point.setVisibility(View.GONE);
				itemViewHolder.description.setVisibility(View.GONE);
			}

			itemViewHolder.optionsBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					int position = itemViewHolder.getAdapterPosition();
					if (position < 0) {
						return;
					}
					if (markerInHistory) {
						app.getMapMarkersHelper().restoreMarkerFromHistory(marker, 0);
					} else {
						app.getMapMarkersHelper().moveMapMarkerToHistory(marker);
					}
					createDisplayGroups();
					updateShowDirectionMarkers();
					notifyDataSetChanged();
					if (!markerInHistory) {
						snackbar = Snackbar.make(itemViewHolder.itemView, R.string.marker_moved_to_history, Snackbar.LENGTH_LONG)
								.setAction(R.string.shared_string_undo, new View.OnClickListener() {
									@Override
									public void onClick(View view) {
										mapActivity.getMyApplication().getMapMarkersHelper().restoreMarkerFromHistory(marker, 0);
										createDisplayGroups();
										updateShowDirectionMarkers();
										notifyDataSetChanged();
									}
								});
						View snackBarView = snackbar.getView();
						TextView tv = (TextView) snackBarView.findViewById(android.support.design.R.id.snackbar_action);
						tv.setTextColor(ContextCompat.getColor(mapActivity, R.color.color_dialog_buttons_dark));
						snackbar.show();
					}
				}
			});
			itemViewHolder.iconReorder.setVisibility(View.GONE);
			itemViewHolder.flagIconLeftSpace.setVisibility(View.VISIBLE);
			boolean lastItem = position == getItemCount() - 1;
			if ((getItemCount() > position + 1 && getItemViewType(position + 1) == HEADER_TYPE) || lastItem) {
				itemViewHolder.divider.setVisibility(View.GONE);
			} else {
				itemViewHolder.divider.setVisibility(View.VISIBLE);
			}
			itemViewHolder.bottomShadow.setVisibility(lastItem ? View.VISIBLE : View.GONE);

			LatLon markerLatLon = new LatLon(marker.getLatitude(), marker.getLongitude());
			DashLocationFragment.updateLocationView(useCenter, location,
					heading, markerImageViewToUpdate, drawableResToUpdate, markerToHighlight ? color : 0,
					itemViewHolder.distance, markerLatLon,
					screenOrientation, app, mapActivity, true);
		} else if (holder instanceof MapMarkerHeaderViewHolder) {
			final MapMarkerHeaderViewHolder headerViewHolder = (MapMarkerHeaderViewHolder) holder;
			final Object header = getItem(position);
			String headerString;
			if (header instanceof Integer) {
				headerViewHolder.icon.setVisibility(View.GONE);
				headerViewHolder.iconSpace.setVisibility(View.VISIBLE);
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
				headerViewHolder.disableGroupSwitch.setVisibility(View.GONE);
			} else if (header instanceof GroupHeader) {
				final GroupHeader groupHeader = (GroupHeader) header;
				final MapMarkersGroup group = groupHeader.getGroup();
				String groupName = group.getName();
				if (groupName.equals("")) {
					groupName = app.getString(R.string.shared_string_favorites);
				}
				headerString = groupName + " \u2014 "
						+ group.getActiveMarkers().size()
						+ "/" + group.getMarkers().size();
				headerViewHolder.icon.setVisibility(View.VISIBLE);
				headerViewHolder.iconSpace.setVisibility(View.GONE);
				headerViewHolder.icon.setImageDrawable(iconsCache.getIcon(groupHeader.getIconRes(), R.color.divider_color));
				boolean groupIsDisabled = group.isDisabled();
				headerViewHolder.disableGroupSwitch.setVisibility(View.VISIBLE);
				CompoundButton.OnCheckedChangeListener checkedChangeListener = new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton compoundButton, boolean enabled) {
						group.setDisabled(!enabled);
						app.getMapMarkersHelper().updateGroupDisabled(group, !enabled);
						createDisplayGroups();
						updateShowDirectionMarkers();
						notifyDataSetChanged();
						if (!enabled) {
							snackbar = Snackbar.make(holder.itemView, app.getString(R.string.group_will_be_removed_after_restart), Snackbar.LENGTH_LONG)
									.setAction(R.string.shared_string_undo, new View.OnClickListener() {
										@Override
										public void onClick(View view) {
											headerViewHolder.disableGroupSwitch.setChecked(true);
										}
									});
							View snackBarView = snackbar.getView();
							TextView tv = (TextView) snackBarView.findViewById(android.support.design.R.id.snackbar_action);
							tv.setTextColor(ContextCompat.getColor(mapActivity, R.color.color_dialog_buttons_dark));
							snackbar.show();
						}
					}
				};
				headerViewHolder.disableGroupSwitch.setOnCheckedChangeListener(null);
				headerViewHolder.disableGroupSwitch.setChecked(!groupIsDisabled);
				headerViewHolder.disableGroupSwitch.setOnCheckedChangeListener(checkedChangeListener);
			} else {
				throw new IllegalArgumentException("Unsupported header");
			}
			headerViewHolder.title.setText(headerString);
			headerViewHolder.bottomShadow.setVisibility(position == getItemCount() - 1 ? View.VISIBLE : View.GONE);
		} else if (holder instanceof MapMarkersShowHideHistoryViewHolder) {
			final MapMarkersShowHideHistoryViewHolder showHideHistoryViewHolder = (MapMarkersShowHideHistoryViewHolder) holder;
			final ShowHideHistoryButton showHideHistoryButton = (ShowHideHistoryButton) getItem(position);
			final boolean showHistory = showHideHistoryButton.isShowHistory();
			if (position == getItemCount() - 1) {
				showHideHistoryViewHolder.bottomShadow.setVisibility(View.VISIBLE);
			} else {
				showHideHistoryViewHolder.bottomShadow.setVisibility(View.GONE);
			}
			showHideHistoryViewHolder.title.setText(app.getString(showHistory ? R.string.hide_passed : R.string.show_passed));
			showHideHistoryViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					showHideHistoryButton.setShowHistory(!showHistory);
					createDisplayGroups();
					notifyDataSetChanged();
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
		} else if (item instanceof GroupHeader || item instanceof Integer) {
			return HEADER_TYPE;
		} else if (item instanceof ShowHideHistoryButton) {
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
		String monthStr = dateFormat.format(date);
		if (monthStr.length() > 1) {
			monthStr = Character.toUpperCase(monthStr.charAt(0)) + monthStr.substring(1);
		}
		return monthStr;
	}

	public interface MapMarkersGroupsAdapterListener {

		void onItemClick(View view);
	}
}

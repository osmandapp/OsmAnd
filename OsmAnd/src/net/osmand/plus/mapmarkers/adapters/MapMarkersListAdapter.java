package net.osmand.plus.mapmarkers.adapters;

import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.GeocodingLookupService.AddressLookupRequest;
import net.osmand.plus.GeocodingLookupService.OnAddressLookupResult;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.util.MapUtils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MapMarkersListAdapter extends RecyclerView.Adapter<MapMarkerItemViewHolder>
		implements MapMarkersItemTouchHelperCallback.ItemTouchHelperAdapter {

	private static final int LOCATION_ITEM_ID = 0;
	private static final int ROUND_TRIP_FINISH_ITEM_ID = 1;

	private MapActivity mapActivity;
	private List<Object> items = new LinkedList<>();
	private MapMarkersListAdapterListener listener;

	private int startPos = -1;
	private int finishPos = -1;
	private int firstSelectedMarkerPos = -1;

	private boolean showLocationItem;
	private Location myLoc;
	private AddressLookupRequest locRequest;
	private PointDescription locDescription;

	private Map<Pair<WptPt, WptPt>, List<WptPt>> snappedToRoadPoints;

	private boolean inRoundTrip;
	private boolean showRoundTripItem;
	private boolean inDragAndDrop;

	public void setAdapterListener(MapMarkersListAdapterListener listener) {
		this.listener = listener;
	}

	public void setSnappedToRoadPoints(Map<Pair<WptPt, WptPt>, List<WptPt>> snappedToRoadPoints) {
		this.snappedToRoadPoints = snappedToRoadPoints;
	}

	public MapMarkersListAdapter(MapActivity mapActivity) {
		locDescription = new PointDescription(PointDescription.POINT_TYPE_MY_LOCATION,
				mapActivity.getString(R.string.shared_string_location));
		this.mapActivity = mapActivity;
		inRoundTrip = mapActivity.getMyApplication().getSettings().ROUTE_MAP_MARKERS_ROUND_TRIP.get();
		reloadData();
	}

	@Override
	public MapMarkerItemViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
		View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.map_marker_item_new, viewGroup, false);
		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				listener.onItemClick(view);
			}
		});
		return new MapMarkerItemViewHolder(view);
	}

	@Override
	public void onBindViewHolder(final MapMarkerItemViewHolder holder, int pos) {
		OsmandApplication app = mapActivity.getMyApplication();
		boolean night = app.getDaynightHelper().isNightModeForMapControls();
		UiUtilities iconsCache = app.getUIUtilities();

		boolean locationItem = showLocationItem && pos == 0;
		boolean firstMarkerItem = showLocationItem ? pos == 1 : pos == 0;
		boolean lastMarkerItem = pos == getItemCount() - 1;
		boolean start = pos == startPos;
		final boolean finish = pos == finishPos && startPos != finishPos;
		boolean firstSelectedMarker = pos == firstSelectedMarkerPos;
		boolean roundTripFinishItem = finish && showRoundTripItem;

		boolean useLocation = app.getMapMarkersHelper().isStartFromMyLocation() && showLocationItem;

		MapMarker marker = null;
		Location location = null;
		Object item = getItem(pos);
		if (item instanceof Location) {
			location = (Location) item;
		} else {
			marker = (MapMarker) item;
		}

		holder.mainLayout.setBackgroundColor(ContextCompat.getColor(mapActivity, night ? R.color.list_background_color_dark : R.color.list_background_color_light));
		holder.title.setTextColor(ContextCompat.getColor(mapActivity, night ? R.color.color_white : R.color.color_black));
		holder.title.setText(location != null ? mapActivity.getString(R.string.shared_string_my_location) : marker.getName(mapActivity));
		holder.iconDirection.setVisibility(View.GONE);
		holder.optionsBtn.setVisibility(roundTripFinishItem ? View.VISIBLE : View.GONE);
		if (roundTripFinishItem) {
			holder.optionsBtn.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_remove_dark));
			TypedValue outValue = new TypedValue();
			mapActivity.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
			holder.optionsBtn.setBackgroundResource(outValue.resourceId);
			holder.optionsBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					listener.onDisableRoundTripClick();
				}
			});
		}
		holder.divider.setBackgroundColor(ContextCompat.getColor(mapActivity, night ? R.color.app_bar_color_dark : R.color.divider_color_light));
		holder.divider.setVisibility(lastMarkerItem ? View.GONE : View.VISIBLE);
		holder.checkBox.setVisibility(roundTripFinishItem ? View.GONE : View.VISIBLE);
		if (!roundTripFinishItem) {
			holder.checkBox.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					listener.onCheckBoxClick(holder.itemView);
				}
			});
			holder.checkBoxContainer.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					holder.checkBox.performClick();
				}
			});
		}
		holder.bottomShadow.setVisibility(lastMarkerItem ? View.VISIBLE : View.GONE);
		holder.iconReorder.setVisibility(View.VISIBLE);
		holder.iconReorder.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_reorder));
		holder.description.setTextColor(ContextCompat.getColor(mapActivity, night ? R.color.icon_color_default_dark : R.color.icon_color_default_light));

		holder.firstDescription.setVisibility((start || finish) ? View.VISIBLE : View.GONE);
		if (start) {
			holder.firstDescription.setText(mapActivity.getString(R.string.shared_string_control_start) + " • ");
		} else if (finish) {
			holder.firstDescription.setText(mapActivity.getString(R.string.shared_string_finish) + " • ");
		}

		if (location != null) {
			holder.icon.setImageDrawable(ContextCompat.getDrawable(mapActivity, R.drawable.ic_action_location_color));
		} else {
			int res = start ? R.drawable.ic_action_point_start : (finish ? R.drawable.ic_action_point_destination : R.drawable.ic_action_flag_dark);
			holder.icon.setImageDrawable(iconsCache.getIcon(res, MapMarker.getColorId(marker.colorIndex)));
		}

		if (locationItem || roundTripFinishItem) {
			holder.iconReorder.setAlpha(.5f);
			holder.iconReorder.setOnTouchListener(null);
		}

		if (locationItem) {
			holder.topDivider.setVisibility(View.VISIBLE);
			holder.checkBox.setChecked(app.getMapMarkersHelper().isStartFromMyLocation());
			holder.distance.setVisibility(View.GONE);
			holder.description.setText(locDescription.getName());
		} else if (roundTripFinishItem) {
			holder.topDivider.setVisibility(View.GONE);
			holder.description.setText(mapActivity.getString(R.string.round_trip));
		} else {
			holder.topDivider.setVisibility((!showLocationItem && firstMarkerItem) ? View.VISIBLE : View.GONE);
			holder.checkBox.setChecked(marker.selected);

			holder.iconReorder.setAlpha(1f);
			holder.iconReorder.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View view, MotionEvent event) {
					if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
						inDragAndDrop = true;
						if (showRoundTripItem) {
							int roundTripItemPos = finishPos;
							reloadData();
							notifyItemRemoved(roundTripItemPos);
						}
						listener.onDragStarted(holder);
					}
					return false;
				}
			});

			String descr;
			if ((descr = marker.groupName) != null) {
				if (descr.equals("")) {
					descr = mapActivity.getString(R.string.shared_string_favorites);
				}
			} else {
				descr = OsmAndFormatter.getFormattedDate(app, marker.creationDate);
			}
			holder.description.setText(descr);
		}

		boolean showDistance = !roundTripFinishItem && (locationItem ? useLocation : marker != null && marker.selected);
		int visibility = showDistance ? View.VISIBLE : View.GONE;
		holder.distance.setVisibility(visibility);
		holder.point.setVisibility(visibility);
		holder.leftPointSpace.setVisibility(visibility);
		holder.rightPointSpace.setVisibility(visibility);
		if (showDistance) {
			holder.distance.setTextColor(ContextCompat.getColor(mapActivity, night ? R.color.color_distance : R.color.color_myloc_distance));
			LatLon first = firstSelectedMarker && useLocation
					? new LatLon(myLoc.getLatitude(), myLoc.getLongitude())
					: getPreviousSelectedMarkerLatLon(pos);
			float dist = 0;
			if (first != null && marker != null) {
				WptPt pt1 = new WptPt();
				pt1.lat = first.getLatitude();
				pt1.lon = first.getLongitude();
				WptPt pt2 = new WptPt();
				pt2.lat = marker.getLatitude();
				pt2.lon = marker.getLongitude();
				List<WptPt> points = snappedToRoadPoints.get(new Pair<>(pt1, pt2));
				if (points != null) {
					for (int i = 0; i < points.size() - 1; i++) {
						dist += (float) MapUtils.getDistance(points.get(i).lat, points.get(i).lon,
								points.get(i + 1).lat, points.get(i + 1).lon);
					}
				} else {
					dist = (float) MapUtils.getDistance(pt1.lat, pt1.lon, pt2.lat, pt2.lon);
				}
			}
			holder.distance.setText(OsmAndFormatter.getFormattedDistance(dist, app));
		}
	}

	@Override
	public long getItemId(int position) {
		if (showLocationItem && position == 0) {
			return LOCATION_ITEM_ID;
		}
		if (showRoundTripItem && position == finishPos) {
			return ROUND_TRIP_FINISH_ITEM_ID;
		}
		return getItem(position).hashCode();
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	public Object getItem(int position) {
		return items.get(position);
	}

	@Override
	public void onSwipeStarted() {

	}

	@Override
	public boolean onItemMove(int from, int to) {
		if (showLocationItem && to == 0) {
			return false;
		}
		int offset = showLocationItem ? 1 : 0;
		Collections.swap(mapActivity.getMyApplication().getMapMarkersHelper().getMapMarkers(), from - offset, to - offset);
		Collections.swap(items, from, to);
		notifyItemMoved(from, to);
		return true;
	}

	@Override
	public void onItemSwiped(RecyclerView.ViewHolder holder) {

	}

	@Override
	public void onItemDismiss(RecyclerView.ViewHolder holder) {
		inDragAndDrop = false;
		listener.onDragEnded(holder);
	}

	private LatLon getPreviousSelectedMarkerLatLon(int currentMarkerPos) {
		for (int i = currentMarkerPos - 1; i >= 0; i--) {
			Object item = items.get(i);
			if (item instanceof MapMarker) {
				MapMarker m = (MapMarker) item;
				if (m.selected) {
					return m.point;
				}
			}
		}
		return null;
	}

	public void reloadData() {
		items.clear();
		OsmandApplication app = mapActivity.getMyApplication();
		myLoc = app.getLocationProvider().getLastStaleKnownLocation();
		showLocationItem = myLoc != null;
		inRoundTrip = app.getSettings().ROUTE_MAP_MARKERS_ROUND_TRIP.get();
		if (showLocationItem) {
			lookupLocationAddress(app);
			items.add(myLoc);
		}
		items.addAll(mapActivity.getMyApplication().getMapMarkersHelper().getMapMarkers());
		calculateStartAndFinishPos();
		showRoundTripItem = inRoundTrip && !inDragAndDrop && startPos != -1;
		if (showRoundTripItem) {
			items.add(finishPos, items.get(startPos));
		}
	}

	private void lookupLocationAddress(OsmandApplication app) {
		LatLon loc = new LatLon(myLoc.getLatitude(), myLoc.getLongitude());
		if (locRequest == null || !locRequest.getLatLon().equals(loc)) {
			if (locRequest != null) {
				app.getGeocodingLookupService().cancel(locRequest);
			}
			locRequest = new AddressLookupRequest(loc, new OnAddressLookupResult() {
				@Override
				public void geocodingDone(String address) {
					locRequest = null;
					locDescription.setName(address);
					if (showLocationItem) {
						notifyItemChanged(0);
					}
				}
			}, null);
			app.getGeocodingLookupService().lookupAddress(locRequest);
		}
	}

	private void calculateStartAndFinishPos() {
		OsmandApplication app = mapActivity.getMyApplication();
		boolean startCalculated = false;
		boolean finishCalculated = false;
		boolean firstSelectedMarkerCalculated = false;
		if (app.getMapMarkersHelper().isStartFromMyLocation() && showLocationItem) {
			startPos = 0;
			startCalculated = true;
			if (inRoundTrip && !inDragAndDrop) {
				finishPos = 1;
				finishCalculated = true;
			}
		}
		for (int i = 0; i < items.size(); i++) {
			Object item = items.get(i);
			if (item instanceof MapMarker) {
				MapMarker m = (MapMarker) item;
				if (m.selected) {
					if (!startCalculated) {
						startPos = i;
						startCalculated = true;
					}
					firstSelectedMarkerPos = i;
					firstSelectedMarkerCalculated = true;
					break;
				}
			}
		}
		for (int i = items.size() - 1; i >= 0; i--) {
			Object item = items.get(i);
			if (item instanceof MapMarker) {
				MapMarker m = (MapMarker) item;
				if (m.selected) {
					finishPos = i + (inRoundTrip && !inDragAndDrop ? 1 : 0);
					finishCalculated = true;
					break;
				}
			}
		}
		if (!startCalculated) {
			startPos = -1;
		}
		if (!finishCalculated) {
			finishPos = -1;
		}
		if (!firstSelectedMarkerCalculated) {
			firstSelectedMarkerPos = -1;
		}
	}

	public interface MapMarkersListAdapterListener {

		void onDisableRoundTripClick();

		void onCheckBoxClick(View view);

		void onItemClick(View view);

		void onDragStarted(RecyclerView.ViewHolder holder);

		void onDragEnded(RecyclerView.ViewHolder holder);
	}
}

package net.osmand.plus.helpers;

import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.GeocodingLookupService;
import net.osmand.plus.IconsCache;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.controls.ListDividerShape;
import net.osmand.plus.views.controls.StableArrayAdapter;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MapMarkerDialogHelper {
	public static final int ACTIVE_MARKERS = 0;
	public static final int MARKERS_HISTORY = 1;

	private MapActivity mapActivity;
	private OsmandApplication app;
	private boolean sorted;
	private boolean nightMode;

	public MapMarkerDialogHelper(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		app = mapActivity.getMyApplication();
	}

	public boolean isNightMode() {
		return nightMode;
	}

	public void setNightMode(boolean nightMode) {
		this.nightMode = nightMode;
	}

	public boolean isSorted() {
		return sorted;
	}

	public void setSorted(boolean sorted) {
		this.sorted = sorted;
	}

	public AdapterView.OnItemClickListener getItemClickListener(final ArrayAdapter<Object> listAdapter) {
		return new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int item, long l) {
				Object obj = listAdapter.getItem(item);
				if (obj instanceof MapMarker) {
					MapMarker marker = (MapMarker) obj;
					if (!marker.history) {
						showOnMap(marker);
					} else {
						showHistoryOnMap(marker);
					}
				}
			}
		};
	}

	public StableArrayAdapter getMapMarkersListAdapter() {

		final List<Object> objects = getListObjects();
		List<Object> activeObjects = getActiveObjects(objects);

		final StableArrayAdapter listAdapter = new StableArrayAdapter(mapActivity,
				R.layout.waypoint_reached, R.id.title, objects, activeObjects) {

			@Override
			public void buildDividers() {
				dividers = getCustomDividers(getObjects());
			}

			@Override
			public boolean isEnabled(int position) {
				Object obj = getItem(position);
				return obj instanceof MapMarker;
			}

			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				// User super class to create the View
				View v = convertView;
				Object obj = getItem(position);
				boolean labelView = (obj instanceof Integer);
				boolean topDividerView = (obj instanceof Boolean) && ((Boolean) obj);
				boolean bottomDividerView = (obj instanceof Boolean) && !((Boolean) obj);
				if (labelView) {
					v = createItemForCategory(this, (Integer) obj);
					AndroidUtils.setListItemBackground(mapActivity, v, nightMode);
				} else if (topDividerView) {
					v = mapActivity.getLayoutInflater().inflate(R.layout.card_top_divider, null);
				} else if (bottomDividerView) {
					v = mapActivity.getLayoutInflater().inflate(R.layout.card_bottom_divider, null);
				} else if (obj instanceof MapMarker) {
					MapMarker marker = (MapMarker) obj;
					v = updateMapMarkerItemView(v, marker);
					AndroidUtils.setListItemBackground(mapActivity, v, nightMode);
				}
				return v;
			}
		};

		for (Object p : objects) {
			if (p instanceof MapMarker) {
				final MapMarker marker = (MapMarker) p;
				if (marker.getOriginalPointDescription() != null
						&& marker.getOriginalPointDescription().isSearchingAddress(mapActivity)) {
					GeocodingLookupService.AddressLookupRequest lookupRequest
							= new GeocodingLookupService.AddressLookupRequest(marker.point, new GeocodingLookupService.OnAddressLookupResult() {
						@Override
						public void geocodingDone(String address) {
							reloadListAdapter(listAdapter);
						}
					}, null);
					app.getGeocodingLookupService().lookupAddress(lookupRequest);
				}
			}
		}

		return listAdapter;
	}

	private List<Drawable> getCustomDividers(List<Object> points) {
		int color;
		if (nightMode) {
			color = mapActivity.getResources().getColor(R.color.dashboard_divider_dark);
		} else {
			color = mapActivity.getResources().getColor(R.color.dashboard_divider_light);
		}

		Shape fullDividerShape = new ListDividerShape(color, 0);
		Shape halfDividerShape = new ListDividerShape(color, AndroidUtils.dpToPx(mapActivity, 56f));

		final ShapeDrawable fullDivider = new ShapeDrawable(fullDividerShape);
		final ShapeDrawable halfDivider = new ShapeDrawable(halfDividerShape);

		int divHeight = AndroidUtils.dpToPx(mapActivity, 1f);
		fullDivider.setIntrinsicHeight(divHeight);
		halfDivider.setIntrinsicHeight(divHeight);

		List<Drawable> res = new ArrayList<>();
		for (int i = 0; i < points.size(); i++) {
			Object obj = points.get(i);
			Object objNext = i + 1 < points.size() ? points.get(i + 1) : null;

			if (objNext == null) {
				break;
			}

			boolean bottomDividerViewNext = (objNext instanceof Boolean) && !((Boolean) objNext);

			boolean mapMarker = (obj instanceof MapMarker);
			boolean mapMarkerNext = (objNext instanceof MapMarker);

			Drawable d = null;

			if (mapMarkerNext) {
				if (mapMarker) {
					d = halfDivider;
				} else {
					d = fullDivider;
				}
			} else if (mapMarker && !bottomDividerViewNext) {
				d = fullDivider;
			}

			res.add(d);
		}
		return res;
	}

	protected View createItemForCategory(final ArrayAdapter<Object> listAdapter, final int type) {
		View v = mapActivity.getLayoutInflater().inflate(R.layout.waypoint_header, null);
		v.findViewById(R.id.check_item).setVisibility(View.GONE);
		v.findViewById(R.id.ProgressBar).setVisibility(View.GONE);

		final Button btn = (Button) v.findViewById(R.id.header_button);
		btn.setTextColor(!nightMode ? mapActivity.getResources().getColor(R.color.map_widget_blue)
				: mapActivity.getResources().getColor(R.color.osmand_orange));
		btn.setText(mapActivity.getString(R.string.shared_string_clear));
		btn.setVisibility(View.VISIBLE);
		btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (type == MARKERS_HISTORY) {
					AlertDialog.Builder builder = new AlertDialog.Builder(mapActivity);
					builder.setMessage(mapActivity.getString(R.string.clear_markers_history_q))
							.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									listAdapter.notifyDataSetInvalidated();
									app.getMapMarkersHelper().removeMarkersHistory();
									reloadListAdapter(listAdapter);
								}
							})
							.setNegativeButton(R.string.shared_string_no, null)
							.show();
				} else if (type == ACTIVE_MARKERS) {
					AlertDialog.Builder builder = new AlertDialog.Builder(mapActivity);
					builder.setMessage(mapActivity.getString(R.string.clear_active_markers_q))
							.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									listAdapter.notifyDataSetInvalidated();
									app.getMapMarkersHelper().removeActiveMarkers();
									reloadListAdapter(listAdapter);
								}
							})
							.setNegativeButton(R.string.shared_string_no, null)
							.show();
				}
			}
		});

		TextView tv = (TextView) v.findViewById(R.id.header_text);
		AndroidUtils.setTextPrimaryColor(mapActivity, tv, nightMode);
		tv.setText(getHeader(type));
		return v;
	}

	protected View updateMapMarkerItemView(View v, final MapMarker marker) {
		if (v == null || v.findViewById(R.id.info_close) == null) {
			v = mapActivity.getLayoutInflater().inflate(R.layout.waypoint_reached, null);
		}
		updateMapMarkerInfoView(v, marker);
		final View more = v.findViewById(R.id.all_points);
		final View move = v.findViewById(R.id.info_move);
		final View remove = v.findViewById(R.id.info_close);
		remove.setVisibility(View.GONE);
		move.setVisibility(View.GONE);
		more.setVisibility(View.GONE);
		return v;
	}

	protected void updateMapMarkerInfoView(View localView, final MapMarker marker) {
		TextView text = (TextView) localView.findViewById(R.id.waypoint_text);
		TextView textShadow = (TextView) localView.findViewById(R.id.waypoint_text_shadow);
		TextView textDist = (TextView) localView.findViewById(R.id.waypoint_dist);
		if (!marker.history) {
			((ImageView) localView.findViewById(R.id.waypoint_icon))
					.setImageDrawable(getMapMarkerIcon(app, marker.colorIndex));
			AndroidUtils.setTextPrimaryColor(mapActivity, text, nightMode);
			textDist.setTextColor(mapActivity.getResources().getColor(R.color.color_myloc_distance));
		} else {
			((ImageView) localView.findViewById(R.id.waypoint_icon))
					.setImageDrawable(app.getIconsCache()
							.getContentIcon(R.drawable.ic_action_flag_dark, !nightMode));
			AndroidUtils.setTextSecondaryColor(mapActivity, text, nightMode);
			AndroidUtils.setTextSecondaryColor(mapActivity, textDist, nightMode);
		}

		int dist = marker.dist;

		//if (dist > 0) {
		textDist.setText(OsmAndFormatter.getFormattedDistance(dist, app));
		//} else {
		//	textDist.setText("");
		//}

		localView.findViewById(R.id.waypoint_deviation).setVisibility(View.GONE);

		String descr;
		PointDescription pd = marker.getPointDescription(app);
		if (Algorithms.isEmpty(pd.getName())) {
			descr = pd.getTypeName();
		} else {
			descr = pd.getName();
		}

		if (textShadow != null) {
			textShadow.setText(descr);
		}
		text.setText(descr);

		localView.findViewById(R.id.waypoint_desc_text).setVisibility(View.GONE);
		/*
		String pointDescription = "";
		TextView descText = (TextView) localView.findViewById(R.id.waypoint_desc_text);
		if (descText != null) {
			AndroidUtils.setTextSecondaryColor(this, descText, nightMode);
			pointDescription = marker.getPointDescription(this).getTypeName();
		}

		if (descr.equals(pointDescription)) {
			pointDescription = "";
		}
		if (dist > 0 && !Algorithms.isEmpty(pointDescription)) {
			pointDescription = "  •  " + pointDescription;
		}
		if (descText != null) {
			descText.setText(pointDescription);
		}
		*/
	}

	public void showOnMap(MapMarker marker) {
		app.getSettings().setMapLocationToShow(marker.getLatitude(), marker.getLongitude(),
				15, marker.getPointDescription(mapActivity), true, marker);
		MapActivity.launchMapActivityMoveToTop(mapActivity);
	}

	public void showHistoryOnMap(MapMarker marker) {
		app.getSettings().setMapLocationToShow(marker.getLatitude(), marker.getLongitude(),
				15, new PointDescription(PointDescription.POINT_TYPE_LOCATION,
						marker.getPointDescription(mapActivity).getName()),
				false, null);
		MapActivity.launchMapActivityMoveToTop(mapActivity);
	}

	protected String getHeader(int type) {
		String str = mapActivity.getString(R.string.map_markers);
		switch (type) {
			case ACTIVE_MARKERS:
				str = mapActivity.getString(R.string.active_markers);
				break;
			case MARKERS_HISTORY:
				str = mapActivity.getString(R.string.shared_string_history);
				break;
		}
		return str;
	}
	
	public void reloadListAdapter(ArrayAdapter<Object> listAdapter) {
		listAdapter.setNotifyOnChange(false);
		listAdapter.clear();
		List<Object> objects = getListObjects();
		for (Object point : objects) {
			listAdapter.add(point);
		}
		if (listAdapter instanceof StableArrayAdapter) {
			((StableArrayAdapter) listAdapter).updateObjects(objects, getActiveObjects(objects));
		}
		listAdapter.notifyDataSetChanged();
	}

	public void calcDistance(LatLon anchor, List<MapMarker> markers) {
		for (MapMarker m : markers) {
			m.dist = (int) (MapUtils.getDistance(m.getLatitude(), m.getLongitude(),
					anchor.getLatitude(), anchor.getLongitude()));
		}
	}

	protected List<Object> getListObjects() {
		final List<Object> objects = new ArrayList<>();
		final MapMarkersHelper markersHelper = app.getMapMarkersHelper();

		LatLon mapLocation =
				new LatLon(mapActivity.getMapView().getLatitude(), mapActivity.getMapView().getLongitude());

		List<MapMarker> activeMarkers = new ArrayList<>(markersHelper.getActiveMapMarkers());
		calcDistance(mapLocation, activeMarkers);
		if (sorted) {
			Collections.sort(activeMarkers, new Comparator<MapMarker>() {
				@Override
				public int compare(MapMarker lhs, MapMarker rhs) {
					return lhs.dist < rhs.dist ? -1 : (lhs.dist == rhs.dist ? 0 : 1);
				}
			});
		}
		if (activeMarkers.size() > 0) {
			objects.add(ACTIVE_MARKERS);
			objects.addAll(activeMarkers);
			objects.add(false);
		}

		List<MapMarker> markersHistory = new ArrayList<>(markersHelper.getMapMarkersHistory());
		calcDistance(mapLocation, markersHistory);
		if (markersHistory.size() > 0) {
			if (activeMarkers.size() > 0) {
				objects.add(true);
			}
			objects.add(MARKERS_HISTORY);
			objects.addAll(markersHistory);
			objects.add(false);
		}

		return objects;
	}

	private List<Object> getActiveObjects(List<Object> objects) {
		List<Object> activeObjects = new ArrayList<>();
		for (Object obj : objects) {
			if (obj instanceof MapMarker) {
				activeObjects.add(obj);
			}
		}
		return activeObjects;
	}

	public static Drawable getMapMarkerIcon(OsmandApplication app, int colorIndex) {
		IconsCache iconsCache = app.getIconsCache();
		switch (colorIndex) {
			case 0:
				return iconsCache.getIcon(R.drawable.map_marker_blue);
			case 1:
				return iconsCache.getIcon(R.drawable.map_marker_green);
			case 2:
				return iconsCache.getIcon(R.drawable.map_marker_orange);
			case 3:
				return iconsCache.getIcon(R.drawable.map_marker_red);
			case 4:
				return iconsCache.getIcon(R.drawable.map_marker_yellow);
			default:
				return iconsCache.getIcon(R.drawable.map_marker_blue);
		}
	}
}

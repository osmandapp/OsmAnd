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
import android.widget.ListView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.views.DirectionDrawable;
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
	private MapMarkersHelper markersHelper;
	private boolean sorted;
	private boolean nightMode;

	private boolean useCenter;
	private LatLon loc;
	private Float heading;
	private int screenOrientation;
	private boolean reloading;
	private long lastUpdateTime;

	public MapMarkerDialogHelper(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		app = mapActivity.getMyApplication();
		markersHelper = app.getMapMarkersHelper();
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
						showMarkerOnMap(mapActivity, marker);
					} else {
						showHistoryOnMap(marker);
					}
				}
			}
		};
	}

	public StableArrayAdapter getMapMarkersListAdapter() {

		screenOrientation = DashLocationFragment.getScreenOrientation(mapActivity);
		calculateLocationParams();

		final List<Object> objects = getListObjects();
		List<Object> activeObjects = getActiveObjects(objects);

		final StableArrayAdapter listAdapter = new StableArrayAdapter(mapActivity,
				R.layout.map_marker_item, R.id.title, objects, activeObjects) {

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
									markersHelper.removeMarkersHistory();
									if (markersHelper.getActiveMapMarkers().size() == 0) {
										mapActivity.getDashboard().hideDashboard();
									} else {
										reloadListAdapter(listAdapter);
									}
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
									markersHelper.removeActiveMarkers();
									if (markersHelper.getMapMarkersHistory().size() == 0) {
										mapActivity.getDashboard().hideDashboard();
									} else {
										reloadListAdapter(listAdapter);
									}
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
			v = mapActivity.getLayoutInflater().inflate(R.layout.map_marker_item, null);
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
		ImageView arrow = (ImageView) localView.findViewById(R.id.direction);
		ImageView waypointIcon = (ImageView) localView.findViewById(R.id.waypoint_icon);
		TextView waypointDeviation = (TextView) localView.findViewById(R.id.waypoint_deviation);
		TextView descText = (TextView) localView.findViewById(R.id.waypoint_desc_text);
		if (text == null || textDist == null || arrow == null || waypointIcon == null
				|| waypointDeviation == null || descText == null) {
			return;
		}
		float[] mes = new float[2];
		if (loc != null && marker.point != null) {
			Location.distanceBetween(marker.getLatitude(), marker.getLongitude(), loc.getLatitude(), loc.getLongitude(), mes);
		}
		boolean newImage = false;
		int arrowResId = R.drawable.ic_destination_arrow_white;
		DirectionDrawable dd;
		if (!(arrow.getDrawable() instanceof DirectionDrawable)) {
			newImage = true;
			dd = new DirectionDrawable(mapActivity, arrow.getWidth(), arrow.getHeight());
		} else {
			dd = (DirectionDrawable) arrow.getDrawable();
		}
		if (!marker.history) {
			dd.setImage(arrowResId, useCenter ? R.color.color_distance : R.color.color_myloc_distance);
		} else {
			dd.setImage(arrowResId, nightMode ? R.color.secondary_text_dark : R.color.secondary_text_light);
		}
		if (loc == null || heading == null || marker.point == null) {
			dd.setAngle(0);
		} else {
			dd.setAngle(mes[1] - heading + 180 + screenOrientation);
		}
		if (newImage) {
			arrow.setImageDrawable(dd);
		}
		arrow.setVisibility(View.VISIBLE);
		arrow.invalidate();

		if (!marker.history) {
			waypointIcon.setImageDrawable(getMapMarkerIcon(app, marker.colorIndex));
			AndroidUtils.setTextPrimaryColor(mapActivity, text, nightMode);
			textDist.setTextColor(mapActivity.getResources()
					.getColor(useCenter ? R.color.color_distance : R.color.color_myloc_distance));
		} else {
			waypointIcon.setImageDrawable(app.getIconsCache()
					.getContentIcon(R.drawable.ic_action_flag_dark, !nightMode));
			AndroidUtils.setTextSecondaryColor(mapActivity, text, nightMode);
			AndroidUtils.setTextSecondaryColor(mapActivity, textDist, nightMode);
		}

		int dist = (int) mes[0];
		textDist.setText(OsmAndFormatter.getFormattedDistance(dist, app));

		waypointDeviation.setVisibility(View.GONE);

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

		descText.setVisibility(View.GONE);
		/*
		String pointDescription = "";
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

	protected void updateMapMarkerArrowDistanceView(View localView, final MapMarker marker) {
		TextView textDist = (TextView) localView.findViewById(R.id.waypoint_dist);
		ImageView arrow = (ImageView) localView.findViewById(R.id.direction);
		if (textDist == null || arrow == null) {
			return;
		}
		float[] mes = new float[2];
		if (loc != null && marker.point != null) {
			Location.distanceBetween(marker.getLatitude(), marker.getLongitude(), loc.getLatitude(), loc.getLongitude(), mes);
		}
		boolean newImage = false;
		int arrowResId = R.drawable.ic_destination_arrow_white;
		DirectionDrawable dd;
		if (!(arrow.getDrawable() instanceof DirectionDrawable)) {
			newImage = true;
			dd = new DirectionDrawable(mapActivity, arrow.getWidth(), arrow.getHeight());
		} else {
			dd = (DirectionDrawable) arrow.getDrawable();
		}
		if (!marker.history) {
			dd.setImage(arrowResId, useCenter ? R.color.color_distance : R.color.color_myloc_distance);
		} else {
			dd.setImage(arrowResId, nightMode ? R.color.secondary_text_dark : R.color.secondary_text_light);
		}
		if (loc == null || heading == null || marker.point == null) {
			dd.setAngle(0);
		} else {
			dd.setAngle(mes[1] - heading + 180 + screenOrientation);
		}
		if (newImage) {
			arrow.setImageDrawable(dd);
		}
		arrow.invalidate();

		int dist = (int) mes[0];
		textDist.setText(OsmAndFormatter.getFormattedDistance(dist, app));
	}

	public static void showMarkerOnMap(MapActivity mapActivity, MapMarker marker) {
		mapActivity.getMyApplication().getSettings().setMapLocationToShow(marker.getLatitude(), marker.getLongitude(),
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
		reloading = true;
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
		reloading = false;
	}

	public void calcDistance(LatLon anchor, List<MapMarker> markers) {
		for (MapMarker m : markers) {
			m.dist = (int) (MapUtils.getDistance(m.getLatitude(), m.getLongitude(),
					anchor.getLatitude(), anchor.getLongitude()));
		}
	}

	protected List<Object> getListObjects() {
		final List<Object> objects = new ArrayList<>();

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
		return app.getIconsCache().getIcon(R.drawable.ic_action_flag_dark, getMapMarkerColorId(colorIndex));
	}

	public static int getMapMarkerColorId(int colorIndex) {
		int colorId;
		switch (colorIndex) {
			case 0:
				colorId = R.color.marker_blue;
				break;
			case 1:
				colorId = R.color.marker_green;
				break;
			case 2:
				colorId = R.color.marker_orange;
				break;
			case 3:
				colorId = R.color.marker_red;
				break;
			case 4:
				colorId = R.color.marker_yellow;
				break;
			case 5:
				colorId = R.color.marker_teal;
				break;
			case 6:
				colorId = R.color.marker_purple;
				break;
			default:
				colorId = R.color.marker_blue;
		}
		return colorId;
	}

	public void updateLocation(ListView listView, boolean compassChanged) {
		if ((compassChanged && !mapActivity.getDashboard().isMapLinkedToLocation())
				|| reloading || System.currentTimeMillis() - lastUpdateTime < 100) {
			return;
		}

		lastUpdateTime = System.currentTimeMillis();

		try {
			calculateLocationParams();

			for (int i = listView.getFirstVisiblePosition(); i <= listView.getLastVisiblePosition(); i++) {
				Object obj = listView.getItemAtPosition(i);
				View v = listView.getChildAt(i - listView.getFirstVisiblePosition());
				if (obj instanceof MapMarker && v != null) {
					updateMapMarkerArrowDistanceView(v, (MapMarker) obj);
				}
			}
		} catch (Exception e) {
		}
	}

	public void updateMarkerView(ListView listView, MapMarker marker) {
		try {
			for (int i = listView.getFirstVisiblePosition(); i <= listView.getLastVisiblePosition(); i++) {
				Object obj = listView.getItemAtPosition(i);
				View v = listView.getChildAt(i - listView.getFirstVisiblePosition());
				if (obj == marker) {
					updateMapMarkerInfoView(v, (MapMarker) obj);
				}
			}
		} catch (Exception e) {
		}
	}

	private void calculateLocationParams() {
		DashboardOnMap d = mapActivity.getDashboard();
		if (d == null) {
			return;
		}

		float head = d.getHeading();
		float mapRotation = d.getMapRotation();
		LatLon mw = d.getMapViewLocation();
		Location l = d.getMyLocation();
		boolean mapLinked = d.isMapLinkedToLocation() && l != null;
		LatLon myLoc = l == null ? null : new LatLon(l.getLatitude(), l.getLongitude());
		useCenter = !mapLinked;
		loc = (useCenter ? mw : myLoc);
		heading = useCenter ? -mapRotation : head;
	}
}

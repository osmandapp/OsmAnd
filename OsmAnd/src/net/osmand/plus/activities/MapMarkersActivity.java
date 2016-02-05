package net.osmand.plus.activities;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
import net.osmand.plus.views.controls.ListDividerShape;
import net.osmand.plus.views.controls.StableArrayAdapter;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public class MapMarkersActivity extends OsmandListActivity {

	public static final int ACTIVE_MARKERS = 0;
	public static final int MARKERS_HISTORY = 1;

	private boolean nightMode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getMyApplication().applyTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map_markers);
		getSupportActionBar().setTitle(R.string.map_markers);

		//nightMode = getMyApplication().getDaynightHelper().isNightModeForMapControls();
		nightMode = !getMyApplication().getSettings().isLightContent();
		setListAdapter(getMapMarkersListAdapter());
	}

	@Override
	public StableArrayAdapter getListAdapter() {
		return (StableArrayAdapter) super.getListAdapter();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		getListAdapter().getItem(position);
		//
	}

	@Override
	protected void onResume() {
		super.onResume();
		getListAdapter().notifyDataSetChanged();
	}

	public StableArrayAdapter getMapMarkersListAdapter() {

		final List<Object> objects = getListObjects();
		List<Object> activeObjects = getActiveObjects(objects);

		final StableArrayAdapter listAdapter = new StableArrayAdapter(getMyApplication(),
				R.layout.waypoint_reached, R.id.title, objects, activeObjects) {

			@Override
			public void buildDividers() {
				dividers = getCustomDividers(getObjects());
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
					v = createItemForCategory((Integer) obj);
					AndroidUtils.setListItemBackground(MapMarkersActivity.this, v, nightMode);
				} else if (topDividerView) {
					v = getLayoutInflater().inflate(R.layout.card_top_divider, null);
				} else if (bottomDividerView) {
					v = getLayoutInflater().inflate(R.layout.card_bottom_divider, null);
				} else if (obj instanceof MapMarker) {
					MapMarker marker = (MapMarker) obj;
					v = updateMapMarkerItemView(v, marker);
					AndroidUtils.setListItemBackground(MapMarkersActivity.this, v, nightMode);
				}
				return v;
			}
		};

		for (Object p : objects) {
			if (p instanceof MapMarker) {
				final MapMarker marker = (MapMarker) p;
				if (marker.getOriginalPointDescription() != null
						&& marker.getOriginalPointDescription().isSearchingAddress(this)) {
					GeocodingLookupService.AddressLookupRequest lookupRequest
							= new GeocodingLookupService.AddressLookupRequest(marker.point, new GeocodingLookupService.OnAddressLookupResult() {
						@Override
						public void geocodingDone(String address) {
							reloadListAdapter();
						}
					}, null);
					getMyApplication().getGeocodingLookupService().lookupAddress(lookupRequest);
				}
			}
		}

		return listAdapter;
	}

	private List<Drawable> getCustomDividers(List<Object> points) {
		int color;
		if (nightMode) {
			color = getResources().getColor(R.color.dashboard_divider_dark);
		} else {
			color = getResources().getColor(R.color.dashboard_divider_light);
		}

		Shape fullDividerShape = new ListDividerShape(color, 0);
		Shape halfDividerShape = new ListDividerShape(color, AndroidUtils.dpToPx(this, 56f));

		final ShapeDrawable fullDivider = new ShapeDrawable(fullDividerShape);
		final ShapeDrawable halfDivider = new ShapeDrawable(halfDividerShape);

		int divHeight = AndroidUtils.dpToPx(this, 1f);
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

	protected View createItemForCategory(final int type) {
		View v = getLayoutInflater().inflate(R.layout.waypoint_header, null);
		v.findViewById(R.id.check_item).setVisibility(View.GONE);
		v.findViewById(R.id.ProgressBar).setVisibility(View.GONE);

		final Button btn = (Button) v.findViewById(R.id.header_button);
		btn.setTextColor(!nightMode ? getResources().getColor(R.color.map_widget_blue)
				: getResources().getColor(R.color.osmand_orange));
		btn.setText(getString(R.string.shared_string_clear));
		btn.setVisibility(View.VISIBLE);
		btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getListAdapter().notifyDataSetInvalidated();
				reloadListAdapter();
			}
		});

		TextView tv = (TextView) v.findViewById(R.id.header_text);
		AndroidUtils.setTextPrimaryColor(this, tv, nightMode);
		tv.setText(getHeader(type));
		return v;
	}

	protected View updateMapMarkerItemView(View v, final MapMarker marker) {
		if (v == null || v.findViewById(R.id.info_close) == null) {
			v = getLayoutInflater().inflate(R.layout.waypoint_reached, null);
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
		OsmandApplication app = getMyApplication();
		TextView text = (TextView) localView.findViewById(R.id.waypoint_text);
		AndroidUtils.setTextPrimaryColor(this, text, nightMode);
		TextView textShadow = (TextView) localView.findViewById(R.id.waypoint_text_shadow);
		localView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showOnMap(marker);
			}
		});

		TextView textDist = (TextView) localView.findViewById(R.id.waypoint_dist);
		((ImageView) localView.findViewById(R.id.waypoint_icon))
				.setImageDrawable(getMapMarkerIcon(marker.colorIndex));

		LatLon lastKnownMapLocation = app.getSettings().getLastKnownMapLocation();
		int dist = (int) (MapUtils.getDistance(marker.getLatitude(), marker.getLongitude(),
				lastKnownMapLocation.getLatitude(), lastKnownMapLocation.getLongitude()));

		if (dist > 0) {
			textDist.setText(OsmAndFormatter.getFormattedDistance(dist, app));
		} else {
			textDist.setText("");
		}

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
	}

	public void showOnMap(MapMarker marker) {
		getMyApplication().getSettings().setMapLocationToShow(marker.getLatitude(), marker.getLongitude(),
				15, marker.getPointDescription(this), true, marker);
		MapActivity.launchMapActivityMoveToTop(this);
	}

	protected String getHeader(int type) {
		String str = getString(R.string.map_markers);
		switch (type) {
			case ACTIVE_MARKERS:
				str = getString(R.string.active_markers);
				break;
			case MARKERS_HISTORY:
				str = getString(R.string.shared_string_history);
				break;
		}
		return str;
	}

	public void reloadListAdapter() {
		StableArrayAdapter listAdapter = getListAdapter();
		listAdapter.setNotifyOnChange(false);
		listAdapter.clear();
		List<Object> objects = getListObjects();
		for (Object point : objects) {
			listAdapter.add(point);
		}
		listAdapter.updateObjects(objects, getActiveObjects(objects));
		listAdapter.notifyDataSetChanged();
	}

	protected List<Object> getListObjects() {
		final List<Object> objects = new ArrayList<>();
		final MapMarkersHelper markersHelper = getMyApplication().getMapMarkersHelper();

		List<MapMarker> activeMarkers = markersHelper.getActiveMapMarkers();
		if (activeMarkers.size() > 0) {
			objects.add(true);
			objects.add(ACTIVE_MARKERS);
			objects.addAll(activeMarkers);
			objects.add(false);
		}

		List<MapMarker> markersHistory = markersHelper.getMapMarkersHistory();
		if (markersHistory.size() > 0) {
			objects.add(true);
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

	private Drawable getMapMarkerIcon(int colorIndex) {
		IconsCache iconsCache = getMyApplication().getIconsCache();
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
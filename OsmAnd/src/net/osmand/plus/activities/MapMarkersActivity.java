package net.osmand.plus.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.GeocodingLookupService;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.R;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.views.controls.StableArrayAdapter;

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

		nightMode = getMyApplication().getDaynightHelper().isNightModeForMapControls();
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
				dividers = getCustomDividers(ctx, getObjects(), nightMode);
			}

			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				// User super class to create the View
				View v = convertView;
				final ArrayAdapter<Object> thisAdapter = this;
				Object obj = getItem(position);
				boolean labelView = (obj instanceof Integer);
				boolean topDividerView = (obj instanceof Boolean) && ((Boolean) obj);
				boolean bottomDividerView = (obj instanceof Boolean) && !((Boolean) obj);
				if (labelView) {
					v = createItemForCategory(ctx, (Integer) obj, running, position, thisAdapter, nightMode);
					AndroidUtils.setListItemBackground(MapMarkersActivity.this, v, nightMode);
				} else if (topDividerView) {
					v = ctx.getLayoutInflater().inflate(R.layout.card_top_divider, null);
				} else if (bottomDividerView) {
					v = ctx.getLayoutInflater().inflate(R.layout.card_bottom_divider, null);
				} else if (obj instanceof MapMarker) {
					MapMarker marker = (MapMarker) obj;
					v = updateWaypointItemView(edit, deletedPoints, app, ctx, helper, v, marker, this,
							nightMode, flat);
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
							if (helperCallbacks != null) {
								helperCallbacks.reloadAdapter();
							} else {
								reloadListAdapter(listAdapter);
							}
						}
					}, null);
					getMyApplication().getGeocodingLookupService().lookupAddress(lookupRequest);
				}
			}
		}

		return listAdapter;
	}

	protected View createItemForCategory(final int type, final int position, final ArrayAdapter<Object> thisAdapter, boolean nightMode) {
		View v = getLayoutInflater().inflate(R.layout.waypoint_header, null);
		final CompoundButton btn = (CompoundButton) v.findViewById(R.id.check_item);
		btn.setVisibility(waypointHelper.isTypeConfigurable(type) ? View.VISIBLE : View.GONE);
		btn.setOnCheckedChangeListener(null);
		final boolean checked = waypointHelper.isTypeEnabled(type);
		btn.setChecked(checked);
		btn.setEnabled(running[0] == -1);
		v.findViewById(R.id.ProgressBar).setVisibility(position == running[0] ? View.VISIBLE : View.GONE);
		btn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				running[0] = position;
				thisAdapter.notifyDataSetInvalidated();
				if (type == WaypointHelper.POI && isChecked) {
					selectPoi(running, thisAdapter, type, isChecked, ctx);
				} else {
					enableType(running, thisAdapter, type, isChecked);
				}
			}

		});

		TextView tv = (TextView) v.findViewById(R.id.header_text);
		AndroidUtils.setTextPrimaryColor(mapActivity, tv, nightMode);
		tv.setText(getHeader(type, checked, ctx));
		return v;
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


	//LatLon lastKnownMapLocation = getMyApplication().getSettings().getLastKnownMapLocation();
	//int dist = (int) (MapUtils.getDistance(marker.getLatitude(), marker.getLongitude(),
	//lastKnownMapLocation.getLatitude(), lastKnownMapLocation.getLongitude()));

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
}
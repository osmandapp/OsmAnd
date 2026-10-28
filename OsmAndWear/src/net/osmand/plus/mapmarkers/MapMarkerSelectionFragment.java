package net.osmand.plus.mapmarkers;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.annotation.Nullable;

import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.helpers.MapMarkerDialogHelper;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu.OnMarkerSelectListener;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu.PointType;

import java.util.List;

public class MapMarkerSelectionFragment extends BaseOsmAndDialogFragment {

	public static final String TAG = "MapMarkerSelectionFragment";
	private static final String POINT_TYPE_KEY = "point_type";

	private LatLon loc;
	private Float heading;
	private boolean useCenter;
	private boolean nightMode;
	private PointType pointType;

	private OnMarkerSelectListener onClickListener;
	private int screenOrientation;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		Bundle bundle = null;
		if (getArguments() != null) {
			bundle = getArguments();
		} else if (savedInstanceState != null) {
			bundle = savedInstanceState;
		}
		if (bundle != null) {
			pointType = PointType.valueOf(bundle.getString(POINT_TYPE_KEY));
		}

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			MapRouteInfoMenu routeInfoMenu = mapActivity.getMapRouteInfoMenu();
			onClickListener = routeInfoMenu.getOnMarkerSelectListener();

			screenOrientation = AndroidUiHelper.getScreenOrientation(mapActivity);

			MapViewTrackingUtilities trackingUtils = mapActivity.getMapViewTrackingUtilities();
			if (trackingUtils != null) {
				Float head = trackingUtils.getHeading();
				float mapRotation = mapActivity.getMapRotate();
				LatLon mw = mapActivity.getMapLocation();
				boolean mapLinked = trackingUtils.isMapLinkedToLocation();
				useCenter = !mapLinked;
				loc = mw;
				if (useCenter) {
					heading = -mapRotation;
				} else {
					heading = head;
				}
			}
		}

		View view = themedInflater.inflate(R.layout.map_marker_selection_fragment, container, false);
		ImageButton closeButton = view.findViewById(R.id.closeButton);
		Drawable icBack = app.getUIUtilities().getIcon(AndroidUtils.getNavigationIconResId(app));
		closeButton.setImageDrawable(icBack);
		closeButton.setOnClickListener(v -> dismiss());

		ListView listView = view.findViewById(android.R.id.list);
		ArrayAdapter<MapMarker> adapter = new MapMarkersListAdapter();
		List<MapMarker> markers = app.getMapMarkersHelper().getMapMarkers();
		if (markers.size() > 0) {
			for (MapMarker marker : markers) {
				adapter.add(marker);
			}
		}
		listView.setAdapter(adapter);
		listView.setOnItemClickListener((parent, v, position, id) -> {
			if (onClickListener != null) {
				onClickListener.onSelect(position, pointType);
			}
			dismiss();
		});
		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(POINT_TYPE_KEY, pointType.name());
	}

	private class MapMarkersListAdapter extends ArrayAdapter<MapMarker> {

		public MapMarkersListAdapter() {
			super(getMapActivity(), R.layout.map_marker_item);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			MapMarker marker = getItem(position);
			if (convertView == null) {
				convertView = getMapActivity().getLayoutInflater().inflate(R.layout.map_marker_item, null);
			}
			MapMarkerDialogHelper.updateMapMarkerInfo(getContext(), convertView, loc, heading,
					useCenter, nightMode, screenOrientation, marker);
			View remove = convertView.findViewById(R.id.info_close);
			remove.setVisibility(View.GONE);
			AndroidUtils.setListItemBackground(getMapActivity(), convertView, nightMode);

			return convertView;
		}
	}

	public static MapMarkerSelectionFragment newInstance(PointType pointType) {
		MapMarkerSelectionFragment fragment = new MapMarkerSelectionFragment();
		Bundle args = new Bundle();
		args.putString(POINT_TYPE_KEY, pointType.name());
		fragment.setArguments(args);
		return fragment;
	}

	public MapActivity getMapActivity() {
		Context ctx = getContext();
		if (ctx instanceof MapActivity) {
			return (MapActivity) ctx;
		} else {
			return null;
		}
	}
}

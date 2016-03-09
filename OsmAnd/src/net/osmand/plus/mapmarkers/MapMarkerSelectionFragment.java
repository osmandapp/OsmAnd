package net.osmand.plus.mapmarkers;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.plus.helpers.MapMarkerDialogHelper;
import net.osmand.plus.mapcontextmenu.other.MapRouteInfoMenu;
import net.osmand.plus.mapcontextmenu.other.MapRouteInfoMenu.OnMarkerSelectListener;

import java.util.List;

public class MapMarkerSelectionFragment extends BaseOsmAndDialogFragment {
	public static final String TAG = "MapMarkerSelectionFragment";
	private static final String TARGET_KEY = "target_key";

	private LatLon loc;
	private Float heading;
	private boolean useCenter;
	private boolean nightMode;
	private int screenOrientation;
	private boolean target;

	private OnMarkerSelectListener onClickListener;
	private OnDismissListener onDismissListener;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		Bundle bundle = null;
		if (getArguments() != null) {
			bundle = getArguments();
		} else if (savedInstanceState != null) {
			bundle = savedInstanceState;
		}
		if (bundle != null) {
			target = bundle.getBoolean(TARGET_KEY);
		}

		MapActivity mapActivity = getMapActivity();
		OsmandApplication app = getMyApplication();
		if (mapActivity != null) {
			MapRouteInfoMenu routeInfoMenu = mapActivity.getMapLayers().getMapControlsLayer().getMapRouteInfoMenu();
			onClickListener = routeInfoMenu.getOnMarkerSelectListener();
			onDismissListener = routeInfoMenu.getOnDismissDialogListener();

			screenOrientation = DashLocationFragment.getScreenOrientation(mapActivity);

			MapViewTrackingUtilities trackingUtils = mapActivity.getMapViewTrackingUtilities();
			if (trackingUtils != null) {
				Float head = trackingUtils.getHeading();
				float mapRotation = mapActivity.getMapRotate();
				LatLon mw = mapActivity.getMapLocation();
				Location l = trackingUtils.getMyLocation();
				boolean mapLinked = trackingUtils.isMapLinkedToLocation() && l != null;
				LatLon myLoc = l == null ? null : new LatLon(l.getLatitude(), l.getLongitude());
				useCenter = !mapLinked;
				loc = (useCenter ? mw : myLoc);
				heading = useCenter ? -mapRotation : head;
			}
		}
		nightMode = !app.getSettings().isLightContent();

		View view = inflater.inflate(R.layout.map_marker_selection_fragment, container, false);
		ImageButton closeButton = (ImageButton) view.findViewById(R.id.closeButton);
		closeButton.setImageDrawable(getMyApplication().getIconsCache().getIcon(R.drawable.ic_action_mode_back));
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		ListView listView = (ListView) view.findViewById(android.R.id.list);
		final ArrayAdapter<MapMarker> adapter = new MapMarkersListAdapter();
		List<MapMarker> markers = getMyApplication().getMapMarkersHelper().getActiveMapMarkers();
		if (markers.size() > 0) {
			for (MapMarker marker : markers) {
				adapter.add(marker);
			}
		}
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (onClickListener != null) {
					onClickListener.onSelect(position, target);
				}
				dismiss();
			}
		});
		return view;
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		if (onDismissListener != null) {
			onDismissListener.onDismiss(dialog);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(TARGET_KEY, target);
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
					useCenter, nightMode, screenOrientation, false, null, marker);
			final View remove = convertView.findViewById(R.id.info_close);
			remove.setVisibility(View.GONE);
			AndroidUtils.setListItemBackground(getMapActivity(), convertView, nightMode);

			return convertView;
		}
	}

	public static MapMarkerSelectionFragment newInstance(boolean target) {
		MapMarkerSelectionFragment fragment = new MapMarkerSelectionFragment();
		Bundle args = new Bundle();
		args.putBoolean(TARGET_KEY, target);
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

	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getContext().getApplicationContext();
	}
}

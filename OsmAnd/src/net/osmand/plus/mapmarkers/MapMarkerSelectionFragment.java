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
import android.widget.AdapterView.OnItemClickListener;
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

import java.util.List;

public class MapMarkerSelectionFragment extends BaseOsmAndDialogFragment {
	public static final String TAG = "MapMarkerSelectionFragment";

	private LatLon loc;
	private Float heading;
	private boolean useCenter;
	private boolean nightMode;
	private int screenOrientation;

	private OnItemClickListener clickListener;
	private OnDismissListener dismissListener;

	public void setClickListener(OnItemClickListener clickListener) {
		this.clickListener = clickListener;
	}

	public void setDismissListener(OnDismissListener dismissListener) {
		this.dismissListener = dismissListener;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		MapActivity mapActivity = getMapActivity();
		OsmandApplication app = getMyApplication();
		if (mapActivity != null) {
			screenOrientation = DashLocationFragment.getScreenOrientation(mapActivity);

			MapViewTrackingUtilities trackingUtils = mapActivity.getMapViewTrackingUtilities();
			float head = trackingUtils.getHeading();
			float mapRotation = mapActivity.getMapRotate();
			LatLon mw = mapActivity.getMapLocation();
			Location l = trackingUtils.getMyLocation();
			boolean mapLinked = trackingUtils.isMapLinkedToLocation() && l != null;
			LatLon myLoc = l == null ? null : new LatLon(l.getLatitude(), l.getLongitude());
			useCenter = !mapLinked;
			loc = (useCenter ? mw : myLoc);
			heading = useCenter ? -mapRotation : head;
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
			adapter.addAll(markers);
		}
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (clickListener != null) {
					clickListener.onItemClick(parent, view, position, id);
				}
				dismiss();
			}
		});
		return view;
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		if (dismissListener != null) {
			dismissListener.onDismiss(dialog);
		}
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
			MapMarkerDialogHelper.updateMapMarkerInfoView(getContext(), convertView, loc, heading,
					useCenter, nightMode, screenOrientation, marker);
			final View remove = convertView.findViewById(R.id.info_close);
			remove.setVisibility(View.GONE);
			AndroidUtils.setListItemBackground(getMapActivity(), convertView, nightMode);

			return convertView;
		}
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

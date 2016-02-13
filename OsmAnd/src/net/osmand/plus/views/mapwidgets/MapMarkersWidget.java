package net.osmand.plus.views.mapwidgets;

import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.Location;
import net.osmand.data.PointDescription;
import net.osmand.plus.IconsCache;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.MapMarkerDialogHelper;
import net.osmand.plus.views.DirectionDrawable;
import net.osmand.util.Algorithms;

import java.util.List;

public class MapMarkersWidget {

	public static final int MIN_DIST_OK_VISIBLE = 50;

	private final MapActivity map;
	private MapMarkersHelper helper;
	private int screenOrientation;
	private boolean portraitMode;
	private boolean largeDevice;

	private View topBar;
	private View topBar2nd;
	private View rowView;
	private View rowView2nd;
	private ImageView arrowImg;
	private ImageView arrowImg2nd;
	private TextView distText;
	private TextView distText2nd;
	private TextView addressText;
	private TextView addressText2nd;
	private ImageButton okButton;
	private ImageButton okButton2nd;
	private ImageButton moreButton;
	private ImageButton moreButton2nd;

	public MapMarkersWidget(final MapActivity map) {
		this.map = map;
		helper = map.getMyApplication().getMapMarkersHelper();
		screenOrientation = DashLocationFragment.getScreenOrientation(map);
		portraitMode = AndroidUiHelper.isOrientationPortrait(map);
		largeDevice = AndroidUiHelper.isXLargeDevice(map);

		topBar = map.findViewById(R.id.map_markers_top_bar);
		topBar2nd = map.findViewById(R.id.map_markers_top_bar_2nd);
		rowView = map.findViewById(R.id.map_marker_row);
		rowView2nd = map.findViewById(R.id.map_marker_row_2nd);
		arrowImg = (ImageView) map.findViewById(R.id.map_marker_arrow);
		arrowImg2nd = (ImageView) map.findViewById(R.id.map_marker_arrow_2nd);
		distText = (TextView) map.findViewById(R.id.map_marker_dist);
		distText2nd = (TextView) map.findViewById(R.id.map_marker_dist_2nd);
		addressText = (TextView) map.findViewById(R.id.map_marker_address);
		addressText2nd = (TextView) map.findViewById(R.id.map_marker_address_2nd);
		okButton = (ImageButton) map.findViewById(R.id.marker_btn_ok);
		okButton2nd = (ImageButton) map.findViewById(R.id.marker_btn_ok_2dn);
		moreButton = (ImageButton) map.findViewById(R.id.marker_btn_more);
		moreButton2nd = (ImageButton) map.findViewById(R.id.marker_btn_more_2nd);

		rowView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showMarkerOnMap(0);
			}
		});
		rowView2nd.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showMarkerOnMap(1);
			}
		});

		IconsCache iconsCache = map.getMyApplication().getIconsCache();
		if (isLandscapeLayout()) {
			moreButton.setVisibility(View.GONE);
		} else {
			moreButton.setImageDrawable(iconsCache.getIcon(R.drawable.ic_overflow_menu_white, R.color.marker_top_2nd_line_color));
			moreButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					MapActivity.clearPrevActivityIntent();
					map.getDashboard().setDashboardVisibility(true, DashboardOnMap.DashboardType.MAP_MARKERS);
				}
			});
		}
		if (moreButton2nd != null) {
			moreButton2nd.setImageDrawable(iconsCache.getIcon(R.drawable.ic_overflow_menu_white, R.color.marker_top_2nd_line_color));
			moreButton2nd.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					MapActivity.clearPrevActivityIntent();
					map.getDashboard().setDashboardVisibility(true, DashboardOnMap.DashboardType.MAP_MARKERS);
				}
			});
		}
		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				removeMarker(0);
			}
		});
		okButton2nd.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				removeMarker(1);
			}
		});

		updateVisibility(false);
	}

	private void removeMarker(int index) {
		if (helper.getActiveMapMarkers().size() > index) {
			MapMarker marker = helper.getActiveMapMarkers().get(index);
			helper.removeMapMarker(marker.index);
			helper.addMapMarkerHistory(marker);
		}
	}

	private void showMarkerOnMap(int index) {
		if (helper.getActiveMapMarkers().size() > index) {
			MapMarker marker = helper.getActiveMapMarkers().get(index);
			MapMarkerDialogHelper.showMarkerOnMap(map, marker);
		}
	}

	public boolean updateVisibility(boolean visible) {
		return updateVisibility(topBar, visible);
	}

	public boolean updateVisibility(View v, boolean visible) {
		if (visible != (v.getVisibility() == View.VISIBLE)) {
			if (visible) {
				v.setVisibility(View.VISIBLE);
			} else {
				v.setVisibility(View.GONE);
			}
			v.invalidate();
			return true;
		}
		return false;
	}

	public void updateInfo(int zoom) {
		List<MapMarker> markers = helper.getActiveMapMarkers();
		if (zoom < 3 || markers.size() == 0 || map.getMyApplication().getRoutingHelper().isFollowingMode()
				|| map.getMapLayers().getMapControlsLayer().getMapRouteInfoMenu().isVisible()) {
			updateVisibility(false);
			return;
		}

		Location loc = map.getMapViewTrackingUtilities().getMyLocation();
		Float heading = map.getMapViewTrackingUtilities().getHeading();

		MapMarker marker = markers.get(0);
		updateUI(loc, heading, marker, arrowImg, distText, okButton, addressText, true);

		if (markers.size() > 1) {
			marker = markers.get(1);
			updateUI(loc, heading, marker, arrowImg2nd, distText2nd, okButton2nd, addressText2nd, false);
			updateVisibility(topBar2nd, true);
		} else {
			updateVisibility(topBar2nd, false);
		}

		updateVisibility(true);
	}

	private void updateUI(Location loc, Float heading, MapMarker marker, ImageView arrowImg,
						  TextView distText, ImageButton okButton, TextView addressText, boolean firstLine) {
		float[] mes = new float[2];
		if (loc != null && marker.point != null) {
			Location.distanceBetween(marker.getLatitude(), marker.getLongitude(), loc.getLatitude(), loc.getLongitude(), mes);
		}

		boolean newImage = false;
		DirectionDrawable dd;
		if (!(arrowImg.getDrawable() instanceof DirectionDrawable)) {
			newImage = true;
			dd = new DirectionDrawable(map, arrowImg.getWidth(), arrowImg.getHeight());
		} else {
			dd = (DirectionDrawable) arrowImg.getDrawable();
		}
		dd.setImage(R.drawable.map_arrow_to_destination, MapMarkerDialogHelper.getMapMarkerColorId(marker.colorIndex));
		if (loc == null || heading == null || marker.point == null) {
			dd.setAngle(0);
		} else {
			dd.setAngle(mes[1] - heading + 90 + screenOrientation);
		}
		if (newImage) {
			arrowImg.setImageDrawable(dd);
		}
		arrowImg.invalidate();

		int dist = (int) mes[0];
		distText.setText(OsmAndFormatter.getFormattedDistance(dist, map.getMyApplication()));
		updateVisibility(okButton, dist < MIN_DIST_OK_VISIBLE);

		String descr;
		PointDescription pd = marker.getPointDescription(map);
		if (Algorithms.isEmpty(pd.getName())) {
			descr = pd.getTypeName();
		} else {
			descr = pd.getName();
		}
		if (!firstLine && !isLandscapeLayout()) {
			descr = "  •  " + descr;
		}

		addressText.setText(descr);
	}

	public boolean isLandscapeLayout() {
		return !portraitMode && !largeDevice;
	}

}

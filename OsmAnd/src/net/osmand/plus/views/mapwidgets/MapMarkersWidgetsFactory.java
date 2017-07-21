package net.osmand.plus.views.mapwidgets;

import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.Location;
import net.osmand.data.LatLon;
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
import net.osmand.plus.mapcontextmenu.other.MapRouteInfoMenu;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.DirectionDrawable;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.List;

public class MapMarkersWidgetsFactory {

	public static final int MIN_DIST_OK_VISIBLE = 40; // meters
	public static final int MIN_DIST_2ND_ROW_SHOW = 150; // meters

	private final MapActivity map;
	private MapMarkersHelper helper;
	private int screenOrientation;
	private boolean portraitMode;

	private View topBar;
	private View addressTopBar;
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

	private LatLon loc;

	public MapMarkersWidgetsFactory(final MapActivity map) {
		this.map = map;
		helper = map.getMyApplication().getMapMarkersHelper();
		screenOrientation = DashLocationFragment.getScreenOrientation(map);
		portraitMode = AndroidUiHelper.isOrientationPortrait(map);

		addressTopBar = map.findViewById(R.id.map_top_bar);
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
		if (isLandscapeLayout() && helper.getMapMarkers().size() > 1) {
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
		if (helper.getMapMarkers().size() > index) {
			MapMarker marker = helper.getMapMarkers().get(index);
			helper.removeMapMarker(marker.index);
			helper.addMapMarkerHistory(marker);
		}
	}

	private void showMarkerOnMap(int index) {
		if (helper.getMapMarkers().size() > index) {
			MapMarker marker = helper.getMapMarkers().get(index);
			AnimateDraggingMapThread thread = map.getMapView().getAnimatedDraggingThread();
			LatLon pointToNavigate = marker.point;
			if (pointToNavigate != null) {
				int fZoom = map.getMapView().getZoom() < 15 ? 15 : map.getMapView().getZoom();
				thread.startMoving(pointToNavigate.getLatitude(), pointToNavigate.getLongitude(), fZoom, true);
			}
			//MapMarkerDialogHelper.showMarkerOnMap(map, marker);
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

	public int getTopBarHeight() {
		return topBar.getHeight();
	}

	public boolean isTopBarVisible() {
		return topBar.getVisibility() == View.VISIBLE
				&& map.findViewById(R.id.MapHudButtonsOverlay).getVisibility() == View.VISIBLE;
	}

	public void updateInfo(LatLon customLocation, int zoom) {
		if (!map.getMyApplication().getSettings().USE_MAP_MARKERS.get()) {
			return;
		}

		if (customLocation != null) {
			loc = customLocation;
		} else {
			Location l = map.getMapViewTrackingUtilities().getMyLocation();
			if (l != null) {
				loc = new LatLon(l.getLatitude(), l.getLongitude());
			} else {
				loc = null;
			}
		}

		List<MapMarker> markers = helper.getMapMarkers();
		if (zoom < 3 || markers.size() == 0
				|| !map.getMyApplication().getSettings().MAP_MARKERS_MODE.get().isToolbar()
				|| map.getMyApplication().getRoutingHelper().isFollowingMode()
				|| map.getMyApplication().getRoutingHelper().isRoutePlanningMode()
				|| MapRouteInfoMenu.isVisible()
				|| addressTopBar.getVisibility() == View.VISIBLE
				|| map.isTopToolbarActive()) {
			updateVisibility(false);
			return;
		}

		Float heading = map.getMapViewTrackingUtilities().getHeading();

		MapMarker marker = markers.get(0);
		updateUI(loc, heading, marker, arrowImg, distText, okButton, addressText, true, customLocation != null);

		if (markers.size() > 1) {
			marker = markers.get(1);
			if (loc != null && customLocation == null) {
				for (int i = 1; i < markers.size(); i++) {
					MapMarker m = markers.get(i);
					m.dist = (int) (MapUtils.getDistance(m.getLatitude(), m.getLongitude(),
							loc.getLatitude(), loc.getLongitude()));
					if (m.dist < MIN_DIST_2ND_ROW_SHOW && marker.dist > m.dist) {
						marker = m;
					}
				}
			}
			updateUI(loc, heading, marker, arrowImg2nd, distText2nd, okButton2nd, addressText2nd, false, customLocation != null);
			updateVisibility(topBar2nd, true);
		} else {
			updateVisibility(topBar2nd, false);
		}

		updateVisibility(true);
	}

	private void updateUI(LatLon loc, Float heading, MapMarker marker, ImageView arrowImg,
						  TextView distText, ImageButton okButton, TextView addressText,
						  boolean firstLine, boolean customLocation) {
		float[] mes = new float[2];
		if (loc != null && marker.point != null) {
			Location.distanceBetween(marker.getLatitude(), marker.getLongitude(), loc.getLatitude(), loc.getLongitude(), mes);
		}

		if (customLocation) {
			heading = 0f;
		}

		boolean newImage = false;
		DirectionDrawable dd;
		if (!(arrowImg.getDrawable() instanceof DirectionDrawable)) {
			newImage = true;
			dd = new DirectionDrawable(map, arrowImg.getWidth(), arrowImg.getHeight());
		} else {
			dd = (DirectionDrawable) arrowImg.getDrawable();
		}
		dd.setImage(R.drawable.ic_arrow_marker_diretion, MapMarkerDialogHelper.getMapMarkerColorId(marker.colorIndex));
		if (heading != null && loc != null) {
			dd.setAngle(mes[1] - heading + 180 + screenOrientation);
		}
		if (newImage) {
			arrowImg.setImageDrawable(dd);
		}
		arrowImg.invalidate();

		int dist = (int) mes[0];
		String txt;
		if (loc != null) {
			txt = OsmAndFormatter.getFormattedDistance(dist, map.getMyApplication());
		} else {
			txt = "—";
		}
		if (txt != null) {
			distText.setText(txt);
		}
		updateVisibility(okButton, !customLocation && loc != null && dist < MIN_DIST_OK_VISIBLE);

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

	public TextInfoWidget createMapMarkerControl(final MapActivity map, final boolean firstMarker) {
		return new DistanceToMapMarkerControl(map, firstMarker) {
			@Override
			public LatLon getLatLon() {
				return loc;
			}

			@Override
			protected void click(OsmandMapTileView view) {
				showMarkerOnMap(firstMarker ? 0 : 1);
			}
		};
	}

	public boolean isLandscapeLayout() {
		return !portraitMode;
	}

	public abstract static class DistanceToMapMarkerControl extends TextInfoWidget {

		private boolean firstMarker;
		private final OsmandMapTileView view;
		private MapActivity map;
		private MapMarkersHelper helper;
		private float[] calculations = new float[1];
		private int cachedMeters;
		private int cachedMarkerColorIndex = -1;
		private Boolean cachedNightMode = null;

		public DistanceToMapMarkerControl(MapActivity map, boolean firstMarker) {
			super(map);
			this.map = map;
			this.firstMarker = firstMarker;
			this.view = map.getMapView();
			helper = map.getMyApplication().getMapMarkersHelper();
			setText(null, null);
			setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					click(view);
				}
			});
		}

		protected abstract void click(OsmandMapTileView view);

		public abstract LatLon getLatLon();

		@Override
		public boolean updateInfo(DrawSettings drawSettings) {
			MapMarker marker = getMarker();
			if (marker == null
					|| map.getMyApplication().getRoutingHelper().isRoutePlanningMode()
					|| map.getMyApplication().getRoutingHelper().isFollowingMode()) {
				cachedMeters = 0;
				setText(null, null);
				return false;
			}
			boolean res = false;
			int d = getDistance();
			if (cachedMeters != d) {
				cachedMeters = d;
				String ds = OsmAndFormatter.getFormattedDistance(cachedMeters, view.getApplication());
				int ls = ds.lastIndexOf(' ');
				if (ls == -1) {
					setText(ds, null);
				} else {
					setText(ds.substring(0, ls), ds.substring(ls + 1));
				}
				res = true;
			}

			if (marker.colorIndex != -1) {
				if (marker.colorIndex != cachedMarkerColorIndex
						|| cachedNightMode == null || cachedNightMode != isNight()) {
					setImageDrawable(map.getMyApplication().getIconsCache()
							.getIcon(isNight() ? R.drawable.widget_marker_night : R.drawable.widget_marker_day,
									R.drawable.widget_marker_triangle,
									MapMarkerDialogHelper.getMapMarkerColorId(marker.colorIndex)));
					cachedMarkerColorIndex = marker.colorIndex;
					cachedNightMode = isNight();
					res = true;
				}
			}
			return res;
		}

		public LatLon getPointToNavigate() {
			MapMarker marker = getMarker();
			if (marker != null) {
				return marker.point;
			}
			return null;
		}

		private MapMarker getMarker() {
			List<MapMarker> markers = helper.getMapMarkers();
			if (firstMarker) {
				if (markers.size() > 0) {
					return markers.get(0);
				}
			} else {
				if (markers.size() > 1) {
					return markers.get(1);
				}
			}
			return null;
		}

		public int getDistance() {
			int d = 0;
			LatLon l = getPointToNavigate();
			if (l != null) {
				LatLon loc = getLatLon();
				if (loc == null) {
					Location.distanceBetween(view.getLatitude(), view.getLongitude(), l.getLatitude(), l.getLongitude(), calculations);
				} else {
					Location.distanceBetween(loc.getLatitude(), loc.getLongitude(), l.getLatitude(), l.getLongitude(), calculations);
				}
				d = (int) calculations[0];
			}
			return d;
		}
	}
}

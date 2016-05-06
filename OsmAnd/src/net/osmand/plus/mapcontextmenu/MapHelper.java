package net.osmand.plus.mapcontextmenu;

import android.content.res.Resources;
import android.util.TypedValue;

import net.osmand.AndroidUtils;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.OsmandMapTileView;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

public class MapHelper {

	public static final float MARKER_PADDING_DP = 20f;
	public static final float MARKER_PADDING_X_DP = 50f;

	private MapContextMenu menu;

	private int markerPaddingPx;
	private int markerPaddingXPx;

	private int screenHeight;

	private OsmandMapTileView map;
	private LatLon mapCenter;
	private int origMarkerX;
	private int origMarkerY;
	private boolean customMapCenter;

	private final MapActivity mMapActivity;
	private final int bottomSheetViewHeght;

	public MapHelper(MapActivity mapActivity, int bottomSheetViewHeightDp) {
		this.mMapActivity = mapActivity;
		screenHeight = AndroidUtils.getScreenHeight(mapActivity);

		markerPaddingPx = dpToPx(MARKER_PADDING_DP);
		markerPaddingXPx = dpToPx(MARKER_PADDING_X_DP);

		menu = mapActivity.getContextMenu();

		map = mapActivity.getMapView();
		RotatedTileBox box = map.getCurrentRotatedTileBox().copy();
		customMapCenter = menu.getMapCenter() != null;
		if (!customMapCenter) {
			mapCenter = box.getCenterLatLon();
			menu.setMapCenter(mapCenter);
			double markerLat = menu.getLatLon().getLatitude();
			double markerLon = menu.getLatLon().getLongitude();
			origMarkerX = (int) box.getPixXFromLatLon(markerLat, markerLon);
			origMarkerY = (int) box.getPixYFromLatLon(markerLat, markerLon);
		} else {
			mapCenter = menu.getMapCenter();
			origMarkerX = box.getCenterPixelX();
			origMarkerY = box.getCenterPixelY();
		}

		menu.updateData();
		bottomSheetViewHeght = dpToPx(bottomSheetViewHeightDp);
	}

	public void updateMapCenter(LatLon mapCenter) {
		customMapCenter = true;
		menu.setMapCenter(mapCenter);
		this.mapCenter = mapCenter;
		RotatedTileBox box = map.getCurrentRotatedTileBox().copy();
		origMarkerX = box.getCenterPixelX();
		origMarkerY = box.getCenterPixelY();
	}

	private int getZoom() {
		int zoom;
		zoom = menu.getMapZoom();
		if (zoom == 0) {
			zoom = map.getZoom();
		}
		return zoom;
	}

	public void showOnMap(LatLon latLon, boolean updateCoords) {
		AnimateDraggingMapThread thread = map.getAnimatedDraggingThread();
		int fZoom = getZoom();
		double flat = latLon.getLatitude();
		double flon = latLon.getLongitude();

		RotatedTileBox cp = map.getCurrentRotatedTileBox().copy();
		cp.setCenterLocation(0.5f, map.getMapPosition() == OsmandSettings.BOTTOM_CONSTANT ? 0.15f : 0.5f);
		cp.setLatLonCenter(flat, flon);
		cp.setZoom(fZoom);
		flat = cp.getLatFromPixel(cp.getPixWidth() / 2, cp.getPixHeight() / 2);
		flon = cp.getLonFromPixel(cp.getPixWidth() / 2, cp.getPixHeight() / 2);

		if (updateCoords) {
			mapCenter = new LatLon(flat, flon);
			menu.setMapCenter(mapCenter);
			origMarkerX = cp.getCenterPixelX();
			origMarkerY = cp.getCenterPixelY();
		}

		LatLon adjustedLatLon = getAdjustedMarkerLocation(getPosY(), new LatLon(flat, flon), true, fZoom);
		flat = adjustedLatLon.getLatitude();
		flon = adjustedLatLon.getLongitude();

		thread.startMoving(flat, flon, fZoom, true);
	}

	private int getPosY() {
		return screenHeight;
	}

	private LatLon getAdjustedMarkerLocation(int y, LatLon reqMarkerLocation, boolean center, int zoom) {
		double markerLat = reqMarkerLocation.getLatitude();
		double markerLon = reqMarkerLocation.getLongitude();
		RotatedTileBox box = map.getCurrentRotatedTileBox().copy();
		box.setCenterLocation(0.5f, map.getMapPosition() == OsmandSettings.BOTTOM_CONSTANT ? 0.15f : 0.5f);
		box.setZoom(zoom);
		int markerMapCenterX = (int) box.getPixXFromLatLon(mapCenter.getLatitude(), mapCenter.getLongitude());
		int markerMapCenterY = (int) box.getPixYFromLatLon(mapCenter.getLatitude(), mapCenter.getLongitude());
		float cpyOrig = box.getCenterPixelPoint().y;

		box.setCenterLocation(0.5f, 0.5f);
		int markerX = (int) box.getPixXFromLatLon(markerLat, markerLon);
		int markerY = (int) box.getPixYFromLatLon(markerLat, markerLon);
		QuadPoint cp = box.getCenterPixelPoint();
		float cpx = cp.x;
		float cpy = cp.y;

		float cpyDelta = menu.isLandscapeLayout() ? 0 : cpyOrig - cpy;

		markerY += cpyDelta;
		y += cpyDelta;
		float origMarkerY = this.origMarkerY + cpyDelta;

		LatLon latlon;
		if (center) {
			latlon = reqMarkerLocation;
		} else {
			latlon = box.getLatLonFromPixel(markerMapCenterX, markerMapCenterY);
		}
		if (menu.isLandscapeLayout()) {
			int x = dpToPx(menu.getLandscapeWidthDp());
			if (markerX - markerPaddingXPx < x || markerX > origMarkerX) {
				int dx = (x + markerPaddingXPx) - markerX;
				int dy = 0;
				if (center) {
					dy = (int) cpy - markerY;
				} else {
					cpy = cpyOrig;
				}
				if (dx >= 0 || center) {
					latlon = box.getLatLonFromPixel(cpx - dx, cpy - dy);
				}
			}
		} else {
			if (markerY + markerPaddingPx > y || markerY < origMarkerY) {
				int dx = 0;
				int dy = markerY - (y - markerPaddingPx);
				if (markerY - dy <= origMarkerY) {
					if (center) {
						dx = markerX - (int) cpx;
					}
					latlon = box.getLatLonFromPixel(cpx + dx, cpy + dy);
				}
			}
		}
		return latlon;
	}

	private int dpToPx(float dp) {
		Resources r = mMapActivity.getResources();
		return (int) TypedValue.applyDimension(COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
	}
}

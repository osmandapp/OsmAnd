package net.osmand.plus.views;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

import java.util.List;

public class MapMarkersLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider {
	private final MapActivity map;
	private OsmandMapTileView view;

	private Paint bitmapPaint;
	private Bitmap markerBitmapBlue;
	private Bitmap markerBitmapGreen;
	private Bitmap markerBitmapOrange;
	private Bitmap markerBitmapRed;
	private Bitmap markerBitmapYellow;

	public MapMarkersLayer(MapActivity map) {
		this.map = map;
	}

	private void initUI() {
		bitmapPaint = new Paint();
		bitmapPaint.setDither(true);
		bitmapPaint.setAntiAlias(true);
		bitmapPaint.setFilterBitmap(true);
		markerBitmapBlue = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_marker_blue);
		markerBitmapGreen = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_marker_green);
		markerBitmapOrange = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_marker_orange);
		markerBitmapRed = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_marker_red);
		markerBitmapYellow = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_marker_yellow);
	}

	private Bitmap getMapMarkerBitmap(int colorIndex) {
		switch (colorIndex) {
			case 0:
				return markerBitmapBlue;
			case 1:
				return markerBitmapGreen;
			case 2:
				return markerBitmapOrange;
			case 3:
				return markerBitmapRed;
			case 4:
				return markerBitmapYellow;
			default:
				return markerBitmapBlue;
		}
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		initUI();
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tb, DrawSettings nightMode) {
		if (tb.getZoom() < 3) {
			return;
		}

		MapMarkersHelper markersHelper = map.getMyApplication().getMapMarkersHelper();
		for (MapMarker marker : markersHelper.getActiveMapMarkers()) {
			if (isLocationVisible(tb, marker)) {
				Bitmap bmp = getMapMarkerBitmap(marker.colorIndex);
				int marginX = bmp.getWidth() / 6;
				int marginY = bmp.getHeight();
				int locationX = tb.getPixXFromLonNoRot(marker.getLongitude());
				int locationY = tb.getPixYFromLatNoRot(marker.getLatitude());
				canvas.rotate(-tb.getRotate(), locationX, locationY);
				canvas.drawBitmap(bmp, locationX - marginX, locationY - marginY, bitmapPaint);
				canvas.rotate(tb.getRotate(), locationX, locationY);
			}
		}
	}

	public boolean isLocationVisible(RotatedTileBox tb, MapMarker marker) {
		if (marker == null || tb == null) {
			return false;
		}
		return tb.containsLatLon(marker.getLatitude(), marker.getLongitude());
	}


	@Override
	public void destroyLayer() {

	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public boolean disableSingleTap() {
		return false;
	}

	@Override
	public boolean disableLongPressOnMap() {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o) {
		MapMarkersHelper markersHelper = map.getMyApplication().getMapMarkersHelper();
		List<MapMarker> markers = markersHelper.getActiveMapMarkers();
		int r = getRadiusPoi(tileBox);
		for (int i = 0; i < markers.size(); i++) {
			MapMarker marker = markers.get(i);
			LatLon latLon = marker.point;
			if (latLon != null) {
				int ex = (int) point.x;
				int ey = (int) point.y;
				int x = (int) tileBox.getPixXFromLatLon(latLon.getLatitude(), latLon.getLongitude());
				int y = (int) tileBox.getPixYFromLatLon(latLon.getLatitude(), latLon.getLongitude());
				if (calculateBelongs(ex, ey, x, y, r)) {
					o.add(marker);
				}
			}
		}


	}

	private boolean calculateBelongs(int ex, int ey, int objx, int objy, int radius) {
		return Math.abs(objx - ex) <= radius && (ey - objy) <= radius && (objy - ey) <= 2.5 * radius;
	}

	public int getRadiusPoi(RotatedTileBox tb) {
		int r;
		final double zoom = tb.getZoom();
		if (zoom <= 15) {
			r = 10;
		} else if (zoom <= 16) {
			r = 14;
		} else if (zoom <= 17) {
			r = 16;
		} else {
			r = 18;
		}
		return (int) (r * tb.getDensity());
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof MapMarker) {
			return ((MapMarker) o).point;
		}
		return null;
	}

	@Override
	public String getObjectDescription(Object o) {
		if (o instanceof MapMarker) {
			return ((MapMarker) o).getPointDescription(view.getContext()).getFullPlainName(view.getContext());
		}
		return null;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof MapMarker) {
			return ((MapMarker) o).getPointDescription(view.getContext());
		}
		return null;
	}
}

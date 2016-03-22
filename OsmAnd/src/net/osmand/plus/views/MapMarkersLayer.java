package net.osmand.plus.views;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Handler;
import android.os.Message;
import android.view.GestureDetector;
import android.view.MotionEvent;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.ContextMenuLayer.IContextMenuProviderSelection;
import net.osmand.plus.views.mapwidgets.MapMarkersWidgetsFactory;

import java.util.ArrayList;
import java.util.List;

public class MapMarkersLayer extends OsmandMapLayer implements IContextMenuProvider, IContextMenuProviderSelection {
	protected static final int DIST_TO_SHOW = 80;
	protected static final long USE_FINGER_LOCATION_DELAY = 1000;
	private static final int MAP_REFRESH_MESSAGE = OsmAndConstants.UI_HANDLER_MAP_VIEW + 6;

	private final MapActivity map;
	private OsmandMapTileView view;

	private MapMarkersWidgetsFactory widgetsFactory;

	private Paint bitmapPaint;
	private Bitmap markerBitmapBlue;
	private Bitmap markerBitmapGreen;
	private Bitmap markerBitmapOrange;
	private Bitmap markerBitmapRed;
	private Bitmap markerBitmapYellow;
	private Bitmap markerBitmapTeal;
	private Bitmap markerBitmapPurple;

	private Paint bitmapPaintDestBlue;
	private Paint bitmapPaintDestGreen;
	private Paint bitmapPaintDestOrange;
	private Paint bitmapPaintDestRed;
	private Paint bitmapPaintDestYellow;
	private Paint bitmapPaintDestTeal;
	private Paint bitmapPaintDestPurple;
	private Bitmap arrowToDestination;
	private float[] calculations = new float[2];

	private Paint paint;
	private Path path;
	private List<LatLon> route = new ArrayList<>();

	private LatLon fingerLocation;
	private boolean hasMoved;
	private boolean moving;
	private boolean useFingerLocation;
	private GestureDetector longTapDetector;
	private Handler handler;

	public MapMarkersLayer(MapActivity map) {
		this.map = map;
	}

	public MapMarkersWidgetsFactory getWidgetsFactory() {
		return widgetsFactory;
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
		markerBitmapTeal = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_marker_teal);
		markerBitmapPurple = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_marker_purple);

		arrowToDestination = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_arrow_to_destination);
		bitmapPaintDestBlue = createPaintDest(R.color.marker_blue);
		bitmapPaintDestGreen = createPaintDest(R.color.marker_green);
		bitmapPaintDestOrange = createPaintDest(R.color.marker_orange);
		bitmapPaintDestRed = createPaintDest(R.color.marker_red);
		bitmapPaintDestYellow = createPaintDest(R.color.marker_yellow);
		bitmapPaintDestTeal = createPaintDest(R.color.marker_teal);
		bitmapPaintDestPurple = createPaintDest(R.color.marker_purple);

		path = new Path();
		paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(7 * view.getDensity());
		paint.setAntiAlias(true);
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setColor(map.getResources().getColor(R.color.marker_red));
		paint.setAlpha(200);

		widgetsFactory = new MapMarkersWidgetsFactory(map);
	}

	private Paint createPaintDest(int colorId) {
		Paint paint = new Paint();
		paint.setDither(true);
		paint.setAntiAlias(true);
		paint.setFilterBitmap(true);
		int color = map.getResources().getColor(colorId);
		paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
		return paint;
	}

	private Paint getMarkerDestPaint(int colorIndex) {
		switch (colorIndex) {
			case 0:
				return bitmapPaintDestBlue;
			case 1:
				return bitmapPaintDestGreen;
			case 2:
				return bitmapPaintDestOrange;
			case 3:
				return bitmapPaintDestRed;
			case 4:
				return bitmapPaintDestYellow;
			case 5:
				return bitmapPaintDestTeal;
			case 6:
				return bitmapPaintDestPurple;
			default:
				return bitmapPaintDestBlue;
		}
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
			case 5:
				return markerBitmapTeal;
			case 6:
				return markerBitmapPurple;
			default:
				return markerBitmapBlue;
		}
	}

	public void setRoute(List<LatLon> points) {
		route.clear();
		route.addAll(points);
	}

	public boolean clearRoute() {
		boolean res = route.size() > 0;
		route.clear();
		return res;
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		handler = new Handler();
		initUI();
		longTapDetector = new GestureDetector(view.getContext(), new GestureDetector.OnGestureListener() {
			@Override
			public boolean onDown(MotionEvent e) {
				return false;
			}

			@Override
			public void onShowPress(MotionEvent e) {

			}

			@Override
			public boolean onSingleTapUp(MotionEvent e) {
				return false;
			}

			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
				return false;
			}

			@Override
			public void onLongPress(MotionEvent e) {
				cancelFingerAction();
			}

			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
				return false;
			}
		});
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tb, DrawSettings nightMode) {

		widgetsFactory.updateInfo(useFingerLocation ? fingerLocation : null, tb.getZoom());

		if (tb.getZoom() < 3 || !map.getMyApplication().getSettings().USE_MAP_MARKERS.get()) {
			return;
		}

		MapMarkersHelper markersHelper = map.getMyApplication().getMapMarkersHelper();
		if (route.size() > 0) {
			path.reset();
			boolean first = true;
			Location myLocation = map.getMapViewTrackingUtilities().getMyLocation();
			if (markersHelper.isStartFromMyLocation() && myLocation != null) {
				int locationX = tb.getPixXFromLonNoRot(myLocation.getLongitude());
				int locationY = tb.getPixYFromLatNoRot(myLocation.getLatitude());
				path.moveTo(locationX, locationY);
				first = false;
			}
			for (LatLon point : route) {
				int locationX = tb.getPixXFromLonNoRot(point.getLongitude());
				int locationY = tb.getPixYFromLatNoRot(point.getLatitude());
				if (first) {
					path.moveTo(locationX, locationY);
					first = false;
				} else {
					path.lineTo(locationX, locationY);
				}
			}
			canvas.drawPath(path, paint);
		}

		List<MapMarker> activeMapMarkers = markersHelper.getActiveMapMarkers();
		for (int i = 0; i < activeMapMarkers.size(); i++) {
			MapMarker marker = activeMapMarkers.get(i);
			if (isLocationVisible(tb, marker) && !overlappedByWaypoint(marker)) {
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

		boolean show = useFingerLocation && map.getMyApplication().getSettings().SHOW_DESTINATION_ARROW.get();
		if (show && fingerLocation != null) {
			List<MapMarker> sortedMapMarkers = markersHelper.getSortedMapMarkers();
			int i = 0;
			for (MapMarker marker : sortedMapMarkers) {
				if (!isLocationVisible(tb, marker)) {
					canvas.save();
					net.osmand.Location.distanceBetween(fingerLocation.getLatitude(), fingerLocation.getLongitude(),
							marker.getLatitude(), marker.getLongitude(), calculations);
					float bearing = calculations[1] - 90;
					float radiusBearing = DIST_TO_SHOW * tb.getDensity();
					final QuadPoint cp = tb.getCenterPixelPoint();
					canvas.rotate(bearing, cp.x, cp.y);
					canvas.translate(-24 * tb.getDensity() + radiusBearing, -22 * tb.getDensity());
					canvas.drawBitmap(arrowToDestination, cp.x, cp.y, getMarkerDestPaint(marker.colorIndex));
					canvas.restore();
				}
				i++;
				if (i > 1) {
					break;
				}
			}
		}
	}

	public boolean isLocationVisible(RotatedTileBox tb, MapMarker marker) {
		if (marker == null || tb == null) {
			return false;
		}
		return containsLatLon(tb, marker.getLatitude(), marker.getLongitude());
	}

	public boolean containsLatLon(RotatedTileBox tb, double lat, double lon) {
		double widgetHeight = 0;
		if (widgetsFactory.isTopBarVisible()) {
			widgetHeight = widgetsFactory.getTopBarHeight();
		}
		double tx = tb.getPixXFromLatLon(lat, lon);
		double ty = tb.getPixYFromLatLon(lat, lon);
		return tx >= 0 && tx <= tb.getPixWidth() && ty >= widgetHeight && ty <= tb.getPixHeight();
	}

	public boolean overlappedByWaypoint(MapMarker marker) {
		List<TargetPoint> targetPoints = map.getMyApplication().getTargetPointsHelper().getAllPoints();
		for (TargetPoint t : targetPoints) {
			if (t.point.equals(marker.point)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void destroyLayer() {
	}

	@Override
	public boolean onTouchEvent(MotionEvent event, RotatedTileBox tileBox) {
		if (!longTapDetector.onTouchEvent(event)) {
			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					float x = event.getX();
					float y = event.getY();
					fingerLocation = tileBox.getLatLonFromPixel(x, y);
					hasMoved = false;
					moving = true;
					break;

				case MotionEvent.ACTION_MOVE:
					if (!hasMoved) {
						if (!handler.hasMessages(MAP_REFRESH_MESSAGE)) {
							Message msg = Message.obtain(handler, new Runnable() {
								@Override
								public void run() {
									handler.removeMessages(MAP_REFRESH_MESSAGE);
									if (moving) {
										if (!useFingerLocation) {
											useFingerLocation = true;
											map.refreshMap();
										}
									}
								}
							});
							msg.what = MAP_REFRESH_MESSAGE;
							handler.sendMessageDelayed(msg, USE_FINGER_LOCATION_DELAY);
						}
						hasMoved = true;
					}
					break;

				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
					cancelFingerAction();
					break;
			}
		}
		return super.onTouchEvent(event, tileBox);
	}

	private void cancelFingerAction() {
		handler.removeMessages(MAP_REFRESH_MESSAGE);
		useFingerLocation = false;
		moving = false;
		fingerLocation = null;
		map.refreshMap();
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
	public boolean isObjectClickable(Object o) {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o) {
		if (tileBox.getZoom() < 3 || !map.getMyApplication().getSettings().USE_MAP_MARKERS.get()) {
			return;
		}

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

	@Override
	public int getOrder(Object o) {
		return 0;
	}

	@Override
	public void setSelectedObject(Object o) {
		if (o instanceof MapMarker) {
			MapMarkersHelper markersHelper = map.getMyApplication().getMapMarkersHelper();
			MapMarker marker = (MapMarker) o;
			List<MapMarker> sortedMarkers = markersHelper.getSortedMapMarkers();
			int i = sortedMarkers.indexOf(marker);
			if (i != -1) {
				sortedMarkers.remove(i);
				sortedMarkers.add(0, marker);
				markersHelper.normalizePositions();
			}
		}
	}

	@Override
	public void clearSelectedObject() {
	}
}

package net.osmand.plus.views;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.GPXUtilities.TrkSegment;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.ContextMenuLayer.ApplyMovedObjectCallback;
import net.osmand.plus.views.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.ContextMenuLayer.IContextMenuProviderSelection;
import net.osmand.plus.views.mapwidgets.MapMarkersWidgetsFactory;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

import gnu.trove.list.array.TIntArrayList;

public class MapMarkersLayer extends OsmandMapLayer implements IContextMenuProvider,
		IContextMenuProviderSelection, ContextMenuLayer.IMoveObjectProvider {

	protected static final int DIST_TO_SHOW = 80;
	private static final int TEXT_SIZE = 12;
	private static final int VERTICAL_OFFSET = 10;

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

	private final RenderingLineAttributes lineAttrs = new RenderingLineAttributes("measureDistanceLine");
	private final RenderingLineAttributes textAttrs = new RenderingLineAttributes("rulerLineFont");
	private Paint paint;
	private TrkSegment route;

	private TIntArrayList tx = new TIntArrayList();
	private TIntArrayList ty = new TIntArrayList();
	private Path linePath = new Path();

	private ContextMenuLayer contextMenuLayer;

	private boolean inPlanRouteMode;

	public MapMarkersLayer(MapActivity map) {
		this.map = map;
	}

	public MapMarkersWidgetsFactory getWidgetsFactory() {
		return widgetsFactory;
	}

	public boolean isInPlanRouteMode() {
		return inPlanRouteMode;
	}

	public void setInPlanRouteMode(boolean inPlanRouteMode) {
		this.inPlanRouteMode = inPlanRouteMode;
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

		paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(7 * view.getDensity());
		paint.setAntiAlias(true);
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setColor(ContextCompat.getColor(map, R.color.marker_red));
		paint.setAlpha(200);

		float textSize = TEXT_SIZE * map.getResources().getDisplayMetrics().density;
		textAttrs.paint.setTextSize(textSize);
		textAttrs.paint2.setTextSize(textSize);

		widgetsFactory = new MapMarkersWidgetsFactory(map);

		contextMenuLayer = view.getLayerByClass(ContextMenuLayer.class);
	}

	private Paint createPaintDest(int colorId) {
		Paint paint = new Paint();
		paint.setDither(true);
		paint.setAntiAlias(true);
		paint.setFilterBitmap(true);
		int color = ContextCompat.getColor(map, colorId);
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

	public void setRoute(TrkSegment route) {
		this.route = route;
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		initUI();
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
		Location myLoc = map.getMyApplication().getLocationProvider().getLastStaleKnownLocation();
		MapMarkersHelper markersHelper = map.getMyApplication().getMapMarkersHelper();
		List<MapMarker> activeMapMarkers = markersHelper.getMapMarkers();

		if (route != null && route.points.size() > 0) {
			route.renders.clear();
			route.renders.add(new Renderable.StandardTrack(new ArrayList<>(route.points), 17.2));
			route.drawRenderers(view.getZoom(), paint, canvas, tileBox);
		}

		if (markersHelper.isStartFromMyLocation() && myLoc != null) {
			lineAttrs.updatePaints(view, nightMode, tileBox);
			textAttrs.updatePaints(view, nightMode, tileBox);
			textAttrs.paint.setStyle(Paint.Style.FILL);

			int locX = (int) tileBox.getPixXFromLatLon(myLoc.getLatitude(), myLoc.getLongitude());
			int locY = (int) tileBox.getPixYFromLatLon(myLoc.getLatitude(), myLoc.getLongitude());
			int[] colors = MapMarker.getColors(map);
			for (int i = 0; i < activeMapMarkers.size() && i < 2; i++) {
				MapMarker marker = activeMapMarkers.get(i);
				int markerX = (int) tileBox.getPixXFromLatLon(marker.getLatitude(), marker.getLongitude());
				int markerY = (int) tileBox.getPixYFromLatLon(marker.getLatitude(), marker.getLongitude());

				linePath.reset();
				tx.clear();
				ty.clear();

				tx.add(locX);
				ty.add(locY);
				tx.add(markerX);
				ty.add(markerY);

				calculatePath(tileBox, tx, ty, linePath);
				PathMeasure pm = new PathMeasure(linePath, false);
				float[] pos = new float[2];
				pm.getPosTan(pm.getLength() / 2, pos, null);

				float generalDist = (float) MapUtils.getDistance(myLoc.getLatitude(), myLoc.getLongitude(), marker.getLatitude(), marker.getLongitude());
				String generalDistSt = OsmAndFormatter.getFormattedDistance(generalDist, view.getApplication());
				boolean locationInvisible = locX < 0 || locX > tileBox.getPixWidth() || locY < 0 || locY > tileBox.getPixHeight();
				String distanceText;
				if (locationInvisible) {
					float centerToMarkerDist = (float) MapUtils.getDistance(tileBox.getLatLonFromPixel(pos[0], pos[1]), marker.getLatitude(), marker.getLongitude());
					String centerToMarkerDistSt = OsmAndFormatter.getFormattedDistance(centerToMarkerDist, view.getApplication());
					if (locX >= markerX) {
						distanceText = centerToMarkerDistSt + " | " + generalDistSt;
					} else {
						distanceText = generalDistSt + " | " + centerToMarkerDistSt;
					}
				} else {
					distanceText = generalDistSt;
				}
				Rect bounds = new Rect();
				textAttrs.paint.getTextBounds(distanceText, 0, distanceText.length(), bounds);
				float hOffset = pm.getLength() / 2 - bounds.width() / 2;
				lineAttrs.paint.setColor(colors[marker.colorIndex]);

				canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
				canvas.drawPath(linePath, lineAttrs.paint);
				if (locationInvisible && !(pos[0] == 0 && pos[1] == 0)) {
					canvas.drawCircle(pos[0], pos[1], 5, new Paint());
				}
				if (locX >= markerX) {
					canvas.rotate(180, pos[0], pos[1]);
					canvas.drawTextOnPath(distanceText, linePath, hOffset, bounds.height() + VERTICAL_OFFSET, textAttrs.paint2);
					canvas.drawTextOnPath(distanceText, linePath, hOffset, bounds.height() + VERTICAL_OFFSET, textAttrs.paint);
					canvas.rotate(-180, pos[0], pos[1]);
				} else {
					canvas.drawTextOnPath(distanceText, linePath, hOffset, -VERTICAL_OFFSET, textAttrs.paint2);
					canvas.drawTextOnPath(distanceText, linePath, hOffset, -VERTICAL_OFFSET, textAttrs.paint);
				}
				canvas.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
			}
		}
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
		Location myLoc = map.getMyApplication().getLocationProvider().getLastStaleKnownLocation();
		widgetsFactory.updateInfo(myLoc == null
				? tileBox.getCenterLatLon() : new LatLon(myLoc.getLatitude(), myLoc.getLongitude()), tileBox.getZoom());
		OsmandSettings settings = map.getMyApplication().getSettings();

		if (tileBox.getZoom() < 3 || !settings.USE_MAP_MARKERS.get()) {
			return;
		}

		MapMarkersHelper markersHelper = map.getMyApplication().getMapMarkersHelper();

		for (MapMarker marker : markersHelper.getMapMarkers()) {
			if (isLocationVisible(tileBox, marker) && !overlappedByWaypoint(marker)
					&& !isInMotion(marker)) {
				Bitmap bmp = getMapMarkerBitmap(marker.colorIndex);
				int marginX = bmp.getWidth() / 6;
				int marginY = bmp.getHeight();
				int locationX = tileBox.getPixXFromLonNoRot(marker.getLongitude());
				int locationY = tileBox.getPixYFromLatNoRot(marker.getLatitude());
				canvas.rotate(-tileBox.getRotate(), locationX, locationY);
				canvas.drawBitmap(bmp, locationX - marginX, locationY - marginY, bitmapPaint);
				canvas.rotate(tileBox.getRotate(), locationX, locationY);
			}
		}

		if (settings.SHOW_ARROWS_TO_FIRST_MARKERS.get()) {
			LatLon loc = tileBox.getCenterLatLon();
			int i = 0;
			for (MapMarker marker : markersHelper.getMapMarkers()) {
				if (!isLocationVisible(tileBox, marker) && !isInMotion(marker)) {
					canvas.save();
					net.osmand.Location.distanceBetween(loc.getLatitude(), loc.getLongitude(),
							marker.getLatitude(), marker.getLongitude(), calculations);
					float bearing = calculations[1] - 90;
					float radiusBearing = DIST_TO_SHOW * tileBox.getDensity();
					final QuadPoint cp = tileBox.getCenterPixelPoint();
					canvas.rotate(bearing, cp.x, cp.y);
					canvas.translate(-24 * tileBox.getDensity() + radiusBearing, -22 * tileBox.getDensity());
					canvas.drawBitmap(arrowToDestination, cp.x, cp.y, getMarkerDestPaint(marker.colorIndex));
					canvas.restore();
				}
				i++;
				if (i > 1) {
					break;
				}
			}
		}

		if (contextMenuLayer.getMoveableObject() instanceof MapMarker) {
			MapMarker objectInMotion = (MapMarker) contextMenuLayer.getMoveableObject();
			Bitmap bitmap = getMapMarkerBitmap(objectInMotion.colorIndex);
			PointF pf = contextMenuLayer.getMovableCenterPoint(tileBox);
			int marginX = bitmap.getWidth() / 6;
			int marginY = bitmap.getHeight();
			float locationX = pf.x;
			float locationY = pf.y;
			canvas.rotate(-tileBox.getRotate(), locationX, locationY);
			canvas.drawBitmap(bitmap, locationX - marginX, locationY - marginY, bitmapPaint);
		}
	}

	private boolean isInMotion(@NonNull MapMarker marker) {
		return marker.equals(contextMenuLayer.getMoveableObject());
	}

	public boolean isLocationVisible(RotatedTileBox tb, MapMarker marker) {
		//noinspection SimplifiableIfStatement
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
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public boolean disableSingleTap() {
		return inPlanRouteMode;
	}

	@Override
	public boolean disableLongPressOnMap() {
		return inPlanRouteMode;
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
		List<MapMarker> markers = markersHelper.getMapMarkers();
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
			map.getMyApplication().getMapMarkersHelper().moveMarkerToTop((MapMarker) o);
			map.getMyApplication().getSettings().MAP_MARKERS_ORDER_BY_MODE.set(OsmandSettings.MapMarkersOrderByMode.CUSTOM);
		}
	}

	@Override
	public void clearSelectedObject() {
	}

	@Override
	public boolean isObjectMovable(Object o) {
		return o instanceof MapMarker;
	}

	@Override
	public void applyNewObjectPosition(@NonNull Object o, @NonNull LatLon position,
									   @Nullable ApplyMovedObjectCallback callback) {
		boolean result = false;
		MapMarker newObject = null;
		if (o instanceof MapMarker) {
			MapMarkersHelper markersHelper = map.getMyApplication().getMapMarkersHelper();
			MapMarker marker = (MapMarker) o;

			PointDescription originalDescription = marker.getOriginalPointDescription();
			if (originalDescription.isLocation()) {
				originalDescription.setName(PointDescription.getSearchAddressStr(map));
			}
			markersHelper.moveMapMarker(marker, position);
			int index = markersHelper.getMapMarkers().indexOf(marker);
			if (index != -1) {
				newObject = markersHelper.getMapMarkers().get(index);
			}
			result = true;
		}
		if (callback != null) {
			callback.onApplyMovedObject(result, newObject == null ? o : newObject);
		}
	}
}

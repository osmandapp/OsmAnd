package net.osmand.plus.measurementtool;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.MapUtils;

import java.util.LinkedList;
import java.util.List;

import gnu.trove.list.array.TIntArrayList;

public class MeasurementToolLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider {

	private OsmandMapTileView view;
	private boolean inMeasurementMode;
	private boolean inMovePointMode;
	private final LinkedList<WptPt> measurementPoints = new LinkedList<>();
	private Bitmap centerIconDay;
	private Bitmap centerIconNight;
	private Bitmap pointIcon;
	private Bitmap movePointIcon;
	private Paint bitmapPaint;
	private final RenderingLineAttributes lineAttrs = new RenderingLineAttributes("measureDistanceLine");
	private final Path path = new Path();
	private int marginX;
	private int marginY;
	private final TIntArrayList tx = new TIntArrayList();
	private final TIntArrayList ty = new TIntArrayList();
	private OnSingleTapListener singleTapListener;
	private OnEnterMovePointModeListener enterMovePointModeListener;
	private int movePointPos;
	private WptPt pointBeforeMovement;

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;

		centerIconDay = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_ruler_center_day);
		centerIconNight = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_ruler_center_night);
		pointIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_pedestrian_location);
		movePointIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_mapillary_location);

		bitmapPaint = new Paint();
		bitmapPaint.setAntiAlias(true);
		bitmapPaint.setDither(true);
		bitmapPaint.setFilterBitmap(true);

		marginY = pointIcon.getHeight() / 2;
		marginX = pointIcon.getWidth() / 2;
	}

	void setOnSingleTapListener(OnSingleTapListener listener) {
		this.singleTapListener = listener;
	}

	void setOnEnterMovePointModeListener(OnEnterMovePointModeListener listener) {
		this.enterMovePointModeListener = listener;
	}

	WptPt getPointBeforeMovement() {
		return pointBeforeMovement;
	}

	int getMovePointPosition() {
		return movePointPos;
	}

	public boolean isInMeasurementMode() {
		return inMeasurementMode;
	}

	boolean isInMovePointMode() {
		return inMovePointMode;
	}

	void setInMeasurementMode(boolean inMeasurementMode) {
		this.inMeasurementMode = inMeasurementMode;
	}

	public int getPointsCount() {
		return measurementPoints.size();
	}

	public LinkedList<WptPt> getMeasurementPoints() {
		return measurementPoints;
	}

	String getDistanceSt() {
		float dist = 0;
		if (measurementPoints.size() > 0) {
			for (int i = 1; i < measurementPoints.size(); i++) {
				dist += MapUtils.getDistance(measurementPoints.get(i - 1).lat, measurementPoints.get(i - 1).lon,
						measurementPoints.get(i).lat, measurementPoints.get(i).lon);
			}
		}
		return OsmAndFormatter.getFormattedDistance(dist, view.getApplication());
	}

	@Override
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		if (inMeasurementMode && !inMovePointMode && singleTapListener != null) {
			singleTapListener.onSingleTap();
		}
		return false;
	}

	@Override
	public boolean onLongPressEvent(PointF point, RotatedTileBox tileBox) {
		if (inMovePointMode || measurementPoints.size() == 0) {
			return false;
		}
		double pressedPointLat = tileBox.getLatFromPixel(point.x, point.y);
		double pressedPointLon = tileBox.getLonFromPixel(point.x, point.y);
		getPointToMove(pressedPointLat, pressedPointLon);
		if (pointBeforeMovement != null && movePointPos != -1) {
			enterMovingPointMode();
			if (inMeasurementMode && inMovePointMode && enterMovePointModeListener != null) {
				enterMovePointModeListener.onEnterMovePointMode();
			}
		}
		return false;
	}

	private void enterMovingPointMode() {
		inMovePointMode = true;
		moveMapToPoint(movePointPos);
	}

	private void getPointToMove(double lat, double lon) {
		RotatedTileBox tb = view.getCurrentRotatedTileBox();
		double lowestDistance = Double.MAX_VALUE;
		for (int i = 0; i < measurementPoints.size(); i++) {
			WptPt pt = measurementPoints.get(i);
			if (tb.containsLatLon(pt.getLatitude(), pt.getLongitude())) {
				double latDiff = pt.getLatitude() - lat;
				double lonDiff = pt.getLongitude() - lon;
				double distToPoint = Math.sqrt(Math.pow(latDiff, 2) + Math.pow(lonDiff, 2));
				if (distToPoint < lowestDistance) {
					lowestDistance = distToPoint;
					pointBeforeMovement = new WptPt(pt);
					movePointPos = i;
				}
			}
		}
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tb, DrawSettings settings) {
		if (inMeasurementMode) {
			lineAttrs.updatePaints(view, settings, tb);
			if (!inMovePointMode) {
				drawCenterIcon(canvas, tb, tb.getCenterPixelPoint(), settings.isNightMode());
			}

			if (measurementPoints.size() > 0) {
				path.reset();
				tx.reset();
				ty.reset();
				for (int i = 0; i < measurementPoints.size(); i++) {
					WptPt pt = measurementPoints.get(i);
					int locX;
					int locY;
					if (inMovePointMode && movePointPos == i) {
						locX = tb.getCenterPixelX();
						locY = tb.getCenterPixelY();
					} else {
						locX = tb.getPixXFromLonNoRot(pt.lon);
						locY = tb.getPixYFromLatNoRot(pt.lat);
					}
					if (i == 0) {
						path.moveTo(locX, locY);
					} else {
						path.lineTo(locX, locY);
					}
					tx.add(locX);
					ty.add(locY);
				}
				if (!inMovePointMode) {
					path.lineTo(tb.getCenterPixelX(), tb.getCenterPixelY());
					tx.add(tb.getCenterPixelX());
					ty.add(tb.getCenterPixelY());
				}
				calculatePath(tb, tx, ty, path);
				canvas.drawPath(path, lineAttrs.paint);

				WptPt pointToDrawOnTop = null;
				for (int i = 0; i < measurementPoints.size(); i++) {
					WptPt pt = measurementPoints.get(i);
					if (inMovePointMode && i == movePointPos) {
						pointToDrawOnTop = pt;
					} else {
						if (tb.containsLatLon(pt.lat, pt.lon)) {
							int locX = tb.getPixXFromLonNoRot(pt.lon);
							int locY = tb.getPixYFromLatNoRot(pt.lat);
							canvas.drawBitmap(pointIcon, locX - marginX, locY - marginY, bitmapPaint);
						}
					}
				}
				if (pointToDrawOnTop != null) {
					int locX = tb.getCenterPixelX();
					int locY = tb.getCenterPixelY();
					canvas.drawBitmap(movePointIcon, locX - marginX, locY - marginY, bitmapPaint);
				}
			}
		}
	}

	void exitMovePointMode() {
		inMovePointMode = false;
		movePointPos = -1;
		pointBeforeMovement = null;
	}

	private void drawCenterIcon(Canvas canvas, RotatedTileBox tb, QuadPoint center, boolean nightMode) {
		canvas.rotate(-tb.getRotate(), center.x, center.y);
		if (nightMode) {
			canvas.drawBitmap(centerIconNight, center.x - centerIconNight.getWidth() / 2,
					center.y - centerIconNight.getHeight() / 2, bitmapPaint);
		} else {
			canvas.drawBitmap(centerIconDay, center.x - centerIconDay.getWidth() / 2,
					center.y - centerIconDay.getHeight() / 2, bitmapPaint);
		}
		canvas.rotate(tb.getRotate(), center.x, center.y);
	}

	public WptPt addPoint(int position) {
		RotatedTileBox tb = view.getCurrentRotatedTileBox();
		LatLon l = tb.getLatLonFromPixel(tb.getCenterPixelX(), tb.getCenterPixelY());
		WptPt pt = new WptPt();
		pt.lat = l.getLatitude();
		pt.lon = l.getLongitude();
		boolean allowed = measurementPoints.size() == 0 || !measurementPoints.get(measurementPoints.size() - 1).equals(pt);
		if (allowed) {
			measurementPoints.add(position, pt);
			return pt;
		}
		return null;
	}

	WptPt getMovedPointToApply() {
		RotatedTileBox tb = view.getCurrentRotatedTileBox();
		LatLon latLon = tb.getCenterLatLon();
		WptPt pt = measurementPoints.get(movePointPos);
		pt.lat = latLon.getLatitude();
		pt.lon = latLon.getLongitude();
		return pt;
	}

	public void moveMapToPoint(int pos) {
		if (measurementPoints.size() > 0) {
			if (pos >= measurementPoints.size()) {
				pos = measurementPoints.size() - 1;
			} else if (pos < 0) {
				pos = 0;
			}
			WptPt pt = measurementPoints.get(pos);
			view.getAnimatedDraggingThread().startMoving(pt.getLatitude(), pt.getLongitude(), view.getZoom(), true);
		}
	}

	public void refreshMap() {
		view.refreshMap();
	}

	@Override
	public void destroyLayer() {

	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o) {

	}

	@Override
	public LatLon getObjectLocation(Object o) {
		return null;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		return null;
	}

	@Override
	public boolean disableSingleTap() {
		return isInMeasurementMode();
	}

	@Override
	public boolean disableLongPressOnMap() {
		return isInMeasurementMode();
	}

	@Override
	public boolean isObjectClickable(Object o) {
		return !isInMeasurementMode();
	}

	interface OnSingleTapListener {
		void onSingleTap();
	}

	interface OnEnterMovePointModeListener {
		void onEnterMovePointMode();
	}
}

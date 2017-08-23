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
import net.osmand.plus.GPXUtilities.TrkSegment;
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
	private List<WptPt> measurementPoints = new LinkedList<>();
	private List<WptPt> snappedToRoadPoints = new LinkedList<>();
	private Bitmap centerIconDay;
	private Bitmap centerIconNight;
	private Bitmap pointIcon;
	private Bitmap applyingPointIcon;
	private Paint bitmapPaint;
	private final RenderingLineAttributes lineAttrs = new RenderingLineAttributes("measureDistanceLine");
	private int marginPointIconX;
	private int marginPointIconY;
	private int marginApplyingPointIconX;
	private int marginApplyingPointIconY;
	private Path path = new Path();
	private TIntArrayList tx = new TIntArrayList();
	private TIntArrayList ty = new TIntArrayList();
	private OnMeasureDistanceToCenter measureDistanceToCenterListener;
	private OnSingleTapListener singleTapListener;
	private OnEnterMovePointModeListener enterMovePointModeListener;
	private int selectedPointPos = -1;
	private WptPt selectedCachedPoint;
	private LatLon pressedPointLatLon;
	private boolean overlapped;
	private int pointsToDraw = 50;
	private MeasurementEditingContext measurementEditingContext;

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;

		centerIconDay = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_ruler_center_day);
		centerIconNight = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_ruler_center_night);
		pointIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_measure_point_day);
		applyingPointIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_measure_point_move_day);

		bitmapPaint = new Paint();
		bitmapPaint.setAntiAlias(true);
		bitmapPaint.setDither(true);
		bitmapPaint.setFilterBitmap(true);

		marginPointIconY = pointIcon.getHeight() / 2;
		marginPointIconX = pointIcon.getWidth() / 2;

		marginApplyingPointIconY = applyingPointIcon.getHeight() / 2;
		marginApplyingPointIconX = applyingPointIcon.getWidth() / 2;
	}

	void setOnSingleTapListener(OnSingleTapListener listener) {
		this.singleTapListener = listener;
	}

	void setMeasurementEditingContext(MeasurementEditingContext measurementEditingContext) {
		this.measurementEditingContext = measurementEditingContext;
	}

	void setOnEnterMovePointModeListener(OnEnterMovePointModeListener listener) {
		this.enterMovePointModeListener = listener;
	}

	void setOnMeasureDistanceToCenterListener(OnMeasureDistanceToCenter listener) {
		this.measureDistanceToCenterListener = listener;
	}

	void setSelectedPointPos(int pos) {
		selectedPointPos = pos;
	}

	void setSelectedCachedPoint(WptPt selectedCachedPoint) {
		this.selectedCachedPoint = selectedCachedPoint;
	}

	WptPt getSelectedCachedPoint() {
		return selectedCachedPoint;
	}

	int getSelectedPointPos() {
		return selectedPointPos;
	}

	public boolean isInMeasurementMode() {
		return inMeasurementMode;
	}

	void setInMeasurementMode(boolean inMeasurementMode) {
		this.inMeasurementMode = inMeasurementMode;
	}

	public List<WptPt> getMeasurementPoints() {
		return measurementPoints;
	}

	void setMeasurementPoints(List<WptPt> points) {
		measurementPoints = points;
	}

	public List<WptPt> getSnappedToRoadPoints() {
		return snappedToRoadPoints;
	}

	public void setSnappedToRoadPoints(List<WptPt> snappedToRoadPoints) {
		this.snappedToRoadPoints = snappedToRoadPoints;
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
		if (singleTapListener != null) {
			if (inMeasurementMode
					&& !measurementEditingContext.isInMovePointMode()
					&& !measurementEditingContext.isInAddPointBeforeMode()
					&& !measurementEditingContext.isInAddPointAfterMode()) {
				if (!overlapped) {
					selectPoint(point.x, point.y, true);
				}
				if (selectedPointPos == -1) {
					pressedPointLatLon = tileBox.getLatLonFromPixel(point.x, point.y);
					singleTapListener.onAddPoint();
				}
			}
		}
		return false;
	}

	void clearSelection() {
		selectedPointPos = -1;
		selectedCachedPoint = null;
	}

	@Override
	public boolean onLongPressEvent(PointF point, RotatedTileBox tileBox) {
		if (inMeasurementMode) {
			if (!overlapped && !measurementEditingContext.isInMovePointMode()
					&& !measurementEditingContext.isInAddPointBeforeMode()
					&& !measurementEditingContext.isInAddPointAfterMode()
					&& measurementPoints.size() > 0) {
				selectPoint(point.x, point.y, false);
				if (selectedCachedPoint != null && selectedPointPos != -1) {
					enterMovingPointMode();
					if (inMeasurementMode && measurementEditingContext.isInMovePointMode() && enterMovePointModeListener != null) {
						enterMovePointModeListener.onEnterMovePointMode();
					}
				}
			}
		}
		return false;
	}

	void enterMovingPointMode() {
		measurementEditingContext.setInMovePointMode(true);
		moveMapToPoint(selectedPointPos);
	}

	void enterAddingPointAfterMode() {
		measurementEditingContext.setInAddPointAfterMode(true);
		moveMapToPoint(selectedPointPos);
	}

	void enterAddingPointBeforeMode() {
		measurementEditingContext.setInAddPointBeforeMode(true);
		moveMapToPoint(selectedPointPos);
	}

	private void selectPoint(double x, double y, boolean singleTap) {
		RotatedTileBox tb = view.getCurrentRotatedTileBox();
		double lowestDistance = view.getResources().getDimension(R.dimen.measurement_tool_select_radius);
		for (int i = 0; i < measurementPoints.size(); i++) {
			WptPt pt = measurementPoints.get(i);
			if (tb.containsLatLon(pt.getLatitude(), pt.getLongitude())) {
				double xDiff = tb.getPixXFromLonNoRot(pt.getLongitude()) - x;
				double yDiff = tb.getPixYFromLatNoRot(pt.getLatitude()) - y;
				double distToPoint = Math.sqrt(Math.pow(xDiff, 2) + Math.pow(yDiff, 2));
				if (distToPoint < lowestDistance) {
					lowestDistance = distToPoint;
					selectedCachedPoint = new WptPt(pt);
					selectedPointPos = i;
				}
			}
		}
		if (singleTap && singleTapListener != null) {
			singleTapListener.onSelectPoint(selectedPointPos, selectedCachedPoint);
		}
	}

	void selectPoint(int position) {
		selectedCachedPoint = new WptPt(measurementPoints.get(position));
		selectedPointPos = position;
		if (singleTapListener != null) {
			singleTapListener.onSelectPoint(selectedPointPos, selectedCachedPoint);
		}
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tb, DrawSettings settings) {
		if (inMeasurementMode) {
			lineAttrs.updatePaints(view, settings, tb);

			TrkSegment before = measurementEditingContext.getBeforeTrkSegmentLine();
			before.drawRenderers(view.getZoom(), lineAttrs.paint, canvas, tb);

			TrkSegment after = measurementEditingContext.getAfterTrkSegmentLine();
			after.drawRenderers(view.getZoom(), lineAttrs.paint, canvas, tb);
		}
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tb, DrawSettings settings) {
		if (inMeasurementMode) {
			lineAttrs.updatePaints(view, settings, tb);

			if (!measurementEditingContext.isInMovePointMode()
					&& !measurementEditingContext.isInAddPointBeforeMode()
					&& !measurementEditingContext.isInAddPointAfterMode()) {
				drawCenterIcon(canvas, tb, tb.getCenterPixelPoint(), settings.isNightMode());
				if (measureDistanceToCenterListener != null) {
					float distance = 0;
					if (measurementPoints.size() > 0) {
						WptPt lastPoint = measurementPoints.get(measurementPoints.size() - 1);
						LatLon centerLatLon = tb.getCenterLatLon();
						distance = (float) MapUtils.getDistance(lastPoint.lat, lastPoint.lon, centerLatLon.getLatitude(), centerLatLon.getLongitude());
					}
					measureDistanceToCenterListener.onMeasure(distance);
				}
			}

			List<WptPt> drawPoints;
			if (snappedToRoadPoints.size() > 0) {
				drawPoints = snappedToRoadPoints;
			} else {
				drawPoints = measurementPoints;
			}

			if (drawPoints.size() > 0) {
				path.reset();
				tx.reset();
				ty.reset();

				WptPt pt = drawPoints.get(drawPoints.size() - 1);
				int locX = tb.getPixXFromLonNoRot(pt.lon);
				int locY = tb.getPixYFromLatNoRot(pt.lat);
				path.moveTo(locX, locY);
				tx.add(locX);
				ty.add(locY);
				path.lineTo(tb.getCenterPixelX(), tb.getCenterPixelY());
				tx.add(tb.getCenterPixelX());
				ty.add(tb.getCenterPixelY());
				calculatePath(tb, tx, ty, path);
				canvas.drawPath(path, lineAttrs.paint);
			}

			overlapped = false;
			int drawn = 0;
			for (int i = 0; i < measurementPoints.size(); i++) {
				WptPt pt = measurementPoints.get(i);
				int locX = tb.getPixXFromLonNoRot(pt.lon);
				int locY = tb.getPixYFromLatNoRot(pt.lat);
				if (locX >= 0 && locX <= tb.getPixWidth() && locY >= 0 && locY <= tb.getPixHeight()) {
					if (!(measurementEditingContext.isInMovePointMode() && i == selectedPointPos)) {
						drawn++;
						if (drawn > pointsToDraw) {
							overlapped = true;
							break;
						}
					}
				}
			}
			if (overlapped) {
				WptPt pt = measurementPoints.get(0);
				int locX = tb.getPixXFromLonNoRot(pt.lon);
				int locY = tb.getPixYFromLatNoRot(pt.lat);
				if (locX >= 0 && locX <= tb.getPixWidth() && locY >= 0 && locY <= tb.getPixHeight()) {
					if (!(measurementEditingContext.isInMovePointMode() && 0 == selectedPointPos)) {
						canvas.drawBitmap(pointIcon, locX - marginPointIconX, locY - marginPointIconY, bitmapPaint);
					}
				}
				pt = measurementPoints.get(measurementPoints.size() - 1);
				locX = tb.getPixXFromLonNoRot(pt.lon);
				locY = tb.getPixYFromLatNoRot(pt.lat);
				if (locX >= 0 && locX <= tb.getPixWidth() && locY >= 0 && locY <= tb.getPixHeight()) {
					if (!(measurementEditingContext.isInMovePointMode() && measurementPoints.size() - 1 == selectedPointPos)) {
						canvas.drawBitmap(pointIcon, locX - marginPointIconX, locY - marginPointIconY, bitmapPaint);
					}
				}
			} else {
				for (int i = 0; i < measurementPoints.size(); i++) {
					WptPt pt = measurementPoints.get(i);
					int locX = tb.getPixXFromLonNoRot(pt.lon);
					int locY = tb.getPixYFromLatNoRot(pt.lat);
					if (locX >= 0 && locX <= tb.getPixWidth() && locY >= 0 && locY <= tb.getPixHeight()) {
						if (!(measurementEditingContext.isInMovePointMode() && i == selectedPointPos)) {
							canvas.drawBitmap(pointIcon, locX - marginPointIconX, locY - marginPointIconY, bitmapPaint);
						}
					}
				}
			}

			if (measurementEditingContext.isInMovePointMode()
					|| measurementEditingContext.isInAddPointBeforeMode()
					|| measurementEditingContext.isInAddPointAfterMode()) {
				int locX = tb.getCenterPixelX();
				int locY = tb.getCenterPixelY();
				canvas.drawBitmap(applyingPointIcon, locX - marginApplyingPointIconX, locY - marginApplyingPointIconY, bitmapPaint);
			}
		}
	}

	void exitMovePointMode() {
		measurementEditingContext.setInMovePointMode(false);
	}

	void exitAddPointAfterMode() {
		measurementEditingContext.setInAddPointAfterMode(false);
	}

	void exitAddPointBeforeMode() {
		measurementEditingContext.setInAddPointBeforeMode(false);
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

	public WptPt addCenterPoint(int position) {
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

	public WptPt addPoint(int position) {
		if (pressedPointLatLon != null) {
			WptPt pt = new WptPt();
			pt.lat = pressedPointLatLon.getLatitude();
			pt.lon = pressedPointLatLon.getLongitude();
			pressedPointLatLon = null;
			boolean allowed = measurementPoints.size() == 0 || !measurementPoints.get(measurementPoints.size() - 1).equals(pt);
			if (allowed) {
				measurementPoints.add(position, pt);
				moveMapToPoint(position);
				return pt;
			}
		}
		return null;
	}

	WptPt getMovedPointToApply() {
		RotatedTileBox tb = view.getCurrentRotatedTileBox();
		LatLon latLon = tb.getCenterLatLon();
		WptPt pt = measurementPoints.get(selectedPointPos);
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

		void onAddPoint();

		void onSelectPoint(int selectedPointPos, WptPt selectedCachedPoint);
	}

	interface OnEnterMovePointModeListener {
		void onEnterMovePointMode();
	}

	interface OnMeasureDistanceToCenter {
		void onMeasure(float distance);
	}
}

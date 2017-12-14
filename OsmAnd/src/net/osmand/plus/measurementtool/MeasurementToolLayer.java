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
import net.osmand.plus.views.Renderable;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

import gnu.trove.list.array.TIntArrayList;

public class MeasurementToolLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider {
	private static final int POINTS_TO_DRAW = 50;

	private OsmandMapTileView view;
	private boolean inMeasurementMode;
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
	private final Path path = new Path();
	private final TIntArrayList tx = new TIntArrayList();
	private final TIntArrayList ty = new TIntArrayList();
	private OnMeasureDistanceToCenter measureDistanceToCenterListener;
	private OnSingleTapListener singleTapListener;
	private OnEnterMovePointModeListener enterMovePointModeListener;
	private LatLon pressedPointLatLon;
	private boolean overlapped;
	private MeasurementEditingContext editingCtx;

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

	void setEditingCtx(MeasurementEditingContext editingCtx) {
		this.editingCtx = editingCtx;
	}

	void setOnEnterMovePointModeListener(OnEnterMovePointModeListener listener) {
		this.enterMovePointModeListener = listener;
	}

	void setOnMeasureDistanceToCenterListener(OnMeasureDistanceToCenter listener) {
		this.measureDistanceToCenterListener = listener;
	}

	public MeasurementEditingContext getEditingCtx() {
		return editingCtx;
	}

	public boolean isInMeasurementMode() {
		return inMeasurementMode;
	}

	void setInMeasurementMode(boolean inMeasurementMode) {
		this.inMeasurementMode = inMeasurementMode;
	}

	String getDistanceSt() {
		float dist = 0;
		List<WptPt> points = editingCtx.getBeforeTrkSegmentLine().points;
		if (points.size() > 0) {
			for (int i = 1; i < points.size(); i++) {
				dist += MapUtils.getDistance(points.get(i - 1).lat, points.get(i - 1).lon,
						points.get(i).lat, points.get(i).lon);
			}
		}
		return OsmAndFormatter.getFormattedDistance(dist, view.getApplication());
	}

	@Override
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		if (inMeasurementMode && editingCtx.getSelectedPointPosition() == -1) {
			if (!overlapped) {
				selectPoint(point.x, point.y, true);
			}
			if (editingCtx.getSelectedPointPosition() == -1) {
				pressedPointLatLon = tileBox.getLatLonFromPixel(point.x, point.y);
				if (singleTapListener != null) {
					singleTapListener.onAddPoint();
				}
			}
		}
		return false;
	}

	@Override
	public boolean onLongPressEvent(PointF point, RotatedTileBox tileBox) {
		if (inMeasurementMode) {
			if (!overlapped && getEditingCtx().getSelectedPointPosition() == -1 && editingCtx.getPointsCount() > 0) {
				selectPoint(point.x, point.y, false);
				if (editingCtx.getSelectedPointPosition() != -1) {
					enterMovingPointMode();
					if (enterMovePointModeListener != null) {
						enterMovePointModeListener.onEnterMovePointMode();
					}
				}
			}
		}
		return false;
	}

	void enterMovingPointMode() {
		moveMapToPoint(editingCtx.getSelectedPointPosition());
		WptPt pt = editingCtx.removePoint(editingCtx.getSelectedPointPosition(), false);
		editingCtx.setOriginalPointToMove(pt);
		editingCtx.splitSegments(editingCtx.getSelectedPointPosition());
	}

	private void selectPoint(double x, double y, boolean singleTap) {
		RotatedTileBox tb = view.getCurrentRotatedTileBox();
		double lowestDistance = view.getResources().getDimension(R.dimen.measurement_tool_select_radius);
		for (int i = 0; i < editingCtx.getPointsCount(); i++) {
			WptPt pt = editingCtx.getPoints().get(i);
			if (tb.containsLatLon(pt.getLatitude(), pt.getLongitude())) {
				double xDiff = tb.getPixXFromLonNoRot(pt.getLongitude()) - x;
				double yDiff = tb.getPixYFromLatNoRot(pt.getLatitude()) - y;
				double distToPoint = Math.sqrt(Math.pow(xDiff, 2) + Math.pow(yDiff, 2));
				if (distToPoint < lowestDistance) {
					lowestDistance = distToPoint;
					editingCtx.setSelectedPointPosition(i);
				}
			}
		}
		if (singleTap && singleTapListener != null) {
			singleTapListener.onSelectPoint(editingCtx.getSelectedPointPosition());
		}
	}

	void selectPoint(int position) {
		editingCtx.setSelectedPointPosition(position);
		if (singleTapListener != null) {
			singleTapListener.onSelectPoint(editingCtx.getSelectedPointPosition());
		}
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tb, DrawSettings settings) {
		if (inMeasurementMode) {
			lineAttrs.updatePaints(view, settings, tb);

			TrkSegment before = editingCtx.getBeforeTrkSegmentLine();
			before.renders.clear();
			before.renders.add(new Renderable.StandardTrack(new ArrayList<>(before.points), 17.2));
			before.drawRenderers(view.getZoom(), lineAttrs.paint, canvas, tb);

			TrkSegment after = editingCtx.getAfterTrkSegmentLine();
			after.renders.clear();
			after.renders.add(new Renderable.StandardTrack(new ArrayList<>(after.points), 17.2));
			after.drawRenderers(view.getZoom(), lineAttrs.paint, canvas, tb);
		}
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tb, DrawSettings settings) {
		if (inMeasurementMode) {
			lineAttrs.updatePaints(view, settings, tb);

			if (editingCtx.getSelectedPointPosition() == -1) {
				drawCenterIcon(canvas, tb, tb.getCenterPixelPoint(), settings.isNightMode());
				if (measureDistanceToCenterListener != null) {
					float distance = 0;
					if (editingCtx.getPointsCount() > 0) {
						WptPt lastPoint = editingCtx.getPoints().get(editingCtx.getPointsCount() - 1);
						LatLon centerLatLon = tb.getCenterLatLon();
						distance = (float) MapUtils.getDistance(lastPoint.lat, lastPoint.lon, centerLatLon.getLatitude(), centerLatLon.getLongitude());
					}
					measureDistanceToCenterListener.onMeasure(distance);
				}
			}

			TrkSegment before = editingCtx.getBeforeTrkSegmentLine();
			TrkSegment after = editingCtx.getAfterTrkSegmentLine();

			if (before.points.size() > 0 || after.points.size() > 0) {
				path.reset();
				tx.reset();
				ty.reset();

				if (before.points.size() > 0) {
					WptPt pt = before.points.get(before.points.size() - 1);
					int locX = tb.getPixXFromLonNoRot(pt.lon);
					int locY = tb.getPixYFromLatNoRot(pt.lat);
					tx.add(locX);
					ty.add(locY);
					tx.add(tb.getCenterPixelX());
					ty.add(tb.getCenterPixelY());
				}
				if (after.points.size() > 0) {
					if (before.points.size() == 0) {
						tx.add(tb.getCenterPixelX());
						ty.add(tb.getCenterPixelY());
					}
					WptPt pt = after.points.get(0);
					int locX = tb.getPixXFromLonNoRot(pt.lon);
					int locY = tb.getPixYFromLatNoRot(pt.lat);
					tx.add(locX);
					ty.add(locY);
				}

				calculatePath(tb, tx, ty, path);
				canvas.drawPath(path, lineAttrs.paint);
			}

			List<WptPt> points = new ArrayList<>();
			points.addAll(editingCtx.getBeforePoints());
			points.addAll(editingCtx.getAfterPoints());
			overlapped = false;
			int drawn = 0;
			for (int i = 0; i < points.size(); i++) {
				WptPt pt = points.get(i);
				int locX = tb.getPixXFromLonNoRot(pt.lon);
				int locY = tb.getPixYFromLatNoRot(pt.lat);
				if (locX >= 0 && locX <= tb.getPixWidth() && locY >= 0 && locY <= tb.getPixHeight()) {
					drawn++;
					if (drawn > POINTS_TO_DRAW) {
						overlapped = true;
						break;
					}
				}
			}
			if (overlapped) {
				WptPt pt = points.get(0);
				int locX = tb.getPixXFromLonNoRot(pt.lon);
				int locY = tb.getPixYFromLatNoRot(pt.lat);
				if (locX >= 0 && locX <= tb.getPixWidth() && locY >= 0 && locY <= tb.getPixHeight()) {
					canvas.drawBitmap(pointIcon, locX - marginPointIconX, locY - marginPointIconY, bitmapPaint);
				}
				pt = points.get(points.size() - 1);
				locX = tb.getPixXFromLonNoRot(pt.lon);
				locY = tb.getPixYFromLatNoRot(pt.lat);
				if (locX >= 0 && locX <= tb.getPixWidth() && locY >= 0 && locY <= tb.getPixHeight()) {
					canvas.drawBitmap(pointIcon, locX - marginPointIconX, locY - marginPointIconY, bitmapPaint);

				}
			} else {
				for (int i = 0; i < points.size(); i++) {
					WptPt pt = points.get(i);
					int locX = tb.getPixXFromLonNoRot(pt.lon);
					int locY = tb.getPixYFromLatNoRot(pt.lat);
					if (locX >= 0 && locX <= tb.getPixWidth() && locY >= 0 && locY <= tb.getPixHeight()) {
						canvas.drawBitmap(pointIcon, locX - marginPointIconX, locY - marginPointIconY, bitmapPaint);
					}
				}
			}

			if (editingCtx.getSelectedPointPosition() != -1) {
				int locX = tb.getCenterPixelX();
				int locY = tb.getCenterPixelY();
				canvas.drawBitmap(applyingPointIcon, locX - marginApplyingPointIconX, locY - marginApplyingPointIconY, bitmapPaint);
			}
		}
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

	public WptPt addCenterPoint() {
		RotatedTileBox tb = view.getCurrentRotatedTileBox();
		LatLon l = tb.getLatLonFromPixel(tb.getCenterPixelX(), tb.getCenterPixelY());
		WptPt pt = new WptPt();
		pt.lat = l.getLatitude();
		pt.lon = l.getLongitude();
		boolean allowed = editingCtx.getPointsCount() == 0 || !editingCtx.getPoints().get(editingCtx.getPointsCount() - 1).equals(pt);
		if (allowed) {
			editingCtx.addPoint(pt);
			return pt;
		}
		return null;
	}

	public WptPt addPoint() {
		if (pressedPointLatLon != null) {
			WptPt pt = new WptPt();
			double lat = pressedPointLatLon.getLatitude();
			double lon = pressedPointLatLon.getLongitude();
			pt.lat = lat;
			pt.lon = lon;
			pressedPointLatLon = null;
			boolean allowed = editingCtx.getPointsCount() == 0 || !editingCtx.getPoints().get(editingCtx.getPointsCount() - 1).equals(pt);
			if (allowed) {
				editingCtx.addPoint(pt);
				moveMapToLatLon(lat, lon);
				return pt;
			}
		}
		return null;
	}

	WptPt getMovedPointToApply() {
		RotatedTileBox tb = view.getCurrentRotatedTileBox();
		LatLon latLon = tb.getCenterLatLon();
		WptPt pt = new WptPt(editingCtx.getOriginalPointToMove());
		pt.lat = latLon.getLatitude();
		pt.lon = latLon.getLongitude();
		return pt;
	}

	private void moveMapToLatLon(double lat, double lon) {
		view.getAnimatedDraggingThread().startMoving(lat, lon, view.getZoom(), true);
	}

	public void moveMapToPoint(int pos) {
		if (editingCtx.getPointsCount() > 0) {
			if (pos >= editingCtx.getPointsCount()) {
				pos = editingCtx.getPointsCount() - 1;
			} else if (pos < 0) {
				pos = 0;
			}
			WptPt pt = editingCtx.getPoints().get(pos);
			moveMapToLatLon(pt.getLatitude(), pt.getLongitude());
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
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o, boolean unknownLocation) {

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

	@Override
	public boolean runExclusiveAction(Object o, boolean unknownLocation) {
		return false;
	}

	interface OnSingleTapListener {

		void onAddPoint();

		void onSelectPoint(int selectedPointPos);
	}

	interface OnEnterMovePointModeListener {
		void onEnterMovePointMode();
	}

	interface OnMeasureDistanceToCenter {
		void onMeasure(float distance);
	}
}

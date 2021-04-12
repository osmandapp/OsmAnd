package net.osmand.plus.measurementtool;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.measurementtool.MeasurementEditingContext.AdditionMode;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.Renderable;
import net.osmand.plus.views.layers.ContextMenuLayer;
import net.osmand.plus.views.layers.geometry.GeometryWay;
import net.osmand.plus.views.layers.geometry.MultiProfileGeometryWay;
import net.osmand.plus.views.layers.geometry.MultiProfileGeometryWayContext;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

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
	private final RenderingLineAttributes multiProfileLineAttrs = new RenderingLineAttributes("multiProfileMeasureDistanceLine");

	private MultiProfileGeometryWay multiProfileGeometry;
	private MultiProfileGeometryWayContext multiProfileGeometryWayContext;

	private int marginPointIconX;
	private int marginPointIconY;
	private int marginApplyingPointIconX;
	private int marginApplyingPointIconY;
	private final Path path = new Path();

	private final List<Float> tx = new ArrayList<>();
	private final List<Float> ty = new ArrayList<>();
	private OnMeasureDistanceToCenter measureDistanceToCenterListener;

	private OnSingleTapListener singleTapListener;
	private OnEnterMovePointModeListener enterMovePointModeListener;
	private LatLon pressedPointLatLon;
	private boolean overlapped;
	private boolean tapsDisabled;
	private MeasurementEditingContext editingCtx;

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;

		centerIconDay = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_ruler_center_day);
		centerIconNight = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_ruler_center_night);
		pointIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_measure_point_day);
		applyingPointIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_measure_point_move_day);

		float density = view.getDensity();
		multiProfileLineAttrs.isPaint_1 = false;
		multiProfileLineAttrs.paint_1.setColor(0xFFFFFFFF);
		multiProfileLineAttrs.paint_1.setStyle(Paint.Style.FILL);
		multiProfileLineAttrs.paint.setStrokeWidth(density * 14);
		multiProfileLineAttrs.paint2.setStrokeWidth(density * 10);
		multiProfileLineAttrs.isPaint3 = false;
		multiProfileLineAttrs.paint3.setStrokeWidth(density * 2);

		multiProfileGeometryWayContext = new MultiProfileGeometryWayContext(
				view.getContext(), view.getApplication().getUIUtilities(),  density);
		multiProfileGeometry = new MultiProfileGeometryWay(multiProfileGeometryWayContext);

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

	public void setTapsDisabled(boolean tapsDisabled) {
		this.tapsDisabled = tapsDisabled;
	}

	public boolean isTapsDisabled() {
		return tapsDisabled;
	}

	@Override
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		if (inMeasurementMode && !tapsDisabled && editingCtx.getSelectedPointPosition() == -1) {
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
		if (inMeasurementMode && !tapsDisabled) {
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
			lineAttrs.updatePaints(view.getApplication(), settings, tb);

			if (editingCtx.isInApproximationMode()) {
				List<List<WptPt>> originalPointsList = editingCtx.getOriginalSegmentPointsList();
				if (originalPointsList != null) {
					lineAttrs.customColorPaint.setColor(ContextCompat.getColor(view.getContext(),
							R.color.activity_background_transparent_color_dark));
					for (List<WptPt> points : originalPointsList) {
						new Renderable.StandardTrack(new ArrayList<>(points), 17.2).
								drawSegment(view.getZoom(), lineAttrs.customColorPaint, canvas, tb);
					}
				}
			}

			if (editingCtx.isInMultiProfileMode()) {
				multiProfileGeometryWayContext.updatePaints(settings.isNightMode(), multiProfileLineAttrs);
				multiProfileGeometry.updateRoute(tb, editingCtx.getRoadSegmentData(), editingCtx.getBeforeSegments(), editingCtx.getAfterSegments());
				multiProfileGeometry.drawSegments(canvas, tb);
			} else {
				multiProfileGeometry.clearWay();
				List<TrkSegment> before = editingCtx.getBeforeTrkSegmentLine();
				for (TrkSegment segment : before) {
					new Renderable.StandardTrack(new ArrayList<>(segment.points), 17.2).
							drawSegment(view.getZoom(), lineAttrs.paint, canvas, tb);
				}

				List<TrkSegment> after = editingCtx.getAfterTrkSegmentLine();
				for (TrkSegment segment : after) {
					new Renderable.StandardTrack(new ArrayList<>(segment.points), 17.2).
							drawSegment(view.getZoom(), lineAttrs.paint, canvas, tb);
				}
			}

			drawPoints(canvas, tb);
		}
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tb, DrawSettings settings) {
		if (inMeasurementMode) {
			lineAttrs.updatePaints(view.getApplication(), settings, tb);
			if (!editingCtx.isInApproximationMode()) {
				drawBeforeAfterPath(canvas, tb);
			}

			if (editingCtx.getSelectedPointPosition() == -1) {
				drawCenterIcon(canvas, tb, settings.isNightMode());
				if (measureDistanceToCenterListener != null) {
					float distance = 0;
					float bearing = 0;
					if (editingCtx.getPointsCount() > 0) {
						WptPt lastPoint = editingCtx.getPoints().get(editingCtx.getPointsCount() - 1);
						LatLon centerLatLon = tb.getCenterLatLon();
						distance = (float) MapUtils.getDistance(
								lastPoint.lat, lastPoint.lon, centerLatLon.getLatitude(), centerLatLon.getLongitude());
						bearing = getLocationFromLL(lastPoint.lat, lastPoint.lon)
								.bearingTo(getLocationFromLL(centerLatLon.getLatitude(), centerLatLon.getLongitude()));
					}
					measureDistanceToCenterListener.onMeasure(distance, bearing);
				}
			}

			List<WptPt> beforePoints = editingCtx.getBeforePoints();
			List<WptPt> afterPoints = editingCtx.getAfterPoints();
			if (beforePoints.size() > 0) {
				drawPointIcon(canvas, tb, beforePoints.get(beforePoints.size() - 1), true);
			}
			if (afterPoints.size() > 0) {
				drawPointIcon(canvas, tb, afterPoints.get(0), true);
			}

			if (editingCtx.getSelectedPointPosition() != -1) {
				canvas.rotate(-tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
				int locX = tb.getCenterPixelX();
				int locY = tb.getCenterPixelY();
				canvas.drawBitmap(applyingPointIcon, locX - marginApplyingPointIconX, locY - marginApplyingPointIconY, bitmapPaint);
				canvas.rotate(tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
			}
		}
	}

	private boolean isInTileBox(RotatedTileBox tb, WptPt point) {
		QuadRect latLonBounds = tb.getLatLonBounds();
		return point.getLatitude() >= latLonBounds.bottom && point.getLatitude() <= latLonBounds.top
				&& point.getLongitude() >= latLonBounds.left && point.getLongitude() <= latLonBounds.right;
	}

	private void drawPoints(Canvas canvas, RotatedTileBox tb) {
		canvas.rotate(-tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());

		WptPt lastBeforePoint = null;
		List<WptPt> points = new ArrayList<>(editingCtx.getBeforePoints());
		if (points.size() > 0) {
			lastBeforePoint = points.get(points.size() - 1);
		}
		WptPt firstAfterPoint = null;
		List<WptPt> afterPoints = editingCtx.getAfterPoints();
		if (afterPoints.size() > 0) {
			firstAfterPoint = afterPoints.get(0);
		}
		points.addAll(afterPoints);
		overlapped = false;
		int drawn = 0;
		for (int i = 0; i < points.size(); i++) {
			WptPt pt = points.get(i);
			if (tb.containsLatLon(pt.lat, pt.lon)) {
				drawn++;
				if (drawn > POINTS_TO_DRAW) {
					overlapped = true;
					break;
				}
			}
		}
		if (overlapped) {
			WptPt pt = points.get(0);
			drawPointIcon(canvas, tb, pt, false);
			pt = points.get(points.size() - 1);
			drawPointIcon(canvas, tb, pt, false);
		} else {
			for (int i = 0; i < points.size(); i++) {
				WptPt pt = points.get(i);
				drawPointIcon(canvas, tb, pt, false);
			}
		}

		canvas.rotate(tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
	}

	private void drawBeforeAfterPath(Canvas canvas, RotatedTileBox tb) {
		List<TrkSegment> before = editingCtx.getBeforeSegments();
		List<TrkSegment> after = editingCtx.getAfterSegments();
		if (before.size() > 0 || after.size() > 0) {
			path.reset();
			tx.clear();
			ty.clear();

			boolean hasPointsBefore = false;
			boolean hasGapBefore = false;
			if (before.size() > 0) {
				TrkSegment segment = before.get(before.size() - 1);
				if (segment.points.size() > 0) {
					hasPointsBefore = true;
					WptPt pt = segment.points.get(segment.points.size() - 1);
					hasGapBefore = pt.isGap();
					if (!pt.isGap() || (editingCtx.isInAddPointMode() && !editingCtx.isInAddPointBeforeMode())) {
						float locX = tb.getPixXFromLatLon(pt.lat, pt.lon);
						float locY = tb.getPixYFromLatLon(pt.lat, pt.lon);
						tx.add(locX);
						ty.add(locY);
					}
					tx.add((float) tb.getCenterPixelX());
					ty.add((float) tb.getCenterPixelY());
				}
			}
			if (after.size() > 0) {
				TrkSegment segment = after.get(0);
				if (segment.points.size() > 0) {
					if (!hasPointsBefore) {
						tx.add((float) tb.getCenterPixelX());
						ty.add((float) tb.getCenterPixelY());
					}
					if (!hasGapBefore || (editingCtx.isInAddPointMode() && editingCtx.isInAddPointBeforeMode())) {
						WptPt pt = segment.points.get(0);
						float locX = tb.getPixXFromLatLon(pt.lat, pt.lon);
						float locY = tb.getPixYFromLatLon(pt.lat, pt.lon);
						tx.add(locX);
						ty.add(locY);
					}
				}
			}

			if (!tx.isEmpty() && !ty.isEmpty()) {
				GeometryWay.calculatePath(tb, tx, ty, path);
				canvas.drawPath(path, lineAttrs.paint);
			}
		}
	}

	private void drawCenterIcon(Canvas canvas, RotatedTileBox tb, boolean nightMode) {
		canvas.rotate(-tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
		Bitmap centerBmp = nightMode ? centerIconNight : centerIconDay;
		canvas.drawBitmap(centerBmp, tb.getCenterPixelX() - centerBmp.getWidth() / 2f,
				tb.getCenterPixelY() - centerBmp.getHeight() / 2f, bitmapPaint);
		canvas.rotate(tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
	}

	private void drawPointIcon(Canvas canvas, RotatedTileBox tb, WptPt pt, boolean rotate) {
		if (rotate) {
			canvas.rotate(-tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
		}
		float locX = tb.getPixXFromLatLon(pt.lat, pt.lon);
		float locY = tb.getPixYFromLatLon(pt.lat, pt.lon);
		if (editingCtx.isInMultiProfileMode()) {
			canvas.drawBitmap(multiProfileGeometryWayContext.getPointIcon(), locX - multiProfileGeometryWayContext.pointIconSize / 2,
					locY - multiProfileGeometryWayContext.pointIconSize / 2, bitmapPaint);
		} else {
			if (tb.containsPoint(locX, locY, 0)) {
				canvas.drawBitmap(pointIcon, locX - marginPointIconX, locY - marginPointIconY, bitmapPaint);
			}
		}
		if (rotate) {
			canvas.rotate(tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
		}
	}

	public WptPt addCenterPoint(boolean addPointBefore) {
		RotatedTileBox tb = view.getCurrentRotatedTileBox();
		LatLon l = tb.getCenterLatLon();
		WptPt pt = new WptPt();
		pt.lat = l.getLatitude();
		pt.lon = l.getLongitude();
		boolean allowed = editingCtx.getPointsCount() == 0 || !editingCtx.getPoints().get(editingCtx.getPointsCount() - 1).equals(pt);
		if (allowed) {
			editingCtx.addPoint(pt, addPointBefore ? AdditionMode.ADD_BEFORE : AdditionMode.ADD_AFTER);
			return pt;
		}
		return null;
	}

	public WptPt addPoint(boolean addPointBefore) {
		if (pressedPointLatLon != null) {
			WptPt pt = new WptPt();
			double lat = pressedPointLatLon.getLatitude();
			double lon = pressedPointLatLon.getLongitude();
			pt.lat = lat;
			pt.lon = lon;
			pressedPointLatLon = null;
			boolean allowed = editingCtx.getPointsCount() == 0 || !editingCtx.getPoints().get(editingCtx.getPointsCount() - 1).equals(pt);
			if (allowed) {
				editingCtx.addPoint(pt, addPointBefore ? AdditionMode.ADD_BEFORE : AdditionMode.ADD_AFTER);
				moveMapToLatLon(lat, lon);
				return pt;
			}
		}
		return null;
	}

	WptPt getMovedPointToApply() {
		RotatedTileBox tb = view.getCurrentRotatedTileBox();
		LatLon latLon = tb.getCenterLatLon();
		WptPt originalPoint = editingCtx.getOriginalPointToMove();
		WptPt point = new WptPt(originalPoint);
		point.lat = latLon.getLatitude();
		point.lon = latLon.getLongitude();
		point.copyExtensions(originalPoint);
		return point;
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
	public boolean disableLongPressOnMap(PointF point, RotatedTileBox tileBox) {
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

	@Override
	public boolean showMenuAction(@Nullable Object o) {
		return false;
	}

	private Location getLocationFromLL(double lat, double lon) {
		Location l = new Location("");
		l.setLatitude(lat);
		l.setLongitude(lon);
		return l;
	}

	interface OnSingleTapListener {

		void onAddPoint();

		void onSelectPoint(int selectedPointPos);
	}

	interface OnEnterMovePointModeListener {
		void onEnterMovePointMode();
	}

	interface OnMeasureDistanceToCenter {
		void onMeasure(float distance, float bearing);
	}
}
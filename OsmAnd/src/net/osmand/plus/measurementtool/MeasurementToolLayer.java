package net.osmand.plus.measurementtool;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ChartPointsHelper;
import net.osmand.plus.R;
import net.osmand.plus.mapcontextmenu.other.TrackChartPoints;
import net.osmand.plus.measurementtool.MeasurementEditingContext.AdditionMode;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.Renderable;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.geometry.GeometryWay;
import net.osmand.plus.views.layers.geometry.MultiProfileGeometryWay;
import net.osmand.plus.views.layers.geometry.MultiProfileGeometryWayContext;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MeasurementToolLayer extends OsmandMapLayer implements IContextMenuProvider {

	private static final int START_ZOOM = 8;
	private static final int MIN_POINTS_PERCENTILE = 5;

	private OsmandMapTileView view;
	private boolean inMeasurementMode;

	private Bitmap centerIconDay;
	private Bitmap centerIconNight;
	private Bitmap pointIcon;
	private Bitmap applyingPointIcon;
	private Paint bitmapPaint;
	private final RenderingLineAttributes lineAttrs = new RenderingLineAttributes("measureDistanceLine");

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
	private boolean tapsDisabled;
	private MeasurementEditingContext editingCtx;

	private Integer pointsStartZoom = null;
	private TrackChartPoints trackChartPoints;
	private ChartPointsHelper chartPointsHelper;

	private final Path multiProfilePath = new Path();
	private final PathMeasure multiProfilePathMeasure = new PathMeasure(multiProfilePath, false);

	public MeasurementToolLayer(@NonNull Context ctx) {
		super(ctx);
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		this.view = view;
		this.chartPointsHelper = new ChartPointsHelper(view.getContext());

		centerIconDay = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_ruler_center_day);
		centerIconNight = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_ruler_center_night);
		pointIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_measure_point_day);
		applyingPointIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_measure_point_move_day);

		multiProfileGeometryWayContext = new MultiProfileGeometryWayContext(view.getContext(),
				view.getApplication().getUIUtilities(), view.getDensity());
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

	public void setTrackChartPoints(TrackChartPoints trackChartPoints) {
		this.trackChartPoints = trackChartPoints;
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
			int startZoom = getPointsStartZoom();
			boolean pointSelected = tileBox.getZoom() >= startZoom && selectPoint(point.x, point.y, true);
			boolean profileIconSelected = !pointSelected && selectPointForAppModeChange(point, tileBox);
			if (!pointSelected && !profileIconSelected) {
				pressedPointLatLon = tileBox.getLatLonFromPixel(point.x, point.y);
				if (singleTapListener != null) {
					singleTapListener.onAddPoint();
				}
			}
		}
		return false;
	}

	private boolean selectPointForAppModeChange(PointF point, RotatedTileBox tileBox) {
		int pointIdx = getPointIdxByProfileIconOnMap(point, tileBox);
		if (pointIdx != -1 && singleTapListener != null) {
			editingCtx.setSelectedPointPosition(pointIdx);
			singleTapListener.onSelectProfileIcon(pointIdx);
			return true;
		}
		return false;
	}

	private int getPointIdxByProfileIconOnMap(PointF point, RotatedTileBox tileBox) {
		multiProfilePath.reset();
		Map<Pair<WptPt, WptPt>, RoadSegmentData> roadSegmentData = editingCtx.getRoadSegmentData();
		List<WptPt> points = editingCtx.getPoints();

		double minDist = view.getResources().getDimension(R.dimen.measurement_tool_select_radius);
		int indexOfMinDist = -1;
		for (int i = 0; i < points.size() - 1; i++) {
			WptPt currentPoint = points.get(i);
			WptPt nextPoint = points.get(i + 1);
			if (currentPoint.isGap()) {
				continue;
			}

			List<LatLon> routeBetweenPoints = MultiProfileGeometryWay.getRoutePoints(
					currentPoint, nextPoint, roadSegmentData);
			PointF profileIconPos = MultiProfileGeometryWay.getIconCenter(tileBox, routeBetweenPoints,
					path, multiProfilePathMeasure);
			if (profileIconPos != null && tileBox.containsPoint(profileIconPos.x, profileIconPos.y, 0)) {
				double dist = MapUtils.getSqrtDistance(point.x, point.y, profileIconPos.x, profileIconPos.y);
				if (dist < minDist) {
					indexOfMinDist = i;
					minDist = dist;
				}
			}
		}

		return indexOfMinDist;
	}

	@Override
	public boolean onLongPressEvent(PointF point, RotatedTileBox tileBox) {
		if (inMeasurementMode && !tapsDisabled) {
			int startZoom = getPointsStartZoom();
			if (tileBox.getZoom() >= startZoom
					&& editingCtx.getSelectedPointPosition() == -1
					&& editingCtx.getPointsCount() > 0) {
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

	private boolean selectPoint(float x, float y, boolean singleTap) {
		RotatedTileBox tb = view.getCurrentRotatedTileBox();
		double lowestDistance = view.getResources().getDimension(R.dimen.measurement_tool_select_radius);
		for (int i = 0; i < editingCtx.getPointsCount(); i++) {
			WptPt pt = editingCtx.getPoints().get(i);
			if (tb.containsLatLon(pt.getLatitude(), pt.getLongitude())) {
				float ptX = tb.getPixXFromLatLon(pt.lat, pt.lon);
				float ptY = tb.getPixYFromLatLon(pt.lat, pt.lon);
				double distToPoint = MapUtils.getSqrtDistance(x, y, ptX, ptY);
				if (distToPoint < lowestDistance) {
					lowestDistance = distToPoint;
					editingCtx.setSelectedPointPosition(i);
				}
			}
		}
		if (singleTap && singleTapListener != null) {
			singleTapListener.onSelectPoint(editingCtx.getSelectedPointPosition());
		}
		return editingCtx.getSelectedPointPosition() != -1;
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
				multiProfileGeometryWayContext.setNightMode(settings.isNightMode());
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

			canvas.rotate(-tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
			drawPoints(canvas, tb);
			if (trackChartPoints != null) {
				drawTrackChartPoints(trackChartPoints, canvas, tb);
			}
			canvas.rotate(tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
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

	private void drawTrackChartPoints(@NonNull TrackChartPoints trackChartPoints, Canvas canvas, RotatedTileBox tileBox) {
		LatLon highlightedPoint = trackChartPoints.getHighlightedPoint();
		if (highlightedPoint != null) {
			chartPointsHelper.drawHighlightedPoint(highlightedPoint, canvas, tileBox);
		}

		List<LatLon> xAxisPoint = trackChartPoints.getXAxisPoints();
		if (!Algorithms.isEmpty(xAxisPoint)) {
			chartPointsHelper.drawXAxisPoints(xAxisPoint, lineAttrs.defaultColor, canvas, tileBox);
		}
	}

	private void drawPoints(@NonNull Canvas canvas, @NonNull RotatedTileBox tileBox) {
		List<WptPt> points = new ArrayList<>(editingCtx.getBeforePoints());
		points.addAll(editingCtx.getAfterPoints());

		if (pointsStartZoom == null && points.size() > 100) {
			double percentile = getPointsDensity();
			pointsStartZoom = getStartZoom(percentile);
		}
		int startZoom = getPointsStartZoom();
		if (tileBox.getZoom() >= startZoom) {
			for (int i = 0; i < points.size(); i++) {
				WptPt point = points.get(i);
				if (isInTileBox(tileBox, point)) {
					drawPointIcon(canvas, tileBox, point, false);
				}
			}
		}
	}

	private double getPointsDensity() {
		List<WptPt> points = new ArrayList<>(editingCtx.getBeforePoints());
		points.addAll(editingCtx.getAfterPoints());

		List<Double> distances = new ArrayList<>();
		WptPt prev = null;
		for (WptPt wptPt : points) {
			if (prev != null) {
				double dist = MapUtils.getDistance(wptPt.lat, wptPt.lon, prev.lat, prev.lon);
				distances.add(dist);
			}
			prev = wptPt;
		}
		Collections.sort(distances);
		return Algorithms.getPercentile(distances, MIN_POINTS_PERCENTILE);
	}

	private int getPointsStartZoom() {
		return pointsStartZoom != null ? pointsStartZoom : START_ZOOM;
	}

	private int getStartZoom(double density) {
		if (density < 2) {
			return 21;
		} else if (density < 5) {
			return 20;
		} else if (density < 10) {
			return 19;
		} else if (density < 20) {
			return 18;
		} else if (density < 50) {
			return 17;
		} else if (density < 100) {
			return 16;
		} else if (density < 200) {
			return 15;
		} else if (density < 500) {
			return 14;
		} else if (density < 1000) {
			return 13;
		} else if (density < 2000) {
			return 11;
		} else if (density < 5000) {
			return 10;
		} else if (density < 10000) {
			return 9;
		}
		return START_ZOOM;
	}

	private void drawBeforeAfterPath(Canvas canvas, RotatedTileBox tb) {
		canvas.rotate(-tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
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
			canvas.rotate(tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
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
		if (tb.containsPoint(locX, locY, 0)) {
			canvas.drawBitmap(pointIcon, locX - marginPointIconX, locY - marginPointIconY, bitmapPaint);
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
		pointsStartZoom = null;
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

		void onSelectProfileIcon(int startPointPos);
	}

	interface OnEnterMovePointModeListener {
		void onEnterMovePointMode();
	}

	interface OnMeasureDistanceToCenter {
		void onMeasure(float distance, float bearing);
	}
}
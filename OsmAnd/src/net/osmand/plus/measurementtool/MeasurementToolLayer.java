package net.osmand.plus.measurementtool;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Pair;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.measurementtool.MeasurementEditingContext.AdditionMode;
import net.osmand.plus.profiles.ProfileIconColors;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.Renderable;
import net.osmand.plus.views.layers.ContextMenuLayer;
import net.osmand.plus.views.layers.geometry.GeometryWay;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

public class MeasurementToolLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider {
	private static final int POINTS_TO_DRAW = 50;

	private OsmandMapTileView view;
	private boolean inMeasurementMode;
	private boolean multipleProfileMode = false;
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
	private final List<Float> tx = new ArrayList<>();
	private final List<Float> ty = new ArrayList<>();
	private OnMeasureDistanceToCenter measureDistanceToCenterListener;
	private OnSingleTapListener singleTapListener;
	private OnEnterMovePointModeListener enterMovePointModeListener;
	private LatLon pressedPointLatLon;
	private boolean overlapped;
	private boolean tapsDisabled;
	private MeasurementEditingContext editingCtx;
	private UiUtilities iconsCache;
	private Map<String, Bitmap> profileIconsBitmapCache;

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

		iconsCache = new UiUtilities(view.getApplication());
		profileIconsBitmapCache = new HashMap<>();
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
			multipleProfileMode = editingCtx.hasMultipleProfiles();

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

			drawSegments(canvas, tb, settings, editingCtx.getBeforeTrkSegmentLine());
			drawSegments(canvas, tb, settings, editingCtx.getAfterTrkSegmentLine());
			drawPoints(canvas, tb);
		}
	}

	private void drawSegments(Canvas canvas, RotatedTileBox tb, DrawSettings settings, List<TrkSegment> segments) {
		for (int i = 0; i < segments.size(); i++) {
			List<WptPt> points = segments.get(i).points;
			Renderable.RenderableSegment renderable;
			if (multipleProfileMode) {
				Map<String, Pair<Integer, Bitmap>> profileValues = getProfileValues(settings.isNightMode());
				DisplayMetrics displayMetrics = view.getContext().getResources().getDisplayMetrics();
				renderable = new Renderable.MultiProfileTrack(new ArrayList<>(points),
						profileValues, displayMetrics, 17.2);
			} else {
				renderable = new Renderable.StandardTrack(new ArrayList<>(points), 17.2);
			}
			renderable.drawSegment(view.getZoom(), lineAttrs.paint, canvas, tb);
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
			if (pt != lastBeforePoint && pt != firstAfterPoint && isInTileBox(tb, pt)) {
				drawPointIcon(canvas, tb, pt, false);
			}
		} else {
			for (int i = 0; i < points.size(); i++) {
				WptPt pt = points.get(i);
				if (pt != lastBeforePoint && pt != firstAfterPoint && isInTileBox(tb, pt)) {
					drawPointIcon(canvas, tb, pt, false);
				}
			}
		}

		canvas.rotate(tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
	}

	// Will be replaced with icon in Bitmap
	private void drawMultiProfilePointIcon(Canvas canvas, float centerX, float centerY) {
		Context ctx = view.getContext();
		Path path = new Path();
		Paint paint = new Paint();
		paint.setStyle(Paint.Style.FILL);
		path.addCircle(centerX, centerY, AndroidUtils.dpToPx(ctx, 12.5f), Path.Direction.CW);
		paint.setColor(Color.BLACK);
		canvas.drawPath(path, paint);
		path.reset();
		path.addCircle(centerX, centerY, AndroidUtils.dpToPx(ctx, 12), Path.Direction.CW);
		paint.setColor(Color.WHITE);
		canvas.drawPath(path, paint);
		path.reset();
		path.addCircle(centerX, centerY, AndroidUtils.dpToPx(ctx, 8), Path.Direction.CW);
		paint.setColor(Algorithms.parseColor("#637EFB"));
		canvas.drawPath(path, paint);
	}

	private Map<String, Pair<Integer, Bitmap>> getProfileValues(boolean night) {
		Map<String, Pair<Integer, Bitmap>> profileColors = new HashMap<>();
		int splitColor = ContextCompat.getColor(view.getContext(), ProfileIconColors.DARK_YELLOW.getColor(night));
		profileColors.put(ApplicationMode.DEFAULT.getStringKey(),
				new Pair<>(splitColor, getSizedBitmap(ApplicationMode.DEFAULT.getStringKey(),
						R.drawable.ic_action_split_interval, splitColor, 28)));
		List<ApplicationMode> modes = new ArrayList<>(ApplicationMode.values(view.getApplication()));
		modes.remove(ApplicationMode.DEFAULT);
		for (ApplicationMode mode : modes) {
			if (!"public_transport".equals(mode.getRoutingProfile())) {
				int color = mode.getProfileColor(night);
				int iconSize = mode.getStringKey().equals(ApplicationMode.BICYCLE.getStringKey()) ? 24 : 28;
				profileColors.put(mode.getStringKey(),
						new Pair<>(color, getSizedBitmap(mode.getStringKey(), mode.getIconRes(), color, iconSize)));
			}
		}
		return profileColors;
	}

	private Bitmap getSizedBitmap(String modeKey, @DrawableRes int iconRes, @ColorInt int color, int dp) {
		String key = modeKey + color;
		if (!profileIconsBitmapCache.containsKey(key)) {
			Drawable drawable = iconsCache.getPaintedIcon(iconRes, color);
			Bitmap bitmap = AndroidUtils.createScaledBitmap(view.getContext(), drawable, dp, dp, false);
			profileIconsBitmapCache.put(key, bitmap);
		}
		return profileIconsBitmapCache.get(key);
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
		if (tb.containsPoint(locX, locY, 0)) {
			if (multipleProfileMode) {
				drawMultiProfilePointIcon(canvas, locX, locY);
			} else {
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

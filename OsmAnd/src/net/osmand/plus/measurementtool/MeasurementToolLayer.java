package net.osmand.plus.measurementtool;

import android.content.Context;
import android.graphics.*;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.Location;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.MapMarkerBuilder;
import net.osmand.core.jni.MapMarkersCollection;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.Utilities;
import net.osmand.data.DataTileManager;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ChartPointsHelper;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.charts.TrackChartPoints;
import net.osmand.plus.measurementtool.MeasurementEditingContext.AdditionMode;
import net.osmand.plus.render.OsmandDashPathEffect;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.Renderable.RenderableSegment;
import net.osmand.plus.views.Renderable.StandardTrack;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.layers.core.LocationPointsTileProvider;
import net.osmand.plus.views.layers.core.TilePointsProvider;
import net.osmand.plus.views.layers.geometry.GeometryWayPathAlgorithms;
import net.osmand.plus.views.layers.geometry.GpxGeometryWay;
import net.osmand.plus.views.layers.geometry.GpxGeometryWayContext;
import net.osmand.plus.views.layers.geometry.MultiProfileGeometryWay;
import net.osmand.plus.views.layers.geometry.MultiProfileGeometryWayContext;
import net.osmand.shared.gpx.GpxUtilities;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.shared.routing.ColoringType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MeasurementToolLayer extends OsmandMapLayer implements IContextMenuProvider {

	private static final int START_ZOOM = 8;
	private static final int MIN_POINTS_PERCENTILE = 20;
	private static final int MAX_VISIBLE_POINTS = 30;
	// roughly 10 points per tile
	private static final double MIN_DISTANCE_TO_SHOW_REF_ZOOM = MapUtils.getTileDistanceWidth(START_ZOOM) / 10;

	private boolean inMeasurementMode;

	private Bitmap centerIconDay;
	private Bitmap centerIconNight;
	private Bitmap pointIcon;
	private Bitmap oldMovedPointIcon;
	private Bitmap applyingPointIcon;
	private Paint bitmapPaint;
	private final RenderingLineAttributes lineAttrs = new RenderingLineAttributes("measureDistanceLine");

	private MultiProfileGeometryWay multiProfileGeometry;
	private MultiProfileGeometryWayContext multiProfileGeometryWayContext;
	private GpxGeometryWayContext wayContext;
	private List<RenderableSegment> segmentsRenderablesCached = new ArrayList<>();
	private List<RenderableSegment> approximationRenderablesCached = new ArrayList<>();
	private int beforePointsCountCached;
	private int afterPointsCountCached;
	private int originalPointsCountCached;
	private MapMarkersCollection activePointsCollection;
	private net.osmand.core.jni.MapMarker centerPointMarker;
	private net.osmand.core.jni.MapMarker beforePointMarker;
	private net.osmand.core.jni.MapMarker afterPointMarker;
	private net.osmand.core.jni.MapMarker selectedPointMarker;
	private LocationPointsTileProvider trackChartPointsProvider;
	private MapMarkersCollection highlightedPointCollection;
	private net.osmand.core.jni.MapMarker highlightedPointMarker;
	private Bitmap highlightedPointImage;
	private TilePointsProvider<WptCollectionPoint> pointsProvider;

	private int marginPointIconX;
	private int marginPointIconY;
	private int marginApplyingPointIconX;
	private int marginApplyingPointIconY;
	private final Path path = new Path();

	private final List<Float> tx = new ArrayList<>();
	private final List<Float> ty = new ArrayList<>();
	private List<WptPt> beforeAfterWpt = new ArrayList<>();
	private RenderableSegment beforeAfterRenderer;
	private OnMeasureDistanceToCenter measureDistanceToCenterListener;

	private OnSingleTapListener singleTapListener;
	private OnEnterMovePointModeListener enterMovePointModeListener;
	private LatLon pressedPointLatLon;
	private boolean tapsDisabled;
	private MeasurementEditingContext editingCtx;

	private boolean showPointsMinZoom;
	private int showPointsZoomCache;
	private boolean oldMovedPointRedraw;

	private TrackChartPoints trackChartPoints;
	private List<LatLon> xAxisPointsCached = new ArrayList<>();
	private ChartPointsHelper chartPointsHelper;

	private final Path multiProfilePath = new Path();
	private final PathMeasure multiProfilePathMeasure = new PathMeasure(multiProfilePath, false);

	private boolean forceUpdateBufferImage;
	private boolean forceUpdateOnDraw;

	public MeasurementToolLayer(@NonNull Context ctx) {
		super(ctx);
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);

		this.chartPointsHelper = new ChartPointsHelper(getContext());

		createResources(view);
	}

	private void createResources(@NonNull OsmandMapTileView view) {
		centerIconDay = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_ruler_center_day);
		centerIconNight = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_ruler_center_night);
		pointIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_measure_point_day);
		oldMovedPointIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_measure_point_day_disable);
		applyingPointIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_measure_point_move_day);
		highlightedPointImage = chartPointsHelper.createHighlightedPointBitmap();

		multiProfileGeometryWayContext = new MultiProfileGeometryWayContext(getContext(),
				view.getApplication().getUIUtilities(), view.getDensity());
		multiProfileGeometry = new MultiProfileGeometryWay(multiProfileGeometryWayContext);
		multiProfileGeometry.baseOrder = getBaseOrder() - 10;
		wayContext = new GpxGeometryWayContext(getContext(), view.getDensity());

		bitmapPaint = new Paint();
		bitmapPaint.setAntiAlias(true);
		bitmapPaint.setDither(true);
		bitmapPaint.setFilterBitmap(true);

		marginPointIconY = pointIcon.getHeight() / 2;
		marginPointIconX = pointIcon.getWidth() / 2;

		marginApplyingPointIconY = applyingPointIcon.getHeight() / 2;
		marginApplyingPointIconX = applyingPointIcon.getWidth() / 2;
	}

	@Override
	protected void updateResources() {
		super.updateResources();
		createResources(view);
	}

	@Override
	public void setMapActivity(@Nullable MapActivity mapActivity) {
		super.setMapActivity(mapActivity);
		forceUpdateOnDraw |= mapActivityInvalidated;
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
	public boolean onSingleTap(@NonNull PointF point, @NonNull RotatedTileBox tileBox) {
		if (inMeasurementMode && !tapsDisabled && editingCtx.getSelectedPointPosition() == -1) {
			boolean pointSelected = showPointsMinZoom && selectPoint(point.x, point.y, true);
			boolean profileIconSelected = !pointSelected && selectPointForAppModeChange(point, tileBox);
			if (!pointSelected && !profileIconSelected) {
				pressedPointLatLon = NativeUtilities.getLatLonFromElevatedPixel(getMapRenderer(), tileBox, point);
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
	public boolean onLongPressEvent(@NonNull PointF point, @NonNull RotatedTileBox tileBox) {
		if (inMeasurementMode && !tapsDisabled) {
			if (showPointsMinZoom
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
		oldMovedPointRedraw = true;
		editingCtx.setOriginalPointToMove(pt);
		editingCtx.splitSegments(editingCtx.getSelectedPointPosition());
	}

	private boolean selectPoint(float x, float y, boolean singleTap) {
		RotatedTileBox tb = view.getCurrentRotatedTileBox();
		double lowestDistance = view.getResources().getDimension(R.dimen.measurement_tool_select_radius);
		for (int i = 0; i < editingCtx.getPointsCount(); i++) {
			WptPt pt = editingCtx.getPoints().get(i);
			MapRendererView mapRenderer = getMapRenderer();
			if (mapRenderer != null) {
				PointI point31 = Utilities.convertLatLonTo31(new net.osmand.core.jni.LatLon(pt.getLat(), pt.getLon()));
				if (mapRenderer.isPositionVisible(point31)) {
					PointF pixel = NativeUtilities.getElevatedPixelFromLatLon(mapRenderer, tb, pt.getLat(), pt.getLon());
					double distToPoint = MapUtils.getSqrtDistance(x, y, pixel.x, pixel.y);
					if (distToPoint < lowestDistance) {
						lowestDistance = distToPoint;
						editingCtx.setSelectedPointPosition(i);
					}
				}
			} else {
				if (tb.containsLatLon(pt.getLatitude(), pt.getLongitude())) {
					float ptX = tb.getPixXFromLatLon(pt.getLat(), pt.getLon());
					float ptY = tb.getPixYFromLatLon(pt.getLat(), pt.getLon());
					double distToPoint = MapUtils.getSqrtDistance(x, y, ptX, ptY);
					if (distToPoint < lowestDistance) {
						lowestDistance = distToPoint;
						editingCtx.setSelectedPointPosition(i);
					}
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
		super.onPrepareBufferImage(canvas, tb, settings);
		boolean hasMapRenderer = hasMapRenderer();
		boolean mapRendererChanged = hasMapRenderer && this.mapRendererChanged;
		if (isDrawingEnabled()) {
			boolean updated = lineAttrs.updatePaints(view.getApplication(), settings, tb) || forceUpdateBufferImage || mapActivityInvalidated || mapRendererChanged;
			if (mapRendererChanged) {
				this.mapRendererChanged = false;
			}
			if (editingCtx.isInApproximationMode()) {
				drawApproximatedLines(canvas, tb, updated);
			}
			if (editingCtx.isInMultiProfileMode()) {
				if (hasMapRenderer) {
					clearCachedSegmentsPointsCounters();
					clearCachedSegmentsRenderables();
				}
				boolean changed = multiProfileGeometryWayContext.setNightMode(settings.isNightMode());
				changed |= multiProfileGeometry.updateRoute(tb, editingCtx.getRoadSegmentData(),
						editingCtx.getBeforeSegments(), editingCtx.getAfterSegments());
				changed |= mapActivityInvalidated;
				if (hasMapRenderer) {
					if (changed) {
						multiProfileGeometry.resetSymbolProviders();
						multiProfileGeometry.drawSegments(canvas, tb);
					}
				} else {
					multiProfileGeometry.drawSegments(canvas, tb);
				}
			} else {
				multiProfileGeometry.clearWay();
				drawSegmentLines(canvas, tb, updated);
			}

			canvas.rotate(-tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
			drawPoints(canvas, tb);
			if (hasMapRenderer) {
				if (updated) {
					recreateHighlightedPointCollection();
				}
				drawTrackChartPointsOpenGl(trackChartPoints, getMapRenderer(), tb);
			} else {
				drawTrackChartPoints(trackChartPoints, canvas, tb);
			}
			canvas.rotate(tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
		} else if (hasMapRenderer) {
			clearCachedCounters();
			clearCachedRenderables();
			clearPointsProvider();
			clearXAxisPoints();
			setHighlightedPointMarkerVisibility(false);
			multiProfileGeometry.clearWay();
		}
		mapActivityInvalidated = false;
		forceUpdateBufferImage = false;
	}

	@Nullable
	private float[] getDashPattern(@NonNull Paint paint) {
		float[] intervals = null;
		PathEffect pathEffect = paint.getPathEffect();
		if (pathEffect instanceof OsmandDashPathEffect) {
			intervals = ((OsmandDashPathEffect) pathEffect).getIntervals();
		}
		return intervals;
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tb, DrawSettings settings) {
		boolean hasMapRenderer = hasMapRenderer();
		if (isDrawingEnabled()) {
			boolean updated = lineAttrs.updatePaints(view.getApplication(), settings, tb) || forceUpdateOnDraw;
			if (!editingCtx.isInApproximationMode()) {
				drawBeforeAfterPath(canvas, tb, updated);
			} else {
				resetBeforeAfterRenderer();
			}
			if (hasMapRenderer) {
				if (updated || activePointsCollection == null) {
					recreateActivePointsCollection(settings.isNightMode());
				}
			}
			if (editingCtx.getSelectedPointPosition() == -1) {
				if (hasMapRenderer) {
					if (centerPointMarker != null) {
						centerPointMarker.setPosition(new PointI(tb.getCenter31X(), tb.getCenter31Y()));
						centerPointMarker.setIsHidden(false);
					}
				} else {
					drawCenterIcon(canvas, tb, settings.isNightMode());
				}
				if (measureDistanceToCenterListener != null) {
					float distance = 0;
					float bearing = 0;
					if (editingCtx.getPointsCount() > 0) {
						WptPt lastPoint = editingCtx.getPoints().get(editingCtx.getPointsCount() - 1);
						LatLon centerLatLon = tb.getCenterLatLon();
						distance = (float) MapUtils.getDistance(
								lastPoint.getLat(), lastPoint.getLon(), centerLatLon.getLatitude(), centerLatLon.getLongitude());
						bearing = getLocationFromLL(lastPoint.getLat(), lastPoint.getLon())
								.bearingTo(getLocationFromLL(centerLatLon.getLatitude(), centerLatLon.getLongitude()));
					}
					measureDistanceToCenterListener.onMeasure(distance, bearing);
				}
			} else if (hasMapRenderer && centerPointMarker != null) {
				centerPointMarker.setIsHidden(true);
			}
			List<WptPt> beforePoints = editingCtx.getBeforePoints();
			List<WptPt> afterPoints = editingCtx.getAfterPoints();
			if (beforePoints.size() > 0) {
				WptPt point = beforePoints.get(beforePoints.size() - 1);
				if (hasMapRenderer) {
					if (beforePointMarker != null) {
						beforePointMarker.setPosition(new PointI(MapUtils.get31TileNumberX(point.getLongitude()),
								MapUtils.get31TileNumberY(point.getLatitude())));
						beforePointMarker.setIsHidden(false);
					}
				} else {
					drawPointIcon(canvas, tb, point, true);
				}
			} else if (hasMapRenderer && beforePointMarker != null) {
				beforePointMarker.setIsHidden(true);
			}
			if (afterPoints.size() > 0) {
				WptPt point = afterPoints.get(0);
				if (hasMapRenderer) {
					if (afterPointMarker != null) {
						afterPointMarker.setPosition(new PointI(MapUtils.get31TileNumberX(point.getLongitude()),
								MapUtils.get31TileNumberY(point.getLatitude())));
						afterPointMarker.setIsHidden(false);
					}
				} else {
					drawPointIcon(canvas, tb, point, true);
				}
			} else if (hasMapRenderer && afterPointMarker != null) {
				afterPointMarker.setIsHidden(true);
			}
			if (editingCtx.getSelectedPointPosition() != -1) {
				if (hasMapRenderer) {
					if (selectedPointMarker != null) {
						selectedPointMarker.setPosition(new PointI(tb.getCenter31X(), tb.getCenter31Y()));
						selectedPointMarker.setIsHidden(false);
					}
				} else {
					canvas.rotate(-tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
					int locX = tb.getCenterPixelX();
					int locY = tb.getCenterPixelY();
					canvas.drawBitmap(applyingPointIcon, locX - marginApplyingPointIconX, locY - marginApplyingPointIconY, bitmapPaint);
					canvas.rotate(tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
				}
			} else if (hasMapRenderer && selectedPointMarker != null) {
				selectedPointMarker.setIsHidden(true);
			}
		} else if (hasMapRenderer) {
			resetBeforeAfterRenderer();
			clearActivePointsCollection();
		}
		forceUpdateOnDraw = false;
	}

	private boolean isDrawingEnabled() {
		MapActivity mapActivity = getMapActivity();
		return mapActivity != null && inMeasurementMode && mapActivity.getFragmentsHelper().getGpsFilterFragment() == null;
	}

	private boolean isInTileBox(@NonNull QuadRect rect, @NonNull WptPt point) {
		double y = point.getLatitude();
		double x = point.getLongitude();
		return rect.contains(x, y, x, y);
	}

	private void drawSegmentLines(@NonNull Canvas canvas, @NonNull RotatedTileBox tb, boolean forceDraw) {
		List<TrkSegment> beforeSegments = new ArrayList<>(editingCtx.getBeforeTrkSegmentLine());
		List<TrkSegment> afterSegments = new ArrayList<>(editingCtx.getAfterTrkSegmentLine());
		List<TrkSegment> segments = new ArrayList<>(beforeSegments);
		segments.addAll(afterSegments);
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			int beforePointsCount = GpxUtilities.INSTANCE.calculateTrackPoints(beforeSegments);
			int afterPointsCount = GpxUtilities.INSTANCE.calculateTrackPoints(afterSegments);
			boolean draw = forceDraw;
			draw |= beforePointsCountCached != beforePointsCount;
			draw |= afterPointsCountCached != afterPointsCount;
			clearCachedSegmentsPointsCounters();
			beforePointsCountCached = beforePointsCount;
			afterPointsCountCached = afterPointsCount;

			List<RenderableSegment> cached = new ArrayList<>();
			if (draw) {
				clearCachedSegmentsRenderables();
				int baseOrder = getBaseOrder() - 10;
				QuadRect correctedQuadRect = getCorrectedQuadRect(tb.getLatLonBounds());
				for (TrkSegment segment : segments) {
					RenderableSegment renderer = null;
					if (!segment.getPoints().isEmpty()) {
						renderer = new StandardTrack(new ArrayList<>(segment.getPoints()), 17.2);
						segment.setRenderer(renderer);
						GpxGeometryWay geometryWay = new GpxGeometryWay(wayContext);
						geometryWay.baseOrder = baseOrder--;
						renderer.setTrackParams(lineAttrs.paint.getColor(), "", ColoringType.TRACK_SOLID, null, null);
						renderer.setDrawArrows(false);
						renderer.setGeometryWay(geometryWay);
						cached.add(renderer);
					}
					if (renderer != null) {
						renderer.drawGeometry(canvas, tb, correctedQuadRect, lineAttrs.paint.getColor(),
								lineAttrs.paint.getStrokeWidth(), getDashPattern(lineAttrs.paint));
					}
				}
				segmentsRenderablesCached = cached;
			}
		} else {
			for (TrkSegment segment : segments) {
				new StandardTrack(new ArrayList<>(segment.getPoints()), 17.2)
						.drawSegment(view.getZoom(), lineAttrs.paint, canvas, tb);
			}
		}
	}

	private void drawApproximatedLines(@NonNull Canvas canvas, @NonNull RotatedTileBox tb, boolean forceDraw) {
		MapRendererView mapRenderer = getMapRenderer();
		List<List<WptPt>> originalPointsList = editingCtx.getOriginalSegmentPointsList();
		if (!Algorithms.isEmpty(originalPointsList)) {
			int color = ContextCompat.getColor(getContext(), R.color.activity_background_transparent_color_dark);
			if (mapRenderer != null) {
				int originalPointsCount = 0;
				for (List<WptPt> points : originalPointsList) {
					originalPointsCount += points.size();
				}
				boolean draw = forceDraw;
				draw |= originalPointsCountCached != originalPointsCount;
				clearCachedOriginalPointsCounter();
				originalPointsCountCached = originalPointsCount;
				List<RenderableSegment> cached = new ArrayList<>();
				if (draw) {
					clearCachedApproximationRenderables();
					int baseOrder = getBaseOrder() - 10;
					QuadRect correctedQuadRect = getCorrectedQuadRect(tb.getLatLonBounds());
					for (List<WptPt> points : originalPointsList) {
						RenderableSegment renderer = null;
						if (!points.isEmpty()) {
							renderer = new StandardTrack(new ArrayList<>(points), 17.2);
							GpxGeometryWay geometryWay = new GpxGeometryWay(wayContext);
							geometryWay.baseOrder = baseOrder--;
							renderer.setTrackParams(color, "", ColoringType.TRACK_SOLID, null, null);
							renderer.setDrawArrows(false);
							renderer.setGeometryWay(geometryWay);
							cached.add(renderer);
						}
						if (renderer != null) {
							renderer.drawGeometry(canvas, tb, correctedQuadRect, color,
									lineAttrs.paint.getStrokeWidth(), getDashPattern(lineAttrs.paint));
						}
					}
					approximationRenderablesCached = cached;
				}
			} else {
				lineAttrs.customColorPaint.setColor(color);
				for (List<WptPt> points : originalPointsList) {
					new StandardTrack(new ArrayList<>(points), 17.2).
							drawSegment(view.getZoom(), lineAttrs.customColorPaint, canvas, tb);
				}
			}
		} else if (mapRenderer != null) {
			clearCachedRenderables();
		}
	}

	private void clearCachedCounters() {
		clearCachedSegmentsPointsCounters();
		clearCachedOriginalPointsCounter();
	}

	private void clearCachedSegmentsPointsCounters() {
		afterPointsCountCached = 0;
		beforePointsCountCached = 0;
	}

	private void clearCachedOriginalPointsCounter() {
		originalPointsCountCached = 0;
	}

	private void clearCachedRenderables() {
		clearCachedSegmentsRenderables();
		clearCachedApproximationRenderables();
	}

	private void clearCachedSegmentsRenderables() {
		clearCachedRenderables(segmentsRenderablesCached);
		segmentsRenderablesCached = new ArrayList<>();
	}

	private void clearCachedApproximationRenderables() {
		clearCachedRenderables(approximationRenderablesCached);
		approximationRenderablesCached = new ArrayList<>();
	}

	private void clearCachedRenderables(@NonNull List<RenderableSegment> cached) {
		for (RenderableSegment renderer : cached) {
			GpxGeometryWay geometryWay = renderer.getGeometryWay();
			if (geometryWay != null) {
				geometryWay.resetSymbolProviders();
			}
		}
	}

	private void drawTrackChartPoints(@Nullable TrackChartPoints trackChartPoints,
	                                  @NonNull Canvas canvas, @NonNull RotatedTileBox tileBox) {
		if (trackChartPoints != null) {
			LatLon highlightedPoint = trackChartPoints.getHighlightedPoint();
			if (highlightedPoint != null) {
				chartPointsHelper.drawHighlightedPoint(highlightedPoint, canvas, tileBox);
			}
			List<LatLon> xAxisPoint = trackChartPoints.getXAxisPoints();
			if (!Algorithms.isEmpty(xAxisPoint)) {
				chartPointsHelper.drawXAxisPoints(xAxisPoint, lineAttrs.defaultColor, canvas, tileBox);
			}
		}
	}

	private void drawPoints(@NonNull Canvas canvas, @NonNull RotatedTileBox tileBox) {
		int zoom = tileBox.getZoom();
		MapRendererView mapRenderer = getMapRenderer();
		if (showPointsZoomCache != zoom || mapActivityInvalidated || oldMovedPointRedraw) {
			List<WptPt> points = new ArrayList<>(editingCtx.getBeforePoints());
			points.addAll(editingCtx.getAfterPoints());
			showPointsZoomCache = zoom;
			boolean showPointsMinZoom = points.size() > 0 && calcZoomToShowPoints(tileBox, points, showPointsZoomCache);
			if (mapRenderer != null) {
				if ((showPointsMinZoom && !this.showPointsMinZoom) || oldMovedPointRedraw) {
					clearPointsProvider();
					DataTileManager<WptCollectionPoint> tilePoints = new DataTileManager<>(zoom);
					if (oldMovedPointRedraw && editingCtx.getOriginalPointToMove() != null) {
						WptPt point = editingCtx.getOriginalPointToMove();
						tilePoints.registerObject(point.getLatitude(), point.getLongitude(),
								new WptCollectionPoint(point, oldMovedPointIcon));
					}
					for (WptPt point : points) {
						tilePoints.registerObject(point.getLatitude(), point.getLongitude(),
								new WptCollectionPoint(point, pointIcon));
					}
					pointsProvider = new TilePointsProvider<>(getContext(), tilePoints,
							getPointsOrder() - 500, false, null, getTextScale(), view.getDensity(),
							START_ZOOM, 31);
					pointsProvider.drawSymbols(mapRenderer);
				}
			}
			this.showPointsMinZoom = showPointsMinZoom;
			oldMovedPointRedraw = false;
		}
		if (showPointsMinZoom) {
			if (!hasMapRenderer()) {
				drawPoints(canvas, tileBox, editingCtx.getBeforePoints());
				drawPoints(canvas, tileBox, editingCtx.getAfterPoints());
			}
		} else {
			clearPointsProvider();
		}
	}

	private void clearPointsProvider() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null && pointsProvider != null) {
			pointsProvider.clearSymbols(mapRenderer);
			pointsProvider = null;
		}
	}

	private void drawPoints(Canvas canvas, RotatedTileBox tileBox, List<WptPt> points) {
		QuadRect rect = tileBox.getLatLonBounds();
		for (int i = 0; i < points.size(); i++) {
			WptPt point = points.get(i);
			if (isInTileBox(rect, point)) {
				drawPointIcon(canvas, tileBox, point, false);
			}
		}
	}

	private boolean calcZoomToShowPoints(@NonNull RotatedTileBox tileBox, @NonNull List<WptPt> points, int currentZoom) {
		if (currentZoom >= 21) {
			return true;
		}
		if (currentZoom < START_ZOOM) {
			return false;
		}
		int counter = 0;
		WptPt prev = null;
		QuadRect rect = tileBox.getLatLonBounds();
		List<Double> distances = new ArrayList<>();

		for (WptPt wptPt : points) {
			if (prev != null) {
				double dist = MapUtils.getDistance(wptPt.getLat(), wptPt.getLon(), prev.getLat(), prev.getLon());
				distances.add(dist);
			}
			if (counter <= MAX_VISIBLE_POINTS && isInTileBox(rect, wptPt)) {
				counter++;
			}
			prev = wptPt;
		}
		Collections.sort(distances);
		double dist = Algorithms.getPercentile(distances, MIN_POINTS_PERCENTILE);
		int zoomMultiplier = (1 << (currentZoom - START_ZOOM));
		return dist > (MIN_DISTANCE_TO_SHOW_REF_ZOOM / zoomMultiplier) || counter <= MAX_VISIBLE_POINTS;
	}

	private void drawBeforeAfterPath(Canvas canvas, RotatedTileBox tb, boolean forceDraw) {
		boolean hasMapRenderer = hasMapRenderer();
		canvas.rotate(-tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
		List<TrkSegment> before = editingCtx.getBeforeSegments();
		List<TrkSegment> after = editingCtx.getAfterSegments();
		if (before.size() > 0 || after.size() > 0) {
			path.reset();
			tx.clear();
			ty.clear();

			List<WptPt> beforeAfterWpt = new ArrayList<>();
			WptPt centerWpt = new WptPt();
			centerWpt.setLat(tb.getCenterLatLon().getLatitude());
			centerWpt.setLon(tb.getCenterLatLon().getLongitude());
			boolean hasPointsBefore = false;
			if (before.size() > 0) {
				TrkSegment segment = before.get(before.size() - 1);
				if (segment.getPoints().size() > 0) {
					hasPointsBefore = true;
					WptPt pt = segment.getPoints().get(segment.getPoints().size() - 1);
					if (!pt.isGap() || (editingCtx.isInAddPointMode() && !editingCtx.isInAddPointBeforeMode())) {
						if (hasMapRenderer) {
							beforeAfterWpt.add(pt);
						} else {
							tx.add(tb.getPixXFromLatLon(pt.getLat(), pt.getLon()));
							ty.add(tb.getPixYFromLatLon(pt.getLat(), pt.getLon()));
						}
					}
					if (hasMapRenderer) {
						beforeAfterWpt.add(centerWpt);
					} else {
						tx.add((float) tb.getCenterPixelX());
						ty.add((float) tb.getCenterPixelY());
					}
				}
			}
			if (after.size() > 0 && !isLastPointOfSegmentSelected()) {
				TrkSegment segment = after.get(0);
				if (segment.getPoints().size() > 0) {
					if (!hasPointsBefore) {
						if (hasMapRenderer) {
							beforeAfterWpt.add(centerWpt);
						} else {
							tx.add((float) tb.getCenterPixelX());
							ty.add((float) tb.getCenterPixelY());
						}
					}
					WptPt pt = segment.getPoints().get(0);
					if (hasMapRenderer) {
						beforeAfterWpt.add(pt);
					} else {
						tx.add(tb.getPixXFromLatLon(pt.getLat(), pt.getLon()));
						ty.add(tb.getPixYFromLatLon(pt.getLat(), pt.getLon()));
					}
				}
			}
			if (!tx.isEmpty() && !ty.isEmpty()) {
				GeometryWayPathAlgorithms.calculatePath(tb, tx, ty, path);
				canvas.drawPath(path, lineAttrs.paint);
			}
			if (!beforeAfterWpt.isEmpty()) {
				if (!Algorithms.objectEquals(this.beforeAfterWpt, beforeAfterWpt)) {
					RenderableSegment renderer = beforeAfterRenderer;
					GpxGeometryWay geometryWay;
					if (renderer != null) {
						geometryWay = renderer.getGeometryWay();
					} else {
						geometryWay = new GpxGeometryWay(wayContext);
						geometryWay.baseOrder = getBaseOrder() - 100;
					}
					renderer = new StandardTrack(new ArrayList<>(beforeAfterWpt), 17.2);
					renderer.setTrackParams(lineAttrs.paint.getColor(), "", ColoringType.TRACK_SOLID, null, null);
					renderer.setDrawArrows(false);
					renderer.setGeometryWay(geometryWay);
					renderer.drawGeometry(canvas, tb, tb.getLatLonBounds(), lineAttrs.paint.getColor(),
							lineAttrs.paint.getStrokeWidth(), getDashPattern(lineAttrs.paint));
					beforeAfterRenderer = renderer;
				}
			} else {
				resetBeforeAfterRenderer();
			}
			this.beforeAfterWpt = beforeAfterWpt;
			canvas.rotate(tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
		} else {
			resetBeforeAfterRenderer();
		}
	}

	private boolean isLastPointOfSegmentSelected() {
		return editingCtx.getOriginalPointToMove() != null && editingCtx.getOriginalPointToMove().isGap();
	}

	private void resetBeforeAfterRenderer() {
		if (beforeAfterRenderer != null) {
			GpxGeometryWay geometryWay = beforeAfterRenderer.getGeometryWay();
			if (geometryWay != null) {
				geometryWay.resetSymbolProviders();
			}
			beforeAfterRenderer = null;
		}
	}

	private void recreateActivePointsCollection(boolean nightMode) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			clearActivePointsCollection();
			activePointsCollection = new MapMarkersCollection();
			// Center marker
			MapMarkerBuilder builder = new MapMarkerBuilder();
			builder.setBaseOrder(getPointsOrder() - 600);
			builder.setIsAccuracyCircleSupported(false);
			builder.setIsHidden(true);
			builder.setPinIcon(NativeUtilities.createSkImageFromBitmap(nightMode ? centerIconNight : centerIconDay));
			centerPointMarker = builder.buildAndAddToCollection(activePointsCollection);
			mapRenderer.addSymbolsProvider(activePointsCollection);
			// Before marker
			builder = new MapMarkerBuilder();
			builder.setBaseOrder(getPointsOrder() - 600);
			builder.setIsAccuracyCircleSupported(false);
			builder.setIsHidden(true);
			builder.setPinIcon(NativeUtilities.createSkImageFromBitmap(pointIcon));
			beforePointMarker = builder.buildAndAddToCollection(activePointsCollection);
			// After marker
			builder = new MapMarkerBuilder();
			builder.setBaseOrder(getPointsOrder() - 600);
			builder.setIsAccuracyCircleSupported(false);
			builder.setIsHidden(true);
			builder.setPinIcon(NativeUtilities.createSkImageFromBitmap(pointIcon));
			afterPointMarker = builder.buildAndAddToCollection(activePointsCollection);
			// Selected marker
			builder = new MapMarkerBuilder();
			builder.setBaseOrder(getPointsOrder() - 600);
			builder.setIsAccuracyCircleSupported(false);
			builder.setIsHidden(true);
			builder.setPinIcon(NativeUtilities.createSkImageFromBitmap(applyingPointIcon));
			selectedPointMarker = builder.buildAndAddToCollection(activePointsCollection);
			mapRenderer.addSymbolsProvider(activePointsCollection);
		}
	}

	private void clearActivePointsCollection() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null && activePointsCollection != null) {
			mapRenderer.removeSymbolsProvider(activePointsCollection);
			activePointsCollection = null;
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
		float locX = tb.getPixXFromLatLon(pt.getLat(), pt.getLon());
		float locY = tb.getPixYFromLatLon(pt.getLat(), pt.getLon());
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
		pt.setLat(l.getLatitude());
		pt.setLon(l.getLongitude());
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
			pt.setLat(lat);
			pt.setLon(lon);
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
		point.setLat(latLon.getLatitude());
		point.setLon(latLon.getLongitude());
		point.copyExtensions(originalPoint);
		return point;
	}

	public void exitMovePointMode() {
		oldMovedPointRedraw = true;
	}

	private void moveMapToLatLon(double lat, double lon) {
		view.getAnimatedDraggingThread().startMoving(lat, lon, view.getZoom());
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

	private void drawTrackChartPointsOpenGl(@Nullable TrackChartPoints chartPoints, @NonNull MapRendererView mapRenderer,
	                                        @NonNull RotatedTileBox tileBox) {
		if (chartPoints != null) {
			LatLon highlightedPoint = trackChartPoints.getHighlightedPoint();
			PointI highlightedPosition = null;
			if (highlightedPoint != null) {
				highlightedPosition = new PointI(
						MapUtils.get31TileNumberX(highlightedPoint.getLongitude()),
						MapUtils.get31TileNumberY(highlightedPoint.getLatitude()));
			}
			PointI highlightedMarkerPosition = highlightedPointMarker != null ? highlightedPointMarker.getPosition() : null;
			boolean highlightedPositionChanged = highlightedPosition != null && highlightedMarkerPosition != null
					&& (highlightedPosition.getX() != highlightedMarkerPosition.getX()
					|| highlightedPosition.getY() != highlightedMarkerPosition.getY());
			if (highlightedPosition == null) {
				setHighlightedPointMarkerVisibility(false);
			} else if (highlightedPositionChanged) {
				setHighlightedPointMarkerPosition(highlightedPosition);
				setHighlightedPointMarkerVisibility(true);
			}
			List<LatLon> xAxisPoints = chartPoints.getXAxisPoints();
			if (Algorithms.objectEquals(xAxisPointsCached, xAxisPoints)
					&& trackChartPointsProvider != null && !mapActivityInvalidated) {
				return;
			}
			xAxisPointsCached = xAxisPoints;
			clearXAxisPoints();
			if (!Algorithms.isEmpty(xAxisPoints)) {
				Bitmap pointBitmap = chartPointsHelper.createXAxisPointBitmap(lineAttrs.defaultColor, tileBox.getDensity());
				trackChartPointsProvider = new LocationPointsTileProvider(getPointsOrder() - 500, xAxisPoints, pointBitmap);
				trackChartPointsProvider.drawPoints(mapRenderer);
			}
		} else {
			xAxisPointsCached = new ArrayList<>();
			clearXAxisPoints();
			setHighlightedPointMarkerVisibility(false);
		}
	}

	private void setHighlightedPointMarkerPosition(PointI position) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null && highlightedPointMarker != null) {
			highlightedPointMarker.setPosition(position);
		}
	}

	private void setHighlightedPointMarkerVisibility(boolean visible) {
		if (highlightedPointMarker != null) {
			highlightedPointMarker.setIsHidden(!visible);
		}
	}

	private void clearXAxisPoints() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null && trackChartPointsProvider != null) {
			trackChartPointsProvider.clearPoints(mapRenderer);
			trackChartPointsProvider = null;
		}
	}

	private void recreateHighlightedPointCollection() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			clearHighlightedPointCollection();

			highlightedPointCollection = new MapMarkersCollection();
			MapMarkerBuilder builder = new MapMarkerBuilder();
			builder.setBaseOrder(getPointsOrder() - 600);
			builder.setIsAccuracyCircleSupported(false);
			builder.setIsHidden(true);
			builder.setPinIcon(NativeUtilities.createSkImageFromBitmap(highlightedPointImage));
			highlightedPointMarker = builder.buildAndAddToCollection(highlightedPointCollection);
			mapRenderer.addSymbolsProvider(highlightedPointCollection);
		}
	}

	private void clearHighlightedPointCollection() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null && highlightedPointCollection != null) {
			mapRenderer.removeSymbolsProvider(highlightedPointCollection);
			highlightedPointCollection = null;
		}
	}

	public void refreshMap() {
		forceUpdateBufferImage = true;
		showPointsZoomCache = 0;
		showPointsMinZoom = false;
		view.refreshMap();
	}

	@Override
	protected void cleanupResources() {
		super.cleanupResources();
		clearCachedCounters();
		clearCachedRenderables();
		clearPointsProvider();
		clearXAxisPoints();
		multiProfileGeometry.clearWay();
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o,
	                                    boolean unknownLocation, boolean excludeUntouchableObjects) {

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
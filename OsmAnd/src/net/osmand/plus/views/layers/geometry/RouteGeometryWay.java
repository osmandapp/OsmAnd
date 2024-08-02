package net.osmand.plus.views.layers.geometry;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import net.osmand.Location;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QVectorPointI;
import net.osmand.core.jni.VectorLine;
import net.osmand.core.jni.VectorLineBuilder;
import net.osmand.core.jni.VectorLinesCollection;
import net.osmand.data.RotatedTileBox;
import net.osmand.shared.routing.ColoringType;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.layers.RouteLayer.ActionPoint;
import net.osmand.plus.views.layers.geometry.GeometryWayDrawer.DrawPathData31;
import net.osmand.shared.ColorPalette;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import gnu.trove.list.array.TByteArrayList;

public class RouteGeometryWay extends
		MultiColoringGeometryWay<RouteGeometryWayContext, MultiColoringGeometryWayDrawer<RouteGeometryWayContext>> {

	public static final int MIN_COLOR_SQUARE_DISTANCE = 15_000;

	private final RoutingHelper helper;
	private RouteCalculationResult route;
	@NonNull
	private List<Integer> forceIncludedIndexes = new ArrayList<>();

	private Integer customDirectionArrowColor;

	//OpenGL
	private boolean drawDirectionArrows = true;
	private VectorLinesCollection actionLinesCollection;

	private List<Segment> cachedSegments = new ArrayList<>();
	private Segment currentCachedSegment = null;

	public RouteGeometryWay(RouteGeometryWayContext context) {
		super(context, new MultiColoringGeometryWayDrawer<>(context));
		this.helper = context.getApp().getRoutingHelper();
		this.linesPriority = Long.MAX_VALUE;
	}

	public void setRouteStyleParams(int pathColor,
	                                float pathWidth,
	                                boolean drawDirectionArrows,
	                                @Nullable @ColorInt Integer directionArrowColor,
	                                @NonNull ColoringType routeColoringType,
	                                @Nullable String routeInfoAttribute,
									@Nullable String gradientPalette) {
		this.coloringChanged = this.coloringType != routeColoringType || !this.gradientPalette.equals(gradientPalette)
				|| routeColoringType == ColoringType.ATTRIBUTE
				&& !Algorithms.objectEquals(this.routeInfoAttribute, routeInfoAttribute);

		boolean widthChanged = !Algorithms.objectEquals(customWidth, pathWidth);
		if (widthChanged) {
			updateStylesWidth(pathWidth);
		}
		if (this.drawDirectionArrows != drawDirectionArrows) {
			resetArrowsProvider();
		}
		updatePaints(pathWidth, routeColoringType);
		getDrawer().setColoringType(routeColoringType);

		this.customColor = pathColor;
		this.customWidth = pathWidth;
		this.drawDirectionArrows = drawDirectionArrows;
		this.customDirectionArrowColor = directionArrowColor;
		this.coloringType = routeColoringType;
		this.routeInfoAttribute = routeInfoAttribute;
		this.gradientPalette = gradientPalette;
	}

	@Override
	protected void updatePaints(@Nullable Float width, @NonNull ColoringType routeColoringType) {
		super.updatePaints(width, routeColoringType);
		Paint.Cap cap = routeColoringType.isGradient() || routeColoringType.isRouteInfoAttribute() ?
				Paint.Cap.ROUND : getContext().getAttrs().paint.getStrokeCap();
		getContext().getAttrs().customColorPaint.setStrokeCap(cap);
	}

	public boolean updateRoute(@NonNull RotatedTileBox tb, @NonNull RouteCalculationResult route) {
		if (coloringChanged || tb.getMapDensity() != getMapDensity() || this.route != route) {
			this.route = route;
			coloringChanged = false;
			List<Location> locations = route.getImmutableAllLocations();
			if (coloringType.isGradient()) {
				updateGradientWay(tb, locations);
			} else if (coloringType.isRouteInfoAttribute()) {
				updateSolidMultiColorRoute(tb, locations, route.getOriginalRoute());
			} else {
				updateWay(locations, tb);
			}
			return true;
		}
		return false;
	}

	public void setForceIncludedPointIndexesFromActionPoints(@Nullable List<ActionPoint> actionPoints) {
		List<Integer> indexes = new ArrayList<>();
		if (actionPoints != null) {
			for (ActionPoint actionPoint : actionPoints) {
				if (actionPoint != null) {
					indexes.add(actionPoint.index);
					if (actionPoint.normalizedOffset > 0) {
						indexes.add(actionPoint.index + 1);
					}
				}
			}
		}
		this.forceIncludedIndexes = indexes;
		this.zooms.clear();
	}

	@Override
	public void drawSegments(@NonNull RotatedTileBox tb, @Nullable Canvas canvas, double topLatitude,
	                         double leftLongitude, double bottomLatitude, double rightLongitude,
	                         Location lastProjection, int startLocationIndex) {
		cachedSegments.clear();
		super.drawSegments(tb, canvas, topLatitude, leftLongitude, bottomLatitude, rightLongitude, lastProjection, startLocationIndex);
	}

	@Override
	protected boolean shouldSkipLocation(@Nullable TByteArrayList simplification, Map<Integer, GeometryWayStyle<?>> styleMap, int locationIdx) {
		return super.shouldSkipLocation(simplification, styleMap, locationIdx)
				&& !forceIncludedIndexes.contains(locationIdx);
	}

	@Override
	protected boolean addInitialPoint(RotatedTileBox tb,
	                                  double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude,
	                                  GeometryWayStyle<?> style, boolean previousVisible, Location lastPoint, int startLocationIndex) {
		boolean added = super.addInitialPoint(tb, topLatitude, leftLongitude, bottomLatitude, rightLongitude,
				style, previousVisible, lastPoint, startLocationIndex);
		if (added) {
			if (currentCachedSegment == null) {
				currentCachedSegment = new Segment();
			}
			currentCachedSegment.initialLocations.add(lastPoint);
		}
		return added;
	}

	@Nullable
	@Override
	protected List<List<DrawPathData31>> cutStartOfCachedPath(@NonNull MapRendererView mapRenderer,
	                                                          @NonNull RotatedTileBox tb,
	                                                          int startLocationIndex,
	                                                          boolean previousVisible) {
		List<List<DrawPathData31>> croppedPathData31 = super.cutStartOfCachedPath(mapRenderer, tb, startLocationIndex, previousVisible);
		if (croppedPathData31 == null) {
			return null;
		}

		List<Segment> segments = new ArrayList<>();
		for (List<DrawPathData31> segmentData : croppedPathData31) {

			Segment segment = new Segment();
			segment.indexes = new ArrayList<>();
			segment.styles = new ArrayList<>();

			for (int lineIndex = 0; lineIndex < segmentData.size(); lineIndex++) {
				DrawPathData31 line = segmentData.get(lineIndex);

				boolean lastLine = lineIndex + 1 == segmentData.size();
				int endIndex = lastLine
						? line.indexes.size()
						: line.indexes.size() - 1;
				for (int i = 0; i < endIndex; i++) {
					int index = line.indexes.get(i);
					if (index >= INITIAL_POINT_INDEX_SHIFT) {
						int x31 = line.tx.get(i);
						int y31 = line.ty.get(i);
						double lat = MapUtils.get31LatitudeY(y31);
						double lon = MapUtils.get31LongitudeX(x31);
						segment.initialLocations.add(new Location("", lat, lon));
					}

					segment.indexes.add(index);
					segment.styles.add(line.style);
				}
			}
			segments.add(segment);
		}

		cachedSegments = segments;

		return croppedPathData31;
	}

	@Override
	public void drawRouteSegment(@NonNull RotatedTileBox tb, @Nullable Canvas canvas, List<GeometryWayPoint> points, double distToFinish) {
		super.drawRouteSegment(tb, canvas, points, distToFinish);

		// FIXME do we always call on draw?
		Segment segment = currentCachedSegment != null ? currentCachedSegment : new Segment();
		segment.indexes = new ArrayList<>();
		segment.styles = new ArrayList<>();
		for (GeometryWayPoint p : points) {
			segment.indexes.add(p.index);
			segment.styles.add(p.style);
		}
		cachedSegments.add(segment);
		currentCachedSegment = null;
	}

	@NonNull
	@Override
	protected List<Integer> getForceIncludedLocationIndexes() {
		return forceIncludedIndexes;
	}

	@NonNull
	@Override
	public GeometryWayStyle<?> getDefaultWayStyle() {
		return coloringType.isGradient()
				? getGradientWayStyle()
				: getArrowWayStyle(customColor);
	}

	@NonNull
	@Override
	public GeometrySolidWayStyle<RouteGeometryWayContext> getSolidWayStyle(int lineColor) {
		return getArrowWayStyle(lineColor);
	}

	@NonNull
	private GeometrySolidWayStyle<RouteGeometryWayContext> getArrowWayStyle(int lineColor) {
		int directionArrowColor = customDirectionArrowColor != null
				? customDirectionArrowColor
				: getContext().getPaintIcon().getColor();
		return new GeometrySolidWayStyle<>(getContext(), lineColor, customWidth, directionArrowColor, true);
	}

	@Override
	protected PathGeometryZoom getGeometryZoom(RotatedTileBox tb) {
		if (coloringType.isGradient()) {
			int zoom = tb.getZoom();
			PathGeometryZoom zm = zooms.get(zoom);
			if (zm == null) {
				zm = new GradientPathGeometryZoom(getLocationProvider(), tb, true, forceIncludedIndexes);
				zooms.put(zoom, zm);
			}
			return zm;
		}
		return super.getGeometryZoom(tb);
	}

	@Override
	public Location getNextVisiblePoint() {
		return helper.getRoute().getCurrentStraightAnglePoint();
	}

	@Override
	protected boolean shouldDrawArrows() {
		return drawDirectionArrows;
	}

	public void clearRoute() {
		if (route != null) {
			route = null;
			clearWay();
		}
	}

	public void resetSymbolProviders() {
		super.resetSymbolProviders();
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			if (actionLinesCollection != null) {
				mapRenderer.removeSymbolsProvider(actionLinesCollection);
				actionLinesCollection = null;
			}
		}
	}

	public void resetActionLines() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			if (actionLinesCollection != null) {
				mapRenderer.removeSymbolsProvider(actionLinesCollection);
				actionLinesCollection = null;
			}
		}
	}

	public List<List<ActionPoint>> getActionArrows(List<ActionPoint> actionPoints) {
		List<List<ActionPoint>> ll = new ArrayList<>();
		ll.add(new ArrayList<>());
		int index = 0;
		for (int i = 0; i < actionPoints.size(); i++) {
			ActionPoint actionPoint = actionPoints.get(i);
			if (actionPoint != null) {
				ll.get(index).add(actionPoint);
			} else if (i < actionPoints.size() - 1) {
				index++;
				ll.add(new ArrayList<>());
			}
		}
		return ll;
	}

	public void buildActionArrows(@NonNull List<ActionPoint> actionPoints, int customTurnArrowColor) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null) {
			return;
		}
		int baseOrder = this.baseOrder - 1000;
		List<List<ActionPoint>> actionArrows = getActionArrows(actionPoints);
		if (!actionArrows.isEmpty()) {
			int lineIdx = 0;
			if (actionLinesCollection == null) {
				actionLinesCollection = new VectorLinesCollection();
			}
			long initialLinesCount = actionLinesCollection.getLines().size();
			for (List<ActionPoint> line : actionArrows) {
				int arrowColor = getContrastArrowColor(line, customTurnArrowColor);
				QVectorPointI points = new QVectorPointI();
				for (ActionPoint point : line) {
					int x = MapUtils.get31TileNumberX(point.location.getLongitude());
					int y = MapUtils.get31TileNumberY(point.location.getLatitude());
					points.add(new PointI(x, y));
				}
				if (lineIdx < initialLinesCount) {
					VectorLine vectorLine = actionLinesCollection.getLines().get(lineIdx);
					vectorLine.setPoints(points);
					vectorLine.setIsHidden(false);
					vectorLine.setLineWidth(customWidth);
					vectorLine.setFillColor(NativeUtilities.createFColorARGB(arrowColor));
					lineIdx++;
				} else {
					VectorLineBuilder vectorLineBuilder = new VectorLineBuilder();
					vectorLineBuilder.setBaseOrder(baseOrder--)
							.setIsHidden(false)
							.setLineId((int) actionLinesCollection.getLines().size())
							.setLineWidth(customWidth)
							.setPoints(points)
							.setEndCapStyle(VectorLine.EndCapStyle.ARROW.ordinal())
							.setFillColor(NativeUtilities.createFColorARGB(arrowColor));
					vectorLineBuilder.buildAndAddToCollection(actionLinesCollection);
				}
			}
			while (lineIdx < initialLinesCount) {
				actionLinesCollection.getLines().get(lineIdx).setIsHidden(true);
				lineIdx++;
			}
			mapRenderer.addSymbolsProvider(actionLinesCollection);
		}
	}

	@ColorInt
	public int getContrastArrowColor(@NonNull List<ActionPoint> line, @ColorInt int originalArrowColor) {
		Context context = getContext().getCtx();

		int lightColor = ColorUtilities.getSecondaryIconColor(context, false);
		int darkColor = ColorUtilities.getSecondaryIconColor(context, true);

		Map<Integer, Integer> lowDistanceCounts = new LinkedHashMap<>();
		lowDistanceCounts.put(originalArrowColor, 0);
		lowDistanceCounts.put(lightColor, 0);
		lowDistanceCounts.put(darkColor, 0);

		for (ActionPoint actionPoint : line) {
			Integer lineColor = getActionPointColor(actionPoint);
			if (lineColor == null) {
				return originalArrowColor;
			}

			for (Entry<Integer, Integer> entry : lowDistanceCounts.entrySet()) {
				int color = entry.getKey();
				int count = entry.getValue();

				if (ColorUtilities.getColorsSquareDistance(color, lineColor) < MIN_COLOR_SQUARE_DISTANCE) {
					entry.setValue(count + 1);
				}
			}
		}

		int minLowDistanceCount = Integer.MAX_VALUE;
		for (int count : lowDistanceCounts.values()) {
			if (count < minLowDistanceCount) {
				minLowDistanceCount = count;
			}
		}

		for (Entry<Integer, Integer> entry : lowDistanceCounts.entrySet()) {
			int color = entry.getKey();
			int count = entry.getValue();
			if (count == minLowDistanceCount) {
				return color;
			}
		}

		return originalArrowColor;
	}

	@ColorInt
	private Integer getActionPointColor(@NonNull ActionPoint actionPoint) {
		if (styleMap.isEmpty()) {
			return getDefaultWayStyle().getColor();
		}

		for (Segment segment : cachedSegments) {
			if (!segment.isCompleted()) {
				return null;
			}
			int pointOrder = segment.getPointOrders(actionPoint.index);
			if (pointOrder == -1) {
				return null;
			}

			GeometryWayStyle<?> style = segment.styles.get(pointOrder);
			if (style instanceof GeometryGradientWayStyle<?>) {
				GeometryGradientWayStyle<?> gradientStyle = (GeometryGradientWayStyle<?>) style;
				if (pointOrder + 1 == segment.styles.size()) {
					return gradientStyle.nextColor;
				} else {
					int startColor = gradientStyle.currColor;
					int endColor = gradientStyle.nextColor;
					return ColorPalette.Companion.getIntermediateColor(startColor, endColor, actionPoint.normalizedOffset);
				}
			} else {
				return style.getColor();
			}
		}

		return null;
	}

	private static class Segment {
		public List<Location> initialLocations = new ArrayList<>();
		public List<Integer> indexes;
		public List<GeometryWayStyle<?>> styles;

		public boolean isCompleted() {
			return indexes != null && styles != null;
		}

		public int getPointOrders(int index) {
			for (int pointOrder = 0; pointOrder < indexes.size(); pointOrder++) {
				int pointIndex = indexes.get(pointOrder);

				if (index >= 0 && index == pointIndex || index == -1 && pointIndex >= INITIAL_POINT_INDEX_SHIFT) {
					return pointOrder;
				}

				if (pointIndex > index && pointIndex < INITIAL_POINT_INDEX_SHIFT) {
					return -1;
				}
			}

			return -1;
		}
	}
}
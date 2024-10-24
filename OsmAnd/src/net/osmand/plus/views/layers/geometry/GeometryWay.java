package net.osmand.plus.views.layers.geometry;

import android.graphics.Canvas;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererContext;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.VectorLineArrowsProvider;
import net.osmand.core.jni.VectorLinesCollection;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.shared.gpx.GpxUtilities;
import net.osmand.plus.track.Gpx3DVisualizationType;
import net.osmand.plus.views.layers.geometry.GeometryWayDrawer.DrawPathData;
import net.osmand.plus.views.layers.geometry.GeometryWayDrawer.DrawPathData31;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import gnu.trove.list.array.TByteArrayList;

public abstract class GeometryWay<T extends GeometryWayContext, D extends GeometryWayDrawer<T>> {

	protected static final int INITIAL_POINT_INDEX_SHIFT = 1 << 30;
	private final Log log = PlatformUtil.getLog(GeometryWay.class);

	private double mapDensity;
	private final T context;
	private final D drawer;
	private GeometryWayProvider locationProvider;
	protected Map<Integer, GeometryWayStyle<?>> styleMap = Collections.emptyMap();
	protected TreeMap<Integer, PathGeometryZoom> zooms = new TreeMap<>();

	// cache arrays
	List<GeometryWayPoint> points = new ArrayList<>();
	//OpenGL
	protected final List<List<DrawPathData31>> pathsData31Cache = new ArrayList<>();
	protected int startLocationIndexCached = -1;
	public int baseOrder = -1;
	public long linesPriority = Long.MIN_VALUE;
	public VectorLinesCollection vectorLinesCollection;
	public VectorLineArrowsProvider vectorLineArrowsProvider;

	public interface GeometryWayProvider {
		double getLatitude(int index);

		double getLongitude(int index);

		int getSize();

		float getHeight(int index);
	}

	public GeometryWay(T context, D drawer) {
		this.context = context;
		this.drawer = drawer;
	}

	protected GeometryWayStyle<?> getStyle(int index, GeometryWayStyle<?> defaultWayStyle) {
		List<Integer> list = new ArrayList<>(styleMap.keySet());
		for (int i = list.size() - 1; i >= 0; i--) {
			int c = list.get(i);
			if (c <= index) {
				return styleMap.get(c);
			}
		}
		return defaultWayStyle;
	}

	public T getContext() {
		return context;
	}

	public D getDrawer() {
		return drawer;
	}

	public double getMapDensity() {
		return mapDensity;
	}

	@Nullable
	public GeometryWayProvider getLocationProvider() {
		return locationProvider;
	}

	public boolean hasMapRenderer() {
		return context.hasMapRenderer();
	}

	@Nullable
	public MapRendererView getMapRenderer() {
		return context.getMapRenderer();
	}

	protected boolean isHeightmapsActive() {
		MapRendererContext mapRendererContext = NativeCoreContext.getMapRendererContext();
		if (mapRendererContext != null) {
			return mapRendererContext.isHeightmapsActive();
		}
		return false;
	}

	protected void updateWay(@NonNull GeometryWayProvider locationProvider, @NonNull RotatedTileBox tb) {
		updateWay(locationProvider, null, tb);
	}

	protected void updateWay(@NonNull GeometryWayProvider locationProvider, @Nullable Map<Integer, GeometryWayStyle<?>> styleMap, @NonNull RotatedTileBox tb) {
		this.locationProvider = locationProvider;
		this.styleMap = styleMap == null ? Collections.emptyMap() : styleMap;
		this.mapDensity = tb.getMapDensity();
		this.zooms = new TreeMap<>();
		clearPathCache();
	}

	public void updateWay(@NonNull List<Location> locations, @NonNull RotatedTileBox tb) {
		updateWay(locations, null, tb);
	}

	public void updateWay(@NonNull List<Location> locations, @Nullable Map<Integer, GeometryWayStyle<?>> styleMap, @NonNull RotatedTileBox tb) {
		this.locationProvider = new GeometryWayLocationProvider(locations);
		this.styleMap = styleMap == null ? Collections.emptyMap() : styleMap;
		this.mapDensity = tb.getMapDensity();
		this.zooms = new TreeMap<>();
		clearPathCache();
	}

	public void clearWay() {
		this.locationProvider = null;
		this.styleMap = Collections.emptyMap();
		this.zooms = new TreeMap<>();
		resetSymbolProviders();
		clearPathCache();
	}

	private void clearPathCache() {
		pathsData31Cache.clear();
		startLocationIndexCached = -1;
	}

	public void resetSymbolProviders() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			clearPathCache();
			if (vectorLineArrowsProvider != null) {
				mapRenderer.removeSymbolsProvider(vectorLineArrowsProvider);
				vectorLineArrowsProvider = null;
			}
			if (vectorLinesCollection != null) {
				mapRenderer.removeSymbolsProvider(vectorLinesCollection);
				vectorLinesCollection = null;
			}
		}
	}

	public void resetArrowsProvider() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			if (vectorLineArrowsProvider != null) {
				mapRenderer.removeSymbolsProvider(vectorLineArrowsProvider);
				vectorLineArrowsProvider = null;
			}
		}
	}

	protected PathGeometryZoom getGeometryZoom(RotatedTileBox tb) {
		int zoom = tb.getZoom();
		PathGeometryZoom zm = zooms.size() > zoom ? zooms.get(zoom) : null;
		if (zm == null) {
			boolean simplify = tb.getZoom() < context.getSimplificationZoom();
			zm = new PathGeometryZoom(locationProvider, tb, simplify, getForceIncludedLocationIndexes());
			zooms.put(zoom, zm);
		}
		return zm;
	}

	@NonNull
	protected List<Integer> getForceIncludedLocationIndexes() {
		return Collections.emptyList();
	}

	@NonNull
	public abstract GeometryWayStyle<?> getDefaultWayStyle();

	public Location getNextVisiblePoint() {
		return null;
	}

	public void drawSegments(@NonNull RotatedTileBox tb, @Nullable Canvas canvas, double topLatitude, double leftLongitude,
	                         double bottomLatitude, double rightLongitude, Location lastProjection, int startLocationIndex) {
		if (locationProvider == null || locationProvider.getSize() == 0) {
			return;
		}

		MapRendererView mapRenderer = getMapRenderer();
		boolean hasMapRenderer = mapRenderer != null;
		PathGeometryZoom geometryZoom = !hasMapRenderer ? getGeometryZoom(tb) : null;
		TByteArrayList simplification = geometryZoom != null ? geometryZoom.getSimplifyPoints() : null;
		List<Double> odistances = geometryZoom != null ? geometryZoom.getDistances() : null;

		clearArrays();

		GeometryWayStyle<?> defaultWayStyle = getDefaultWayStyle();
		GeometryWayStyle<?> style = defaultWayStyle;
		boolean previousVisible = false;
		if (lastProjection != null) {
			previousVisible = addInitialPoint(tb, topLatitude, leftLongitude, bottomLatitude, rightLongitude,
					style, previousVisible, lastProjection, startLocationIndex);
		}
		Location nextVisiblePoint = getNextVisiblePoint();
		if (nextVisiblePoint != null) {
			boolean added = addInitialPoint(tb, topLatitude, leftLongitude, bottomLatitude, rightLongitude,
					style, previousVisible, nextVisiblePoint, startLocationIndex);
			if (added) {
				previousVisible = true;
			}
		}

		if (hasMapRenderer && !pathsData31Cache.isEmpty()) {
			cutStartOfCachedPath(mapRenderer, tb, startLocationIndex, previousVisible);
			return;
		}

		GeometryWayProvider locationProvider = this.locationProvider;
		int previous = -1;
		int previousVisibleIdx = -1;
		boolean ignorePrevious = false;

		for (int i = startLocationIndex - (hasMapRenderer  && startLocationIndex > 0 ? 1 : 0); i < locationProvider.getSize(); i++) {
			style = getStyle(i, defaultWayStyle);
			if (shouldSkipLocation(simplification, styleMap, i)) {
				continue;
			}
			if (shouldAddLocation(simplification, leftLongitude, rightLongitude, bottomLatitude, topLatitude,
					locationProvider, previousVisibleIdx, i)) {
				double dist = previous == -1 || odistances == null ? 0 : odistances.get(i);
				if (hasMapRenderer && previous != -1) {
					double prevLat = locationProvider.getLatitude(previous);
					double prevLon = locationProvider.getLongitude(previous);
					double lat = locationProvider.getLatitude(i);
					double lon = locationProvider.getLongitude(i);
					dist = MapUtils.getDistance(prevLat, prevLon, lat, lon);
				}
				if (!previousVisible && !ignorePrevious) {
					if (previous != -1 && !isPreviousPointFarAway(locationProvider, previous, i)) {
						addLocation(tb, previous, dist, style, points);
					} else if (lastProjection != null) {
						addLocation(tb, lastProjection.getLatitude(), lastProjection.getLongitude(), i - 1, dist, true,
								getStyle(i - 1, style), points); // first point
					}
				}
				addLocation(tb, i, dist, style, points);
				previousVisible = true;
				previousVisibleIdx = i;
				ignorePrevious = false;
			} else if (previousVisible) {
				if (isPreviousPointFarAway(locationProvider, previousVisibleIdx, i)) {
					ignorePrevious = true;
					previousVisibleIdx = -1;
				} else {
					addLocation(tb, i, previous == -1 || odistances == null ? 0 : odistances.get(i), style,
							points);
					ignorePrevious = false;
				}
				double distToFinish = 0;
				if (odistances != null) {
					for (int ki = i + 1; ki < odistances.size(); ki++) {
						distToFinish += odistances.get(ki);
					}
				}
				drawRouteSegment(tb, canvas, points, distToFinish);
				previousVisible = false;
				clearArrays();
			}
			previous = i;
		}
		drawRouteSegment(tb, canvas, points, 0);
	}

	protected boolean shouldSkipLocation(@Nullable TByteArrayList simplification, Map<Integer, GeometryWayStyle<?>> styleMap, int locationIdx) {
		return simplification != null && simplification.getQuick(locationIdx) == 0 && !styleMap.containsKey(locationIdx);
	}

	protected boolean shouldAddLocation(@Nullable TByteArrayList simplification, double leftLon, double rightLon,
	                                    double bottomLat, double topLat, GeometryWayProvider provider,
	                                    int previousVisible, int currLocationIdx) {
		if (hasMapRenderer()) {
			return true;
		}
		double lat = provider.getLatitude(currLocationIdx);
		double lon = provider.getLongitude(currLocationIdx);
		boolean insideOfBounds = leftLon <= lon && lon <= rightLon && bottomLat <= lat && lat <= topLat;
		if (!insideOfBounds) {
			return false;
		} else if (previousVisible >= 0) {
			double prevLon = provider.getLongitude(previousVisible);
			boolean primeMeridianPoints = Math.max(prevLon, lon) == GpxUtilities.PRIME_MERIDIAN
					&& Math.min(prevLon, lon) == -GpxUtilities.PRIME_MERIDIAN;
			return !primeMeridianPoints;
		} else {
			return true;
		}
	}

	private boolean isPreviousPointFarAway(GeometryWayProvider locations, int prevLocationIdx,
	                                       int currLocationIdx) {
		if (prevLocationIdx < 0) {
			return false;
		}
		double prevLon = locations.getLongitude(prevLocationIdx);
		double currLon = locations.getLongitude(currLocationIdx);
		return Math.abs(currLon - prevLon) >= 180;
	}

	protected void addLocation(RotatedTileBox tb, int locationIdx, double dist,
	                           GeometryWayStyle<?> style, List<GeometryWayPoint> points) {
		addLocation(tb, locationProvider.getLatitude(locationIdx), locationProvider.getLongitude(locationIdx),
				locationIdx, dist, false, style, points);
	}

	protected void addLocation(RotatedTileBox tb, double latitude, double longitude, int locationIdx,
	                           double dist, boolean initialPoint,
	                           GeometryWayStyle<?> style, List<GeometryWayPoint> points) {
		GeometryWayPoint pnt = new GeometryWayPoint();
		pnt.index = (initialPoint ? INITIAL_POINT_INDEX_SHIFT : 0) + locationIdx;

		if (hasMapRenderer()) {
			pnt.tx31 = MapUtils.get31TileNumberX(longitude);
			pnt.ty31 = MapUtils.get31TileNumberY(latitude);
			pnt.height = locationProvider.getHeight(locationIdx);
			pnt.style = style;
			pnt.distance = dist;
			points.add(pnt);
			return;
		}

		float x = tb.getPixXFromLatLon(latitude, longitude);
		float y = tb.getPixYFromLatLon(latitude, longitude);
		float px = x;
		float py = y;
		if (points.size() > 0) {
			px = points.get(points.size() - 1).tx;
			py = points.get(points.size() - 1).ty;
		}
		double angle = 0;
		if (px != x || py != y) {
			double angleRad = Math.atan2(y - py, x - px);
			angle = (angleRad * 180 / Math.PI) + 90f;
		}
		double distSegment = dist != 0 ? dist : Math.sqrt((y - py) * (y - py) + (x - px) * (x - px));
		pnt.tx = x;
		pnt.ty = y;
		pnt.angle = angle;
		pnt.distance = distSegment;
		pnt.style = style;
		points.add(pnt);
	}

	protected boolean addInitialPoint(RotatedTileBox tb, double topLatitude, double leftLongitude, double bottomLatitude,
	                                  double rightLongitude, GeometryWayStyle<?> style, boolean previousVisible,
	                                  Location lastPoint, int startLocationIndex) {
		if (hasMapRenderer() || (leftLongitude <= lastPoint.getLongitude() && lastPoint.getLongitude() <= rightLongitude
				&& bottomLatitude <= lastPoint.getLatitude() && lastPoint.getLatitude() <= topLatitude)) {
			addLocation(tb, lastPoint.getLatitude(), lastPoint.getLongitude(), startLocationIndex, 0, true,
					style, points);
			return true;
		}
		return false;
	}

	@Nullable
	protected List<List<DrawPathData31>> cutStartOfCachedPath(@NonNull MapRendererView mapRenderer,
	                                                          @NonNull RotatedTileBox tb,
	                                                          int startLocationIndex,
	                                                          boolean previousVisible) {
		List<List<DrawPathData31>> croppedPathsData31 = new ArrayList<>();
		boolean drawNext = false;
		float passedDist = 0;
		int firstX31 = -1;
		int firstY31 = -1;
		boolean create = !previousVisible || startLocationIndexCached == -1;
		boolean update = false;
		for (List<DrawPathData31> pathsDataList : pathsData31Cache) {
			if (drawNext) {
				croppedPathsData31.add(pathsDataList);
				drawPathLine(tb, pathsDataList);
				continue;
			}
			List<DrawPathData31> newPathsDataList = new ArrayList<>();
			for (DrawPathData31 pathData : pathsDataList) {
				if (drawNext) {
					newPathsDataList.add(pathData);
					continue;
				}
				if (pathData.indexes.size() < 3 || pathData.indexes.contains(startLocationIndex)) {
					List<Integer> ind = new ArrayList<>();
					List<Integer> tx = new ArrayList<>();
					List<Integer> ty = new ArrayList<>();
					List<Float> heights = new ArrayList<>();
					List<Integer> indexes = pathData.indexes;
					for (int i = 0; i < indexes.size(); i++) {
						Integer index = indexes.get(i);
						if (previousVisible && index >= INITIAL_POINT_INDEX_SHIFT) {
							continue;
						}
						if (index >= startLocationIndex - 1) {
							ind.add(index);
							tx.add(pathData.tx.get(i));
							ty.add(pathData.ty.get(i));
							if (pathData.heights != null) {
								heights.add(pathData.heights.get(i));
							}
							if (firstX31 == -1) {
								if (index >= startLocationIndexCached && index > 0) {
									passedDist += pathData.distances.get(i);
								}
								firstX31 = pathData.tx.get(i);
								firstY31 = pathData.ty.get(i);
							}
						} else if (!update && index >= startLocationIndexCached && index > 0) {
							if (pathData.distances.get(i) == 0 && i > 0) {
								passedDist += (float) MapUtils.measuredDist31(
										pathData.tx.get(i - 1), pathData.ty.get(i - 1), pathData.tx.get(i), pathData.ty.get(i));
							} else {
								passedDist += pathData.distances.get(i);
							}
						}
					}
					if (previousVisible) {
						if (startLocationIndexCached == -1) {
							startLocationIndexCached = startLocationIndex;
						}
						if (!update && !this.points.isEmpty() && firstX31 != -1) {
							GeometryWayPoint firstPnt = this.points.get(0);
							passedDist += (float) MapUtils.measuredDist31(firstX31, firstY31, firstPnt.tx31, firstPnt.ty31);
							update = true;
						}
					} else {
						drawNext = true;
					}
					if (create && tx.size() > 1) {
						DrawPathData31 newPathData = new DrawPathData31(ind, tx, ty, pathData.style);
						if (!heights.isEmpty()) {
							newPathData.heights = heights;
						}
						newPathsDataList.add(newPathData);
					}
				}
			}
			if (create) {
				croppedPathsData31.add(newPathsDataList);
				drawPathLine(tb, newPathsDataList);
			}
		}
		if (update) {
			updatePathLine(passedDist);
			if (!create) {
				return null;
			}
		}
		if (shouldDrawArrows()) {
			VectorLinesCollection vectorLinesCollection = this.vectorLinesCollection;
			VectorLineArrowsProvider vectorLineArrowsProvider = this.vectorLineArrowsProvider;

			boolean updateArrowsProvider = vectorLineArrowsProvider == null
					|| !mapRenderer.hasSymbolsProvider(vectorLineArrowsProvider);
			if (vectorLinesCollection != null && updateArrowsProvider) {
				VectorLineArrowsProvider newArrowsProvider = new VectorLineArrowsProvider(vectorLinesCollection);
				newArrowsProvider.setPriority(linesPriority);
				this.vectorLineArrowsProvider = newArrowsProvider;
				mapRenderer.addSymbolsProvider(newArrowsProvider);
			}
		}

		return create ? croppedPathsData31 : null;
	}

	protected boolean shouldDrawArrows() {
		return true;
	}

	private void clearArrays() {
		points.clear();
	}


	public void drawRouteSegment(@NonNull RotatedTileBox tb, @Nullable Canvas canvas,
	                             List<GeometryWayPoint> points, double distToFinish) {
		if (points.size() < 2) {
			return;
		}
		boolean hasPathLine = false;
		boolean canvasRotated = false;
		boolean hasMapRenderer = hasMapRenderer();
		try {
			for (GeometryWayPoint p : points) {
				GeometryWayStyle<?> style = p.style;
				if (style.hasPathLine()) {
					hasPathLine = true;
					break;
				}
			}
			if (hasPathLine) {
				if (hasMapRenderer) {
					List<DrawPathData31> pathsData = GeometryWayPathAlgorithms.calculatePath(points);
					if (!Algorithms.isEmpty(pathsData)) {
						drawPathLine(tb, pathsData);
					}
					pathsData31Cache.add(pathsData);
				} else if (canvas != null) {
					canvas.rotate(-tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
					canvasRotated = true;
					List<DrawPathData> pathsData = GeometryWayPathAlgorithms.calculatePath(tb, points, null, null, null);
					if (!Algorithms.isEmpty(pathsData)) {
						drawPathLine(canvas, tb, pathsData);
					}
					context.clearCustomColor();
					context.clearCustomShader();
				}
			}
			if (shouldDrawArrows()) {
				if (hasMapRenderer) {
					MapRendererView mapRenderer = getMapRenderer();
					VectorLinesCollection vectorLinesCollection = this.vectorLinesCollection;
					VectorLineArrowsProvider vectorLineArrowsProvider = this.vectorLineArrowsProvider;
					if (mapRenderer != null && vectorLinesCollection != null
							&& (vectorLineArrowsProvider == null || !mapRenderer.hasSymbolsProvider(vectorLineArrowsProvider))) {
						VectorLineArrowsProvider arrowsProvider = new VectorLineArrowsProvider(vectorLinesCollection);
						arrowsProvider.setPriority(linesPriority);
						this.vectorLineArrowsProvider = arrowsProvider;
						mapRenderer.addSymbolsProvider(arrowsProvider);
					}
				} else if (canvas != null) {
					drawer.drawArrowsOverPath(canvas, tb, points, distToFinish);
				}
			} else {
				if (hasMapRenderer) {
					MapRendererView mapRenderer = getMapRenderer();
					VectorLineArrowsProvider arrowsProvider = this.vectorLineArrowsProvider;
					if (mapRenderer != null && arrowsProvider != null) {
						mapRenderer.removeSymbolsProvider(arrowsProvider);
						this.vectorLineArrowsProvider = null;
					}
				}
			}
		} finally {
			if (canvasRotated) {
				canvas.rotate(tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
			}
		}
	}

	private void drawPathLine(Canvas canvas, RotatedTileBox tb, List<DrawPathData> pathsData) {
		drawer.drawFullBorder(canvas, tb.getZoom(), pathsData);
		drawer.drawSegmentBorder(canvas, tb.getZoom(), pathsData.get(0));
		for (int i = 1; i <= pathsData.size(); i++) {
			if (i != pathsData.size()) {
				DrawPathData prev = pathsData.get(i);
				if (prev.style.hasPathLine()) {
					drawer.drawSegmentBorder(canvas, tb.getZoom(), prev);
				}
			}
			DrawPathData pd = pathsData.get(i - 1);
			GeometryWayStyle<?> style = pd.style;
			if (style.hasPathLine()) {
				drawer.drawPath(canvas, pd);
			}
		}
	}

	private void drawPathLine(RotatedTileBox tb, List<DrawPathData31> pathsData) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			VectorLinesCollection vectorLinesCollection = this.vectorLinesCollection;
			VectorLinesCollection collection;
			boolean newLine3DState = false;
			if (!Algorithms.isEmpty(pathsData)) {
				newLine3DState = pathsData.get(0).style.trackVisualizationType != Gpx3DVisualizationType.NONE;
			}
			boolean currentLine3DState = vectorLinesCollection != null &&
					vectorLinesCollection.getHasVolumetricSymbols();
			if (vectorLinesCollection == null ||
					!mapRenderer.hasSymbolsProvider(vectorLinesCollection) ||
					newLine3DState != currentLine3DState) {
				if (vectorLinesCollection != null &&
						mapRenderer.hasSymbolsProvider(vectorLinesCollection)) {
					mapRenderer.removeSymbolsProvider(vectorLinesCollection);
				}
				collection = new VectorLinesCollection(newLine3DState);
				collection.setPriority(linesPriority);
			} else {
				collection = vectorLinesCollection;
			}
			drawer.drawPath(collection, baseOrder, shouldDrawArrows(), pathsData);
			mapRenderer.addSymbolsProvider(collection);
			this.vectorLinesCollection = collection;
		}
	}

	private void updatePathLine(float startingDistance) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			VectorLinesCollection vectorLinesCollection = this.vectorLinesCollection;
			if (vectorLinesCollection != null) {
				drawer.updatePath(vectorLinesCollection, startingDistance);
			}
		}
	}
}
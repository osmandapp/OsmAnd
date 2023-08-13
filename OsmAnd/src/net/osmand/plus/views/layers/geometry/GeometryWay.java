package net.osmand.plus.views.layers.geometry;

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.PointF;

import net.osmand.Location;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.VectorLineArrowsProvider;
import net.osmand.core.jni.VectorLinesCollection;
import net.osmand.data.RotatedTileBox;
import net.osmand.gpx.GPXUtilities;
import net.osmand.plus.views.layers.geometry.GeometryWayDrawer.DrawPathData;
import net.osmand.plus.views.layers.geometry.GeometryWayDrawer.DrawPathData31;
import net.osmand.util.Algorithms;
import net.osmand.util.MapAlgorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import gnu.trove.list.array.TByteArrayList;

public abstract class GeometryWay<T extends GeometryWayContext, D extends GeometryWayDrawer<T>> {

	protected static final int INITIAL_POINT_INDEX_SHIFT = 1 << 30;

	private double mapDensity;
	private final T context;
	private final D drawer;
	private GeometryWayProvider locationProvider;
	protected Map<Integer, GeometryWayStyle<?>> styleMap = Collections.emptyMap();
	protected TreeMap<Integer, PathGeometryZoom> zooms = new TreeMap<>();

	// cache arrays
	private final List<Integer> indexes = new ArrayList<>();
	private final List<Float> tx = new ArrayList<>();
	private final List<Float> ty = new ArrayList<>();
	private final List<Double> angles = new ArrayList<>();
	private final List<Double> distances = new ArrayList<>();
	private final List<GeometryWayStyle<?>> styles = new ArrayList<>();

	//OpenGL
	private final List<Integer> tx31 = new ArrayList<>();
	private final List<Integer> ty31 = new ArrayList<>();
	protected List<List<DrawPathData31>> pathsData31Cache = new ArrayList<>();
	public int baseOrder = -1;
	public VectorLinesCollection vectorLinesCollection;
	public VectorLineArrowsProvider vectorLineArrowsProvider;

	public interface GeometryWayProvider {
		double getLatitude(int index);

		double getLongitude(int index);

		int getSize();
	}

	private static class GeometryWayLocationProvider implements GeometryWayProvider {
		private final List<Location> locations;

		public GeometryWayLocationProvider(@NonNull List<Location> locations) {
			this.locations = locations;
		}

		@Override
		public double getLatitude(int index) {
			return locations.get(index).getLatitude();
		}

		@Override
		public double getLongitude(int index) {
			return locations.get(index).getLongitude();
		}

		@Override
		public int getSize() {
			return locations.size();
		}
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

		for (int i = startLocationIndex; i < locationProvider.getSize(); i++) {
			style = getStyle(i, defaultWayStyle);
			if (shouldSkipLocation(simplification, styleMap, i)) {
				continue;
			}
			if (shouldAddLocation(simplification, leftLongitude, rightLongitude, bottomLatitude, topLatitude,
					locationProvider, previousVisibleIdx, i)) {
				double dist = previous == -1 || odistances == null ? 0 : odistances.get(i);
				if (!previousVisible && !ignorePrevious) {
					if (previous != -1 && !isPreviousPointFarAway(locationProvider, previous, i)) {
						addLocation(tb, previous, dist, style, indexes, tx, ty, tx31, ty31, angles, distances, styles);
					} else if (lastProjection != null) {
						addLocation(tb, lastProjection.getLatitude(), lastProjection.getLongitude(), i - 1, dist, true,
								getStyle(i - 1, style), indexes, tx, ty, tx31, ty31, angles, distances, styles); // first point
					}
				}
				addLocation(tb, i, dist, style, indexes, tx, ty, tx31, ty31, angles, distances, styles);
				previousVisible = true;
				previousVisibleIdx = i;
				ignorePrevious = false;
			} else if (previousVisible) {
				if (isPreviousPointFarAway(locationProvider, previousVisibleIdx, i)) {
					ignorePrevious = true;
					previousVisibleIdx = -1;
				} else {
					addLocation(tb, i, previous == -1 || odistances == null ? 0 : odistances.get(i), style,
							indexes, tx, ty, tx31, ty31, angles, distances, styles);
					ignorePrevious = false;
				}
				double distToFinish = 0;
				if (odistances != null) {
					for (int ki = i + 1; ki < odistances.size(); ki++) {
						distToFinish += odistances.get(ki);
					}
				}
				drawRouteSegment(tb, canvas, indexes, tx, ty, tx31, ty31, angles, distances, distToFinish, styles);
				previousVisible = false;
				clearArrays();
			}
			previous = i;
		}
		drawRouteSegment(tb, canvas, indexes, tx, ty, tx31, ty31, angles, distances, 0, styles);
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
			boolean primeMeridianPoints = Math.max(prevLon, lon) == GPXUtilities.PRIME_MERIDIAN
					&& Math.min(prevLon, lon) == -GPXUtilities.PRIME_MERIDIAN;
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
	                           GeometryWayStyle<?> style, List<Integer> indexes,
	                           List<Float> tx, List<Float> ty,
	                           List<Integer> tx31, List<Integer> ty31,
	                           List<Double> angles, List<Double> distances,
	                           List<GeometryWayStyle<?>> styles) {
		addLocation(tb, locationProvider.getLatitude(locationIdx), locationProvider.getLongitude(locationIdx),
				locationIdx, dist, false, style, indexes, tx, ty, tx31, ty31, angles, distances, styles);
	}

	protected void addLocation(RotatedTileBox tb, double latitude, double longitude, int locationIdx,
	                           double dist, boolean initialPoint,
	                           GeometryWayStyle<?> style, List<Integer> indexes,
	                           List<Float> tx, List<Float> ty,
	                           List<Integer> tx31, List<Integer> ty31,
	                           List<Double> angles, List<Double> distances,
	                           List<GeometryWayStyle<?>> styles) {
		indexes.add((initialPoint ? INITIAL_POINT_INDEX_SHIFT : 0) +  locationIdx);

		if (hasMapRenderer())
		{
			tx31.add(MapUtils.get31TileNumberX(longitude));
			ty31.add(MapUtils.get31TileNumberY(latitude));
			styles.add(style);
			return;
		}

		float x = tb.getPixXFromLatLon(latitude, longitude);
		float y = tb.getPixYFromLatLon(latitude, longitude);
		float px = x;
		float py = y;
		int previous = tx.size() - 1;
		if (previous >= 0) {
			px = tx.get(previous);
			py = ty.get(previous);
		}
		double angle = 0;
		if (px != x || py != y) {
			double angleRad = Math.atan2(y - py, x - px);
			angle = (angleRad * 180 / Math.PI) + 90f;
		}
		double distSegment = dist != 0 ? dist : Math.sqrt((y - py) * (y - py) + (x - px) * (x - px));
		tx.add(x);
		ty.add(y);
		angles.add(angle);
		distances.add(distSegment);
		styles.add(style);
	}

	protected boolean addInitialPoint(RotatedTileBox tb, double topLatitude, double leftLongitude, double bottomLatitude,
									  double rightLongitude, GeometryWayStyle<?> style, boolean previousVisible,
									  Location lastPoint, int startLocationIndex) {
		if (hasMapRenderer() || (leftLongitude <= lastPoint.getLongitude() && lastPoint.getLongitude() <= rightLongitude
				&& bottomLatitude <= lastPoint.getLatitude() && lastPoint.getLatitude() <= topLatitude)) {
			addLocation(tb, lastPoint.getLatitude(), lastPoint.getLongitude(), startLocationIndex, 0, true,
					style, indexes, tx, ty, tx31, ty31, angles, distances, styles);
			return true;
		}
		return false;
	}

	protected void cutStartOfCachedPath(@NonNull MapRendererView mapRenderer,
	                                    @NonNull RotatedTileBox tb,
	                                    int startLocationIndex,
	                                    boolean previousVisible) {
		List<List<DrawPathData31>> newPathsData31Cache = new ArrayList<>();
		boolean drawNext = false;
		for (List<DrawPathData31> pathsDataList : pathsData31Cache) {
			if (drawNext) {
				newPathsData31Cache.add(pathsDataList);
				drawPathLine(tb, pathsDataList);
				continue;
			}
			List<DrawPathData31> newPathsDataList = new ArrayList<>();
			for (DrawPathData31 pathData : pathsDataList) {
				if (drawNext) {
					newPathsDataList.add(pathData);
					continue;
				}
				if (pathData.indexes.contains(startLocationIndex)) {
					List<Integer> ind = new ArrayList<>();
					List<Integer> tx = new ArrayList<>();
					List<Integer> ty = new ArrayList<>();
					List<Integer> indexes = pathData.indexes;
					for (int i = 0; i < indexes.size(); i++) {
						Integer index = indexes.get(i);
						if (previousVisible && index >= INITIAL_POINT_INDEX_SHIFT) {
							continue;
						}
						if (index >= startLocationIndex) {
							ind.add(index);
							tx.add(pathData.tx.get(i));
							ty.add(pathData.ty.get(i));
						}
					}
					if (previousVisible) {
						if (!this.indexes.isEmpty()) {
							Integer index = this.indexes.get(0);
							ind.add(0, index);
							tx.add(0, this.tx31.get(0));
							ty.add(0, this.ty31.get(0));
						}
					}
					if (tx.size() > 1) {
						DrawPathData31 newPathData = new DrawPathData31(ind, tx, ty, pathData.style);
						newPathsDataList.add(newPathData);
					}
					drawNext = true;
				}
			}
			newPathsData31Cache.add(newPathsDataList);
			drawPathLine(tb, newPathsDataList);
		}

		if (drawNext) {
			this.pathsData31Cache = newPathsData31Cache;
		}

		if (shouldDrawArrows()) {
			VectorLinesCollection vectorLinesCollection = this.vectorLinesCollection;
			VectorLineArrowsProvider vectorLineArrowsProvider = this.vectorLineArrowsProvider;

			boolean updateArrowsProvider = vectorLineArrowsProvider == null
					|| !mapRenderer.hasSymbolsProvider(vectorLineArrowsProvider);
			if (vectorLinesCollection != null && updateArrowsProvider) {
				VectorLineArrowsProvider newArrowsProvider = new VectorLineArrowsProvider(vectorLinesCollection);
				this.vectorLineArrowsProvider = newArrowsProvider;
				mapRenderer.addSymbolsProvider(newArrowsProvider);
			}
		}
	}

	protected boolean shouldDrawArrows() {
		return true;
	}

	private void clearArrays() {
		indexes.clear();
		tx.clear();
		ty.clear();
		tx31.clear();
		ty31.clear();
		distances.clear();
		angles.clear();
		styles.clear();
	}

	public static boolean isIn(float x, float y, int lx, int ty, int rx, int by) {
		return x >= lx && x <= rx && y >= ty && y <= by;
	}

	public static int calculatePath(@NonNull RotatedTileBox tb,
	                                @NonNull List<Float> xs, @NonNull List<Float> ys,
	                                @NonNull Path path) {
		List<DrawPathData> pathsData = new ArrayList<>();
		int res = calculatePath(tb, xs, ys, null, pathsData);
		if (pathsData.size() > 0) {
			path.addPath(pathsData.get(0).path);
		}
		return res;
	}

	public static int calculatePath(@NonNull RotatedTileBox tb,
	                                @NonNull List<Float> xs, @NonNull List<Float> ys,
									@Nullable List<GeometryWayStyle<?>> styles,
									@NonNull List<DrawPathData> pathsData) {
		boolean segmentStarted = false;
		float prevX = xs.get(0);
		float prevY = ys.get(0);
		int height = tb.getPixHeight();
		int width = tb.getPixWidth();
		int cnt = 0;
		boolean hasStyles = styles != null && styles.size() == xs.size();
		GeometryWayStyle<?> style = hasStyles ? styles.get(0) : null;
		Path path = new Path();
		float prevXorig = prevX;
		float prevYorig = prevY;
		float currXorig = Float.NaN;
		float currYorig = Float.NaN;
		boolean prevIn = isIn(prevX, prevY, 0, 0, width, height);
		for (int i = 1; i < xs.size(); i++) {
			float currX = xs.get(i);
			float currY = ys.get(i);
			currXorig = currX;
			currYorig = currY;
			boolean currIn = isIn(currX, currY, 0, 0, width, height);
			boolean draw = false;
			if (prevIn && currIn) {
				draw = true;
			} else {
				long intersection = MapAlgorithms.calculateIntersection((int) currX, (int) currY,
						(int) prevX, (int) prevY, 0, width, height, 0);
				if (intersection != -1) {
					if (prevIn && (i == 1)) {
						cnt++;
						path.moveTo(prevX, prevY);
						segmentStarted = true;
					}
					prevX = (int) (intersection >> 32);
					prevY = (int) (intersection & 0xffffffff);
					draw = true;
				}
				if (i == xs.size() - 1 && !currIn) {
					long inter = MapAlgorithms.calculateIntersection((int) prevX, (int) prevY,
							(int) currX, (int) currY, 0, width, height, 0);
					if (inter != -1) {
						currX = (int) (inter >> 32);
						currY = (int) (inter & 0xffffffff);
					}
				}
			}
			if (draw) {
				if (!segmentStarted) {
					cnt++;
					path.moveTo(prevX, prevY);
					segmentStarted = true;
				}
				path.lineTo(currX, currY);
			} else {
				segmentStarted = false;
			}
			prevIn = currIn;
			prevX = currX;
			prevY = currY;

			if (hasStyles) {
				GeometryWayStyle<?> newStyle = styles.get(i);
				if (!style.equals(newStyle) || newStyle.isUnique()) {
					pathsData.add(new DrawPathData(path, new PointF(prevXorig, prevYorig),
							new PointF(currXorig, currYorig), style));
					prevXorig = currXorig;
					prevYorig = currYorig;
					path = new Path();
					if (segmentStarted) {
						path.moveTo(currX, currY);
					}
					style = newStyle;
				}
			}
		}
		if (!path.isEmpty() && !Float.isNaN(currXorig)) {
			pathsData.add(new DrawPathData(path, new PointF(prevXorig, prevYorig),
					new PointF(currXorig, currYorig), style));
		}
		return cnt;
	}

	public static void calculatePath(@NonNull List<Integer> indexes,
	                                 @NonNull List<Integer> xs, @NonNull List<Integer> ys,
	                                 @Nullable List<GeometryWayStyle<?>> styles,
	                                 @NonNull List<DrawPathData31> pathsData) {
		boolean hasStyles = styles != null && styles.size() == xs.size();
		GeometryWayStyle<?> style = hasStyles ? styles.get(0) : null;
		List<Integer> ind = new ArrayList<>();
		List<Integer> tx = new ArrayList<>();
		List<Integer> ty = new ArrayList<>();
		ind.add(indexes.get(0));
		tx.add(xs.get(0));
		ty.add(ys.get(0));
		for (int i = 1; i < xs.size(); i++) {
			ind.add(indexes.get(i));
			tx.add(xs.get(i));
			ty.add(ys.get(i));
			if (hasStyles) {
				GeometryWayStyle<?> newStyle = styles.get(i);
				if (!style.equals(newStyle) || newStyle.isUnique()) {
					pathsData.add(new DrawPathData31(ind, tx, ty, style));
					ind = new ArrayList<>();
					tx = new ArrayList<>();
					ty = new ArrayList<>();
					ind.add(indexes.get(i));
					tx.add(xs.get(i));
					ty.add(ys.get(i));
					style = newStyle;
				}
			}
		}
		if (tx.size() > 1) {
			pathsData.add(new DrawPathData31(ind, tx, ty, style));
		}
	}

	public void drawRouteSegment(@NonNull RotatedTileBox tb, @Nullable Canvas canvas,
	                             List<Integer> indexes,
	                             List<Float> tx, List<Float> ty,
	                             List<Integer> tx31, List<Integer> ty31,
								 List<Double> angles, List<Double> distances, double distToFinish,
								 List<GeometryWayStyle<?>> styles) {
		boolean hasMapRenderer = hasMapRenderer();
		if (hasMapRenderer) {
			if (tx31.size() < 2) {
				return;
			}
		} else if (tx.size() < 2) {
			return;
		}
		boolean hasPathLine = false;
		boolean canvasRotated = false;
		try {
			for (GeometryWayStyle<?> style : styles) {
				if (style.hasPathLine()) {
					hasPathLine = true;
					break;
				}
			}
			if (hasPathLine) {
				if (hasMapRenderer) {
					List<DrawPathData31> pathsData = new ArrayList<>();
					calculatePath(indexes, tx31, ty31, styles, pathsData);
					if (!Algorithms.isEmpty(pathsData)) {
						drawPathLine(tb, pathsData);
					}
					pathsData31Cache.add(pathsData);
				} else if (canvas != null) {
					List<DrawPathData> pathsData = new ArrayList<>();
					canvas.rotate(-tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
					canvasRotated = true;
					calculatePath(tb, tx, ty, styles, pathsData);

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
						this.vectorLineArrowsProvider = arrowsProvider;
						mapRenderer.addSymbolsProvider(arrowsProvider);
					}
				} else if (canvas != null) {
					drawer.drawArrowsOverPath(canvas, tb, tx, ty, angles, distances, distToFinish, styles);
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
			VectorLinesCollection collection = vectorLinesCollection == null || !mapRenderer.hasSymbolsProvider(vectorLinesCollection)
					? new VectorLinesCollection() : vectorLinesCollection;
			drawer.drawPath(collection, baseOrder, shouldDrawArrows(), pathsData);
			mapRenderer.addSymbolsProvider(collection);
			this.vectorLinesCollection = collection;
		}
	}

	protected static class PathGeometryZoom {

		private static final float EPSILON_IN_DPI = 2;

		private final TByteArrayList simplifyPoints;
		private final List<Double> distances;
		private final List<Double> angles;

		public PathGeometryZoom(GeometryWayProvider locationProvider, RotatedTileBox tb, boolean simplify,
		                        @NonNull List<Integer> forceIncludedIndexes) {
			//  this.locations = locations;
			tb = new RotatedTileBox(tb);
			tb.setZoomAndAnimation(tb.getZoom(), 0, tb.getZoomFloatPart());
			int size = locationProvider.getSize();
			simplifyPoints = new TByteArrayList(size);
			distances = new ArrayList<>(size);
			angles = new ArrayList<>(size);
			if (simplify) {
				simplifyPoints.fill(0, size, (byte) 0);
				simplify(tb, locationProvider, simplifyPoints);
			} else {
				simplifyPoints.fill(0, size, (byte) 1);
			}
			int previousIndex = -1;
			for (int i = 0; i < size; i++) {
				double d = 0;
				double angle = 0;
				if (simplifyPoints.get(i) > 0 || forceIncludedIndexes.contains(i)) {
					if (previousIndex > -1) {
						float x = tb.getPixXFromLatLon(locationProvider.getLatitude(i), locationProvider.getLongitude(i));
						float y = tb.getPixYFromLatLon(locationProvider.getLatitude(i), locationProvider.getLongitude(i));
						float px = tb.getPixXFromLatLon(locationProvider.getLatitude(previousIndex), locationProvider.getLongitude(previousIndex));
						float py = tb.getPixYFromLatLon(locationProvider.getLatitude(previousIndex), locationProvider.getLongitude(previousIndex));
						d = Math.sqrt((y - py) * (y - py) + (x - px) * (x - px));
						if (px != x || py != y) {
							double angleRad = Math.atan2(y - py, x - px);
							angle = (angleRad * 180 / Math.PI) + 90f;
						}
					}
					previousIndex = i;
				}
				distances.add(d);
				angles.add(angle);
			}
		}

		protected void simplify(RotatedTileBox tb, GeometryWayProvider locationProvider, TByteArrayList simplifyPoints) {
			int size = locationProvider.getSize();
			if (size > 0) {
				simplifyPoints.set(0, (byte) 1);
			}
			double distInPix = (tb.getDistance(0, 0, tb.getPixWidth(), 0) / tb.getPixWidth());
			double cullDistance = (distInPix * (EPSILON_IN_DPI * Math.max(1, tb.getDensity())));
			cullRamerDouglasPeucker(simplifyPoints, locationProvider, 0, size - 1, cullDistance);
		}

		public List<Double> getDistances() {
			return distances;
		}

		public List<Double> getAngles() {
			return angles;
		}

		private void cullRamerDouglasPeucker(TByteArrayList survivor, GeometryWayProvider locationProvider,
											 int start, int end, double epsillon) {
			double dmax = Double.NEGATIVE_INFINITY;
			int index = -1;
			for (int i = start + 1; i < end; i++) {
				double d = MapUtils.getOrthogonalDistance(locationProvider.getLatitude(i), locationProvider.getLongitude(i),
						locationProvider.getLatitude(start), locationProvider.getLongitude(start),
						locationProvider.getLatitude(end), locationProvider.getLongitude(end));
				if (d > dmax) {
					dmax = d;
					index = i;
				}
			}
			if (dmax > epsillon) {
				cullRamerDouglasPeucker(survivor, locationProvider, start, index, epsillon);
				cullRamerDouglasPeucker(survivor, locationProvider, index, end, epsillon);
			} else {
				survivor.set(end, (byte) 1);
			}
		}

		public TByteArrayList getSimplifyPoints() {
			return simplifyPoints;
		}
	}
}
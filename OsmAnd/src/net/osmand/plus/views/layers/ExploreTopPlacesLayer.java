package net.osmand.plus.views.layers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.MapMarker;
import net.osmand.core.jni.MapMarkerBuilder;
import net.osmand.core.jni.MapMarkersCollection;
import net.osmand.core.jni.PointI;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.core.jni.QListMapMarker;
import net.osmand.data.ExploreTopPlacePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.exploreplaces.ExplorePlacesProvider;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.shared.util.ImageLoaderCallback;
import net.osmand.shared.util.LoadingImage;
import net.osmand.shared.util.NetworkImageLoader;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ExploreTopPlacesLayer extends OsmandMapLayer implements IContextMenuProvider, ExplorePlacesProvider.ExplorePlacesListener {

	private static final int START_ZOOM = 2;
	private static final Log LOG = PlatformUtil.getLog(ExploreTopPlacesLayer.class);

	private boolean nightMode;
	private ExploreTopPlacePoint selectedObject;
	private static final int SELECTED_MARKER_ID = -1;

	private static final int SMALL_ICON_BORDER_DP = 1;
	private static final int BIG_ICON_BORDER_DP = 2;
	private static final int SMALL_ICON_SIZE_DP = 20;
	private static final int BIG_ICON_SIZE_DP = 40;
	private static final int POINT_OUTER_COLOR = 0xffffffff;
	private static Bitmap circleBitmap;
	private Bitmap cachedSmallIconBitmap;

	private QuadRect requestQuadRect = null; // null means disabled
	private int requestZoom = 0; // null means disabled

	protected MapMarkersCollection otherPlacesCollection;
	private ExplorePlacesProvider explorePlacesProvider;
	private int cachedExploreDataVersion;
	private List<ExploreTopPlacePoint> places;
	private Map<Long, ExploreTopPlacePoint> visibleTopPlaces;

	// To refresh images
	private static final int TOP_LOAD_PHOTOS = 25;

	private final NetworkImageLoader imageLoader;
	private final Map<String, LoadingImage> loadingImages = new HashMap<>();

	private static class MapPoint {
		private final PointI position;
		private final boolean alreadyExists;
		@Nullable
		private final Bitmap imageBitmap;

		public MapPoint(@NonNull PointI position, @Nullable Bitmap imageBitmap, boolean alreadyExists) {
			this.position = position;
			this.imageBitmap = imageBitmap;
			this.alreadyExists = alreadyExists;
		}
	}

	public ExploreTopPlacesLayer(@NonNull Context ctx) {
		super(ctx);

		imageLoader = new NetworkImageLoader(ctx, true);
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);

		nightMode = getApplication().getDaynightHelper().isNightMode();
		explorePlacesProvider = getApplication().getExplorePlacesProvider();
		explorePlacesProvider.addListener(this);
		recreateBitmaps();
	}



	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
	}

	private void recreateBitmaps() {
		cachedSmallIconBitmap = createSmallPointBitmap();
	}

	@Override
	protected void updateResources() {
		super.updateResources();
		recreateBitmaps();
	}

	@Override
	protected void cleanupResources() {
		super.cleanupResources();
		explorePlacesProvider.removeListener(this);
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		super.onPrepareBufferImage(canvas, tileBox, settings);
		boolean nightMode = settings != null && settings.isNightMode();
		boolean nightModeChanged = this.nightMode != nightMode;
		this.nightMode = nightMode;
		boolean placesUpdated = false;
		if (requestQuadRect != null) {
			int exploreDataVersion = explorePlacesProvider.getDataVersion();
			if (!requestQuadRect.contains(tileBox.getLatLonBounds()) ||
					cachedExploreDataVersion < exploreDataVersion || places == null ||
					(requestZoom < tileBox.getZoom() && requestZoom < ExplorePlacesProvider.MAX_LEVEL_ZOOM_CACHE)) {
				placesUpdated = true;
				RotatedTileBox extended = tileBox.copy();
				extended.increasePixelDimensions(tileBox.getPixWidth() / 4, tileBox.getPixHeight() / 4);
				requestQuadRect = extended.getLatLonBounds();
				requestZoom = tileBox.getZoom();
				cachedExploreDataVersion = explorePlacesProvider.getDataVersion();
				places = explorePlacesProvider.getDataCollection(requestQuadRect);
			}
		} else {
			if (places != null) {
				placesUpdated = true;
				places = null;
				visibleTopPlaces = null;
			}
		}

		ExploreTopPlacePoint selectedObject = getSelectedNearbyPlace();
		long selectedObjectId = selectedObject == null ? 0 : selectedObject.getId();
		long lastSelectedObjectId = this.selectedObject == null ? 0 : this.selectedObject.getId();
		boolean selectedObjectChanged = selectedObjectId != lastSelectedObjectId;
		this.selectedObject = selectedObject;
		if (hasMapRenderer()) {
			if ((mapActivityInvalidated || mapRendererChanged || nightModeChanged || placesUpdated)) {
				visibleTopPlaces = places != null ? getTopPlacesToDisplay(places, tileBox) : null;
				fetchImages();
				updateOtherPlacesCollection(tileBox);
				updateTopPlacesCollection();
				mapRendererChanged = false;
			}
			if (selectedObjectChanged) {
				showSelectedNearbyPoint();
			}
		} else {
			if (places != null && tileBox.getZoom() >= START_ZOOM) {
				float iconSize = getIconSize(view.getApplication());
				QuadTree<QuadRect> boundIntersections = initBoundIntersections(tileBox);
				QuadRect latLonBounds = tileBox.getLatLonBounds();
				List<LatLon> fullObjectsLatLon = new ArrayList<>();
				List<LatLon> smallObjectsLatLon = new ArrayList<>();
				drawPoints(places, latLonBounds, tileBox, boundIntersections, iconSize, canvas,
						fullObjectsLatLon, smallObjectsLatLon);
				this.fullObjectsLatLon = fullObjectsLatLon;
				this.smallObjectsLatLon = smallObjectsLatLon;
			}
		}
		mapActivityInvalidated = false;
	}

	private void drawPoints(List<ExploreTopPlacePoint> pointsToDraw, QuadRect latLonBounds, RotatedTileBox tileBox,
	                        QuadTree<QuadRect> boundIntersections, float iconSize, Canvas canvas,
	                        List<LatLon> fullObjectsLatLon, List<LatLon> smallObjectsLatLon) {
		List<ExploreTopPlacePoint> fullObjects = new ArrayList<>();
		Paint pointPaint = new Paint();
		if (cachedSmallIconBitmap == null) {
			cachedSmallIconBitmap = createSmallPointBitmap();
		}
		for (ExploreTopPlacePoint nearbyPoint : pointsToDraw) {
			double lat = nearbyPoint.getLatitude();
			double lon = nearbyPoint.getLongitude();
			if (lat >= latLonBounds.bottom && lat <= latLonBounds.top
					&& lon >= latLonBounds.left && lon <= latLonBounds.right) {
				float x = tileBox.getPixXFromLatLon(lat, lon);
				float y = tileBox.getPixYFromLatLon(lat, lon);
				if (intersects(boundIntersections, x, y, iconSize, iconSize) || nearbyPoint.getImageBitmap() == null) {
					canvas.drawBitmap(cachedSmallIconBitmap, x, y, pointPaint);
					smallObjectsLatLon.add(new LatLon(lat, lon));
				} else {
					fullObjects.add(nearbyPoint);
					fullObjectsLatLon.add(new LatLon(lat, lon));
				}
			}
		}
		for (ExploreTopPlacePoint point : fullObjects) {
			Bitmap bitmap = point.getImageBitmap();
			if (bitmap != null) {
				Bitmap bigBitmap = createBigBitmap(bitmap, point.getId() == getSelectedObjectId());
				float x = tileBox.getPixXFromLatLon(point.getLatitude(), point.getLongitude());
				float y = tileBox.getPixYFromLatLon(point.getLatitude(), point.getLongitude());
				canvas.drawBitmap(bigBitmap, x - bigBitmap.getWidth() / 2f, y - bigBitmap.getHeight() / 2f, pointPaint);
			}
		}
	}

	private long getSelectedObjectId() {
		return selectedObject == null ? 0 : selectedObject.getId();
	}

	@NonNull
	private Map<Long, ExploreTopPlacePoint> getTopPlacesToDisplay(@NonNull List<ExploreTopPlacePoint> points, @NonNull RotatedTileBox tileBox) {
		Map<Long, ExploreTopPlacePoint> res = new HashMap<>();

		long tileSize31 = (1L << (31 - tileBox.getZoom()));
		double from31toPixelsScale = 256.0 / tileSize31;
		double estimatedIconSize = BIG_ICON_SIZE_DP * getTextScale();
		float iconSize31 = (float) (estimatedIconSize / from31toPixelsScale);

		QuadRect latLonBounds = tileBox.getLatLonBounds();
		int left = MapUtils.get31TileNumberX(latLonBounds.left);
		int top = MapUtils.get31TileNumberY(latLonBounds.top);
		int right = MapUtils.get31TileNumberX(latLonBounds.right);
		int bottom = MapUtils.get31TileNumberY(latLonBounds.bottom);
		QuadTree<QuadRect> boundIntersections = initBoundIntersections(left, top, right, bottom);
		for (ExploreTopPlacePoint point : points) {
			if (Algorithms.isEmpty(point.getIconUrl())) {
				continue;
			}
			int x31 = MapUtils.get31TileNumberX(point.getLongitude());
			int y31 = MapUtils.get31TileNumberY(point.getLatitude());
			if (!intersectsD(boundIntersections, x31, y31, iconSize31, iconSize31)) {
				res.put(point.getId(), point);
			}
			if (res.size() >= TOP_LOAD_PHOTOS) {
				break;
			}
		}
		return res;
	}

	private void updateTopPlacesCollection() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null) {
			return;
		}
		Collection<ExploreTopPlacePoint> points = visibleTopPlaces != null ? visibleTopPlaces.values() : null;
		if (points == null) {
			clearMapMarkersCollections();
			return;
		}
		if (mapMarkersCollection == null) {
			mapMarkersCollection = new MapMarkersCollection();
		}
		QListMapMarker existingMapPoints = mapMarkersCollection.getMarkers();
		int[] existingX = new int[(int)existingMapPoints.size()];
		int[] existingY = new int[(int)existingMapPoints.size()];
		for (int i = 0; i < existingMapPoints.size(); i++) {
			MapMarker mapPoint = existingMapPoints.get(i);
			PointI pos = mapPoint.getPosition();
			existingX[i] = pos.getX();
			existingY[i] = pos.getY();
		}
		List<MapPoint> newPoints = new ArrayList<>();
		for (ExploreTopPlacePoint point : points) {
            PointI position = NativeUtilities.getPoint31FromLatLon(point.getLatitude(), point.getLongitude());
            int x = position.getX();
            int y = position.getY();
            boolean alreadyExists = false;
            for (int i = 0; i < existingX.length; i++) {
                if (x == existingX[i] && y == existingY[i]) {
                    existingX[i] = 0;
                    existingY[i] = 0;
                    alreadyExists = true;
                    break;
                }
            }
            newPoints.add(new MapPoint(position, point.getImageBitmap(), alreadyExists));
        }
		for (MapPoint point : newPoints) {
			Bitmap imageBitmap = point.imageBitmap;
			if (point.alreadyExists || imageBitmap == null) {
				continue;
			}

			Bitmap imageMapBitmap = createBigBitmap(imageBitmap, false);

			MapMarkerBuilder mapMarkerBuilder = new MapMarkerBuilder();
			mapMarkerBuilder.setIsAccuracyCircleSupported(false)
					.setBaseOrder(getPointsOrder())
					.setPinIcon(NativeUtilities.createSkImageFromBitmap(imageMapBitmap))
					.setPosition(point.position)
					.setPinIconVerticalAlignment(MapMarker.PinIconVerticalAlignment.CenterVertical)
					.setPinIconHorisontalAlignment(MapMarker.PinIconHorisontalAlignment.CenterHorizontal)
					.buildAndAddToCollection(mapMarkersCollection);
		}
		for (int i = 0; i < existingX.length; i++) {
			if (existingX[i] != 0 && existingY[i] != 0) {
				mapMarkersCollection.removeMarker(existingMapPoints.get(i));
			}
		}
		mapRenderer.addSymbolsProvider(mapMarkersCollection);
	}

	private void updateOtherPlacesCollection(@NonNull RotatedTileBox tileBox) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null) {
			return;
		}
		Collection<ExploreTopPlacePoint> points = places;
		if (points == null) {
			clearMapMarkersCollections();
			return;
		}
		if (otherPlacesCollection == null) {
			otherPlacesCollection = new MapMarkersCollection();
		}
		if (cachedSmallIconBitmap == null) {
			cachedSmallIconBitmap = createSmallPointBitmap();
		}
		long tileSize31 = (1L << (31 - tileBox.getZoom()));
		double from31toPixelsScale = 256f / tileSize31;
		double estimatedIconSize = SMALL_ICON_SIZE_DP * getTextScale();
		double iconSize31 = estimatedIconSize / from31toPixelsScale;

		QListMapMarker existingMapPoints = otherPlacesCollection.getMarkers();
		int[] existingX = new int[(int)existingMapPoints.size()];
		int[] existingY = new int[(int)existingMapPoints.size()];
		for (int i = 0; i < existingMapPoints.size(); i++) {
			MapMarker mapPoint = existingMapPoints.get(i);
			PointI pos = mapPoint.getPosition();
			existingX[i] = pos.getX();
			existingY[i] = pos.getY();
		}
		List<MapPoint> newPoints = new ArrayList<>();
		QuadRect latLonBounds = tileBox.getLatLonBounds();
		int left = MapUtils.get31TileNumberX(latLonBounds.left);
		int top = MapUtils.get31TileNumberY(latLonBounds.top);
		int right = MapUtils.get31TileNumberX(latLonBounds.right);
		int bottom = MapUtils.get31TileNumberY(latLonBounds.bottom);
		QuadTree<QuadRect> boundIntersections = initBoundIntersections(left, top, right, bottom);
		for (ExploreTopPlacePoint point : points) {
			int x31 = MapUtils.get31TileNumberX(point.getLongitude());
			int y31 = MapUtils.get31TileNumberY(point.getLatitude());
			if (intersectsD(boundIntersections, x31, y31, iconSize31, iconSize31)) {
				continue;
			}
			PointI position = NativeUtilities.getPoint31FromLatLon(point.getLatitude(), point.getLongitude());
			int x = position.getX();
			int y = position.getY();
			boolean alreadyExists = false;
			for (int i = 0; i < existingX.length; i++) {
				if (x == existingX[i] && y == existingY[i]) {
					existingX[i] = 0;
					existingY[i] = 0;
					alreadyExists = true;
				}
			}
			newPoints.add(new MapPoint(position, point.getImageBitmap(), alreadyExists));
		}
		for (MapPoint point : newPoints) {
			MapMarkerBuilder mapMarkerBuilder = new MapMarkerBuilder();
			mapMarkerBuilder.setIsAccuracyCircleSupported(false)
					.setBaseOrder(getPointsOrder() + 100)
					.setPinIcon(NativeUtilities.createSkImageFromBitmap(cachedSmallIconBitmap))
					.setPosition(point.position)
					.setPinIconVerticalAlignment(MapMarker.PinIconVerticalAlignment.CenterVertical)
					.setPinIconHorisontalAlignment(MapMarker.PinIconHorisontalAlignment.CenterHorizontal)
					.buildAndAddToCollection(otherPlacesCollection);
		}
		for (int i = 0; i < existingX.length; i++) {
			if (existingX[i] != 0 && existingY[i] != 0) {
				otherPlacesCollection.removeMarker(existingMapPoints.get(i));
			}
		}
		mapRenderer.addSymbolsProvider(otherPlacesCollection);
	}

	@Override
	protected void clearMapMarkersCollections() {
		super.clearMapMarkersCollections();
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null && otherPlacesCollection != null) {
			mapRenderer.removeSymbolsProvider(otherPlacesCollection);
			otherPlacesCollection = null;
		}
	}

	public synchronized void showSelectedNearbyPoint() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null) {
			return;
		}
		if (mapMarkersCollection == null) {
			mapMarkersCollection = new MapMarkersCollection();
		}

		MapMarker previousSelectedMarker = null;
		QListMapMarker existingMapPoints = mapMarkersCollection.getMarkers();
		for (int i = 0; i < existingMapPoints.size(); i++) {
			MapMarker mapPoint = existingMapPoints.get(i);
			if (mapPoint.getMarkerId() == SELECTED_MARKER_ID) {
				previousSelectedMarker = mapPoint;
				break;
			}
		}
		Bitmap imageBitmap = selectedObject != null ? selectedObject.getImageBitmap() : null;
		if (imageBitmap != null) {
			Bitmap imageMapBitmap = createBigBitmap(imageBitmap, true);

			MapMarkerBuilder mapMarkerBuilder = new MapMarkerBuilder();
			mapMarkerBuilder.setIsAccuracyCircleSupported(false)
					.setMarkerId(SELECTED_MARKER_ID)
					.setBaseOrder(getPointsOrder() - 1)
					.setPinIcon(NativeUtilities.createSkImageFromBitmap(imageMapBitmap))
					.setPosition(NativeUtilities.getPoint31FromLatLon(selectedObject.getLatitude(), selectedObject.getLongitude()))
					.setPinIconVerticalAlignment(MapMarker.PinIconVerticalAlignment.CenterVertical)
					.setPinIconHorisontalAlignment(MapMarker.PinIconHorisontalAlignment.CenterHorizontal)
					.buildAndAddToCollection(mapMarkersCollection);
			mapRenderer.addSymbolsProvider(mapMarkersCollection);
		}
		if (previousSelectedMarker != null) {
			mapMarkersCollection.removeMarker(previousSelectedMarker);
		}
	}

    @Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof ExploreTopPlacePoint) {
			return ((ExploreTopPlacePoint) o).getPointDescription(getContext());
		}
		return null;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> res,
	                                    boolean unknownLocation, boolean excludeUntouchableObjects) {
		List<ExploreTopPlacePoint> points = places;
		if (points != null) {
			getNearbyPlaceFromPoint(tileBox, point, res, points);
		}
	}

	@Override
	public boolean runExclusiveAction(@Nullable Object o, boolean unknownLocation) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && (o instanceof ExploreTopPlacePoint)) {
			getApplication().getExplorePlacesProvider().showPointInContextMenu(mapActivity, (ExploreTopPlacePoint) o);
			return true;
		} else {
			return IContextMenuProvider.super.runExclusiveAction(o, unknownLocation);
		}
	}

	private void getNearbyPlaceFromPoint(RotatedTileBox tb, PointF point, List<? super ExploreTopPlacePoint> res, List<ExploreTopPlacePoint> points) {
		MapRendererView mapRenderer = getMapRenderer();
		float radius = getScaledTouchRadius(getApplication(), tb.getDefaultRadiusPoi()) * TOUCH_RADIUS_MULTIPLIER;
		List<PointI> touchPolygon31 = null;
		if (mapRenderer != null) {
			touchPolygon31 = NativeUtilities.getPolygon31FromPixelAndRadius(mapRenderer, point, radius);
			if (touchPolygon31 == null) {
				return;
			}
		}

		for (ExploreTopPlacePoint nearbyPoint : points) {
			double lat = nearbyPoint.getLatitude();
			double lon = nearbyPoint.getLongitude();
			boolean add = mapRenderer != null
					? NativeUtilities.isPointInsidePolygon(lat, lon, touchPolygon31)
					: tb.isLatLonNearPixel(lat, lon, point.x, point.y, radius);
			if (add) {
				res.add(nearbyPoint);
			}
		}
	}

	@Nullable
	private ExploreTopPlacePoint getSelectedNearbyPlace() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			MapContextMenu mapContextMenu = mapActivity.getContextMenu();
			Object object = mapContextMenu.getObject();
			if (object instanceof ExploreTopPlacePoint && mapContextMenu.isVisible()) {
				return (ExploreTopPlacePoint) object;
			}
		}
		return null;
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof ExploreTopPlacePoint) {
			return new LatLon(((ExploreTopPlacePoint) o).getLatitude(), ((ExploreTopPlacePoint) o).getLongitude());
		}
		return null;
	}

	private void fetchImages() {
		Collection<ExploreTopPlacePoint> places = visibleTopPlaces != null ? visibleTopPlaces.values() : null;
		if (places == null) {
			cancelLoadingImages();
			return;
		}
		Set<String> imagesToLoad = places.stream()
				.map(ExploreTopPlacePoint::getIconUrl).collect(Collectors.toSet());
		loadingImages.entrySet().removeIf(entry -> {
			if (!imagesToLoad.contains(entry.getKey())) {
				entry.getValue().cancel();
				return true;
			}
			return false;
		});

		for (ExploreTopPlacePoint point : places) {
			String url = point.getIconUrl();
			if (point.getImageBitmap() != null || loadingImages.containsKey(url)) {
				continue;
			}
			loadingImages.put(url, imageLoader.loadImage(url, new ImageLoaderCallback() {
				@Override
				public void onStart(@Nullable Bitmap bitmap) {
				}

				@Override
				public void onSuccess(@NonNull Bitmap bitmap) {
					loadingImages.remove(url);
					if (visibleTopPlaces != null) {
						ExploreTopPlacePoint place = visibleTopPlaces.get(point.getId());
						if (place != null) {
							point.setImageBitmap(bitmap);
							updateTopVisiblePlaces();
						}
					}
				}

				@Override
				public void onError() {
					loadingImages.remove(url);
					LOG.error(String.format("Coil failed to load %s", url));
				}
			}, false));
		}
	}

	private void updateTopVisiblePlaces() {
		updateTopPlacesCollection();
	}

	private void cancelLoadingImages() {
		loadingImages.values().forEach(LoadingImage::cancel);
		loadingImages.clear();
	}

	public void enableLayer(boolean enable) {
		requestQuadRect = enable ? new QuadRect() : null;
	}

	@Override
	public void onNewExplorePlacesDownloaded() {
		getApplication().getOsmandMap().refreshMap();
	}

	private Bitmap createSmallPointBitmap() {
		Context ctx = getContext();
		int borderWidth = AndroidUtils.dpToPx(ctx, SMALL_ICON_BORDER_DP);
		Bitmap circle = getCircle();
		int smallIconSize = AndroidUtils.dpToPx(ctx, SMALL_ICON_SIZE_DP);
		Bitmap bitmapResult = Bitmap.createBitmap(smallIconSize, smallIconSize, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmapResult);
		Paint bitmapPaint = createBitmapPaint();
		bitmapPaint.setColorFilter(new PorterDuffColorFilter(POINT_OUTER_COLOR, PorterDuff.Mode.SRC_IN));
		Rect srcRect = new Rect(0, 0, circle.getWidth(), circle.getHeight());
		RectF dstRect = new RectF(0f, 0f, (float) smallIconSize, (float) smallIconSize);
		canvas.drawBitmap(circle, srcRect, dstRect, bitmapPaint);
		bitmapPaint.setColorFilter(new PorterDuffColorFilter(
				ColorUtilities.getColor(ctx, R.color.poi_background), PorterDuff.Mode.SRC_IN));
		dstRect = new RectF(
				(float) borderWidth,
				(float) borderWidth,
				(float) (smallIconSize - borderWidth * 2),
				(float) (smallIconSize - borderWidth * 2));
		canvas.drawBitmap(circle, srcRect, dstRect, bitmapPaint);
		bitmapResult = AndroidUtils.scaleBitmap(bitmapResult, smallIconSize, smallIconSize, false);
		return bitmapResult;
	}

	private Bitmap createBigBitmap(Bitmap loadedBitmap, boolean isSelected) {
		OsmandApplication app = getApplication();
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		int borderWidth = AndroidUtils.dpToPxAuto(app, BIG_ICON_BORDER_DP);
		Bitmap circle = getCircle();
		int bigIconSize = getBigIconSize();
		Bitmap bitmapResult = Bitmap.createBitmap(bigIconSize, bigIconSize, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmapResult);
		Paint bitmapPaint = createBitmapPaint();
		bitmapPaint.setColorFilter(new PorterDuffColorFilter(isSelected ? app.getColor(ColorUtilities.getActiveColorId(nightMode)) : POINT_OUTER_COLOR, PorterDuff.Mode.SRC_IN));
		canvas.drawBitmap(circle, 0f, 0f, bitmapPaint);
		int cx = circle.getWidth() / 2;
		int cy = circle.getHeight() / 2;
		int radius = (Math.min(cx, cy) - borderWidth * 2);
		canvas.save();
		canvas.clipRect(0, 0, circle.getWidth(), circle.getHeight());
		Path circularPath = new Path();
		circularPath.addCircle((float) cx, (float) cy, (float) radius, Path.Direction.CW);
		canvas.clipPath(circularPath);
		Rect srcRect = new Rect(0, 0, loadedBitmap.getWidth(), loadedBitmap.getHeight());
		RectF dstRect = new RectF(0f, 0f, (float) circle.getWidth(), (float) circle.getHeight());
		bitmapPaint.setColorFilter(null);
		canvas.drawBitmap(loadedBitmap, srcRect, dstRect, bitmapPaint);
		canvas.restore();
		bitmapResult = AndroidUtils.scaleBitmap(bitmapResult, bigIconSize, bigIconSize, false);
		return bitmapResult;
	}

	private Bitmap getCircle() {
		if (circleBitmap == null) {
			circleBitmap = RenderingIcons.getBitmapFromVectorDrawable(getContext(), R.drawable.bg_point_circle);
		}
		return circleBitmap;
	}

	private Paint createBitmapPaint() {
		Paint bitmapPaint = new Paint();
		bitmapPaint.setAntiAlias(true);
		bitmapPaint.setDither(true);
		bitmapPaint.setFilterBitmap(true);
		return bitmapPaint;
	}

	private int getBigIconSize() {
		return AndroidUtils.dpToPxAuto(getContext(), BIG_ICON_SIZE_DP);
	}
}
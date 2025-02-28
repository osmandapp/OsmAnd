package net.osmand.plus.views.layers;

import static net.osmand.plus.exploreplaces.ExplorePlacesProviderJava.DEFAULT_LIMIT_POINTS;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.MapMarker;
import net.osmand.core.jni.MapMarkerBuilder;
import net.osmand.core.jni.MapMarkersCollection;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QListMapMarker;
import net.osmand.data.ExploreTopPlacePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.exploreplaces.ExplorePlacesProvider;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.layers.core.ExploreTopPlacesTileProvider;
import net.osmand.shared.util.ImageLoaderCallback;
import net.osmand.shared.util.LoadingImage;
import net.osmand.shared.util.NetworkImageLoader;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ExploreTopPlacesLayer extends OsmandMapLayer implements IContextMenuProvider, ExplorePlacesProvider.ExplorePlacesListener {

	private static final int START_ZOOM = 2;
	private static final Log LOG = PlatformUtil.getLog(ExploreTopPlacesLayer.class);

	private boolean nightMode;
	private ExploreTopPlacePoint selectedObject;
	private static final int SELECTED_MARKER_ID = -1;

	private Bitmap cachedSmallIconBitmap;

	private QuadRect requestQuadRect = null; // null means disabled
	private int requestZoom = 0; // null means disabled

	private ExploreTopPlacesTileProvider topPlacesMapLayerProvider;
	private ExplorePlacesProvider explorePlacesProvider;
	private int cachedExploreDataVersion;
	private List<ExploreTopPlacePoint> places;


	// To refresh images
	private static final int TOP_LOAD_PHOTOS = 25;
	private static final long DEBOUNCE_IMAGE_REFRESH = 1000;

	private RotatedTileBox imagesDisplayedBox = null;
	private int imagesUpdatedVersion;
	private int imagesCachedVersion;
	private long lastImageCacheRefreshed = 0;

	private final NetworkImageLoader imageLoader;
	private final List<LoadingImage> loadingImages = new ArrayList<>();

	private static class MapPoint {
		private final PointI position;
		private final boolean alreadyExists;
		@Nullable
		private final Bitmap imageBitmap;

		public MapPoint(PointI position, @Nullable Bitmap imageBitmap, boolean alreadyExists) {
			this.position = position;
			this.imageBitmap = imageBitmap;
			this.alreadyExists = alreadyExists;
		}
	}

	public ExploreTopPlacesLayer(@NonNull Context ctx) {
		super(ctx);

		imageLoader = new NetworkImageLoader(ctx, false);
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
		cachedSmallIconBitmap = ExploreTopPlacesTileProvider.createSmallPointBitmap(getApplication());
	}

	@Override
	protected void updateResources() {
		super.updateResources();
		recreateBitmaps();
	}

	@Override
	protected void cleanupResources() {
		super.cleanupResources();
		deleteProvider(topPlacesMapLayerProvider);
		topPlacesMapLayerProvider = null;
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
				extended.increasePixelDimensions(tileBox.getPixWidth() / 2, tileBox.getPixHeight() / 2);
				requestQuadRect = extended.getLatLonBounds();
				requestZoom = tileBox.getZoom();
				cachedExploreDataVersion = explorePlacesProvider.getDataVersion();
				places = explorePlacesProvider.getDataCollection(requestQuadRect);
			}
		} else {
			if (places != null) {
				placesUpdated = true;
				places = null;
			}
		}
		placesUpdated = placesUpdated || scheduleImageRefreshes(places, tileBox, placesUpdated);

		ExploreTopPlacePoint selectedObject = getSelectedNearbyPlace();
		long selectedObjectId = selectedObject == null ? 0 : selectedObject.getId();
		long lastSelectedObjectId = this.selectedObject == null ? 0 : this.selectedObject.getId();
		boolean selectedObjectChanged = selectedObjectId != lastSelectedObjectId;
		this.selectedObject = selectedObject;
		if (hasMapRenderer()) {
			if ((mapActivityInvalidated || mapRendererChanged || nightModeChanged || placesUpdated)) {
				updateTopPlacesTileProvider(places != null);
				updateTopPlacesCollection(places, tileBox);
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
			cachedSmallIconBitmap = ExploreTopPlacesTileProvider.createSmallPointBitmap(getApplication());
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
				Bitmap bigBitmap = ExploreTopPlacesTileProvider.createBigBitmap(getApplication(), bitmap, point.getId() == getSelectedObjectId());
				float x = tileBox.getPixXFromLatLon(point.getLatitude(), point.getLongitude());
				float y = tileBox.getPixYFromLatLon(point.getLatitude(), point.getLongitude());
				canvas.drawBitmap(bigBitmap, x - bigBitmap.getWidth() / 2f, y - bigBitmap.getHeight() / 2f, pointPaint);
			}
		}
	}

	private long getSelectedObjectId() {
		return selectedObject == null ? 0 : selectedObject.getId();
	}

	private void updateTopPlacesTileProvider(boolean show) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null) {
			return;
		}
		if (show) {
			if (topPlacesMapLayerProvider == null) {
				topPlacesMapLayerProvider = new ExploreTopPlacesTileProvider(getApplication(),
						getPointsOrder() + 100, DEFAULT_LIMIT_POINTS / 4);
				topPlacesMapLayerProvider.initProvider(mapRenderer);
			}
		} else {
			deleteProvider(topPlacesMapLayerProvider);
			topPlacesMapLayerProvider = null;
		}
	}

	private void updateTopPlacesCollection(@Nullable List<ExploreTopPlacePoint> points, @NonNull RotatedTileBox tileBox) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null) {
			return;
		}
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
		float iconSize = ExploreTopPlacesTileProvider.getBigIconSize(view.getApplication());
		QuadTree<QuadRect> boundIntersections = initBoundIntersections(tileBox);
        for (int j = 0; j < Math.min(points.size(), TOP_LOAD_PHOTOS); j++) {
            ExploreTopPlacePoint point = points.get(j);
            double lat = point.getLatitude();
            double lon = point.getLongitude();

            PointI position = NativeUtilities.getPoint31FromLatLon(lat, lon);
            int x = position.getX();
            int y = position.getY();
            PointF pixel = NativeUtilities.getElevatedPixelFromLatLon(mapRenderer, tileBox, lat, lon);
            if (intersects(boundIntersections, pixel.x, pixel.y, iconSize, iconSize)) {
                continue;
            }
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

			Bitmap imageMapBitmap = ExploreTopPlacesTileProvider
					.createBigBitmap(getApplication(), imageBitmap, false);

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
			Bitmap imageMapBitmap = ExploreTopPlacesTileProvider
					.createBigBitmap(getApplication(), imageBitmap, true);

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

	public void deleteProvider(@Nullable ExploreTopPlacesTileProvider provider) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null || provider == null) {
			return;
		}
		provider.deleteProvider(mapRenderer);
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

	private boolean scheduleImageRefreshes(List<ExploreTopPlacePoint> nearbyPlacePoints, RotatedTileBox tileBox, boolean forceRefresh) {
		if (places == null) {
			if (imagesDisplayedBox != null) {
				cancelLoadingImages();
				imagesDisplayedBox = null;
			}
			return false;
		}
		if (forceRefresh || imagesDisplayedBox == null || imagesDisplayedBox.getZoom() != tileBox.getZoom()
				|| !imagesDisplayedBox.containsTileBox(tileBox)) {
			imagesDisplayedBox = tileBox.copy();
			imagesDisplayedBox.increasePixelDimensions(tileBox.getPixWidth() / 2, tileBox.getPixHeight() / 2);
			imagesUpdatedVersion++;
			List<ExploreTopPlacePoint> placesToDisplayWithPhotos = new ArrayList<>();

			boolean missingPhoto = false;
			for (ExploreTopPlacePoint point : nearbyPlacePoints) {
				if (imagesDisplayedBox.containsLatLon(point.getLatitude(), point.getLongitude()) &&
						!Algorithms.isEmpty(point.getIconUrl())) {
					placesToDisplayWithPhotos.add(point);
					if (point.getImageBitmap() == null) {
						missingPhoto = true;
					}
				}
			}
			if (missingPhoto) {
				Set<String> imagesToLoad = placesToDisplayWithPhotos.stream()
						.map(ExploreTopPlacePoint::getIconUrl).collect(Collectors.toSet());
				loadingImages.removeIf(image -> !imagesToLoad.contains(image.getUrl()) && image.cancel());

				for (ExploreTopPlacePoint point : placesToDisplayWithPhotos) {
					if (point.getImageBitmap() != null) {
						continue;
					}

					String url = point.getIconUrl();
					loadingImages.add(imageLoader.loadImage(url, new ImageLoaderCallback() {
						@Override
						public void onStart(@Nullable Bitmap bitmap) {
						}

						@Override
						public void onSuccess(@NonNull Bitmap bitmap) {
							point.setImageBitmap(bitmap);
							imagesUpdatedVersion++;
						}

						@Override
						public void onError() {
							LOG.error(String.format("Coil failed to load %s", url));
						}
					}, false));
				}
			}
		}
		if (imagesCachedVersion != imagesUpdatedVersion && System.currentTimeMillis() - lastImageCacheRefreshed >
				DEBOUNCE_IMAGE_REFRESH) {
			lastImageCacheRefreshed = System.currentTimeMillis();
			imagesCachedVersion = imagesUpdatedVersion;
			return true;
		}
		return false;
	}

	private void cancelLoadingImages() {
		loadingImages.forEach(LoadingImage::cancel);
		loadingImages.clear();
	}

	public void enableLayer(boolean enable) {
		requestQuadRect = enable ? new QuadRect() : null;
	}

	@Override
	public void onNewExplorePlacesDownloaded() {
		getApplication().getOsmandMap().refreshMap();
	}
}
package net.osmand.plus.views.layers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.PointI;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.ExploreTopPlacePoint;
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
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class ExploreTopPlacesLayer extends OsmandMapLayer implements IContextMenuProvider, ExplorePlacesProvider.ExplorePlacesListener {

	private static final int START_ZOOM = 2;
	private static final Log LOG = PlatformUtil.getLog(ExploreTopPlacesLayer.class);

	private boolean nightMode;
	private ExploreTopPlacePoint selectedObject;

	private Bitmap cachedSmallIconBitmap;

	private QuadRect requestQuadRect = null; // null means disabled
	private int requestZoom = 0; // null means disabled

	private ExploreTopPlacesTileProvider topPlacesMapLayerProvider;
	private ExploreTopPlacesTileProvider selectedTopPlacesMapLayerProvider;
	private ExplorePlacesProvider explorePlacesProvider;
	private int cachedExploreDataVersion;
	private List<ExploreTopPlacePoint> places;


	// To refresh images
	public static final String LOAD_NEARBY_IMAGES_TAG = "load_nearby_images";
	private static final int TOP_LOAD_PHOTOS = 25;
	private static final long DEBOUNCE_IMAGE_REFRESH = 5000;


	private RotatedTileBox imagesDisplayedBox = null;
	private int imagesUpdatedVersion;
	private int imagesCachedVersion;
	private long lastImageCacheRefreshed = 0;


	public ExploreTopPlacesLayer(@NonNull Context ctx) {
		super(ctx);
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
		deleteProvider(selectedTopPlacesMapLayerProvider);
		topPlacesMapLayerProvider = null;
		selectedTopPlacesMapLayerProvider = null;
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
		placesUpdated = placesUpdated || scheduleImageRefreshes(places, tileBox);

		ExploreTopPlacePoint selectedObject = getSelectedNearbyPlace();
		long selectedObjectId = selectedObject == null ? 0 : selectedObject.getId();
		long lastSelectedObjectId = this.selectedObject == null ? 0 : this.selectedObject.getId();
		boolean selectedObjectChanged = selectedObjectId != lastSelectedObjectId;
		this.selectedObject = selectedObject;
		if (hasMapRenderer()) {
			if ((mapActivityInvalidated || mapRendererChanged
					|| nightModeChanged || placesUpdated)) {
				initProviderWithPoints(places);
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
				drawPoints(places, latLonBounds, false, tileBox, boundIntersections, iconSize, canvas,
						fullObjectsLatLon, smallObjectsLatLon);
				this.fullObjectsLatLon = fullObjectsLatLon;
				this.smallObjectsLatLon = smallObjectsLatLon;
			}
		}
		mapActivityInvalidated = false;
	}

	private void drawPoints(List<ExploreTopPlacePoint> pointsToDraw, QuadRect latLonBounds, boolean synced, RotatedTileBox tileBox,
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
				canvas.drawBitmap(bigBitmap, x - bigBitmap.getWidth() / 2, y - bigBitmap.getHeight() / 2, pointPaint);
			}
		}
	}

	private long getSelectedObjectId() {
		return selectedObject == null ? 0 : selectedObject.getId();
	}

	private void initProviderWithPoints(List<ExploreTopPlacePoint> points) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null) {
			return;
		}
		deleteProvider(topPlacesMapLayerProvider);
		if (points == null) {
			topPlacesMapLayerProvider = null;
			return;
		}
		topPlacesMapLayerProvider = new ExploreTopPlacesTileProvider(getApplication(),
				getPointsOrder(),
				view.getDensity(),
				getSelectedObjectId());
		for (ExploreTopPlacePoint nearbyPlacePoint : points) {
			topPlacesMapLayerProvider.addToData(nearbyPlacePoint);
		}
		topPlacesMapLayerProvider.initProvider(mapRenderer);
	}

	public synchronized void showSelectedNearbyPoint() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null) {
			return;
		}
		deleteProvider(selectedTopPlacesMapLayerProvider);
		if (selectedObject != null) {
			selectedTopPlacesMapLayerProvider = new ExploreTopPlacesTileProvider(getApplication(),
					getPointsOrder() - 1,
					view.getDensity(),
					getSelectedObjectId());

			selectedTopPlacesMapLayerProvider.addToData(selectedObject);
			selectedTopPlacesMapLayerProvider.initProvider(mapRenderer);
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
	public boolean onLongPressEvent(@NonNull PointF point, @NonNull RotatedTileBox tileBox) {
		return false;
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
			ArrayList<ExploreTopPlacePoint> foundPoints = new ArrayList<>();
			getNearbyPlaceFromPoint(tileBox, point, foundPoints, points);
			MapActivity activity = getMapActivity();
			if(activity != null) {
				for(ExploreTopPlacePoint foundPoint: foundPoints) {
					Amenity amenity = getMapActivity().getMyApplication().getExplorePlacesProvider().getAmenity(
							new LatLon(foundPoint.getLatitude(), foundPoint.getLongitude()), foundPoint.getId());
					if(amenity == null) {
						res.add(foundPoint);
					}
				}
			}
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

	private boolean scheduleImageRefreshes(List<ExploreTopPlacePoint> nearbyPlacePoints, RotatedTileBox tileBox) {
		if (places == null) {
			if (imagesDisplayedBox != null) {
				LOG.info(String.format("Picasso cancel loading"));
				Picasso.get().cancelTag(LOAD_NEARBY_IMAGES_TAG);
				imagesDisplayedBox = null;
			}
			return false;
		}
		if (imagesDisplayedBox == null || imagesDisplayedBox.getZoom() != tileBox.getZoom() ||
				!imagesDisplayedBox.containsTileBox(tileBox)) {
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
					if (placesToDisplayWithPhotos.size() > TOP_LOAD_PHOTOS) {
						break;
					}
				}
			}
			if (missingPhoto) {
				Picasso.get().cancelTag(LOAD_NEARBY_IMAGES_TAG);
//				LOG.info(String.format("Picasso cancel loading"));

				for (ExploreTopPlacePoint point : placesToDisplayWithPhotos) {
					if (point.getImageBitmap() != null) {
						continue;
					}
					Target imgLoadTarget = new Target() {
						@Override
						public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
							point.setImageBitmap(bitmap);
//							LOG.info(String.format("Picasso loaded %s", point.getIconUrl()));
							imagesUpdatedVersion++;
						}

						@Override
						public void onBitmapFailed(Exception e, Drawable errorDrawable) {
							LOG.error(String.format("Picasso failed to load %s", point.getIconUrl()), e);
						}

						@Override
						public void onPrepareLoad(Drawable placeHolderDrawable) {
						}
					};
//					LOG.info(String.format("Picasso schedule %s", point.getIconUrl()));
					Picasso.get()
							.load(point.getIconUrl())
							.tag(LOAD_NEARBY_IMAGES_TAG)
							.into(imgLoadTarget);
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

	public void enableLayer(boolean enable) {
		requestQuadRect = enable ?
				new QuadRect() : null;
	}

	@Override
	public void onNewExplorePlacesDownloaded() {
		getApplication().getOsmandMap().refreshMap();
	}
}
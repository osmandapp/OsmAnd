package net.osmand.plus.views.layers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.util.Pair;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.TextRasterizer;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.PointImageDrawable;
import net.osmand.plus.views.PointImageUtils;
import net.osmand.plus.views.layers.ContextMenuLayer.ApplyMovedObjectCallback;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.ContextMenuLayer.IMoveObjectProvider;
import net.osmand.plus.views.layers.MapTextLayer.MapTextProvider;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.layers.core.FavoritesTileProvider;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class FavouritesLayer extends OsmandMapLayer implements IContextMenuProvider,
		IMoveObjectProvider, MapTextProvider<FavouritePoint> {

	private static final int START_ZOOM = 6;
	private static final Log LOG = PlatformUtil.getLog(FavouritesLayer.class);

	private FavouritesHelper favouritesHelper;
	private MapMarkersHelper mapMarkersHelper;
	protected List<FavouritePoint> cache = new ArrayList<>();
	private MapTextLayer textLayer;
	@ColorInt
	private int defaultColor;
	@ColorInt
	private int grayColor;
	private OsmandSettings settings;
	private ContextMenuLayer contextMenuLayer;
	private boolean showFavorites;
	private float textScale = 1f;
	private boolean textVisible;
	private boolean nightMode;
	private boolean changeMarkerPositionMode;
	private long favoritesChangedTime;

	public CustomMapObjects<FavouritePoint> customObjectsDelegate;

	//OpenGl
	private FavoritesTileProvider favoritesMapLayerProvider;

	public FavouritesLayer(@NonNull Context ctx) {
		super(ctx);
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);

		settings = view.getApplication().getSettings();
		favouritesHelper = view.getApplication().getFavoritesHelper();
		mapMarkersHelper = view.getApplication().getMapMarkersHelper();
		textLayer = view.getLayerByClass(MapTextLayer.class);
		defaultColor = ContextCompat.getColor(getContext(), R.color.color_favorite);
		grayColor = ContextCompat.getColor(getContext(), R.color.color_favorite_gray);
		contextMenuLayer = view.getLayerByClass(ContextMenuLayer.class);
	}

	@Override
	protected void cleanupResources() {
		super.cleanupResources();
		clearFavorites();
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (contextMenuLayer.getMoveableObject() instanceof FavouritePoint) {
			FavouritePoint objectInMotion = (FavouritePoint) contextMenuLayer.getMoveableObject();
			PointF pf = contextMenuLayer.getMovableCenterPoint(tileBox);
			MapMarker mapMarker = mapMarkersHelper.getMapMarker(objectInMotion);
			float textScale = getTextScale();
			drawBigPoint(canvas, objectInMotion, pf.x, pf.y, mapMarker, textScale);
			if (!changeMarkerPositionMode) {
				changeMarkerPositionMode = true;
				showFavorites();
			}
		} else if (changeMarkerPositionMode) {
			changeMarkerPositionMode = false;
			showFavorites();
		}
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		super.onPrepareBufferImage(canvas, tileBox, settings);
		boolean nightMode = settings != null && settings.isNightMode();
		boolean nightModeChanged = this.nightMode != nightMode;
		this.nightMode = nightMode;
		float textScale = getTextScale();
		boolean textScaleChanged = this.textScale != textScale;
		this.textScale = textScale;
		boolean textVisible = isTextVisible();
		boolean textVisibleChanged = this.textVisible != textVisible;
		this.textVisible = textVisible;
		boolean showFavorites = this.settings.SHOW_FAVORITES.get() || customObjectsDelegate != null;
		boolean showFavoritesChanged = !Algorithms.objectEquals(this.showFavorites, showFavorites);
		this.showFavorites = showFavorites;
		long favoritesChangedTime = favouritesHelper.getLastModifiedTime();
		boolean favoritesChanged = this.favoritesChangedTime != favoritesChangedTime;
		this.favoritesChangedTime = favoritesChangedTime;

		if (hasMapRenderer()) {
			if (mapActivityInvalidated || mapRendererChanged || nightModeChanged || showFavoritesChanged
					|| favoritesChanged || textScaleChanged || textVisibleChanged
					|| (customObjectsDelegate != null && customObjectsDelegate.isChanged())) {
				showFavorites();
				if (customObjectsDelegate != null) {
					customObjectsDelegate.acceptChanges();
				}
				mapRendererChanged = false;
			}
		} else {
			cache.clear();
			if (showFavorites && favouritesHelper.isFavoritesLoaded()) {
				if (tileBox.getZoom() >= START_ZOOM || customObjectsDelegate != null) {
					float iconSize = getIconSize(view.getApplication());
					QuadTree<QuadRect> boundIntersections = initBoundIntersections(tileBox);

					// request to load
					QuadRect latLonBounds = tileBox.getLatLonBounds();
					List<LatLon> fullObjectsLatLon = new ArrayList<>();
					List<LatLon> smallObjectsLatLon = new ArrayList<>();
					if (customObjectsDelegate != null) {
						drawPoints(customObjectsDelegate.getMapObjects(), latLonBounds, false, tileBox, boundIntersections, iconSize, canvas,
								fullObjectsLatLon, smallObjectsLatLon);
					} else {
						for (FavoriteGroup group : getFavoriteGroups()) {
							drawPoints(group.getPoints(), latLonBounds, isSynced(group), tileBox, boundIntersections, iconSize, canvas,
									fullObjectsLatLon, smallObjectsLatLon);
						}
					}
					this.fullObjectsLatLon = fullObjectsLatLon;
					this.smallObjectsLatLon = smallObjectsLatLon;
				}
			}
			if (textVisible) {
				textLayer.putData(this, cache);
			}
		}
		mapActivityInvalidated = false;
	}

	private void drawPoints(List<FavouritePoint> pointsToDraw, QuadRect latLonBounds, boolean synced, RotatedTileBox tileBox,
	                        QuadTree<QuadRect> boundIntersections, float iconSize, Canvas canvas,
	                        List<LatLon> fullObjectsLatLon, List<LatLon> smallObjectsLatLon) {
		List<Pair<FavouritePoint, MapMarker>> fullObjects = new ArrayList<>();

		for (FavouritePoint favoritePoint : pointsToDraw) {
			double lat = favoritePoint.getLatitude();
			double lon = favoritePoint.getLongitude();
			if (favoritePoint.isVisible() && favoritePoint != contextMenuLayer.getMoveableObject()
					&& lat >= latLonBounds.bottom && lat <= latLonBounds.top
					&& lon >= latLonBounds.left && lon <= latLonBounds.right) {
				MapMarker marker = null;
				if (synced) {
					marker = mapMarkersHelper.getMapMarker(favoritePoint);
					if (marker == null || marker.history && !view.getSettings().KEEP_PASSED_MARKERS_ON_MAP.get()) {
						continue;
					}
				}
				cache.add(favoritePoint);
				float x = tileBox.getPixXFromLatLon(lat, lon);
				float y = tileBox.getPixYFromLatLon(lat, lon);

				if (intersects(boundIntersections, x, y, iconSize, iconSize)) {
					@ColorInt
					int color;
					if (marker != null && marker.history) {
						color = grayColor;
					} else {
						color = favouritesHelper.getColorWithCategory(favoritePoint, defaultColor);
					}
					PointImageDrawable pointImageDrawable = PointImageUtils.getFromPoint(
							getContext(), color, true, favoritePoint);
					pointImageDrawable.drawSmallPoint(canvas, x, y, textScale);
					smallObjectsLatLon.add(new LatLon(lat, lon));
				} else {
					fullObjects.add(new Pair<>(favoritePoint, marker));
					fullObjectsLatLon.add(new LatLon(lat, lon));
				}
			}
		}
		for (Pair<FavouritePoint, MapMarker> pair : fullObjects) {
			FavouritePoint favoritePoint = pair.first;
			float x = tileBox.getPixXFromLatLon(favoritePoint.getLatitude(), favoritePoint.getLongitude());
			float y = tileBox.getPixYFromLatLon(favoritePoint.getLatitude(), favoritePoint.getLongitude());
			drawBigPoint(canvas, favoritePoint, x, y, pair.second, textScale);
		}

	}

	private boolean isSynced(@NonNull FavoriteGroup group) {
		MapMarkersGroup markersGroup = mapMarkersHelper.getMarkersGroup(group);
		return markersGroup != null && !markersGroup.isDisabled();
	}

	private void drawBigPoint(Canvas canvas, FavouritePoint favoritePoint, float x, float y, @Nullable MapMarker marker,
							  float textScale) {
		PointImageDrawable pointImageDrawable;
		boolean history = false;
		if (marker != null) {
			pointImageDrawable = PointImageUtils.getOrCreateSyncedIcon(getContext(),
					favouritesHelper.getColorWithCategory(favoritePoint, defaultColor), favoritePoint);
			history = marker.history;
		} else {
			pointImageDrawable = PointImageUtils.getFromPoint(getContext(),
					favouritesHelper.getColorWithCategory(favoritePoint, defaultColor), true, favoritePoint);
		}
		pointImageDrawable.drawPoint(canvas, x, y, textScale, history);
	}

	private List<FavoriteGroup> getFavoriteGroups() {
		return new ArrayList<>(favouritesHelper.getFavoriteGroups());
	}

	public synchronized void showFavorites() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null) {
			return;
		}
		clearFavorites();
		float textScale = getTextScale();
		favoritesMapLayerProvider = new FavoritesTileProvider(getContext(), getPointsOrder(), isTextVisible(),
				getTextStyle(textScale), view.getDensity());

		if (customObjectsDelegate != null) {
			List<FavouritePoint> points = customObjectsDelegate.getMapObjects();
			showFavoritePoints(textScale, false, points);
			favoritesMapLayerProvider.drawSymbols(mapRenderer);
		} else if (settings.SHOW_FAVORITES.get() && favouritesHelper.isFavoritesLoaded()) {
			for (FavoriteGroup group : getFavoriteGroups()) {
				boolean synced = isSynced(group);
				List<FavouritePoint> points = new ArrayList<>(group.getPoints());
				showFavoritePoints(textScale, synced, points);
			}
			favoritesMapLayerProvider.drawSymbols(mapRenderer);
		}
	}

	private void showFavoritePoints(float textScale, boolean synced, List<FavouritePoint> points) {
		for (FavouritePoint favoritePoint : points) {
			if (favoritePoint.isVisible() && favoritePoint != contextMenuLayer.getMoveableObject()) {
				MapMarker marker = null;
				if (synced) {
					marker = mapMarkersHelper.getMapMarker(favoritePoint);
					if (marker == null || marker.history && !view.getSettings().KEEP_PASSED_MARKERS_ON_MAP.get()) {
						continue;
					}
				}
				int color;
				if ((marker != null && marker.history)) {
					color = grayColor;
				} else {
					color = favouritesHelper.getColorWithCategory(favoritePoint, defaultColor);
				}
				favoritesMapLayerProvider.addToData(favoritePoint, color, true, marker != null, textScale);
			}
		}
	}

	private TextRasterizer.Style getTextStyle(float textScale) {
		return MapTextLayer.getTextStyle(getContext(), nightMode, textScale, view.getDensity());
	}

	public void clearFavorites() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null || favoritesMapLayerProvider == null) {
			return;
		}
		favoritesMapLayerProvider.clearSymbols(mapRenderer);
		favoritesMapLayerProvider = null;
	}

	@Override
	public boolean onLongPressEvent(@NonNull PointF point, @NonNull RotatedTileBox tileBox) {
		return false;
	}

	private void getFavoriteFromPoint(RotatedTileBox tb, PointF point, List<? super FavouritePoint> res) {
		List<FavouritePoint> favouritePoints = favouritesHelper.getFavouritePoints();
		if (Algorithms.isEmpty(favouritePoints)) {
			return;
		}

		MapRendererView mapRenderer = getMapRenderer();
		float radius = getScaledTouchRadius(getApplication(), tb.getDefaultRadiusPoi()) * TOUCH_RADIUS_MULTIPLIER;
		List<PointI> touchPolygon31 = null;
		if (mapRenderer != null) {
			touchPolygon31 = NativeUtilities.getPolygon31FromPixelAndRadius(mapRenderer, point, radius);
			if (touchPolygon31 == null) {
				return;
			}
		}

		for (FavouritePoint favouritePoint : favouritePoints) {
			if (!favouritePoint.isVisible()) {
				continue;
			}

			double lat = favouritePoint.getLatitude();
			double lon = favouritePoint.getLongitude();

			boolean add = mapRenderer != null
					? NativeUtilities.isPointInsidePolygon(lat, lon, touchPolygon31)
					: tb.isLatLonNearPixel(lat, lon, point.x, point.y, radius);
			if (add) {
				res.add(favouritePoint);
			}
		}
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof FavouritePoint) {
			return ((FavouritePoint) o).getPointDescription(getContext());
		}
		return null;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> res,
	                                    boolean unknownLocation, boolean excludeUntouchableObjects) {
		if (this.settings.SHOW_FAVORITES.get() && tileBox.getZoom() >= START_ZOOM) {
			getFavoriteFromPoint(tileBox, point, res);
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof FavouritePoint) {
			return new LatLon(((FavouritePoint) o).getLatitude(), ((FavouritePoint) o).getLongitude());
		}
		return null;
	}

	@Override
	public LatLon getTextLocation(FavouritePoint o) {
		return new LatLon(o.getLatitude(), o.getLongitude());
	}

	@Override
	public int getTextShift(FavouritePoint o, RotatedTileBox rb) {
		return (int) (16 * rb.getDensity() * getTextScale());
	}

	@Override
	public String getText(FavouritePoint o) {
		return PointDescription.getSimpleName(o, getContext());
	}

	@Override
	public boolean isTextVisible() {
		return settings.SHOW_POI_LABEL.get();
	}

	@Override
	public boolean isFakeBoldText() {
		return false;
	}

	@Override
	public boolean isObjectMovable(Object o) {
		return o instanceof FavouritePoint;
	}

	@Override
	public void applyNewObjectPosition(@NonNull Object o,
									   @NonNull LatLon position,
									   @Nullable ApplyMovedObjectCallback callback) {
		boolean result = false;
		if (o instanceof FavouritePoint) {
			favouritesHelper.editFavourite((FavouritePoint) o, position.getLatitude(), position.getLongitude());
			favouritesHelper.lookupAddress((FavouritePoint) o);
			result = true;
		}
		if (callback != null) {
			callback.onApplyMovedObject(result, o);
		}
	}

	public void setCustomMapObjects(List<FavouritePoint> favouritePoints) {
		if (customObjectsDelegate != null) {
			customObjectsDelegate.setCustomMapObjects(favouritePoints);
			getApplication().getOsmandMap().refreshMap();
		}
	}
}



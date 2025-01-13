package net.osmand.plus.views.layers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.TextRasterizer;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.NearbyPlacePoint;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.layers.core.NearbyPlacesTileProvider2;
import net.osmand.util.Algorithms;
import net.osmand.wiki.WikiCoreHelper;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NearbyPlacesLayer extends OsmandMapLayer implements IContextMenuProvider {

	private static final int START_ZOOM = 6;
	private static final Log LOG = PlatformUtil.getLog(NearbyPlacesLayer.class);

	protected List<FavouritePoint> cache = new ArrayList<>();
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
	private long favoritesChangedTime;

	private Bitmap circle;
	private Bitmap smallIconBg;
	private Bitmap bigIconBg;
	private Paint pointInnerCircle;
	private Paint pointOuterCircle;
	private Paint bitmapPaint;
	private static final int SMALL_ICON_SIZE_DP = 20;
	private static final int BIG_ICON_SIZE_DP = 40;
	private static final int POINT_OUTER_COLOR = 0x88555555;
	private int smallIconSize;
	private int bigIconSize;


	public CustomMapObjects<NearbyPlacePoint> customObjectsDelegate = new OsmandMapLayer.CustomMapObjects<>();

	//OpenGl
	private NearbyPlacesTileProvider2 favoritesMapLayerProvider;

	public NearbyPlacesLayer(@NonNull Context ctx) {
		super(ctx);
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);

		boolean night = getApplication().getDaynightHelper().isNightMode();
		settings = view.getApplication().getSettings();
		defaultColor = ContextCompat.getColor(getContext(), R.color.color_favorite);
		grayColor = ContextCompat.getColor(getContext(), R.color.color_favorite_gray);
		contextMenuLayer = view.getLayerByClass(ContextMenuLayer.class);

		nightMode = night;

		pointInnerCircle = new Paint();
		pointInnerCircle.setColor(getColor(R.color.poi_background));
		pointInnerCircle.setStyle(Paint.Style.FILL);
		pointInnerCircle.setAntiAlias(true);

		pointOuterCircle = new Paint();
		pointOuterCircle.setColor(POINT_OUTER_COLOR);
		pointOuterCircle.setStyle(Paint.Style.FILL_AND_STROKE);
		pointOuterCircle.setAntiAlias(true);

		bitmapPaint = new Paint();
		bitmapPaint.setAntiAlias(true);
		bitmapPaint.setDither(true);
		bitmapPaint.setFilterBitmap(true);
		recreateBitmaps(night);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
	}

	private void recreateBitmaps(boolean night) {
		float scale = getApplication().getOsmandMap().getCarDensityScaleCoef();
		circle = getScaledBitmap(R.drawable.ic_white_shield_small, scale);
		smallIconBg = getScaledBitmap(night
				? R.drawable.map_pin_user_location_small_night
				: R.drawable.map_pin_user_location_small_day, scale);
		bigIconBg = getScaledBitmap(night
				? R.drawable.map_pin_user_location_night
				: R.drawable.map_pin_user_location_day, scale);

		smallIconSize = AndroidUtils.dpToPxAuto(getContext(), SMALL_ICON_SIZE_DP);
		bigIconSize = AndroidUtils.dpToPxAuto(getContext(), BIG_ICON_SIZE_DP);

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
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		super.onPrepareBufferImage(canvas, tileBox, settings);
		boolean nightMode = settings != null && settings.isNightMode();
		boolean nightModeChanged = this.nightMode != nightMode;
		this.nightMode = nightMode;
		float textScale = getTextScale();
		boolean textScaleChanged = this.textScale != textScale;
		this.textScale = textScale;
		boolean showFavorites = customObjectsDelegate != null && !customObjectsDelegate.getMapObjects().isEmpty();
		boolean showFavoritesChanged = !Algorithms.objectEquals(this.showFavorites, showFavorites);
		this.showFavorites = showFavorites;

		if (hasMapRenderer()) {
			if ((mapActivityInvalidated || mapRendererChanged || nightModeChanged || showFavoritesChanged
					|| textScaleChanged
					|| (customObjectsDelegate != null && customObjectsDelegate.isChanged()))) {
				showFavorites();
				if (customObjectsDelegate != null) {
					customObjectsDelegate.acceptChanges();
				}
				mapRendererChanged = false;
			}
		}
		mapActivityInvalidated = false;
	}

	public synchronized void showFavorites() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null) {
			return;
		}
		clearFavorites();
		float textScale = getTextScale();
		favoritesMapLayerProvider = new NearbyPlacesTileProvider2(getApplication(), this,
				getPointsOrder(),
				view.getDensity());

		if (customObjectsDelegate != null) {
			List<NearbyPlacePoint> points = customObjectsDelegate.getMapObjects();
			showFavoritePoints(textScale, points);
			favoritesMapLayerProvider.drawSymbols(mapRenderer);
		}
	}

	private void showFavoritePoints(float textScale, List<NearbyPlacePoint> points) {
		for (NearbyPlacePoint favoritePoint : points) {
			if (favoritePoint.isVisible() && favoritePoint != contextMenuLayer.getMoveableObject()) {
				int color;
				color = grayColor;
				favoritesMapLayerProvider.addToData(favoritePoint, color, true, textScale);
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
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof FavouritePoint) {
			return new LatLon(((FavouritePoint) o).getLatitude(), ((FavouritePoint) o).getLongitude());
		}
		return null;
	}

	public void setCustomMapObjects(List<WikiCoreHelper.OsmandApiFeatureData> nearbyPlacePoints) {
		if (customObjectsDelegate != null) {
			List<NearbyPlacePoint> nearbyPlacePointsList = nearbyPlacePoints.stream().map(NearbyPlacePoint::new)
					.collect(Collectors.toList());
			customObjectsDelegate.setCustomMapObjects(nearbyPlacePointsList);
			getApplication().getOsmandMap().refreshMap();
		}
	}

	public Paint getPointInnerCircle() {
		return pointInnerCircle;
	}

	public Paint getPointOuterCircle() {
		return pointOuterCircle;
	}

	public Paint getBitmapPaint() {
		return bitmapPaint;
	}

	public Bitmap getCircle() {
		return circle;
	}

	public Bitmap getSmallIconBg() {
		return smallIconBg;
	}

	public Bitmap getBigIconBg() {
		return bigIconBg;
	}

	public int getBigIconSize() {
		return bigIconSize;
	}

	public int getSmallIconSize() {
		return smallIconSize;
	}
}



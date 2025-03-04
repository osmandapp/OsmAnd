package net.osmand.plus.views.layers.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;

import androidx.annotation.NonNull;

import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.AreaI;
import net.osmand.core.jni.MapMarker;
import net.osmand.core.jni.MapTiledCollectionProvider;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QListMapTiledCollectionPoint;
import net.osmand.core.jni.QListPointI;
import net.osmand.core.jni.SingleSkImage;
import net.osmand.core.jni.SwigUtilities;
import net.osmand.core.jni.TextRasterizer;
import net.osmand.core.jni.TileId;
import net.osmand.core.jni.Utilities;
import net.osmand.core.jni.ZoomLevel;
import net.osmand.core.jni.interface_MapTiledCollectionPoint;
import net.osmand.core.jni.interface_MapTiledCollectionProvider;
import net.osmand.data.ExploreTopPlacePoint;
import net.osmand.data.QuadRect;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.exploreplaces.ExplorePlacesProvider;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.util.MapUtils;

import java.util.List;

public class ExploreTopPlacesTileProvider extends interface_MapTiledCollectionProvider {

	private final Context ctx;
	private final ExplorePlacesProvider explorePlacesProvider;
	private MapTiledCollectionProvider providerInstance;

	private static final int TILE_LOADING_TIMEOUT = 5000;
	private static final int SLEEP_INTERVAL = 100;

	private static final int SMALL_ICON_BORDER_DP = 1;
	private static final int BIG_ICON_BORDER_DP = 2;
	private static final int SMALL_ICON_SIZE_DP = 20;
	private static final int BIG_ICON_SIZE_DP = 50;
	private static final int POINT_OUTER_COLOR = 0xffffffff;

	private final int baseOrder;
	private final int tilePointsLimit;
	private Bitmap cachedSmallBitmap;
	private final PointI pinIconOffset;
	private static Bitmap circleBitmap;

	private static Bitmap smallCircleBitmap;
	private static Bitmap bigCircleBitmap;

	private static Bitmap getCircleBitmap(@NonNull Context ctx, int iconSizeDp){
		int iconSizePx = AndroidUtils.dpToPxAuto(ctx, iconSizeDp);
		Bitmap tmpBmp = RenderingIcons.getBitmapFromVectorDrawable(ctx, R.drawable.bg_point_circle);
		Bitmap result = Bitmap.createBitmap(iconSizePx, iconSizePx, Bitmap.Config.ARGB_8888);
		tmpBmp = Bitmap.createScaledBitmap(tmpBmp, iconSizePx, iconSizePx, true);
		Canvas canvas = new Canvas(result);
		canvas.drawBitmap(tmpBmp, 0, 0, new Paint());
		return result;
	}

	private static Bitmap getBigCircle(@NonNull Context ctx) {
		if (bigCircleBitmap == null) {
			bigCircleBitmap = getCircleBitmap(ctx, BIG_ICON_SIZE_DP);
		}
		return bigCircleBitmap;
	}

	private static Bitmap getSmallCircle(@NonNull Context ctx) {
		if (smallCircleBitmap == null) {
			smallCircleBitmap = getCircleBitmap(ctx, SMALL_ICON_SIZE_DP);
		}
		return smallCircleBitmap;
	}

	private class TopPlaceCollectionPoint extends interface_MapTiledCollectionPoint {

        private final PointI point31;

		public TopPlaceCollectionPoint(@NonNull ExploreTopPlacePoint point) {
            int x = MapUtils.get31TileNumberX(point.getLongitude());
			int y = MapUtils.get31TileNumberY(point.getLatitude());
			this.point31 = new PointI(x, y);
		}

		@Override
		public PointI getPoint31() {
			return point31;
		}

		@Override
		public SingleSkImage getImageBitmap(boolean isFullSize) {
			Bitmap bitmap;
			if (cachedSmallBitmap == null) {
				cachedSmallBitmap = createSmallPointBitmap(ctx);
			}
			bitmap = cachedSmallBitmap;
			return NativeUtilities.createSkImageFromBitmap(bitmap);
		}

		@Override
		public String getCaption() {
			return "";
		}
	}

	public ExploreTopPlacesTileProvider(@NonNull Context ctx, int baseOrder, int tilePointsLimit) {
		this.ctx = ctx;
		this.baseOrder = baseOrder;
		this.tilePointsLimit = tilePointsLimit;
		this.pinIconOffset = new PointI(0, 0);
		this.explorePlacesProvider = ((OsmandApplication) ctx.getApplicationContext()).getExplorePlacesProvider();
	}

	public void initProvider(@NonNull MapRendererView mapRenderer) {
		if (providerInstance == null) {
			providerInstance = instantiateProxy();
		}
		mapRenderer.addSymbolsProvider(providerInstance);
	}

	public void deleteProvider(@NonNull MapRendererView mapRenderer) {
		if (providerInstance != null) {
			mapRenderer.removeSymbolsProvider(providerInstance);
			providerInstance = null;
		}
	}

	@Override
	public int getBaseOrder() {
		return baseOrder;
	}

	@Override
	public QListPointI getPoints31() {
		return new QListPointI();
	}

	@Override
	public QListPointI getHiddenPoints() {
		return new QListPointI();
	}

	@Override
	public boolean shouldShowCaptions() {
		return false;
	}

	@Override
	public TextRasterizer.Style getCaptionStyle() {
		return new TextRasterizer.Style();
	}

	@Override
	public double getCaptionTopSpace() {
		return 0.0;
	}

	@Override
	public float getReferenceTileSizeOnScreenInPixels() {
		return 256;
	}

	@Override
	public double getScale() {
		return 1.0d;
	}

	private static Bitmap getCircle(@NonNull Context ctx) {
		if (circleBitmap == null) {
			circleBitmap = RenderingIcons.getBitmapFromVectorDrawable(ctx, R.drawable.bg_point_circle);
		}
		return circleBitmap;
	}

	private static Paint createBitmapPaint() {
		Paint bitmapPaint = new Paint();
		bitmapPaint.setAntiAlias(true);
		bitmapPaint.setDither(true);
		bitmapPaint.setFilterBitmap(true);
		return bitmapPaint;
	}

	@Override
	public SingleSkImage getImageBitmap(int index, boolean isFullSize) {
		return SwigUtilities.nullSkImage();
	}

	@Override
	public String getCaption(int index) {
		return "";
	}

	@Override
	public QListMapTiledCollectionPoint getTilePoints(TileId tileId, ZoomLevel zoom) {
		if (OsmandMapLayer.isMapRendererLost(ctx)) {
			return new QListMapTiledCollectionPoint();
		}

		AreaI tileBBox31 = Utilities.tileBoundingBox31(tileId, zoom);
		double l = MapUtils.get31LongitudeX(tileBBox31.getTopLeft().getX());
		double t = MapUtils.get31LatitudeY(tileBBox31.getTopLeft().getY());
		double r = MapUtils.get31LongitudeX(tileBBox31.getBottomRight().getX());
		double b = MapUtils.get31LatitudeY(tileBBox31.getBottomRight().getY());

		QuadRect tileRect = new QuadRect(l, t, r, b);
		List<ExploreTopPlacePoint> places = explorePlacesProvider.getDataCollection(tileRect, tilePointsLimit);
		int i = 0;
		while (explorePlacesProvider.isLoadingRect(tileRect) && i++ * SLEEP_INTERVAL < TILE_LOADING_TIMEOUT) {
			try {
				Thread.sleep(SLEEP_INTERVAL);
			} catch (InterruptedException ignore) {
			}
		}
		places = explorePlacesProvider.getDataCollection(tileRect, tilePointsLimit);
		if (places.isEmpty() || OsmandMapLayer.isMapRendererLost(ctx)) {
			return new QListMapTiledCollectionPoint();
		}

		QListMapTiledCollectionPoint res = new QListMapTiledCollectionPoint();
		for (ExploreTopPlacePoint place : places) {
            TopPlaceCollectionPoint point = new TopPlaceCollectionPoint(place);
            res.add(point.instantiateProxy(true));
            point.swigReleaseOwnership();
		}
		return res;
	}

	@Override
	public ZoomLevel getMinZoom() {
		return ZoomLevel.ZoomLevel6;
	}

	@Override
	public ZoomLevel getMaxZoom() {
		return ZoomLevel.MaxZoomLevel;
	}

	@Override
	public boolean supportsNaturalObtainDataAsync() {
		return true;
	}

	@Override
	public MapMarker.PinIconVerticalAlignment getPinIconVerticalAlignment() {
		return MapMarker.PinIconVerticalAlignment.CenterVertical;
	}

	@Override
	public MapMarker.PinIconHorisontalAlignment getPinIconHorisontalAlignment() {
		return MapMarker.PinIconHorisontalAlignment.CenterHorizontal;
	}

	@Override
	public PointI getPinIconOffset() {
		return pinIconOffset;
	}

	public static Bitmap createSmallPointBitmap(@NonNull Context ctx) {
		int borderWidth = AndroidUtils.dpToPx(ctx, SMALL_ICON_BORDER_DP);
		Bitmap circle = getSmallCircle(ctx);
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

	public static Bitmap createBigBitmap(@NonNull OsmandApplication app, Bitmap loadedBitmap, boolean isSelected) {
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		int borderWidth = AndroidUtils.dpToPxAuto(app, BIG_ICON_BORDER_DP);
		Bitmap circle = getBigCircle(app);
		int bigIconSize = AndroidUtils.dpToPxAuto(app, BIG_ICON_SIZE_DP);
		Bitmap bitmapResult = Bitmap.createBitmap(bigIconSize, bigIconSize, Bitmap.Config.ARGB_8888);
		circle = Bitmap.createScaledBitmap(circle, bigIconSize, bigIconSize, true);
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

	public static int getBigIconSize(@NonNull OsmandApplication app) {
		return AndroidUtils.dpToPxAuto(app, BIG_ICON_SIZE_DP);
	}
}
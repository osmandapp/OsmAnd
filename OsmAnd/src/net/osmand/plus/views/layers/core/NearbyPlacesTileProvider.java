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
import net.osmand.core.jni.MapMarker;
import net.osmand.core.jni.MapTiledCollectionProvider;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QListMapTiledCollectionPoint;
import net.osmand.core.jni.QListPointI;
import net.osmand.core.jni.SingleSkImage;
import net.osmand.core.jni.SwigUtilities;
import net.osmand.core.jni.TextRasterizer;
import net.osmand.core.jni.TileId;
import net.osmand.core.jni.ZoomLevel;
import net.osmand.core.jni.interface_MapTiledCollectionProvider;
import net.osmand.data.NearbyPlacePoint;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.PointImageUtils;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

public class NearbyPlacesTileProvider extends interface_MapTiledCollectionProvider {

	private static final Log log = LogFactory.getLog(NearbyPlacesTileProvider.class);
	private final QListPointI points31 = new QListPointI();
	private final List<MapLayerData> mapLayerDataList = new ArrayList<>();
	private Bitmap cachedSmallBitmap;
	private final OsmandApplication app;
	private final float density;
	private final PointI offset;
	private MapTiledCollectionProvider providerInstance;
	private int baseOrder;
	private long selectedObjectId;

	private static final int SMALL_ICON_BORDER_DP = 1;
	private static final int BIG_ICON_BORDER_DP = 2;
	private static final int SMALL_ICON_SIZE_DP = 20;
	private static final int BIG_ICON_SIZE_DP = 40;
	private static final int POINT_OUTER_COLOR = 0xffffffff;

	private static Bitmap circleBitmap;

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


	public NearbyPlacesTileProvider(@NonNull OsmandApplication context, int baseOrder, float density, long selectedObjectId) {
		this.app = context;
		this.baseOrder = baseOrder;
		this.density = density;
		this.offset = new PointI(0, 0);
		this.selectedObjectId = selectedObjectId;
	}

	public void drawSymbols(@NonNull MapRendererView mapRenderer) {
		if (providerInstance == null) {
			providerInstance = instantiateProxy();
		}
		mapRenderer.addSymbolsProvider(providerInstance);
	}

	public void clearSymbols(@NonNull MapRendererView mapRenderer) {
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
		return points31;
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
		return -4.0 * density;
	}

	@Override
	public float getReferenceTileSizeOnScreenInPixels() {
		return 256;
	}

	@Override
	public double getScale() {
		return 1.0d;
	}

	@Override
	public SingleSkImage getImageBitmap(int index, boolean isFullSize) {
		NearbyPlacesTileProvider.MapLayerData data = index < mapLayerDataList.size() ? mapLayerDataList.get(index) : null;
		if (data == null) {
			return SwigUtilities.nullSkImage();
		}
		Bitmap bitmap;
		if (isFullSize && data.nearbyPlace.getImageBitmap() != null) {
			bitmap = createBigBitmap(app, data.nearbyPlace.getImageBitmap(), data.nearbyPlace.getId() == selectedObjectId);
		} else {
			if (cachedSmallBitmap == null) {
				cachedSmallBitmap = createSmallPointBitmap(app);
			}
			bitmap = cachedSmallBitmap;
		}
		return NativeUtilities.createSkImageFromBitmap(bitmap);
	}

	@Override
	public String getCaption(int index) {
		MapLayerData data = index < mapLayerDataList.size() ? mapLayerDataList.get(index) : null;
		return data != null ? PointDescription.getSimpleName(data.nearbyPlace, app) : "";
	}

	@Override
	public QListMapTiledCollectionPoint getTilePoints(TileId tileId, ZoomLevel zoom) {
		return new QListMapTiledCollectionPoint();
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
		return false;
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
		return offset;
	}

	public void addToData(@NonNull NearbyPlacePoint nearbyPlacePoint) throws IllegalStateException {
		if (providerInstance != null) {
			throw new IllegalStateException("Provider already instantiated. Data cannot be modified at this stage.");
		}
		int x31 = MapUtils.get31TileNumberX(nearbyPlacePoint.getLongitude());
		int y31 = MapUtils.get31TileNumberY(nearbyPlacePoint.getLatitude());
		points31.add(new PointI(x31, y31));
		mapLayerDataList.add(new MapLayerData(nearbyPlacePoint));
	}

	private static class MapLayerData {
		NearbyPlacePoint nearbyPlace;

		MapLayerData(@NonNull NearbyPlacePoint nearbyPlace) {
			this.nearbyPlace = nearbyPlace;
		}
	}

	public static Bitmap createSmallPointBitmap(@NonNull Context ctx) {
		int borderWidth = AndroidUtils.dpToPx(ctx, SMALL_ICON_BORDER_DP);
		Bitmap circle = getCircle(ctx);
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
		Bitmap circle = getCircle(app);
		int bigIconSize = AndroidUtils.dpToPxAuto(app, BIG_ICON_SIZE_DP);
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


}
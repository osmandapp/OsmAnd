package net.osmand.plus.views.layers.core;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import net.osmand.data.BackgroundType;
import net.osmand.data.NearbyPlacePoint;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.nearbyplaces.NearbyPlacesHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.layers.NearbyPlacesLayer;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NearbyPlacesTileProvider extends interface_MapTiledCollectionProvider {

	private static final Log log = LogFactory.getLog(NearbyPlacesTileProvider.class);
	private final QListPointI points31 = new QListPointI();
	private final List<MapLayerData> mapLayerDataList = new ArrayList<>();
	private final Map<String, Bitmap> bigBitmapCache = new ConcurrentHashMap<>();
	private final Map<String, Bitmap> smallBitmapCache = new ConcurrentHashMap<>();
	private Bitmap cachedSmallBitmap;
	private final OsmandApplication app;
	private final float density;
	private final PointI offset;
	private MapTiledCollectionProvider providerInstance;
	private int baseOrder = 0;
	private final Paint bitmapPaint;
	private Bitmap circle;

	private final Paint pointInnerCircle;
	private final Paint pointOuterCircle;
	private final int smallIconSize;
	private final int bigIconSize;
	private final int pointerOuterColor;
	private final Bitmap smallIconBg;
	private final Bitmap bigIconBg;
	private final NearbyPlacesLayer nearbyPlacesLayer;


	public NearbyPlacesTileProvider(@NonNull OsmandApplication context, NearbyPlacesLayer nearbyPlacesLayer, int baseOrder, float density) {
		this.app = context;
		this.nearbyPlacesLayer = nearbyPlacesLayer;
		this.baseOrder = baseOrder;
		this.density = density;
		this.bitmapPaint = nearbyPlacesLayer.getBitmapPaint();
		this.pointInnerCircle = nearbyPlacesLayer.getPointInnerCircle();
		this.pointOuterCircle = nearbyPlacesLayer.getPointOuterCircle();
		this.circle = nearbyPlacesLayer.getCircle();
		this.smallIconSize = nearbyPlacesLayer.getSmallIconSize();
		this.bigIconSize = nearbyPlacesLayer.getBigIconSize();
		this.bigIconBg = nearbyPlacesLayer.getBigIconBg();
		this.smallIconBg = nearbyPlacesLayer.getSmallIconBg();
		this.offset = new PointI(0, 0);
		this.pointerOuterColor = nearbyPlacesLayer.getPointOuterColor();
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
		Bitmap bitmapResult = null;
		NearbyPlacesTileProvider.MapLayerData data = index < mapLayerDataList.size() ? mapLayerDataList.get(index) : null;
		if (data == null) {
			return SwigUtilities.nullSkImage();
		}
		String key = data.nearbyPlace.photoTitle;
		if (isFullSize && data.nearbyPlace.imageBitmap != null) {
			bitmapResult = bigBitmapCache.get(key);
			if (bitmapResult == null) {
				bigBitmapCache.put(key, createBigBitmap(data.nearbyPlace.imageBitmap));
			}
		} else {
			bitmapResult = NearbyPlacesHelper.INSTANCE.createSmallPointBitmap(nearbyPlacesLayer);
			smallBitmapCache.put(key, bitmapResult);
		}
		return bitmapResult != null ? NativeUtilities.createSkImageFromBitmap(bitmapResult) : SwigUtilities.nullSkImage();
	}

	private @NonNull Bitmap createBigBitmap(@Nullable Bitmap loadedBitmap) {
		Bitmap bitmapResult;
		Bitmap bg = circle;
		bitmapResult = Bitmap.createBitmap(bigIconSize, bigIconSize, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmapResult);
		bitmapPaint.setColorFilter(new PorterDuffColorFilter(pointerOuterColor, PorterDuff.Mode.SRC_IN));
		canvas.drawBitmap(bg, 0, 0, bitmapPaint);
		if (loadedBitmap != null) {
			int cx = bg.getWidth() / 2;
			int cy = bg.getHeight() / 2;
			int radius = Math.min(cx, cy) - 8;
			canvas.save();
			canvas.clipRect(0, 0, bg.getWidth(), bg.getHeight());
			Path circularPath = new Path();
			circularPath.addCircle(cx, cy, radius, Path.Direction.CW);
			canvas.clipPath(circularPath);
			Rect srcRect = new Rect(0, 0, loadedBitmap.getWidth(), loadedBitmap.getHeight());
			RectF dstRect = new RectF(0, 0, bg.getWidth(), bg.getHeight());
			bitmapPaint.setColorFilter(null);
			canvas.drawBitmap(loadedBitmap, srcRect, dstRect, bitmapPaint);
			canvas.restore();
		}
		bitmapResult = AndroidUtils.scaleBitmap(bitmapResult, bigIconSize, bigIconSize, false);
		return bitmapResult;
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

	public void addToData(@NonNull NearbyPlacePoint nearbyPlacePoint, float textScale) throws IllegalStateException {
		if (providerInstance != null) {
			throw new IllegalStateException("Provider already instantiated. Data cannot be modified at this stage.");
		}
		int x31 = MapUtils.get31TileNumberX(nearbyPlacePoint.getLongitude());
		int y31 = MapUtils.get31TileNumberY(nearbyPlacePoint.getLatitude());
		points31.add(new PointI(x31, y31));
		mapLayerDataList.add(new MapLayerData(nearbyPlacePoint,
				textScale));
	}

	private static class MapLayerData {
		NearbyPlacePoint nearbyPlace;
		int color;
		boolean withShadow;
		BackgroundType backgroundType;
		float textScale;

		MapLayerData(@NonNull NearbyPlacePoint nearbyPlace, float textScale) {
			this.nearbyPlace = nearbyPlace;
			this.textScale = textScale;
		}
	}
}
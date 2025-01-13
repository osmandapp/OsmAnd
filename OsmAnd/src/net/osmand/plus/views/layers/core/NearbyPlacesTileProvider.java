package net.osmand.plus.views.layers.core;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
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
import net.osmand.plus.R;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NearbyPlacesTileProvider extends interface_MapTiledCollectionProvider {
	private static final Log LOG = PlatformUtil.getLog(NearbyPlacesTileProvider.class);

	private final QListPointI points31 = new QListPointI();
	private final List<MapLayerData> mapLayerDataList = new ArrayList<>();
	private final Map<Long, Bitmap> bigBitmapCache = new ConcurrentHashMap<>();
	private final Map<Long, Bitmap> smallBitmapCache = new ConcurrentHashMap<>();
	private final OsmandApplication app;
	private final int baseOrder;
	private final boolean textVisible;
	private final TextRasterizer.Style textStyle;
	private final float density;
	private final PointI offset;
	private MapTiledCollectionProvider providerInstance;
	private Bitmap circle;
	private final Paint bitmapPaint;


	public NearbyPlacesTileProvider(@NonNull OsmandApplication app, int baseOrder, boolean textVisible,
	                                @Nullable TextRasterizer.Style textStyle, float density) {
		this.app = app;
		this.baseOrder = baseOrder;
		this.textVisible = textVisible;
		this.textStyle = textStyle;
		this.density = density;
		this.offset = new PointI(0, 0);
		float scale = app.getOsmandMap().getCarDensityScaleCoef();
		circle = app.getUIUtilities().getScaledBitmap(null, R.drawable.ic_white_shield_small, scale);

		bitmapPaint = new Paint();
		bitmapPaint.setAntiAlias(true);
		bitmapPaint.setDither(true);
		bitmapPaint.setFilterBitmap(true);
	}

	public void drawSymbols(@NonNull MapRendererView mapRenderer) {
		LOG.debug("drawSymbols");
		if (providerInstance == null) {
			providerInstance = instantiateProxy();
		}
		mapRenderer.addSymbolsProvider(providerInstance);
	}

	public void clearSymbols(@NonNull MapRendererView mapRenderer) {
		LOG.debug("clearSymbols");
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
		return textVisible;
	}

	@Override
	public TextRasterizer.Style getCaptionStyle() {
		return textStyle;
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
		LOG.debug("getImageBitmap " + index + "__" + isFullSize);
		MapLayerData data = index < mapLayerDataList.size() ? mapLayerDataList.get(index) : null;
		if (data == null) {
			return SwigUtilities.nullSkImage();
		}
		long key = data.getKey();
		Bitmap bitmapResult = null;
		bitmapPaint.setColorFilter(new PorterDuffColorFilter(data.color, PorterDuff.Mode.MULTIPLY));
		bitmapResult = Bitmap.createBitmap(circle.getWidth(), circle.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmapResult);
		canvas.drawBitmap(circle, 0, 0, bitmapPaint);

//
//		if (isFullSize) {
//			bitmap = bigBitmapCache.get(key);
//			if (bitmap == null) {
//				PointImageDrawable drawable;
//				if (data.hasMarker) {
//					drawable = PointImageUtils.getOrCreate(app, data.color, data.withShadow,
//							true, /*data.overlayIconId, */data.backgroundType);
//				} else {
//					drawable = PointImageUtils.getOrCreate(app, data.color,
//							data.withShadow, false, /*data.overlayIconId, */data.backgroundType);
//				}
//				bitmap = drawable.getBigMergedBitmap(data.textScale, false);
//				bigBitmapCache.put(key, bitmap);
//			}
//		} else {
//			bitmap = smallBitmapCache.get(key);
//			if (bitmap == null) {
//				PointImageDrawable drawable = PointImageUtils.getOrCreate(app, data.color,
//						data.withShadow, false, /*data.overlayIconId, */data.backgroundType);
//				bitmap = drawable.getSmallMergedBitmap(data.textScale);
//				smallBitmapCache.put(key, bitmap);
//			}
//		}
		return bitmapResult != null ? NativeUtilities.createSkImageFromBitmap(bitmapResult) : SwigUtilities.nullSkImage();
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

	public void addToData(@NonNull NearbyPlacePoint nearbyPlace, int color, boolean withShadow,
	                      float textScale) throws IllegalStateException {
		if (providerInstance != null) {
			throw new IllegalStateException("Provider already instantiated. Data cannot be modified at this stage.");
		}

		int x31 = MapUtils.get31TileNumberX(nearbyPlace.getLongitude());
		int y31 = MapUtils.get31TileNumberY(nearbyPlace.getLatitude());
		points31.add(new PointI(x31, y31));
		mapLayerDataList.add(new MapLayerData(nearbyPlace, color,
				withShadow, /*nearbyPlace.getOverlayIconId(ctx), */nearbyPlace.getBackgroundType(),
				textScale));
	}

	private static class MapLayerData {
		NearbyPlacePoint nearbyPlace;
		int color;
		boolean withShadow;
		BackgroundType backgroundType;
		float textScale;

		MapLayerData(@NonNull NearbyPlacePoint nearbyPlace, int color,
		             boolean withShadow, @NonNull BackgroundType backgroundType,
		             float textScale) {
			this.nearbyPlace = nearbyPlace;
			this.color = color;
			this.withShadow = withShadow;
			this.backgroundType = backgroundType;
			this.textScale = textScale;
		}

		long getKey() {
			return ((long) color << 6) + ((withShadow ? 1 : 0) << 3)
					+ (int) (textScale * 10) + (backgroundType != null ? backgroundType.ordinal() : 0);
		}
	}
}
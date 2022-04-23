package net.osmand.plus.views.layers.core;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.MapTiledCollectionProvider;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QListPointI;
import net.osmand.core.jni.SWIGTYPE_p_sk_spT_SkImage_const_t;
import net.osmand.core.jni.TextRasterizer;
import net.osmand.core.jni.ZoomLevel;
import net.osmand.core.jni.interface_MapTiledCollectionProvider;
import net.osmand.data.BackgroundType;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.PointImageDrawable;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FavoritesTileProvider extends interface_MapTiledCollectionProvider {

	private final List<FavouritesMapLayerData> favouritesMapLayerDataList = Collections.synchronizedList(new ArrayList<>());
	private final Map<Integer, Bitmap> bigBitmapCache = new ConcurrentHashMap<>();
	private final Map<Integer, Bitmap> smallBitmapCache = new ConcurrentHashMap<>();
	private final int baseOrder;
	private final Context ctx;
	private MapTiledCollectionProvider providerInstance;

	public FavoritesTileProvider(@NonNull Context context, int baseOrder) {
		this.ctx = context;
		this.baseOrder = baseOrder;
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
		}
		favouritesMapLayerDataList.clear();
	}

	@Override
	public int getBaseOrder() {
		return baseOrder;
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
		return 0.0d;
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
	public PointI getPoint31(int index) {
		return favouritesMapLayerDataList.get(index).point;
	}

	@Override
	public int getPointsCount() {
		return favouritesMapLayerDataList.size();
	}

	@Override
	public SWIGTYPE_p_sk_spT_SkImage_const_t getImageBitmap(int index, boolean isFullSize) {
		FavouritesMapLayerData data = index < favouritesMapLayerDataList.size()
				? favouritesMapLayerDataList.get(index) : null;
		if (data == null) {
			Bitmap emptyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
			return NativeUtilities.createSkImageFromBitmap(emptyBitmap);
		}
		Bitmap bitmap;
		if (isFullSize) {
			int bigBitmapKey = getKey(data.colorBigPoint, data.withShadow, data.overlayIconId,
					data.backgroundType, data.hasMarker, data.textScale);
			if (!bigBitmapCache.containsKey(bigBitmapKey)) {
				PointImageDrawable pointImageDrawable;
				if (data.hasMarker) {
					pointImageDrawable = PointImageDrawable.getOrCreate(ctx, data.colorBigPoint,
							data.withShadow, true, data.overlayIconId, data.backgroundType);
				} else {
					pointImageDrawable = PointImageDrawable.getOrCreate(ctx, data.colorBigPoint,
							data.withShadow, false, data.overlayIconId, data.backgroundType);
				}
				bitmap = pointImageDrawable.getBigMergedBitmap(data.textScale);
				bigBitmapCache.put(bigBitmapKey, bitmap);
			} else {
				bitmap = bigBitmapCache.get(bigBitmapKey);
			}
		} else {
			int smallBitmapKey = getKey(data.colorSmallPoint, data.withShadow, data.overlayIconId,
					data.backgroundType, data.hasMarker, data.textScale);
			if (!smallBitmapCache.containsKey(smallBitmapKey)) {
				PointImageDrawable pointImageDrawable = PointImageDrawable.getOrCreate(ctx,
						data.colorSmallPoint, data.withShadow, false, data.overlayIconId, data.backgroundType);
				bitmap = pointImageDrawable.getSmallMergedBitmap(data.textScale);
				smallBitmapCache.put(smallBitmapKey, bitmap);
			} else {
				bitmap = smallBitmapCache.get(smallBitmapKey);
			}
		}
		if (bitmap == null) {
			Bitmap emptyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
			return NativeUtilities.createSkImageFromBitmap(emptyBitmap);
		} else {
			return NativeUtilities.createSkImageFromBitmap(bitmap);
		}
	}

	@Override
	public String getCaption(int index) {
		return "";
	}

	@Override
	public ZoomLevel getMinZoom() {
		return ZoomLevel.ZoomLevel6;
	}

	@Override
	public ZoomLevel getMaxZoom() {
		return ZoomLevel.MaxZoomLevel;
	}

	public void clearData() {
		favouritesMapLayerDataList.clear();
		bigBitmapCache.clear();
		smallBitmapCache.clear();
	}

	public void addToData(int colorSmallPoint, int colorBigPoint, boolean withShadow,
	                      int overlayIconId, BackgroundType backgroundType, boolean hasMarker,
	                      float textScale, double lat, double lon) {
		favouritesMapLayerDataList.add(new FavouritesMapLayerData(colorSmallPoint, colorBigPoint,
				withShadow, overlayIconId, backgroundType, hasMarker, textScale, lat, lon));
	}

	private static class FavouritesMapLayerData {
		PointI point;
		int colorSmallPoint;
		int colorBigPoint;
		boolean withShadow;
		int overlayIconId;
		BackgroundType backgroundType;
		boolean hasMarker;
		float textScale;

		FavouritesMapLayerData(int colorSmallPoint, int colorBigPoint, boolean withShadow, int overlayIconId,
		                       @NonNull BackgroundType backgroundType, boolean hasMarker,
		                       float textScale, double lat, double lon) {
			this.colorBigPoint = colorBigPoint;
			this.colorSmallPoint = colorSmallPoint;
			this.withShadow = withShadow;
			this.overlayIconId = overlayIconId;
			this.backgroundType = backgroundType;
			this.hasMarker = hasMarker;
			this.textScale = textScale;
			int x = MapUtils.get31TileNumberX(lon);
			int y = MapUtils.get31TileNumberY(lat);
			point = new PointI(x, y);
		}
	}

	private int getKey(int color, boolean withShadow, int overlayIconId,
	                   @NonNull BackgroundType backgroundType, boolean hasMarker, float textScale) {
		long hash = ((long) color << 6) + ((long) overlayIconId << 4) + ((withShadow ? 1 : 0) << 3)
				+ ((hasMarker ? 1 : 0) << 2) + (int) (textScale * 10) + backgroundType.ordinal();
		if (hash >= Integer.MAX_VALUE || hash <= Integer.MIN_VALUE) {
			return (int) (hash >> 4);
		}
		return (int) hash;
	}
}

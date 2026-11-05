package net.osmand.plus.views.layers.core;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.*;
import net.osmand.data.BackgroundType;
import net.osmand.data.FavouritePoint;
import net.osmand.data.PointDescription;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.PointImageDrawable;
import net.osmand.plus.views.PointImageUtils;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FavoritesTileProvider extends interface_MapTiledCollectionProvider {

	private final QListPointI points31 = new QListPointI();
	private final List<MapLayerData> mapLayerDataList = new ArrayList<>();
	private final Map<Long, Bitmap> bigBitmapCache = new ConcurrentHashMap<>();
	private final Map<Long, Bitmap> smallBitmapCache = new ConcurrentHashMap<>();
	private final Context ctx;
	private final int baseOrder;
	private final boolean textVisible;
	private final TextRasterizer.Style textStyle;
	private final float density;
	private final PointI offset;
	private MapTiledCollectionProvider providerInstance;

	public FavoritesTileProvider(@NonNull Context context, int baseOrder, boolean textVisible,
	                             @Nullable TextRasterizer.Style textStyle, float density) {
		this.ctx = context;
		this.baseOrder = baseOrder;
		this.textVisible = textVisible;
		this.textStyle = textStyle;
		this.density = density;
		this.offset = new PointI(0, 0);
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
		MapLayerData data = index < mapLayerDataList.size() ? mapLayerDataList.get(index) : null;
		if (data == null) {
			return SwigUtilities.nullSkImage();
		}
		Bitmap bitmap;
		long key = data.getKey();
		if (isFullSize) {
			bitmap = bigBitmapCache.get(key);
			if (bitmap == null) {
				PointImageDrawable drawable;
				if (data.hasMarker) {
					drawable = PointImageUtils.getOrCreate(ctx, data.color, data.withShadow,
							true, data.overlayIconId, data.backgroundType);
				} else {
					drawable = PointImageUtils.getOrCreate(ctx, data.color,
							data.withShadow, false, data.overlayIconId, data.backgroundType);
				}
				bitmap = drawable.getBigMergedBitmap(data.textScale, false);
				bigBitmapCache.put(key, bitmap);
			}
		} else {
			bitmap = smallBitmapCache.get(key);
			if (bitmap == null) {
				PointImageDrawable drawable = PointImageUtils.getOrCreate(ctx, data.color,
						data.withShadow, false, data.overlayIconId, data.backgroundType);
				bitmap = drawable.getSmallMergedBitmap(data.textScale);
				smallBitmapCache.put(key, bitmap);
			}
		}
		return bitmap != null ? NativeUtilities.createSkImageFromBitmap(bitmap) : SwigUtilities.nullSkImage();
	}

	@Override
	public String getCaption(int index) {
		MapLayerData data = index < mapLayerDataList.size() ? mapLayerDataList.get(index) : null;
		return data != null ? PointDescription.getSimpleName(data.favorite, ctx) : "";
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

	public void addToData(@NonNull FavouritePoint favorite, int color, boolean withShadow,
	                      boolean hasMarker, float textScale) throws IllegalStateException {
		if (providerInstance != null) {
			throw new IllegalStateException("Provider already instantiated. Data cannot be modified at this stage.");
		}

		int x31 = MapUtils.get31TileNumberX(favorite.getLongitude());
		int y31 = MapUtils.get31TileNumberY(favorite.getLatitude());
		points31.add(new PointI(x31, y31));
		mapLayerDataList.add(new MapLayerData(favorite, color,
				withShadow, favorite.getOverlayIconId(ctx), favorite.getBackgroundType(),
				hasMarker, textScale));
	}

	private static class MapLayerData {
		FavouritePoint favorite;
		int color;
		boolean withShadow;
		int overlayIconId;
		BackgroundType backgroundType;
		boolean hasMarker;
		float textScale;

		MapLayerData(@NonNull FavouritePoint favorite, int color,
		             boolean withShadow, int overlayIconId, @NonNull BackgroundType backgroundType,
		             boolean hasMarker, float textScale) {
			this.favorite = favorite;
			this.color = color;
			this.withShadow = withShadow;
			this.overlayIconId = overlayIconId;
			this.backgroundType = backgroundType;
			this.hasMarker = hasMarker;
			this.textScale = textScale;
		}

		long getKey() {
			return ((long) color << 6) + ((long) overlayIconId << 4) + ((withShadow ? 1 : 0) << 3)
					+ ((hasMarker ? 1 : 0) << 2) + (int) (textScale * 10) + (backgroundType != null ? backgroundType.ordinal() : 0);
		}
	}
}
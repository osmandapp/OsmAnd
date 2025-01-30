package net.osmand.plus.views.layers.core;

import android.graphics.Bitmap;

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
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.PointImageUtils;
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
	private final Map<String, NearbyPlaceCacheItem> bigBitmapCache = new ConcurrentHashMap<>();
	private Bitmap cachedSmallBitmap;
	private final OsmandApplication app;
	private final float density;
	private final PointI offset;
	private MapTiledCollectionProvider providerInstance;
	private int baseOrder;

	public static class NearbyPlaceCacheItem {
		public final Bitmap bitmap;
		public final boolean isSelected;

		public NearbyPlaceCacheItem(@NonNull Bitmap bitmap, boolean isSelected) {
			this.bitmap = bitmap;
			this.isSelected = isSelected;
		}
	}

	public NearbyPlacesTileProvider(@NonNull OsmandApplication context, int baseOrder, float density) {
		this.app = context;
		this.baseOrder = baseOrder;
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
		NearbyPlaceCacheItem cacheItem;
		NearbyPlacesTileProvider.MapLayerData data = index < mapLayerDataList.size() ? mapLayerDataList.get(index) : null;
		if (data == null) {
			return SwigUtilities.nullSkImage();
		}
		String key = data.nearbyPlace.photoTitle;
		if (isFullSize && data.nearbyPlace.imageBitmap != null) {
			cacheItem = bigBitmapCache.get(key);
			if (cacheItem == null || cacheItem.isSelected != data.nearbyPlace.isSelected) {
				Bitmap bitmap = PointImageUtils.createBigBitmap(app, data.nearbyPlace.imageBitmap, data.nearbyPlace.isSelected);
				cacheItem = new NearbyPlaceCacheItem(bitmap, data.nearbyPlace.isSelected);
				bigBitmapCache.put(key, cacheItem);
			}
		} else {
			if (cachedSmallBitmap == null) {
				cachedSmallBitmap = PointImageUtils.createSmallPointBitmap(app);
			}
			cacheItem = new NearbyPlaceCacheItem(cachedSmallBitmap, data.nearbyPlace.isSelected);
		}
		return NativeUtilities.createSkImageFromBitmap(cacheItem.bitmap);
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
}
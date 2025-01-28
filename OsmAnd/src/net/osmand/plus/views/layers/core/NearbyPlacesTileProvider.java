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
import net.osmand.plus.views.PointImageUtils;
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
	private Bitmap cachedSmallBitmap;
	private final OsmandApplication app;
	private final float density;
	private final PointI offset;
	private MapTiledCollectionProvider providerInstance;
	private int baseOrder;
	private final NearbyPlacesLayer nearbyPlacesLayer;


	public NearbyPlacesTileProvider(@NonNull OsmandApplication context, NearbyPlacesLayer nearbyPlacesLayer, int baseOrder, float density) {
		this.app = context;
		this.nearbyPlacesLayer = nearbyPlacesLayer;
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
		Bitmap bitmapResult;
		NearbyPlacesTileProvider.MapLayerData data = index < mapLayerDataList.size() ? mapLayerDataList.get(index) : null;
		if (data == null) {
			return SwigUtilities.nullSkImage();
		}
		String key = data.nearbyPlace.photoTitle;
		if (isFullSize && data.nearbyPlace.imageBitmap != null) {
			bitmapResult = bigBitmapCache.get(key);
			if (bitmapResult == null) {
				bitmapResult = PointImageUtils.createBigBitmap(nearbyPlacesLayer, data.nearbyPlace.imageBitmap);
				bigBitmapCache.put(key, bitmapResult);
			}
		} else {
			if (cachedSmallBitmap == null) {
				cachedSmallBitmap = PointImageUtils.createSmallPointBitmap(nearbyPlacesLayer);
			}
			bitmapResult = cachedSmallBitmap;
		}
		return NativeUtilities.createSkImageFromBitmap(bitmapResult);
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
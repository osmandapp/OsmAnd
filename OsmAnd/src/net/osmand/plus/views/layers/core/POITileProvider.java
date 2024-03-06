package net.osmand.plus.views.layers.core;

import static net.osmand.osm.MapPoiTypes.ROUTE_ARTICLE_POINT;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

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
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.card.color.palette.main.data.DefaultColors;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.PointImageDrawable;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer.TileBoxRequest;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.List;

public class POITileProvider extends interface_MapTiledCollectionProvider {

	private final Context ctx;
	private final int baseOrder;
	private final boolean textVisible;
	private final TextRasterizer.Style textStyle;
	private final float textScale;
	private final float density;
	private final PointI offset;

	private final OsmandMapLayer.MapLayerData<List<Amenity>> layerData;
	private MapTiledCollectionProvider providerInstance;

	private static class POICollectionPoint extends interface_MapTiledCollectionPoint {

		private final Context ctx;
		private final Amenity amenity;
		private final float textScale;
		private final PointI point31;

		public POICollectionPoint(@NonNull Context ctx, @NonNull Amenity amenity, float textScale) {
			this.ctx = ctx;
			this.amenity = amenity;
			this.textScale = textScale;
			LatLon latLon = amenity.getLocation();
			this.point31 = new PointI(MapUtils.get31TileNumberX(latLon.getLongitude()),
					MapUtils.get31TileNumberY(latLon.getLatitude()));
		}

		private int getColor() {
			int color = 0;
			if (ROUTE_ARTICLE_POINT.equals(amenity.getSubType())) {
				String colorStr = amenity.getColor();
				if (colorStr != null) {
					color = DefaultColors.valueOf(colorStr);
				}
			}
			return color != 0 ? color : ContextCompat.getColor(ctx, R.color.osmand_orange);
		}

		@Override
		public PointI getPoint31() {
			return point31;
		}

		@Override
		public SingleSkImage getImageBitmap(boolean isFullSize) {
			Bitmap bitmap = null;
			if (isFullSize) {
				String id = amenity.getGpxIcon();
				if (id == null) {
					id = RenderingIcons.getIconNameForAmenity(amenity);
				}
				if (id != null) {
					PointImageDrawable pointImageDrawable = PointImageDrawable.getOrCreate(ctx, getColor(),
							true, RenderingIcons.getResId(id));
					pointImageDrawable.setAlpha(0.8f);
					bitmap = pointImageDrawable.getBigMergedBitmap(textScale, false);
				}
			} else {
				PointImageDrawable pointImageDrawable = PointImageDrawable.getOrCreate(ctx, getColor(), true);
				pointImageDrawable.setAlpha(0.8f);
				bitmap = pointImageDrawable.getSmallMergedBitmap(textScale);
			}
			return bitmap != null ? NativeUtilities.createSkImageFromBitmap(bitmap) : SwigUtilities.nullSkImage();
		}

		@Override
		public String getCaption() {
			OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
			String locale = app.getSettings().MAP_PREFERRED_LOCALE.get();
			if (amenity.getType().isWiki()) {
				if (Algorithms.isEmpty(locale)) {
					locale = app.getLanguage();
				}
				locale = PluginsHelper.onGetMapObjectsLocale(amenity, locale);
			}
			return amenity.getName(locale, app.getSettings().MAP_TRANSLITERATE_NAMES.get());
		}
	}

	public POITileProvider(@NonNull Context context, OsmandMapLayer.MapLayerData<List<Amenity>> layerData,
	                       int baseOrder, boolean textVisible, @Nullable TextRasterizer.Style textStyle,
	                       float textScale, float density) {
		this.ctx = context;
		this.layerData = layerData;
		this.baseOrder = baseOrder;
		this.textVisible = textVisible;
		this.textStyle = textStyle != null ? textStyle : new TextRasterizer.Style();
		this.textScale = textScale;
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
		return new QListPointI();
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
	public QListMapTiledCollectionPoint getTilePoints(TileId tileId, ZoomLevel zoom) {
		if (isMapRendererLost()) {
			return new QListMapTiledCollectionPoint();
		}

		OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
		RotatedTileBox tb = app.getOsmandMap().getMapView().getRotatedTileBox();
		TileBoxRequest request = new TileBoxRequest(tb);
		OsmandMapLayer.MapLayerData<List<Amenity>>.DataReadyCallback dataReadyCallback = layerData.getDataReadyCallback(request);
		layerData.addDataReadyCallback(dataReadyCallback);
		long[] start = {System.currentTimeMillis()};
		app.runInUIThread(() -> {
			layerData.queryNewData(request);
			start[0] = System.currentTimeMillis();
		});
		while (System.currentTimeMillis() - start[0] < layerData.DATA_REQUEST_TIMEOUT) {
			if (isMapRendererLost()) {
				return new QListMapTiledCollectionPoint();
			}
			synchronized (dataReadyCallback.getSync()) {
				if (dataReadyCallback.isReady()) {
					break;
				}
				try {
					dataReadyCallback.getSync().wait(50);
				} catch (InterruptedException ignored) {
				}
			}
		}
		layerData.removeDataReadyCallback(dataReadyCallback);

		if (isMapRendererLost()) {
			return new QListMapTiledCollectionPoint();
		}

		List<Amenity> results = dataReadyCallback.getResults();
		if (Algorithms.isEmpty(results)) {
			return new QListMapTiledCollectionPoint();
		}
		AreaI tileBBox31 = Utilities.tileBoundingBox31(tileId, zoom);
		QuadRect latLonBounds = new QuadRect(
				MapUtils.get31LongitudeX(tileBBox31.getTopLeft().getX()),
				MapUtils.get31LatitudeY(tileBBox31.getTopLeft().getY()),
				MapUtils.get31LongitudeX(tileBBox31.getBottomRight().getX()),
				MapUtils.get31LatitudeY(tileBBox31.getBottomRight().getY()));
		QListMapTiledCollectionPoint res = new QListMapTiledCollectionPoint();
		for (Amenity amenity : results) {
			LatLon latLon = amenity.getLocation();
			if (latLonBounds.contains(latLon.getLongitude(), latLon.getLatitude(),
					latLon.getLongitude(), latLon.getLatitude())) {
				POICollectionPoint point = new POICollectionPoint(ctx, amenity, textScale);
				res.add(point.instantiateProxy(true));
				point.swigReleaseOwnership();
			}
		}
		return res;
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
	public ZoomLevel getMinZoom() {
		return ZoomLevel.ZoomLevel9;
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

	private boolean isMapRendererLost() {
		return !((OsmandApplication) ctx.getApplicationContext()).getOsmandMap().getMapView().hasMapRenderer();
	}
}
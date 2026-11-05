package net.osmand.plus.views.layers.core;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
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
import net.osmand.data.BackgroundType;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.TransportStop;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.transport.TransportStopType;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.PointImageDrawable;
import net.osmand.plus.views.PointImageUtils;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer.TileBoxRequest;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.List;

public class TransportStopsTileProvider extends interface_MapTiledCollectionProvider {

	private final Context ctx;
	private final int baseOrder;
	private final boolean textVisible;
	private final TextRasterizer.Style textStyle;
	private final float textScale;
	private final PointI offset;

	private final OsmandMapLayer.MapLayerData<List<TransportStop>> layerData;
	private MapTiledCollectionProvider providerInstance;

	public TransportStopsTileProvider(@NonNull Context context, OsmandMapLayer.MapLayerData<List<TransportStop>> layerData,
                                      int baseOrder, float textScale) {
		this.ctx = context;
		this.layerData = layerData;
		this.baseOrder = baseOrder;
		this.textVisible = false;
		this.textStyle = new TextRasterizer.Style();
		this.textScale = textScale;
		offset = new PointI(0, 0);
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
	public QListMapTiledCollectionPoint getTilePoints(TileId tileId, ZoomLevel zoom) {
		if (isMapRendererLost()) {
			return new QListMapTiledCollectionPoint();
		}

		OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
		RotatedTileBox tb = app.getOsmandMap().getMapView().getRotatedTileBox();
		TileBoxRequest request = new TileBoxRequest(tb);
		OsmandMapLayer.MapLayerData<List<TransportStop>>.DataReadyCallback dataReadyCallback = layerData.getDataReadyCallback(request);
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

		List<TransportStop> results = dataReadyCallback.getResults();
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
		for (TransportStop stop : results) {
			LatLon latLon = stop.getLocation();
			if (latLonBounds.contains(latLon.getLongitude(), latLon.getLatitude(),
					latLon.getLongitude(), latLon.getLatitude())) {
				StopsCollectionPoint point = new StopsCollectionPoint(ctx, stop, textScale, "");
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
		return ZoomLevel.ZoomLevel10;
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

	public static class StopsCollectionPoint extends interface_MapTiledCollectionPoint {

		private final Context ctx;
		private final float textScale;
		private final PointI point31;
		private final String transportRouteType;

		public StopsCollectionPoint(@NonNull Context ctx, @NonNull TransportStop stop, float textScale, String transportRouteType) {
			this.ctx = ctx;
			this.textScale = textScale;
			LatLon latLon = stop.getLocation();
			this.point31 = new PointI(MapUtils.get31TileNumberX(latLon.getLongitude()),
					MapUtils.get31TileNumberY(latLon.getLatitude()));
			this.transportRouteType = transportRouteType;
		}


		@Override
		public PointI getPoint31() {
			return point31;
		}

		@Override
		public SingleSkImage getImageBitmap(boolean isFullSize) {
			Bitmap bitmap;
			if (isFullSize) {
				PointImageDrawable pointImageDrawable = null;
				if (transportRouteType.isEmpty()) {
					pointImageDrawable = PointImageUtils.getOrCreate(ctx,
							ContextCompat.getColor(ctx, R.color.transport_stop_icon_background),
							true,false, R.drawable.mx_highway_bus_stop, BackgroundType.SQUARE);
				} else {
					TransportStopType type = TransportStopType.findType(transportRouteType);
					if (type != null) {
						pointImageDrawable = PointImageUtils.getOrCreate(ctx,
								ContextCompat.getColor(ctx, R.color.transport_stop_icon_background),
								true,false, RenderingIcons.getResId(type.getResName()), BackgroundType.SQUARE);
					}
				}
				if (pointImageDrawable == null) {
					return SwigUtilities.nullSkImage();
				}
				pointImageDrawable.setAlpha(0.9f);
				bitmap = pointImageDrawable.getBigMergedBitmap(textScale, false);
			} else {
				PointImageDrawable pointImageDrawable = PointImageUtils.getOrCreate(ctx,
						ContextCompat.getColor(ctx, R.color.transport_stop_icon_background),
						true, false, 0, BackgroundType.SQUARE);
				pointImageDrawable.setAlpha(0.9f);
				bitmap = pointImageDrawable.getSmallMergedBitmap(textScale);
			}
			return bitmap != null ? NativeUtilities.createSkImageFromBitmap(bitmap) : SwigUtilities.nullSkImage();
		}

		@Override
		public String getCaption() {
			return "";
		}
	}

	private boolean isMapRendererLost() {
		return !((OsmandApplication) ctx.getApplicationContext()).getOsmandMap().getMapView().hasMapRenderer();
	}
}
package net.osmand.plus.views.layers.core;

import static net.osmand.data.FavouritePoint.DEFAULT_UI_ICON_ID;

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
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.osmedit.OsmBugsLayer;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.PointImageDrawable;
import net.osmand.plus.views.PointImageUtils;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer.TileBoxRequest;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.List;

public class OsmBugsTileProvider extends interface_MapTiledCollectionProvider {

	private static final BackgroundType BACKGROUND_TYPE = BackgroundType.COMMENT;

	private final Context ctx;
	private final int baseOrder;
	private final boolean textVisible;
	private final TextRasterizer.Style textStyle;
	private final float textScale;
	private final boolean showClosed;
	private final int minZoom;
	private final PointI offset;

	private final OsmandMapLayer.MapLayerData<List<OsmBugsLayer.OpenStreetNote>> layerData;
	private MapTiledCollectionProvider providerInstance;

	private static class OsmBugsCollectionPoint extends interface_MapTiledCollectionPoint {

		private final Context ctx;
		private final OsmBugsLayer.OpenStreetNote osmNote;
		private final float textScale;
		private final PointI point31;
		private final boolean showClosed;

		public OsmBugsCollectionPoint(@NonNull Context ctx, @NonNull OsmBugsLayer.OpenStreetNote osmNote, float textScale, boolean showClosed) {
			this.ctx = ctx;
			this.osmNote = osmNote;
			this.textScale = textScale;
			int x = MapUtils.get31TileNumberX(osmNote.getLongitude());
			int y = MapUtils.get31TileNumberY(osmNote.getLatitude());
			this.point31 = new PointI(x, y);
			this.showClosed = showClosed;
		}

		@Override
		public PointI getPoint31() {
			return point31;
		}

		@Override
		public SingleSkImage getImageBitmap(boolean isFullSize) {
			Bitmap bitmap = null;
			if (!osmNote.isOpened() && !showClosed) {
				return SwigUtilities.nullSkImage();
			}
			if (isFullSize) {
				int iconId;
				int backgroundColorRes;
				if (osmNote.isOpened()) {
					iconId = R.drawable.mx_special_symbol_remove;
					backgroundColorRes = R.color.osm_bug_unresolved_icon_color;
				} else {
					iconId = R.drawable.mx_special_symbol_check_mark;
					backgroundColorRes = R.color.osm_bug_resolved_icon_color;
				}
				PointImageDrawable pointImageDrawable = PointImageUtils.getOrCreate(ctx,
						ContextCompat.getColor(ctx, backgroundColorRes), true, false, iconId,
						BACKGROUND_TYPE);
				bitmap = pointImageDrawable.getBigMergedBitmap(textScale, false);
			} else {
				int backgroundColorRes;
				if (osmNote.isOpened()) {
					backgroundColorRes = R.color.osm_bug_unresolved_icon_color;
				} else {
					backgroundColorRes = R.color.osm_bug_resolved_icon_color;
				}
				PointImageDrawable pointImageDrawable = PointImageUtils.getOrCreate(ctx,
						ContextCompat.getColor(ctx, backgroundColorRes), true,
						false, DEFAULT_UI_ICON_ID, BACKGROUND_TYPE);
				bitmap = pointImageDrawable.getSmallMergedBitmap(textScale);
			}
			return bitmap != null ? NativeUtilities.createSkImageFromBitmap(bitmap) : SwigUtilities.nullSkImage();
		}

		@Override
		public String getCaption() {
			return "";
		}
	}

	public OsmBugsTileProvider(@NonNull Context context, OsmandMapLayer.MapLayerData<List<OsmBugsLayer.OpenStreetNote>> layerData,
	                           int baseOrder, boolean showClosed, int minZoom, float textScale) {
		this.ctx = context;
		this.layerData = layerData;
		this.baseOrder = baseOrder;
		this.textVisible = false;
		this.textStyle = new TextRasterizer.Style();
		this.textScale = textScale;
		this.showClosed = showClosed;
		this.minZoom = minZoom;
		this.offset = new PointI(0, -BACKGROUND_TYPE.getOffsetY(context, textScale));
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
		OsmandMapLayer.MapLayerData<List<OsmBugsLayer.OpenStreetNote>>.DataReadyCallback dataReadyCallback = layerData.getDataReadyCallback(request);
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

		List<OsmBugsLayer.OpenStreetNote> results = dataReadyCallback.getResults();
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
		for (OsmBugsLayer.OpenStreetNote osmNote : results) {
			if (latLonBounds.contains(osmNote.getLongitude(), osmNote.getLatitude(),
					osmNote.getLongitude(), osmNote.getLatitude())) {
				OsmBugsCollectionPoint point = new OsmBugsCollectionPoint(ctx, osmNote, textScale, showClosed);
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
		return ZoomLevel.swigToEnum(minZoom);
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
		return offset;
	}

	private boolean isMapRendererLost() {
		return !((OsmandApplication) ctx.getApplicationContext()).getOsmandMap().getMapView().hasMapRenderer();
	}
}
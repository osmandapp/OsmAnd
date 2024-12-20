package net.osmand.plus.views.layers.core;

import android.content.Context;
import android.graphics.Bitmap;

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
import net.osmand.core.jni.interface_MapTiledCollectionPoint;
import net.osmand.core.jni.interface_MapTiledCollectionProvider;
import net.osmand.data.DataTileManager;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.util.MapUtils;

import java.util.List;

public class TilePointsProvider<T extends TilePointsProvider.ICollectionPoint> extends interface_MapTiledCollectionProvider {

	private final Context ctx;
	private final int baseOrder;
	private final boolean textVisible;
	private final TextRasterizer.Style textStyle;
	private final float textScale;
	private final float density;
	private final int minZoom;
	private final int maxZoom;
	private final PointI offset;

	private final DataTileManager<T> points;
	private MapTiledCollectionProvider providerInstance;

	public interface ICollectionPoint {
		double getLatitude();

		double getLongitude();

		Bitmap getBigImage(@NonNull Context ctx, float textScale, float density);

		Bitmap getSmallImage(@NonNull Context ctx, float textScale, float density);

		@NonNull
		String getCaption(@NonNull Context ctx);
	}

	private static class CollectionPoint extends interface_MapTiledCollectionPoint {

		private final Context ctx;
		private final ICollectionPoint point;
		private final float textScale;
		private final float density;
		private final PointI point31;

		public CollectionPoint(@NonNull Context ctx, @NonNull ICollectionPoint point, float textScale, float density) {
			this.ctx = ctx;
			this.point = point;
			this.textScale = textScale;
			this.density = density;
			this.point31 = new PointI(MapUtils.get31TileNumberX(point.getLongitude()),
					MapUtils.get31TileNumberY(point.getLatitude()));
		}

		@Override
		public PointI getPoint31() {
			return point31;
		}

		@Override
		public SingleSkImage getImageBitmap(boolean isFullSize) {
			Bitmap bitmap = isFullSize
					? point.getBigImage(ctx, textScale, density)
					: point.getSmallImage(ctx, textScale, density);
			return bitmap != null ? NativeUtilities.createSkImageFromBitmap(bitmap) : SwigUtilities.nullSkImage();
		}

		@Override
		public String getCaption() {
			return point.getCaption(ctx);
		}
	}

	public TilePointsProvider(@NonNull Context context, @NonNull DataTileManager<T> points,
	                          int baseOrder, boolean textVisible,
	                          @Nullable TextRasterizer.Style textStyle, float textScale, float density,
	                          int minZoom, int maxZoom) {
		this.ctx = context;
		this.points = points;
		this.baseOrder = baseOrder;
		this.textVisible = textVisible;
		this.textStyle = textStyle != null ? textStyle : new TextRasterizer.Style();
		this.textScale = textScale;
		this.density = density;
		this.minZoom = minZoom;
		this.maxZoom = maxZoom;
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
		OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
		if (!app.getOsmandMap().getMapView().hasMapRenderer()) {
			return new QListMapTiledCollectionPoint();
		}
		RotatedTileBox tb = app.getOsmandMap().getMapView().getRotatedTileBox();
		QuadRect latLonBounds = tb.getLatLonBounds();
		List<T> tilePoints = points.getObjects(latLonBounds.top, latLonBounds.left, latLonBounds.bottom, latLonBounds.right);
		if (tilePoints.isEmpty()) {
			return new QListMapTiledCollectionPoint();
		}
		QListMapTiledCollectionPoint res = new QListMapTiledCollectionPoint();
		for (T point : tilePoints) {
			CollectionPoint collectionPoint = new CollectionPoint(ctx, point, textScale, density);
			res.add(collectionPoint.instantiateProxy(true));
			collectionPoint.swigReleaseOwnership();
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
		return ZoomLevel.swigToEnum(maxZoom);
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
}
package net.osmand.plus.views.layers.core;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import net.osmand.data.DataTileManager;
import net.osmand.plus.OsmandApplication;
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
	private final IconPixelsCache<Bitmap> iconsCache = new IconPixelsCache<>();
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
		private final IconPixelsCache<Bitmap> iconsCache;
		private final PointI point31;

		public CollectionPoint(@NonNull Context ctx, @NonNull ICollectionPoint point, float textScale, float density,
		                       @NonNull IconPixelsCache<Bitmap> iconsCache) {
			this.ctx = ctx;
			this.point = point;
			this.textScale = textScale;
			this.density = density;
			this.iconsCache = iconsCache;
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
			// points share their bitmaps
			return bitmap != null ? iconsCache.getImage(bitmap, b -> b) : SwigUtilities.nullSkImage();
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
			providerInstance = instantiateProxy(true);
			swigReleaseOwnership();
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
		// The core asks for every tile on the screen and uses only the points of the tile enlarged by a quarter
		// of its size on each side (MapTiledCollectionProvider::obtainTiledSymbols), so take just those
		AreaI tileBBox31 = Utilities.tileBoundingBox31(tileId, zoom);
		long left = tileBBox31.getTopLeft().getX();
		long top = tileBBox31.getTopLeft().getY();
		long right = tileBBox31.getBottomRight().getX();
		long bottom = tileBBox31.getBottomRight().getY();
		long marginX = (right - left) / 16 * 4;
		long marginY = (bottom - top) / 16 * 4;
		left = Math.max(0, left - marginX);
		top = Math.max(0, top - marginY);
		right = Math.min(Integer.MAX_VALUE, right + marginX);
		bottom = Math.min(Integer.MAX_VALUE, bottom + marginY);
		List<T> tilePoints = points.getObjects((int) left, (int) top, (int) right, (int) bottom);
		if (tilePoints.isEmpty()) {
			return new QListMapTiledCollectionPoint();
		}
		QListMapTiledCollectionPoint res = new QListMapTiledCollectionPoint();
		for (T point : tilePoints) {
			int x31 = MapUtils.get31TileNumberX(point.getLongitude());
			int y31 = MapUtils.get31TileNumberY(point.getLatitude());
			if (x31 >= left && x31 <= right && y31 >= top && y31 <= bottom) {
				CollectionPoint collectionPoint = new CollectionPoint(ctx, point, textScale, density, iconsCache);
				res.add(collectionPoint.instantiateProxy(true));
				collectionPoint.swigReleaseOwnership();
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

	@Override
	public boolean waitForLoading() {
		return true;
	}
}
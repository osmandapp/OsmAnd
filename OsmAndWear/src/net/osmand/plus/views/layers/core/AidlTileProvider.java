package net.osmand.plus.views.layers.core;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;

import androidx.annotation.NonNull;

import net.osmand.aidl.AidlMapPointWrapper;
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
import net.osmand.data.LatLon;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.layers.AidlMapLayer;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public class AidlTileProvider extends interface_MapTiledCollectionProvider {

	private final QListPointI points31 = new QListPointI();
	private final List<MapLayerData> mapLayerDataList = new ArrayList<>();

	private final int baseOrder;
	private final boolean textVisible;
	private final TextRasterizer.Style textStyle;
	private final float density;
	private final PointI offset;
	private final PointI bigIconOffset;
	private MapTiledCollectionProvider providerInstance;

	private final AidlMapLayer.PointsType pointsType;
	private final Paint bitmapPaint;
	private final Paint pointInnerCircle;
	private final Paint pointOuterCircle;
	private final int radius;
	private final int smallIconSize;
	private final int bigIconSize;
	private final Bitmap circle;
	private final Bitmap smallIconBg;
	private final Bitmap bigIconBg;
	private final Bitmap bigIconBgStale;


	public AidlTileProvider(AidlMapLayer aidlMapLayer, float density, float yOffset) {
		this.baseOrder = aidlMapLayer.getBaseOrder();
		this.textVisible = true;
		this.textStyle = aidlMapLayer.getTextStyle();
		this.density = density;
		this.pointsType = aidlMapLayer.getPointsType();
		this.bitmapPaint = aidlMapLayer.getBitmapPaint();
		this.pointInnerCircle = aidlMapLayer.getPointInnerCircle();
		this.pointOuterCircle = aidlMapLayer.getPointOuterCircle();
		this.radius = aidlMapLayer.getRadius();
		this.circle = aidlMapLayer.getCircle();
		this.smallIconSize = aidlMapLayer.getSmallIconSize();
		this.bigIconSize = aidlMapLayer.getBigIconSize();
		this.bigIconBg = aidlMapLayer.getBigIconBg();
		this.bigIconBgStale = aidlMapLayer.getBigIconBgStale();
		this.smallIconBg = aidlMapLayer.getSmallIconBg();
		this.offset = new PointI(0, 0);
		this.bigIconOffset = new PointI(0, (int) yOffset);
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
		if (pointsType == AidlMapLayer.PointsType.BIG_ICON) {
			return bigIconOffset.getY();
		}
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
		Bitmap bitmapResult = null;
		if (!isFullSize || pointsType == AidlMapLayer.PointsType.CIRCLE) {
			bitmapPaint.setColorFilter(new PorterDuffColorFilter(data.color, PorterDuff.Mode.MULTIPLY));
			bitmapResult = Bitmap.createBitmap(circle.getWidth(), circle.getHeight(), Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmapResult);
			canvas.drawBitmap(circle, 0, 0, bitmapPaint);
		} else if (pointsType == AidlMapLayer.PointsType.STANDARD) {
			pointInnerCircle.setColor(data.color);
			int width = (int) (2 * radius + density);
			int height = width;
			int cx = width / 2;
			int cy = height / 2;
			bitmapResult = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmapResult);
			canvas.drawCircle(cx, cy, radius + density, pointOuterCircle);
			canvas.drawCircle(cx, cy, radius - density, pointInnerCircle);
		} else if (pointsType == AidlMapLayer.PointsType.SMALL_ICON) {
			bitmapResult = Bitmap.createBitmap(smallIconBg.getWidth(), smallIconBg.getHeight(), Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmapResult);
			bitmapPaint.setColorFilter(new PorterDuffColorFilter(data.color, PorterDuff.Mode.MULTIPLY));
			int cx = smallIconBg.getWidth() / 2;
			int cy = smallIconBg.getHeight() / 2;
			canvas.drawBitmap(smallIconBg, 0, 0, bitmapPaint);
			bitmapPaint.setColorFilter(null);
			canvas.drawBitmap(data.image, null, getDstRect(cx, cy, smallIconSize / 2), bitmapPaint);
		} else if (pointsType == AidlMapLayer.PointsType.BIG_ICON) {
			Bitmap bg = data.isStale ? bigIconBgStale : bigIconBg;
			bitmapResult = Bitmap.createBitmap(bg.getWidth(), bg.getHeight(), Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmapResult);
			bitmapPaint.setColorFilter(null);
			int cx = bg.getWidth() / 2;
			int cy = bg.getHeight() / 2;
			canvas.drawBitmap(bg, 0, 0, bitmapPaint);
			canvas.drawBitmap(data.image, null, getDstRect(cx, cy, bigIconSize / 2), bitmapPaint);
		}
		return bitmapResult != null ? NativeUtilities.createSkImageFromBitmap(bitmapResult) : SwigUtilities.nullSkImage();
	}

	@Override
	public String getCaption(int index) {
		MapLayerData data = index < mapLayerDataList.size() ? mapLayerDataList.get(index) : null;
		return data != null ? data.caption : "";
	}

	@Override
	public QListMapTiledCollectionPoint getTilePoints(TileId tileId, ZoomLevel zoom) {
		return new QListMapTiledCollectionPoint();
	}

	@Override
	public ZoomLevel getMinZoom() {
		return ZoomLevel.ZoomLevel0;
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
		if (pointsType == AidlMapLayer.PointsType.BIG_ICON) {
			return MapMarker.PinIconVerticalAlignment.Top;
		}
		return MapMarker.PinIconVerticalAlignment.CenterVertical;
	}

	@Override
	public MapMarker.PinIconHorisontalAlignment getPinIconHorisontalAlignment() {
		return MapMarker.PinIconHorisontalAlignment.CenterHorizontal;
	}

	@Override
	public PointI getPinIconOffset() {
		if (pointsType == AidlMapLayer.PointsType.BIG_ICON) {
			return bigIconOffset;
		}
		return offset;
	}

	public void addToData(@NonNull AidlMapPointWrapper mapPoint, Bitmap image, boolean isStale, String caption) throws IllegalStateException {
		if (providerInstance != null) {
			throw new IllegalStateException("Provider already instantiated. Data cannot be modified at this stage.");
		}

		LatLon latLon = mapPoint.getLocation();
		int x31 = MapUtils.get31TileNumberX(latLon.getLongitude());
		int y31 = MapUtils.get31TileNumberY(latLon.getLatitude());
		points31.add(new PointI(x31, y31));
		mapLayerDataList.add(new MapLayerData(mapPoint, image, isStale, caption));
	}

	private Rect getDstRect(int centerX, int centerY, int offset) {
		Rect rect = new Rect();
		rect.left = centerX - offset;
		rect.top = centerY - offset;
		rect.right = centerX + offset;
		rect.bottom = centerY + offset;
		return rect;
	}

	private static class MapLayerData {
		boolean isStale;
		int color;
		String caption;
		Bitmap image;

		MapLayerData(@NonNull AidlMapPointWrapper mapPoint, @NonNull Bitmap image, boolean isStale, String caption) {
			this.isStale = isStale;
			this.color = mapPoint.getColor();
			this.caption = caption;
			this.image = image;
		}
	}
}

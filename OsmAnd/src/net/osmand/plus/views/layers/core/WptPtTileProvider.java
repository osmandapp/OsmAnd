package net.osmand.plus.views.layers.core;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXUtilities.WptPt;
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
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.PointImageDrawable;
import net.osmand.plus.views.PointImageUtils;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WptPtTileProvider extends interface_MapTiledCollectionProvider {

   private final Context ctx;
   private final int baseOrder;
   private final boolean textVisible;
   private final TextRasterizer.Style textStyle;
   private final float density;

   private final QListPointI points31 = new QListPointI();
   private final List<MapLayerData> mapLayerDataList = new ArrayList<>();
   private final Map<Integer, Bitmap> bigBitmapCache = new ConcurrentHashMap<>();
   private final Map<Integer, Bitmap> smallBitmapCache = new ConcurrentHashMap<>();

   private MapTiledCollectionProvider providerInstance;
   private final PointI offset;

   public WptPtTileProvider(@NonNull Context context, int baseOrder, boolean textVisible,
                            @Nullable TextRasterizer.Style textStyle, float density) {
      this.ctx = context;
      this.baseOrder = baseOrder;
      this.textVisible = textVisible;
      this.textStyle = textStyle != null ? textStyle : new TextRasterizer.Style();
      this.density = density;
      offset = new PointI(0,0);
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
      MapLayerData data = index < mapLayerDataList.size()
              ? mapLayerDataList.get(index) : null;
      if (data == null) {
         return SwigUtilities.nullSkImage();
      }
      Bitmap bitmap;
      if (isFullSize) {
         int bigBitmapKey = data.getKey();
         bitmap = bigBitmapCache.get(bigBitmapKey);
         if (bitmap == null) {
            PointImageDrawable pointImageDrawable;
            if (data.hasMarker) {
               pointImageDrawable = PointImageUtils.getOrCreateSyncedIcon(ctx, data.color, data.wptPt);
            } else {
               pointImageDrawable = PointImageUtils.getFromPoint(ctx, data.color, data.withShadow, data.wptPt);
            }
            bitmap = pointImageDrawable.getBigMergedBitmap(data.textScale, data.history);
            bigBitmapCache.put(bigBitmapKey, bitmap);
         }
      } else {
         int smallBitmapKey = data.getKey();
         bitmap = smallBitmapCache.get(smallBitmapKey);
         if (bitmap == null) {
            PointImageDrawable pointImageDrawable = PointImageUtils.getFromPoint(ctx, data.color,
                    data.withShadow, data.wptPt);
            bitmap = pointImageDrawable.getSmallMergedBitmap(data.textScale);
            smallBitmapCache.put(smallBitmapKey, bitmap);
         }
      }
      return bitmap != null ? NativeUtilities.createSkImageFromBitmap(bitmap) : SwigUtilities.nullSkImage();
   }

   @Override
   public String getCaption(int index) {
      MapLayerData data = index < mapLayerDataList.size() ? mapLayerDataList.get(index) : null;
      return data != null ? data.wptPt.name : "";
   }

   @Override
   public QListMapTiledCollectionPoint getTilePoints(TileId tileId, ZoomLevel zoom) {
      return new QListMapTiledCollectionPoint();
   }

   @Override
   public ZoomLevel getMinZoom() {
      return ZoomLevel.ZoomLevel7;
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

   public void addToData(@NonNull WptPt wptPt, int color, boolean withShadow,
                         boolean hasMarker, boolean history, float textScale) throws IllegalStateException {
      if (providerInstance != null) {
         throw new IllegalStateException("Provider already instantiated. Data cannot be modified at this stage.");
      }

      int x31 = MapUtils.get31TileNumberX(wptPt.getLongitude());
      int y31 = MapUtils.get31TileNumberY(wptPt.getLatitude());
      points31.add(new PointI(x31, y31));
      mapLayerDataList.add(new MapLayerData(wptPt, color, withShadow,
              hasMarker, history, textScale));
   }

   private static class MapLayerData {
      WptPt wptPt;
      int color;
      boolean withShadow;
      boolean hasMarker;
      boolean history;
      float textScale;

      MapLayerData(@NonNull WptPt wptPt, int color, boolean withShadow,
                   boolean hasMarker, boolean history, float textScale) {
         this.wptPt = wptPt;
         this.color = color;
         this.withShadow = withShadow;
         this.hasMarker = hasMarker;
         this.history = history;
         this.textScale = textScale;
      }

      int getKey() {
         long hash = ((long) color << 6) + ((long) wptPt.hashCode() << 4) + ((withShadow ? 1 : 0) << 3)
                 + ((hasMarker ? 1 : 0) << 2) + (int) (textScale * 10) + (history ? 1 : 0);
         if (hash >= Integer.MAX_VALUE || hash <= Integer.MIN_VALUE) {
            return (int) (hash >> 4);
         }
         return (int) hash;
      }
   }
}
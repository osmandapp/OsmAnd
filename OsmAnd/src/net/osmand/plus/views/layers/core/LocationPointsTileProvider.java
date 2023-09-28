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
import net.osmand.core.jni.TextRasterizer;
import net.osmand.core.jni.TileId;
import net.osmand.core.jni.Utilities;
import net.osmand.core.jni.ZoomLevel;
import net.osmand.core.jni.interface_MapTiledCollectionProvider;
import net.osmand.data.LatLon;
import net.osmand.plus.utils.NativeUtilities;

import java.util.List;

public class LocationPointsTileProvider extends interface_MapTiledCollectionProvider {

   private final QListPointI points31 = new QListPointI();
   private final Bitmap skPointBitmap;
   private final int baseOrder;
   private final PointI offset;
   private MapTiledCollectionProvider providerInstance;

   public LocationPointsTileProvider(int baseOrder, @NonNull List<LatLon> points31,
                                     @NonNull Bitmap pointBitmap) {
      this.baseOrder = baseOrder;
      for (LatLon latLon : points31) {
         int x31 = Utilities.get31TileNumberX(latLon.getLongitude());
         int y31 = Utilities.get31TileNumberY(latLon.getLatitude());
         this.points31.add(new PointI(x31, y31));
      }
      skPointBitmap = pointBitmap;
      this.offset = new PointI(0, 0);
   }

   public void drawPoints(@NonNull MapRendererView mapRenderer) {
      if (providerInstance == null) {
         providerInstance = instantiateProxy();
      }
      mapRenderer.addSymbolsProvider(providerInstance);
   }

   public void clearPoints(@NonNull MapRendererView mapRenderer) {
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
   public SingleSkImage getImageBitmap(int index, boolean isFullSize) {
      return NativeUtilities.createSkImageFromBitmap(skPointBitmap);
   }

   @Override
   public String getCaption(int index) {
      return "";
   }

   @Override
   public QListMapTiledCollectionPoint getTilePoints(TileId tileId, ZoomLevel zoom) {
      return new QListMapTiledCollectionPoint();
   }

   @Override
   public ZoomLevel getMinZoom() {
      return ZoomLevel.MinZoomLevel;
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
}

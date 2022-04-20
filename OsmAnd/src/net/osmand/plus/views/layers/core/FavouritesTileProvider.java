package net.osmand.plus.views.layers.core;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QListPointI;
import net.osmand.core.jni.SWIGTYPE_p_sk_spT_SkImage_const_t;
import net.osmand.core.jni.TextRasterizer;
import net.osmand.core.jni.ZoomLevel;
import net.osmand.core.jni.interface_MapTiledCollectionProvider;
import net.osmand.data.BackgroundType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.PointImageDrawable;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FavouritesTileProvider extends interface_MapTiledCollectionProvider {

    private List<FavouritesMapLayerData> favouritesMapLayerDataList = new ArrayList<>();
    HashMap<String, Bitmap> bigBitmapCache = new HashMap<>();
    HashMap<String, Bitmap> smallBitmapCache = new HashMap<>();
    private int baseOrder;

    public FavouritesTileProvider(int baseOrder) {
        this.baseOrder = baseOrder;
    }

    @Override
    public int getBaseOrder() {
        return baseOrder;
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
    public PointI getPoint31(int index) {
        return favouritesMapLayerDataList.get(index).point;
    }

    @Override
    public int getPointsCount() {
        return favouritesMapLayerDataList.size();
    }

    @Override
    public SWIGTYPE_p_sk_spT_SkImage_const_t getImageBitmap(int index, boolean isFullSize) {
        FavouritesMapLayerData data = favouritesMapLayerDataList.get(index);
        if (isFullSize) {
            if (bigBitmapCache.containsKey(data.bitmapKey)) {
                Bitmap bigBitmap = bigBitmapCache.get(data.bitmapKey);
                return NativeUtilities.createSkImageFromBitmap(bigBitmap);
            }
        } else {
            if (smallBitmapCache.containsKey(data.bitmapKey)) {
                Bitmap smallBitmap = smallBitmapCache.get(data.bitmapKey);
                return NativeUtilities.createSkImageFromBitmap(smallBitmap);
            }
        }
        Bitmap emptyBitmap = Bitmap.createBitmap(0, 0, Bitmap.Config.ARGB_8888);
        return NativeUtilities.createSkImageFromBitmap(emptyBitmap);
    }

    @Override
    public String getCaption(int index) {
        return "";
    }

    @Override
    public ZoomLevel getMinZoom() {
        return ZoomLevel.ZoomLevel6;
    }

    @Override
    public ZoomLevel getMaxZoom() {
        return ZoomLevel.MaxZoomLevel;
    }

    public void clearData() {
        favouritesMapLayerDataList.clear();
        bigBitmapCache.clear();
        smallBitmapCache.clear();
    }

    public void addToData(Context context, int colorSmallPoint, int colorBigPoint, boolean withShadow,
                          int overlayIconId, BackgroundType backgroundType, boolean hasMarker, float textScale, double lat, double lon) {
        favouritesMapLayerDataList.add(new FavouritesMapLayerData(context, colorSmallPoint, colorBigPoint, withShadow,
        overlayIconId, backgroundType, hasMarker, textScale, lat, lon));
    }

    class FavouritesMapLayerData {
        String bitmapKey;
        PointI point;

        FavouritesMapLayerData(Context context, int colorSmallPoint, int colorBigPoint, boolean withShadow,
                               int overlayIconId, BackgroundType backgroundType, boolean hasMarker, float textScale, double lat, double lon) {
            bitmapKey = "" + colorBigPoint + colorSmallPoint + withShadow + overlayIconId + backgroundType.name() + hasMarker + textScale;
            if (!bigBitmapCache.containsKey(bitmapKey)) {
                PointImageDrawable pointImageDrawable;
                if (hasMarker) {
                    pointImageDrawable = PointImageDrawable.getOrCreate(context, colorBigPoint, withShadow, true, overlayIconId, backgroundType);
                } else {
                    pointImageDrawable = PointImageDrawable.getOrCreate(context, colorBigPoint, withShadow, false, overlayIconId, backgroundType);
                }
                Bitmap bigBitmap = pointImageDrawable.getBigMergedBitmap(textScale);
                bigBitmapCache.put(bitmapKey, bigBitmap);
            }
            if (!smallBitmapCache.containsKey(bitmapKey)) {
                PointImageDrawable pointImageDrawable = PointImageDrawable.getOrCreate(context, colorSmallPoint, withShadow, false, overlayIconId, backgroundType);
                Bitmap smallBitmap = pointImageDrawable.getSmallMergedBitmap(textScale);
                smallBitmapCache.put(bitmapKey, smallBitmap);
            }
            int x = MapUtils.get31TileNumberX(lon);
            int y = MapUtils.get31TileNumberY(lat);
            point = new PointI(x, y);
        }
    }
}

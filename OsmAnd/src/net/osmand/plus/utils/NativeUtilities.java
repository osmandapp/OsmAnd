package net.osmand.plus.utils;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.util.Log;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.ColorARGB;
import net.osmand.core.jni.FColorARGB;
import net.osmand.core.jni.FColorRGB;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.SWIGTYPE_p_sk_spT_SkImage_const_t;
import net.osmand.core.jni.SwigUtilities;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.util.MapUtils;

public class NativeUtilities {
	public static SWIGTYPE_p_sk_spT_SkImage_const_t createSkImageFromBitmap(@NonNull Bitmap inputBmp) {
		return SwigUtilities.createSkImageARGB888With(
				inputBmp.getWidth(), inputBmp.getHeight(), AndroidUtils.getByteArrayFromBitmap(inputBmp));
	}

	public static FColorRGB createFColorRGB(@ColorInt int color) {
		return new FColorRGB((color >> 16 & 0xff) / 255.0f,
				((color >> 8) & 0xff) / 255.0f,
				((color) & 0xff) / 255.0f);
	}

	public static FColorARGB createFColorARGB(@ColorInt int color) {
		float a = (color >> 24) & 0xFF;
		float r = (color >> 16) & 0xFF;
		float g = (color >> 8) & 0xFF;
		float b = (color) & 0xFF;
		return new FColorARGB(a / 255, r / 255, g / 255, b / 255);
	}

	public static ColorARGB createColorARGB(@ColorInt int color) {
		int a = (color >> 24) & 0xFF;
		int r = (color >> 16) & 0xFF;
		int g = (color >> 8) & 0xFF;
		int b = (color) & 0xFF;
		return new ColorARGB((short)a, (short)r , (short)g, (short)b);
	}

	public static ColorARGB createColorARGB(@ColorInt int color, int alpha) {
		int r = (color >> 16) & 0xFF;
		int g = (color >> 8) & 0xFF;
		int b = (color) & 0xFF;
		return new ColorARGB((short)alpha, (short)r , (short)g, (short)b);
	}

	@Nullable
	public static PointI get31FromPixel(@NonNull MapRendererView mapRenderer, @Nullable RotatedTileBox tileBox,
	                                    int x, int y) {
		return get31FromPixel(mapRenderer, tileBox, new PointI(x, y), false);
	}

	@Nullable
	public static PointI get31FromPixel(@NonNull MapRendererView mapRenderer, @Nullable RotatedTileBox tileBox,
	                                    int x, int y, boolean useShiftedCenter) {
		return get31FromPixel(mapRenderer, tileBox, new PointI(x, y), useShiftedCenter);
	}

	@Nullable
	public static PointI get31FromPixel(@NonNull MapRendererView mapRenderer, @Nullable RotatedTileBox tileBox,
	                                    @NonNull PointI screenPoint, boolean useShiftedCenter) {
		if (useShiftedCenter && tileBox != null && tileBox.isCenterShifted()) {
			RotatedTileBox tbCenter = tileBox.copy();
			tbCenter.setCenterLocation(0.5f, 0.5f);
			int x = screenPoint.getX() + (tileBox.getCenterPixelX() - tbCenter.getCenterPixelX());
			int y = screenPoint.getY() + (tileBox.getCenterPixelY() - tbCenter.getCenterPixelY());
			screenPoint = new PointI(x, y);
		}
		PointI point31 = new PointI();
		if (mapRenderer.getLocationFromScreenPoint(screenPoint, point31)) {
			return point31;
		}
		return null;
	}

	@Nullable
	public static LatLon getLatLonFromPixel(@Nullable MapRendererView mapRenderer, @Nullable RotatedTileBox tileBox,
	                                        int x, int y) {
		if (mapRenderer == null) {
			return tileBox != null ? tileBox.getLatLonFromPixel(x, y) : null;
		}
		return getLatLonFromPixel(mapRenderer, tileBox, new PointI(x, y));
	}

	@Nullable
	public static LatLon getLatLonFromPixel(@NonNull MapRendererView mapRenderer, @Nullable RotatedTileBox tileBox,
	                                        @NonNull PointI screenPoint) {
		PointI point31 = get31FromPixel(mapRenderer, tileBox, screenPoint, false);
		if (point31 != null) {
			return new LatLon(MapUtils.get31LatitudeY(point31.getY()), MapUtils.get31LongitudeX(point31.getX()));
		}
		return null;
	}

	@NonNull
	public static LatLon getLatLonFromPixel(@Nullable MapRendererView mapRenderer, @NonNull RotatedTileBox tileBox,
	                                        float x, float y) {
		LatLon latLon = mapRenderer != null
				? getLatLonFromPixel(mapRenderer, tileBox, new PointI((int) x, (int) y)) : null;
		if (latLon == null) {
			latLon = tileBox.getLatLonFromPixel(x, y);
		}
		return latLon;
	}

	@NonNull
	public static PointF getPixelFromLatLon(@Nullable MapRendererView mapRenderer, @NonNull RotatedTileBox tileBox,
	                                        double lat, double lon) {
		PointF point = null;
		if (mapRenderer != null) {
			int x31 = MapUtils.get31TileNumberX(lon);
			int y31 = MapUtils.get31TileNumberY(lat);
			PointI screenPoint = new PointI();
			if (mapRenderer.getScreenPointFromLocation(new PointI(x31, y31), screenPoint, true)) {
				point = new PointF(screenPoint.getX(), screenPoint.getY());
			}
		}
		if (point == null) {
			point = new PointF(tileBox.getPixXFromLatLon(lat, lon), tileBox.getPixYFromLatLon(lat, lon));
		}
		return point;
	}

	@NonNull
	public static PointF getPixelFrom31(@Nullable MapRendererView mapRenderer, @NonNull RotatedTileBox tileBox,
	                                    @NonNull PointI point31) {
		int x31 = point31.getX();
		int y31 = point31.getY();
		PointF point = null;
		if (mapRenderer != null) {
			PointI screenPoint = new PointI();
			if (mapRenderer.getScreenPointFromLocation(new PointI(x31, y31), screenPoint, true)) {
				point = new PointF(screenPoint.getX(), screenPoint.getY());
			}
		}
		if (point == null) {
			point = new PointF(tileBox.getPixXFrom31(x31, y31), tileBox.getPixYFrom31(x31, y31));
		}
		return point;
	}

	@NonNull
	public static PointI calculateTarget31(@NonNull MapRendererView mapRenderer,
	                                       double latitude, double longitude, boolean applyNewTarget) {
		PointI target31 = new PointI(MapUtils.get31TileNumberX(longitude), MapUtils.get31TileNumberY(latitude));
		if (applyNewTarget) {
			mapRenderer.setTarget(target31);
		}
		return target31;
	}

	public static boolean containsLatLon(@Nullable MapRendererView mapRenderer, @NonNull RotatedTileBox tileBox,
	                                     @NonNull LatLon latLon) {
		return containsLatLon(mapRenderer, tileBox, latLon.getLatitude(), latLon.getLongitude());
	}

	public static boolean containsLatLon(@Nullable MapRendererView mapRenderer, @NonNull RotatedTileBox tileBox,
	                                     double latitude, double longitude) {
		if (mapRenderer != null) {
			return mapRenderer.isPositionVisible(new PointI(MapUtils.get31TileNumberX(longitude),
					MapUtils.get31TileNumberY(latitude)));
		} else {
			return tileBox.containsLatLon(latitude, longitude);
		}
	}

	public static PointI getPoint31FromLatLon(double lat, double lon) {
		int x31 = MapUtils.get31TileNumberX(lon);
		int y31 = MapUtils.get31TileNumberY(lat);
		return new PointI(x31, y31);
	}
}

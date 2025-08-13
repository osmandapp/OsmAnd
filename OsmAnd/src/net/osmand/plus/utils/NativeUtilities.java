package net.osmand.plus.utils;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.PointF;
import android.util.Pair;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.OnResultCallback;
import net.osmand.core.android.MapRendererContext;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.*;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.plugins.weather.OfflineForecastHelper;
import net.osmand.plus.utils.HeightsResolverTask.HeightsResolverCallback;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.util.MapAlgorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NativeUtilities {

	public static final int MIN_ALTITUDE_VALUE = -20_000;

	public static SingleSkImage createSkImageFromBitmap(@NonNull Bitmap inputBmp) {
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
		return new ColorARGB((short) a, (short) r, (short) g, (short) b);
	}

	public static ColorARGB createColorARGB(@ColorInt int color, int alpha) {
		int r = (color >> 16) & 0xFF;
		int g = (color >> 8) & 0xFF;
		int b = (color) & 0xFF;
		return new ColorARGB((short) alpha, (short) r, (short) g, (short) b);
	}

	public static boolean isSegmentCrossingPolygon(@NonNull PointI start31,
	                                               @NonNull PointI end31,
	                                               @NonNull List<PointI> polygon31) {
		for (int i = 1; i < polygon31.size(); i++) {
			PointI polygonLineStart31 = polygon31.get(i - 1);
			PointI polygonLineEnd31 = polygon31.get(i);
			if (areSegmentsCrossing(polygonLineStart31, polygonLineEnd31, start31, end31)) {
				return true;
			}
		}

		return false;
	}

	// Detect by checking that start and end of ab/cd segment lie on different sides of cd/ab segment
	// https://e-maxx.ru/algo/segments_intersection_checking
	public static boolean areSegmentsCrossing(@NonNull PointI a31, @NonNull PointI b31,
	                                          @NonNull PointI c31, @NonNull PointI d31) {
		return checkSegmentsProjectionsIntersect(a31.getX(), b31.getX(), c31.getX(), d31.getX())
				&& checkSegmentsProjectionsIntersect(a31.getY(), b31.getY(), c31.getY(), d31.getY())
				&& getSignedArea31(a31, b31, c31) * getSignedArea31(a31, b31, d31) <= 0
				&& getSignedArea31(c31, d31, a31) * getSignedArea31(c31, d31, b31) <= 0;
	}

	private static boolean checkSegmentsProjectionsIntersect(int a, int b, int c, int d) {
		if (a > b) {
			int t = a;
			a = b;
			b = t;
		}
		if (c > d) {
			int t = c;
			c = d;
			d = t;
		}
		return Math.max(a, c) <= Math.min(b, d);
	}

	public static long getSignedArea31(@NonNull PointI a31, @NonNull PointI b31, @NonNull PointI c31) {
		return (long) (b31.getX() - a31.getX()) * (c31.getY() - a31.getY())
				- (long) (b31.getY() - a31.getY()) * (c31.getX() - a31.getX());
	}

	public static boolean isPointInsidePolygon(@NonNull LatLon latLon, @NonNull List<PointI> polygon31) {
		return isPointInsidePolygon(getPoint31FromLatLon(latLon), polygon31);
	}

	public static boolean isPointInsidePolygon(double lat, double lon, @NonNull List<PointI> polygon31) {
		return isPointInsidePolygon(getPoint31FromLatLon(lat, lon), polygon31);
	}

	public static boolean isPointInsidePolygon(@NonNull PointI point31, @NonNull List<PointI> polygon31) {
		int intersections = 0;
		for (int i = 1; i < polygon31.size(); i++) {
			PointI previousPoint = polygon31.get(i - 1);
			PointI currentPoint = polygon31.get(i);
			int intersectedX = MapAlgorithms.ray_intersect_x(previousPoint.getX(),
					previousPoint.getY(),
					currentPoint.getX(),
					currentPoint.getY(),
					point31.getY());
			if (Integer.MIN_VALUE != intersectedX && point31.getX() >= intersectedX) {
				intersections++;
			}
		}
		return intersections % 2 == 1;
	}

	@Nullable
	public static List<PointI> getPolygon31FromPixelAndRadius(@NonNull MapRendererView mapRenderer,
	                                                          @NonNull PointF pixel,
	                                                          float radius) {
		QuadRect pixelArea = new QuadRect(
				pixel.x - radius,
				pixel.y - radius,
				pixel.x + radius,
				pixel.y + radius
		);
		return getPolygon31FromScreenArea(mapRenderer, pixelArea);
	}

	@Nullable
	public static List<PointI> getPolygon31FromScreenArea(@NonNull MapRendererView mapRenderer,
	                                                      @NonNull QuadRect screenArea) {
		float leftPix = (float) screenArea.left;
		float topPix = (float) screenArea.top;
		float rightPix = (float) screenArea.right;
		float bottomPix = (float) screenArea.bottom;

		List<PointI> polygon31 = new ArrayList<>();
		polygon31.add(get31FromElevatedPixel(mapRenderer, leftPix, topPix));
		polygon31.add(get31FromElevatedPixel(mapRenderer, rightPix, topPix));
		polygon31.add(get31FromElevatedPixel(mapRenderer, rightPix, bottomPix));
		polygon31.add(get31FromElevatedPixel(mapRenderer, leftPix, bottomPix));
		polygon31.add(polygon31.get(0));

		for (PointI point31 : polygon31) {
			if (point31 == null) {
				return null;
			}
		}

		return polygon31;
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
	public static PointI get31FromElevatedPixel(@NonNull MapRendererView mapRenderer, int x, int y) {
		PointI point31 = new PointI();
		return mapRenderer.getLocationFromElevatedPoint(new PointI(x, y), point31)
				? point31
				: null;
	}

	@Nullable
	public static PointI get31FromElevatedPixel(@NonNull MapRendererView mapRenderer, float x, float y) {
		return get31FromElevatedPixel(mapRenderer, (int) x, (int) y);
	}

	@NonNull
	public static LatLon getLatLonFromElevatedPixel(@Nullable MapRendererView mapRenderer,
	                                                @NonNull RotatedTileBox tileBox,
	                                                @NonNull PointF pixel) {
		return getLatLonFromElevatedPixel(mapRenderer, tileBox, pixel.x, pixel.y);
	}

	@NonNull
	public static LatLon getLatLonFromElevatedPixel(@Nullable MapRendererView mapRenderer,
	                                                @NonNull RotatedTileBox tileBox,
	                                                float x, float y) {
		return getLatLonFromElevatedPixel(mapRenderer, tileBox, (int) x, (int) y);
	}

	@NonNull
	public static LatLon getLatLonFromElevatedPixel(@Nullable MapRendererView mapRenderer,
	                                                @NonNull RotatedTileBox tileBox,
	                                                int x, int y) {
		PointI point31 = null;
		if (mapRenderer != null) {
			point31 = get31FromElevatedPixel(mapRenderer, x, y);
		}

		if (point31 == null) {
			return tileBox.getLatLonFromPixel(x, y);
		}

		double lat = MapUtils.get31LatitudeY(point31.getY());
		double lon = MapUtils.get31LongitudeX(point31.getX());
		return new LatLon(lat, lon);
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

	public static void getAltitudeForLatLon(@Nullable MapRendererView mapRenderer, @Nullable LatLon latLon,
	                                        @NonNull OnResultCallback<Double> callback) {
		if (latLon != null) {
			Double altitude = getAltitudeForLatLon(mapRenderer, latLon);
			if (altitude != null) {
				callback.onResult(altitude);
			} else {
				HeightsResolverCallback heightsCallback = heights -> callback.onResult(heights != null && heights.length > 0 ? (double) heights[0] : null);
				HeightsResolverTask task = new HeightsResolverTask(Collections.singletonList(latLon), heightsCallback);
				OsmAndTaskManager.executeTask(task);
			}
		} else {
			callback.onResult(null);
		}
	}

	public static Double getAltitudeForLatLon(@Nullable MapRendererView mapRenderer, @Nullable LatLon latLon) {
		if (latLon != null) {
			return getAltitudeForLatLon(mapRenderer, latLon.getLatitude(), latLon.getLongitude());
		}
		return null;
	}

	public static Double getAltitudeForLatLon(@Nullable MapRendererView mapRenderer, double lat, double lon) {
		int x = MapUtils.get31TileNumberX(lon);
		int y = MapUtils.get31TileNumberY(lat);
		return getAltitudeForElevatedPoint(mapRenderer, new PointI(x, y));
	}

	public static Double getAltitudeForPixelPoint(@Nullable MapRendererView mapRenderer, @Nullable PointI screenPoint) {
		if (mapRenderer != null && screenPoint != null) {
			PointI elevatedPoint = new PointI();
			if (mapRenderer.getLocationFromElevatedPoint(screenPoint, elevatedPoint)) {
				return getAltitudeForElevatedPoint(mapRenderer, elevatedPoint);
			}
		}
		return null;
	}

	public static Double getAltitudeForElevatedPoint(@Nullable MapRendererView mapRenderer, @Nullable PointI elevatedPoint) {
		double altitude = MIN_ALTITUDE_VALUE;
		if (mapRenderer != null && elevatedPoint != null) {
			altitude = mapRenderer.getLocationHeightInMeters(elevatedPoint);
		}
		return altitude > MIN_ALTITUDE_VALUE ? altitude : null;
	}

	@NonNull
	public static PointF getPixelFromLatLon(@Nullable MapRendererView mapRenderer,
			@NonNull RotatedTileBox tileBox, @NonNull LatLon latLon) {
		return getPixelFromLatLon(mapRenderer, tileBox, latLon.getLatitude(), latLon.getLongitude());
	}

	@NonNull
	public static PointF getPixelFromLatLon(@Nullable MapRendererView mapRenderer,
			@NonNull RotatedTileBox tileBox, double lat, double lon) {
		PointI screenPoint = getScreenPointFromLatLon(mapRenderer, lat, lon);
		if (screenPoint != null) {
			return new PointF(screenPoint.getX(), screenPoint.getY());
		} else {
			return new PointF(tileBox.getPixXFromLatLon(lat, lon), tileBox.getPixYFromLatLon(lat, lon));
		}
	}

	@Nullable
	public static PointI getScreenPointFromLatLon(@Nullable MapRendererView mapRenderer, double lat, double lon) {
		if (mapRenderer != null) {
			int x31 = MapUtils.get31TileNumberX(lon);
			int y31 = MapUtils.get31TileNumberY(lat);
			PointI screenPoint = new PointI();
			if (mapRenderer.getScreenPointFromLocation(new PointI(x31, y31), screenPoint, true)) {
				return screenPoint;
			}
		}
		return null;
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
	public static PointF getElevatedPixelFromLatLon(@Nullable MapRendererView mapRenderer,
	                                                @NonNull RotatedTileBox tileBox,
	                                                @NonNull LatLon latLon) {
		return getElevatedPixelFromLatLon(mapRenderer, tileBox, latLon.getLatitude(), latLon.getLongitude());
	}

	@NonNull
	public static PointF getElevatedPixelFromLatLon(@Nullable MapRendererView mapRenderer,
	                                                @NonNull RotatedTileBox tileBox,
	                                                double lat, double lon) {
		int x31 = MapUtils.get31TileNumberX(lon);
		int y31 = MapUtils.get31TileNumberY(lat);
		return getElevatedPixelFrom31(mapRenderer, tileBox, x31, y31);
	}

	@NonNull
	public static PointF getElevatedPixelFrom31(@Nullable MapRendererView mapRenderer,
	                                            @NonNull RotatedTileBox tileBox,
	                                            int x31, int y31) {
		PointF pixel = null;

		if (mapRenderer != null) {
			PointI point31 = new PointI(x31, y31);
			PointI screenPoint = new PointI();
			if (mapRenderer.getElevatedPointFromLocation(point31, screenPoint, true)) {
				pixel = new PointF(screenPoint.getX(), screenPoint.getY());
			}
		}

		if (pixel == null) {
			float pixX = tileBox.getPixXFrom31(x31, y31);
			float pixY = tileBox.getPixYFrom31(x31, y31);
			pixel = new PointF(pixX, pixY);
		}

		return pixel;
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

	@NonNull
	public static PointI calculateNewTarget31(@NonNull PointI currentTarget31, @NonNull PointI offset31) {
		int deltaX = offset31.getX();
		int deltaY = offset31.getY();
		int nextTargetX = currentTarget31.getX();
		int nextTargetY = currentTarget31.getY();
		if (Integer.MAX_VALUE - nextTargetX < deltaX) {
			deltaX -= Integer.MAX_VALUE;
			deltaX--;
		}
		if (Integer.MAX_VALUE - nextTargetY < deltaY) {
			deltaY -= Integer.MAX_VALUE;
			deltaY--;
		}
		nextTargetX += deltaX;
		nextTargetY += deltaY;
		if (nextTargetX < 0) {
			nextTargetX += Integer.MAX_VALUE;
			nextTargetX++;
		}
		if (nextTargetY < 0) {
			nextTargetY += Integer.MAX_VALUE;
			nextTargetY++;
		}
		return new PointI(nextTargetX, nextTargetY);
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

	@NonNull
	public static PointI getPoint31FromLatLon(@NonNull LatLon latLon) {
		return getPoint31FromLatLon(latLon.getLatitude(), latLon.getLongitude());
	}

	@NonNull
	public static PointI getPoint31FromLatLon(double lat, double lon) {
		int x31 = MapUtils.get31TileNumberX(lon);
		int y31 = MapUtils.get31TileNumberY(lat);
		return new PointI(x31, y31);
	}

	public static float getLocationHeightOrZero(@NonNull MapRendererView mapRenderer, @NonNull PointI location31, @NonNull LatLon location, boolean readGeotiff) {
		float height = mapRenderer.getLocationHeightInMeters(location31);
		if (height > MIN_ALTITUDE_VALUE) {
			return height;
		} else if (readGeotiff) {
			MapRendererContext mapRendererContext = NativeCoreContext.getMapRendererContext();
			if (mapRendererContext != null) {
				List<LatLon> locations = new ArrayList<LatLon>();
				locations.add(location);
				float[] heights = mapRendererContext.calculateHeights(locations);
				if (heights != null && heights.length > 0)
					return heights[0];
			}
		}
		return 0.0f;
	}

	@Nullable
	public static Pair<PointF, PointF> clipLineInVisibleRect(@NonNull MapRendererView mapRenderer,
	                                                         @NonNull RotatedTileBox tileBox,
	                                                         @NonNull PointI start31,
	                                                         @NonNull PointI end31) {
		AreaI screenBbox = mapRenderer.getVisibleBBox31();
		PointI clippedStart31 = null;
		PointI clippedEnd31 = null;
		if (screenBbox.contains(start31)) {
			clippedStart31 = start31;
		}
		if (screenBbox.contains(end31)) {
			clippedEnd31 = end31;
		}
		if (clippedStart31 == null && clippedEnd31 == null) {
			clippedStart31 = new PointI(0, 0);
			clippedEnd31 = new PointI(0, 0);
			if (Utilities.calculateIntersection(start31, end31, screenBbox, clippedStart31)) {
				Utilities.calculateIntersection(end31, start31, screenBbox, clippedEnd31);
			} else {
				return null;
			}
		} else if (clippedStart31 == null) {
			clippedStart31 = new PointI(0, 0);
			if (!Utilities.calculateIntersection(start31, end31, screenBbox, clippedStart31)) {
				return null;
			}
		} else if (clippedEnd31 == null) {
			clippedEnd31 = new PointI(0, 0);
			if (!Utilities.calculateIntersection(end31, start31, screenBbox, clippedEnd31)) {
				return null;
			}
		}
		PointF startPixel = NativeUtilities.getElevatedPixelFrom31(mapRenderer, tileBox,
				clippedStart31.getX(), clippedStart31.getY());
		PointF endPixel = NativeUtilities.getElevatedPixelFrom31(mapRenderer, tileBox,
				clippedEnd31.getX(), clippedEnd31.getY());
		return Pair.create(startPixel, endPixel);
	}

	public static TileIdList convertToQListTileIds(@NonNull List<Long> tileIds) {
		TileIdList qTileIds = new TileIdList();
		for (Long tileId : tileIds) {
			qTileIds.add(TileId.fromXY(OfflineForecastHelper.getTileX(tileId),
					OfflineForecastHelper.getTileY(tileId)));
		}
		return qTileIds;
	}

	@Nullable
	public static Bitmap createBitmap(@NonNull IconData iconData) {
		Bitmap bitmap = Bitmap.createBitmap(iconData.getWidth(), iconData.getHeight(), Config.ARGB_8888);
		boolean ok = OsmAndCore.copyPixels(iconData.getBitmap(), bitmap);
		return ok ? bitmap : null;
	}
}

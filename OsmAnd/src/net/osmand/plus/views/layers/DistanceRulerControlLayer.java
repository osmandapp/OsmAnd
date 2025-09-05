package net.osmand.plus.views.layers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.Pair;
import android.view.MotionEvent;

import net.osmand.Location;
import net.osmand.StateChangedListener;
import net.osmand.core.android.MapRendererContext;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.MapMarker;
import net.osmand.core.jni.MapMarker.PinIconHorisontalAlignment;
import net.osmand.core.jni.MapMarker.PinIconVerticalAlignment;
import net.osmand.core.jni.MapMarkerBuilder;
import net.osmand.core.jni.MapMarkersCollection;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QVectorPointI;
import net.osmand.core.jni.SwigUtilities;
import net.osmand.core.jni.TextRasterizer;
import net.osmand.core.jni.VectorDouble;
import net.osmand.core.jni.VectorLine;
import net.osmand.core.jni.VectorLineBuilder;
import net.osmand.core.jni.VectorLinesCollection;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.render.OsmandDashPathEffect;
import net.osmand.plus.settings.enums.DistanceByTapTextSize;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.layers.geometry.GeometryWayDrawer;
import net.osmand.plus.views.layers.geometry.GeometryWayPathAlgorithms;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

public class DistanceRulerControlLayer extends OsmandMapLayer {

	private static final int VERTICAL_OFFSET = 15;
	private static final long DRAW_TIME = 4000;
	private static final long DELAY_BEFORE_DRAW = 50;
	private static final int DISTANCE_TEXT_SIZE = 16;
	private static final int LABEL_OFFSET = 20;

	private OsmandApplication app;

	private boolean showTwoFingersDistance;
	private boolean showDistBetweenFingerAndLocation;
	private boolean touchOutside;
	private int acceptableTouchRadius;

	private long cacheMultiTouchEndTime;
	private LatLon touchPointLatLon;
	private PointF touchPoint;
	private long touchStartTime;
	private long touchEndTime;
	private boolean touched;
	private boolean wasPinchZoomOrRotation;
	private boolean wasDoubleTapZoom;

	private final Path linePath = new Path();

	private Bitmap centerIconDay;
	private Bitmap centerIconNight;
	private Paint bitmapPaint;

	private RenderingLineAttributes lineAttrs;
	private RenderingLineAttributes lineFontAttrs;

	private Handler handler;

	private boolean singleTouchPointChanged;
	private boolean cachedNightMode;
	private LatLon cachedMyLocation;
	private LatLon cachedFirstTouchLatLon;
	private LatLon cachedSecondTouchLatLon;
	private boolean rotateText;
	private VectorLinesCollection vectorLinesCollection;
	private VectorLine rulerLine;
	private MapMarker distanceMarker;

	private StateChangedListener<DistanceByTapTextSize> textSizeListener;

	public DistanceRulerControlLayer(@NonNull Context ctx) {
		super(ctx);
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);

		app = getApplication();
		touchPoint = new PointF();
		acceptableTouchRadius = app.getResources().getDimensionPixelSize(R.dimen.acceptable_touch_radius);

		createBitmaps(view);

		bitmapPaint = new Paint();
		bitmapPaint.setAntiAlias(true);
		bitmapPaint.setDither(true);
		bitmapPaint.setFilterBitmap(true);

		lineAttrs = new RenderingLineAttributes("rulerLine");
		lineFontAttrs = new RenderingLineAttributes("rulerLineFont");

		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				view.refreshMap();
			}
		};
		addTextSizeListener();
		updateTextSize();
	}

	private void createBitmaps(@NonNull OsmandMapTileView view) {
		centerIconDay = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_ruler_center_day);
		centerIconNight = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_ruler_center_night);
	}

	@Override
	protected void updateResources() {
		super.updateResources();
		if (view != null) {
			createBitmaps(view);
			updateTextSize();
		}
	}

	@Override
	public boolean isMapGestureAllowed(MapGestureType type) {
		return !rulerModeOn() || type != MapGestureType.TWO_POINTERS_ZOOM_OUT;
	}

	@Override
	public boolean onTouchEvent(@NonNull MotionEvent event, @NonNull RotatedTileBox tileBox) {
		if (rulerModeOn() && !showTwoFingersDistance) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				touched = true;
				touchOutside = false;
				touchPoint.set(event.getX(), event.getY());
				touchPointLatLon = NativeUtilities.getLatLonFromElevatedPixel(getMapRenderer(), tileBox,
						event.getX(), event.getY());
				singleTouchPointChanged = true;
				touchStartTime = System.currentTimeMillis();
				wasPinchZoomOrRotation = false;
				wasDoubleTapZoom = false;
			} else if (event.getAction() == MotionEvent.ACTION_MOVE && !touchOutside &&
					!(touched && showDistBetweenFingerAndLocation)) {
				double d = Math.sqrt(Math.pow(event.getX() - touchPoint.x, 2) + Math.pow(event.getY() - touchPoint.y, 2));
				if (d > acceptableTouchRadius) {
					touchOutside = true;
				}
			} else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
				touched = false;
				touchEndTime = System.currentTimeMillis();
				wasDoubleTapZoom = view.isAfterDoubleTap();
				refreshMapDelayed();
			}
		}
		return false;
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tb, DrawSettings settings) {
		MapRendererView mapRenderer = getMapRenderer();
		boolean hasMapRenderer = mapRenderer != null;

		if (rulerModeOn()) {
			OsmandApplication app = view.getApplication();

			boolean nightMode = settings.isNightMode();
			boolean paintUpdated = lineAttrs.updatePaints(app, settings, tb);
			lineFontAttrs.updatePaints(app, settings, tb);
			lineFontAttrs.paint.setStyle(Style.FILL);
			long currentTime = System.currentTimeMillis();

			if (cacheMultiTouchEndTime != view.getMultiTouchEndTime()) {
				cacheMultiTouchEndTime = view.getMultiTouchEndTime();
				refreshMapDelayed();
			}
			if (touched && view.isMultiTouch()) {
				touched = false;
				touchEndTime = currentTime;
			}
			if (getMapView().isPinchZoomingOrRotating()) {
				wasPinchZoomOrRotation = true;
			}

			boolean showTwoFingersDistance =
					currentTime - view.getMultiTouchStartTime() > DELAY_BEFORE_DRAW &&
					(view.isMultiTouch() || currentTime - cacheMultiTouchEndTime < DRAW_TIME);

			boolean showDistBetweenFingerAndLocation = !wasPinchZoomOrRotation &&
					!showTwoFingersDistance &&
					!view.isMultiTouch() &&
					!wasDoubleTapZoom &&
					!touchOutside &&
					touchStartTime - view.getMultiTouchStartTime() > DELAY_BEFORE_DRAW &&
					currentTime - touchStartTime > DELAY_BEFORE_DRAW &&
					(touched || currentTime - touchEndTime < DRAW_TIME);

			Location currentLoc = app.getLocationProvider().getLastKnownLocation();

			if (hasMapRenderer) {
				drawDistanceRulerOpenGl(mapRenderer, canvas, tb, nightMode, paintUpdated, showTwoFingersDistance, showDistBetweenFingerAndLocation);
			} else {
				if (showDistBetweenFingerAndLocation && currentLoc != null) {
					drawDistBetweenFingerAndLocation(canvas, tb, currentLoc, nightMode);
				} else if (showTwoFingersDistance) {
					drawTwoFingersDistance(canvas, tb, view.getFirstTouchPointLatLon(),
							view.getSecondTouchPointLatLon(), nightMode);
				}
			}

			this.showTwoFingersDistance = showTwoFingersDistance;
			this.showDistBetweenFingerAndLocation = showDistBetweenFingerAndLocation;
		} else {
			if (hasMapRenderer) {
				hideDistanceRulerOpenGl();
			}
		}
	}

	public boolean rulerModeOn() {
		return app.getSettings().SHOW_DISTANCE_RULER.get();
	}

	private void refreshMapDelayed() {
		handler.sendEmptyMessageDelayed(0, DRAW_TIME + 50);
	}

	private void drawTwoFingersDistance(Canvas canvas, RotatedTileBox tb, LatLon firstTouch,
	                                    LatLon secondTouch, boolean nightMode) {
		PointF firstScreenPoint = NativeUtilities.getElevatedPixelFromLatLon(getMapRenderer(), tb, firstTouch.getLatitude(), firstTouch.getLongitude());
		PointF secondScreenPoint = NativeUtilities.getElevatedPixelFromLatLon(getMapRenderer(), tb, secondTouch.getLatitude(), secondTouch.getLongitude());
		float x1 = firstScreenPoint.x;
		float y1 = firstScreenPoint.y;
		float x2 = secondScreenPoint.x;
		float y2 = secondScreenPoint.y;

		Path path = new Path();
		path.moveTo(x1, y1);
		path.lineTo(x2, y2);

		String text = OsmAndFormatter.getFormattedDistance((float) MapUtils.getDistance(firstTouch, secondTouch), app);

		canvas.rotate(-tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
		canvas.drawPath(path, lineAttrs.paint);
		drawFingerTouchIcon(canvas, x1, y1, nightMode);
		drawFingerTouchIcon(canvas, x2, y2, nightMode);
		drawTextOnCenterOfPath(canvas, path, text, x1 >= x2);
		canvas.rotate(tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
	}

	private void drawTextOnCenterOfPath(Canvas canvas, Path path, String text, boolean rotate) {
		PathMeasure pm = new PathMeasure(path, false);
		Rect bounds = new Rect();
		lineFontAttrs.paint.getTextBounds(text, 0, text.length(), bounds);
		float hOffset = pm.getLength() / 2 - bounds.width() / 2f;

		if (rotate) {
			float[] pos = new float[2];
			pm.getPosTan(pm.getLength() / 2, pos, null);
			canvas.rotate(180, pos[0], pos[1]);
			canvas.drawTextOnPath(text, path, hOffset, bounds.height() + VERTICAL_OFFSET, lineFontAttrs.paint2);
			canvas.drawTextOnPath(text, path, hOffset, bounds.height() + VERTICAL_OFFSET, lineFontAttrs.paint);
			canvas.rotate(-180, pos[0], pos[1]);
		} else {
			canvas.drawTextOnPath(text, path, hOffset, -VERTICAL_OFFSET, lineFontAttrs.paint2);
			canvas.drawTextOnPath(text, path, hOffset, -VERTICAL_OFFSET, lineFontAttrs.paint);
		}
	}

	private void drawTextOnCenterOfPathOpenGl(String text, boolean nightMode) {
		TextRasterizer.Style style = MapTextLayer.getTextStyle(getContext(), nightMode,
				getApplication().getOsmandMap().getMapDensity(), view.getDensity());

		DistanceByTapTextSize textSize = app.getSettings().DISTANCE_BY_TAP_TEXT_SIZE.get();
		float lineTextSize = app.getResources().getDimension(textSize.getTextSizeId());

		style.setSize(lineTextSize);

		MapMarkerBuilder markerBuilder = new MapMarkerBuilder();
		markerBuilder.setIsHidden(false);
		markerBuilder.setCaption(text);
		markerBuilder.setBaseOrder(getBaseOrder() - 1);
		markerBuilder.setCaptionStyle(style);
		markerBuilder.setUpdateAfterCreated(true);
		distanceMarker = markerBuilder.buildAndAddToCollection(mapMarkersCollection);
		distanceMarker.setOffsetFromLine(LABEL_OFFSET);
	}

	private void drawFingerTouchIcon(Canvas canvas, float x, float y, boolean nightMode) {
		if (nightMode) {
			canvas.drawBitmap(centerIconNight, x - centerIconNight.getWidth() / 2f,
					y - centerIconNight.getHeight() / 2f, bitmapPaint);

		} else {
			canvas.drawBitmap(centerIconDay, x - centerIconDay.getWidth() / 2f,
					y - centerIconDay.getHeight() / 2f, bitmapPaint);
		}
	}

	private void drawDistBetweenFingerAndLocation(Canvas canvas, RotatedTileBox tb, Location currLoc, boolean night) {
		PointF firstScreenPoint = NativeUtilities.getElevatedPixelFromLatLon(getMapRenderer(), tb, touchPointLatLon.getLatitude(), touchPointLatLon.getLongitude());
		PointF secondScreenPoint = NativeUtilities.getElevatedPixelFromLatLon(getMapRenderer(), tb, currLoc.getLatitude(), currLoc.getLongitude());
		float x = firstScreenPoint.x;
		float y = firstScreenPoint.y;

		recalculatePath(tb, firstScreenPoint, secondScreenPoint);

		float dist = (float) MapUtils.getDistance(touchPointLatLon, currLoc.getLatitude(), currLoc.getLongitude());
		String text = OsmAndFormatter.getFormattedDistance(dist, app);
		boolean rotateText = firstScreenPoint.x >= secondScreenPoint.x;

		canvas.rotate(-tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
		canvas.drawPath(linePath, lineAttrs.paint);
		drawFingerTouchIcon(canvas, x, y, night);
		drawTextOnCenterOfPath(canvas, linePath, text, rotateText);
		canvas.rotate(tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
	}

	private void drawDistanceRulerOpenGl(@NonNull MapRendererView mapRenderer,
	                                     @NonNull Canvas canvas,
	                                     @NonNull RotatedTileBox tileBox,
	                                     boolean nightMode,
	                                     boolean paintUpdated,
	                                     boolean showTwoFingersDistance,
	                                     boolean showDistBetweenFingerAndLocation) {
		Location currentLoc = view.getApplication().getLocationProvider().getLastKnownLocation();
		LatLon myLocation = currentLoc == null
				? null
				: new LatLon(currentLoc.getLatitude(), currentLoc.getLongitude());
		boolean appearanceChanged = paintUpdated || cachedNightMode != nightMode;

		boolean disableTwoFingersDistance = !showTwoFingersDistance && this.showTwoFingersDistance;
		boolean updateTwoFingersDistance = showTwoFingersDistance
				&& (appearanceChanged
				|| !Algorithms.objectEquals(cachedFirstTouchLatLon, view.getFirstTouchPointLatLon())
				|| !Algorithms.objectEquals(cachedSecondTouchLatLon, view.getSecondTouchPointLatLon()));
		boolean clearTwoFingersDistance = disableTwoFingersDistance || updateTwoFingersDistance;

		boolean disableMyLocationDistance = !showDistBetweenFingerAndLocation
				&& this.showDistBetweenFingerAndLocation;
		boolean updateMyLocationDistance = showDistBetweenFingerAndLocation
				&& (appearanceChanged
				|| singleTouchPointChanged
				|| !Algorithms.objectEquals(cachedMyLocation, myLocation));
		boolean clearMyLocationDistance = disableMyLocationDistance || updateMyLocationDistance;

		if (clearTwoFingersDistance || clearMyLocationDistance) {
			hideDistanceRulerOpenGl();
		}

		cachedNightMode = nightMode;
		cachedMyLocation = myLocation;
		singleTouchPointChanged = false;
		cachedFirstTouchLatLon = view.getFirstTouchPointLatLon();
		cachedSecondTouchLatLon = view.getSecondTouchPointLatLon();

		double distance = Double.NaN;
		if (showDistBetweenFingerAndLocation && myLocation != null) {
			if (vectorLinesCollection == null) {
				if (mapMarkersCollection == null) {
					mapMarkersCollection = new MapMarkersCollection();
				}

				if (!mapRenderer.hasSymbolsProvider(mapMarkersCollection)) {
					mapRenderer.addSymbolsProvider(mapMarkersCollection);
				}

				if (calculateTextPathOpenGl(mapRenderer, tileBox, touchPointLatLon, myLocation)) {
					distance = MapUtils.getDistance(touchPointLatLon, myLocation);
				}

				if (!Double.isNaN(distance)) {
					String formattedDistance = OsmAndFormatter.getFormattedDistance((float) distance, app);
					drawTextOnCenterOfPathOpenGl(formattedDistance, nightMode);
				}

				drawLineBetweenLocationsOpenGl(mapRenderer, touchPointLatLon, myLocation);
				drawFingerTouchIconsOpenGl(mapRenderer, touchPointLatLon, nightMode);
			}

		} else if (showTwoFingersDistance) {
			if (vectorLinesCollection == null) {
				if (mapMarkersCollection == null) {
					mapMarkersCollection = new MapMarkersCollection();
				}

				if (!mapRenderer.hasSymbolsProvider(mapMarkersCollection)) {
					mapRenderer.addSymbolsProvider(mapMarkersCollection);
				}

				if (calculateTextPathOpenGl(mapRenderer, tileBox, cachedFirstTouchLatLon, cachedSecondTouchLatLon)) {
					distance = MapUtils.getDistance(cachedFirstTouchLatLon, cachedSecondTouchLatLon);
				}

				if (!Double.isNaN(distance)) {
					String formattedDistance = OsmAndFormatter.getFormattedDistance((float) distance, app);
					drawTextOnCenterOfPathOpenGl(formattedDistance, nightMode);
				}

				drawLineBetweenLocationsOpenGl(mapRenderer, cachedFirstTouchLatLon, cachedSecondTouchLatLon);
				drawFingerTouchIconsOpenGl(mapRenderer, cachedFirstTouchLatLon, nightMode);
				drawFingerTouchIconsOpenGl(mapRenderer, cachedSecondTouchLatLon, nightMode);
			}
		}
	}

	private void drawLineBetweenLocationsOpenGl(@NonNull MapRendererView mapRenderer,
	                                            @NonNull LatLon startLatLon,
	                                            @NonNull LatLon endLatLon) {
		PointI start31 = NativeUtilities.getPoint31FromLatLon(startLatLon);
		PointI end31 = NativeUtilities.getPoint31FromLatLon(endLatLon);

		QVectorPointI points31 = new QVectorPointI();
		points31.add(start31);
		points31.add(end31);

		VectorLineBuilder vectorLineBuilder = new VectorLineBuilder();
		vectorLineBuilder.setBaseOrder(getBaseOrder())
				.setIsHidden(false)
				.setLineId(0)
				.setLineWidth(lineAttrs.paint.getStrokeWidth() * GeometryWayDrawer.getVectorLineScale(app))
				.setPoints(points31)
				.setEndCapStyle(VectorLine.EndCapStyle.BUTT.swigValue())
				.setFillColor(NativeUtilities.createFColorARGB(lineAttrs.paint.getColor()));

		PathEffect pathEffect = lineAttrs.paint.getPathEffect();
		if (pathEffect instanceof OsmandDashPathEffect) {
			OsmandDashPathEffect dashPathEffect = (OsmandDashPathEffect) pathEffect;
			VectorDouble lineDash = new VectorDouble();
			for (float f : dashPathEffect.getIntervals()) {
				lineDash.add((double) f * 4.0);
			}
			vectorLineBuilder.setLineDash(lineDash);
		}

		// Marker should be created before as vectorLine.attachMarker() call recreates primitive
		vectorLineBuilder.attachMarker(distanceMarker);

		vectorLinesCollection = new VectorLinesCollection();
		rulerLine = vectorLineBuilder.buildAndAddToCollection(vectorLinesCollection);
		mapRenderer.addSymbolsProvider(vectorLinesCollection);
	}

	private void drawFingerTouchIconsOpenGl(@NonNull MapRendererView mapRenderer,
	                                        @NonNull LatLon touchPoint,
	                                        boolean night) {
		int x31 = MapUtils.get31TileNumberX(touchPoint.getLongitude());
		int y31 = MapUtils.get31TileNumberY(touchPoint.getLatitude());

		Bitmap icon = night ? centerIconNight : centerIconDay;

		MapMarkerBuilder builder = new MapMarkerBuilder();
		builder.setIsHidden(false)
				.setBaseOrder(getBaseOrder() - 1)
				.setPosition(new PointI(x31, y31))
				.setIsAccuracyCircleSupported(false)
				.setPinIconHorisontalAlignment(PinIconHorisontalAlignment.CenterHorizontal)
				.setPinIconVerticalAlignment(PinIconVerticalAlignment.CenterVertical)
				.setPinIcon(NativeUtilities.createSkImageFromBitmap(icon))
				.setUpdateAfterCreated(true);

		builder.buildAndAddToCollection(mapMarkersCollection);
	}

	private boolean calculateTextPathOpenGl(@NonNull MapRendererView mapRenderer,
	                                        @NonNull RotatedTileBox tileBox,
	                                        @NonNull LatLon startLatLon,
	                                        @NonNull LatLon endLatLon) {
		PointI start31 = NativeUtilities.getPoint31FromLatLon(startLatLon);
		PointI end31 = NativeUtilities.getPoint31FromLatLon(endLatLon);
		Pair<PointF, PointF> line = NativeUtilities.clipLineInVisibleRect(mapRenderer, tileBox, start31, end31);
		if (line != null) {
			recalculatePath(tileBox, line.first, line.second);
			rotateText = line.first.x >= line.second.x;
			return true;
		}

		return false;
	}

	private void recalculatePath(@NonNull RotatedTileBox tileBox, @NonNull PointF start, @NonNull PointF end) {
		List<Float> tx = new ArrayList<>();
		List<Float> ty = new ArrayList<>();

		tx.add(start.x);
		tx.add(end.x);
		ty.add(start.y);
		ty.add(end.y);

		linePath.reset();
		GeometryWayPathAlgorithms.calculatePath(tileBox, tx, ty, linePath);
	}

	private void hideDistanceRulerOpenGl() {
		clearVectorLinesCollection();
		clearMapMarkersCollections();
		linePath.reset();
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	protected void cleanupResources() {
		super.cleanupResources();
		clearVectorLinesCollection();
	}

	private void clearVectorLinesCollection() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null && vectorLinesCollection != null) {
			mapRenderer.removeSymbolsProvider(vectorLinesCollection);
			vectorLinesCollection = null;
		}
	}

	private void addTextSizeListener() {
		textSizeListener = change -> updateTextSize();
		app.getSettings().DISTANCE_BY_TAP_TEXT_SIZE.addListener(textSizeListener);
	}

	private void updateTextSize() {
		DistanceByTapTextSize textSize = app.getSettings().DISTANCE_BY_TAP_TEXT_SIZE.get();
		float lineTextSize = app.getResources().getDimension(textSize.getTextSizeId());

		lineFontAttrs.paint.setTextSize(lineTextSize);
		lineFontAttrs.paint2.setTextSize(lineTextSize);
	}
}

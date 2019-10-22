package net.osmand.plus.views;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import net.osmand.PlatformUtil;
import net.osmand.access.AccessibilityActionsProvider;
import net.osmand.core.android.MapRendererView;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.data.QuadPointDouble;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.IMapLocationListener;
import net.osmand.map.MapTileDownloader.DownloadRequest;
import net.osmand.map.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.TwoFingerTapDetector;
import net.osmand.plus.views.MultiTouchSupport.MultiTouchZoomListener;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRuleStorageProperties;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OsmandMapTileView implements IMapDownloaderCallback {
	private static final int MAP_REFRESH_MESSAGE = OsmAndConstants.UI_HANDLER_MAP_VIEW + 4;
	private static final int MAP_FORCE_REFRESH_MESSAGE = OsmAndConstants.UI_HANDLER_MAP_VIEW + 5;
	private static final int BASE_REFRESH_MESSAGE = OsmAndConstants.UI_HANDLER_MAP_VIEW + 3;
	protected final static int LOWEST_ZOOM_TO_ROTATE = 9;
	private static final int MAP_DEFAULT_COLOR = 0xffebe7e4;
	private boolean MEASURE_FPS = false;
	private FPSMeasurement main = new FPSMeasurement();
	private FPSMeasurement additional = new FPSMeasurement();
	private View view;
	private Activity activity;
	private OsmandApplication application;
	protected OsmandSettings settings = null;
	private CanvasColors canvasColors = null;
	private Boolean nightMode = null;

	private class CanvasColors {
		int colorDay = MAP_DEFAULT_COLOR;
		int colorNight = MAP_DEFAULT_COLOR;
	}

	private class FPSMeasurement {
		int fpsMeasureCount = 0;
		int fpsMeasureMs = 0;
		long fpsFirstMeasurement = 0;
		float fps;

		void calculateFPS(long start, long end) {
			fpsMeasureMs += end - start;
			fpsMeasureCount++;
			if (fpsMeasureCount > 10 || (start - fpsFirstMeasurement) > 400) {
				fpsFirstMeasurement = start;
				fps = (1000f * fpsMeasureCount / fpsMeasureMs);
				fpsMeasureCount = 0;
				fpsMeasureMs = 0;
			}
		}
	}


	protected static final int emptyTileDivisor = 16;


	public interface OnTrackBallListener {
		public boolean onTrackBallEvent(MotionEvent e);
	}

	public interface OnLongClickListener {
		public boolean onLongPressEvent(PointF point);
	}

	public interface OnClickListener {
		public boolean onPressEvent(PointF point);
	}

	public interface OnDrawMapListener {
		public void onDrawOverMap();
	}

	protected static final Log LOG = PlatformUtil.getLog(OsmandMapTileView.class);


	private RotatedTileBox currentViewport;

	private float rotate; // accumulate

	private int mapPosition;
	private int mapPositionX;

	private float mapRatioX;
	private float mapRatioY;

	private boolean showMapPosition = true;

	private IMapLocationListener locationListener;

	private OnLongClickListener onLongClickListener;

	private OnClickListener onClickListener;

	private OnTrackBallListener trackBallDelegate;

	private AccessibilityActionsProvider accessibilityActions;

	private List<OsmandMapLayer> layers = new ArrayList<>();

	private BaseMapLayer mainLayer;

	private Map<OsmandMapLayer, Float> zOrders = new HashMap<OsmandMapLayer, Float>();

	private OnDrawMapListener onDrawMapListener;

	// UI Part
	// handler to refresh map (in ui thread - ui thread is not necessary, but msg queue is required).
	protected Handler handler;
	private Handler baseHandler;

	private AnimateDraggingMapThread animatedDraggingThread;

	Paint paintGrayFill;
	Paint paintBlackFill;
	Paint paintWhiteFill;
	Paint paintCenter;

	private DisplayMetrics dm;
	private MapRendererView mapRenderer;

	private Bitmap bufferBitmap;
	private RotatedTileBox bufferImgLoc;
	private Bitmap bufferBitmapTmp;

	private Paint paintImg;

	private GestureDetector gestureDetector;
	private MultiTouchSupport multiTouchSupport;
	private DoubleTapScaleDetector doubleTapScaleDetector;
	private TwoFingerTapDetector twoFingersTapDetector;
	//private boolean afterTwoFingersTap = false;
	private boolean afterDoubleTap = false;
	private boolean wasMapLinkedBeforeGesture = false;

	private LatLon firstTouchPointLatLon;
	private LatLon secondTouchPointLatLon;
	private boolean multiTouch;
	private long multiTouchStartTime;
	private long multiTouchEndTime;
	private boolean wasZoomInMultiTouch;

	public OsmandMapTileView(MapActivity activity, int w, int h) {
		this.activity = activity;
		init(activity, w, h);
	}

	// ///////////////////////////// INITIALIZING UI PART ///////////////////////////////////
	public void init(final MapActivity ctx, int w, int h) {
		application = (OsmandApplication) ctx.getApplicationContext();
		settings = application.getSettings();

		paintGrayFill = new Paint();
		paintGrayFill.setColor(Color.GRAY);
		paintGrayFill.setStyle(Style.FILL);
		// when map rotate
		paintGrayFill.setAntiAlias(true);

		paintBlackFill = new Paint();
		paintBlackFill.setColor(Color.BLACK);
		paintBlackFill.setStyle(Style.FILL);
		// when map rotate
		paintBlackFill.setAntiAlias(true);

		paintWhiteFill = new Paint();
		paintWhiteFill.setColor(Color.WHITE);
		paintWhiteFill.setStyle(Style.FILL);
		// when map rotate
		paintWhiteFill.setAntiAlias(true);

		paintCenter = new Paint();
		paintCenter.setStyle(Style.STROKE);
		paintCenter.setColor(Color.rgb(60, 60, 60));
		paintCenter.setStrokeWidth(2);
		paintCenter.setAntiAlias(true);

		paintImg = new Paint();
		paintImg.setFilterBitmap(true);
//		paintImg.setDither(true);

		handler = new Handler();
		baseHandler = new Handler(application.getResourceManager().getRenderingBufferImageThread().getLooper());
		animatedDraggingThread = new AnimateDraggingMapThread(this);

		WindowManager mgr = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
		dm = new DisplayMetrics();
		mgr.getDefaultDisplay().getMetrics(dm);
		LatLon ll = settings.getLastKnownMapLocation();
		currentViewport = new RotatedTileBox.RotatedTileBoxBuilder().
				setLocation(ll.getLatitude(), ll.getLongitude()).setZoom(settings.getLastKnownMapZoom()).
				setPixelDimensions(w, h).build();
		currentViewport.setDensity(dm.density);
		currentViewport.setMapDensity(getSettingsMapDensity());

		gestureDetector = new GestureDetector(ctx, new MapTileViewOnGestureListener());
		multiTouchSupport = new MultiTouchSupport(ctx, new MapTileViewMultiTouchZoomListener());
		doubleTapScaleDetector = new DoubleTapScaleDetector(this, new MapTileViewMultiTouchZoomListener());
		twoFingersTapDetector = new TwoFingerTapDetector() {
			@Override
			public void onTwoFingerTap() {
				//afterTwoFingersTap = true;
				if (!mapGestureAllowed(OsmandMapLayer.MapGestureType.TWO_POINTERS_ZOOM_OUT)) {
					return;
				}
				if (isZoomingAllowed(getZoom(), -1.1f)) {
					getAnimatedDraggingThread().startZooming(getZoom() - 1, currentViewport.getZoomFloatPart(), false);
					if (wasMapLinkedBeforeGesture) {
						ctx.getMapViewTrackingUtilities().setMapLinkedToLocation(true);
					}
				}
			}
		};
	}

	public void setView(View view) {
		this.view = view;
		view.setClickable(true);
		view.setLongClickable(true);
		view.setFocusable(true);
		refreshMap(true);
	}

	public Boolean onKeyDown(int keyCode, KeyEvent event) {
		return application.accessibilityEnabled() ? false : null;
	}

	public boolean isLayerVisible(OsmandMapLayer layer) {
		return layers.contains(layer);
	}

	public float getZorder(OsmandMapLayer layer) {
		Float z = zOrders.get(layer);
		if (z == null) {
			return 10;
		}
		return z;
	}

	public synchronized void addLayer(OsmandMapLayer layer, float zOrder) {
		int i = 0;
		for (i = 0; i < layers.size(); i++) {
			if (zOrders.get(layers.get(i)) > zOrder) {
				break;
			}
		}
		layer.initLayer(this);
		layers.add(i, layer);
		zOrders.put(layer, zOrder);
	}

	public synchronized void removeLayer(OsmandMapLayer layer) {
		while (layers.remove(layer)) ;
		zOrders.remove(layer);
		layer.destroyLayer();
	}

	public synchronized void removeAllLayers() {
		while (layers.size() > 0) {
			removeLayer(layers.get(0));
		}
	}

	public List<OsmandMapLayer> getLayers() {
		return layers;
	}

	@SuppressWarnings("unchecked")
	public <T extends OsmandMapLayer> T getLayerByClass(Class<T> cl) {
		for (OsmandMapLayer lr : layers) {
			if (cl.isInstance(lr)) {
				return (T) lr;
			}
		}
		return null;
	}

	public int getViewHeight() {
		if (view != null) {
			return view.getHeight();
		} else {
			return 0;
		}
	}

	public OsmandApplication getApplication() {
		return application;
	}

	// ///////////////////////// NON UI PART (could be extracted in common) /////////////////////////////
	public LatLon getFirstTouchPointLatLon() {
		return firstTouchPointLatLon;
	}

	public LatLon getSecondTouchPointLatLon() {
		return secondTouchPointLatLon;
	}

	public boolean isMultiTouch() {
		return multiTouch;
	}

	public long getMultiTouchStartTime() {
		return multiTouchStartTime;
	}

	public long getMultiTouchEndTime() {
		return multiTouchEndTime;
	}

	public boolean isWasZoomInMultiTouch() {
		return wasZoomInMultiTouch;
	}

	public boolean mapGestureAllowed(OsmandMapLayer.MapGestureType type) {
		for (OsmandMapLayer layer : layers) {
			if (!layer.isMapGestureAllowed(type)) {
				return false;
			}
		}
		return true;
	}

	public void setIntZoom(int zoom) {
		zoom = zoom > getMaxZoom() ? getMaxZoom() : zoom;
		zoom = zoom < getMinZoom() ? getMinZoom() : zoom;
		if (mainLayer != null) {
			animatedDraggingThread.stopAnimating();
			currentViewport.setZoomAndAnimation(zoom, 0, 0);
			if (zoom <= LOWEST_ZOOM_TO_ROTATE) {
				rotate = 0;
			}
			currentViewport.setRotate(rotate);
			refreshMap();
		}
	}

	public void setComplexZoom(int zoom, double mapDensity) {
		if (mainLayer != null && zoom <= getMaxZoom() && zoom >= getMinZoom()) {
			animatedDraggingThread.stopAnimating();
			currentViewport.setZoomAndAnimation(zoom, 0);
			currentViewport.setMapDensity(mapDensity);
			if (zoom <= LOWEST_ZOOM_TO_ROTATE) {
				rotate = 0;
			}
			currentViewport.setRotate(rotate);
			refreshMap();
		}
	}


	public boolean isMapRotateEnabled() {
		return getZoom() > LOWEST_ZOOM_TO_ROTATE;
	}

	public void setRotate(float rotate, boolean force) {
		if (isMapRotateEnabled()) {
			float diff = MapUtils.unifyRotationDiff(rotate, getRotate());
			if (Math.abs(diff) > 5 || force) { // check smallest rotation
				animatedDraggingThread.startRotate(rotate);
			}
		}
	}

	public boolean isShowMapPosition() {
		return showMapPosition;
	}

	public void setShowMapPosition(boolean showMapPosition) {
		this.showMapPosition = showMapPosition;
	}

	public float getRotate() {
		return currentViewport.getRotate();
	}


	public void setLatLon(double latitude, double longitude) {
		animatedDraggingThread.stopAnimating();
		currentViewport.setLatLonCenter(latitude, longitude);
		refreshMap();
	}

	public double getLatitude() {
		return currentViewport.getLatitude();
	}

	public double getLongitude() {
		return currentViewport.getLongitude();
	}

	public int getZoom() {
		return currentViewport.getZoom();
	}

	public double getZoomFractionalPart() {
		return currentViewport.getZoomFloatPart();
	}

	public double getSettingsMapDensity() {
		return (getSettings().MAP_DENSITY.get()) * Math.max(1, getDensity());
	}


	public boolean isZooming() {
		return currentViewport.isZoomAnimated();
	}

	public void setMapLocationListener(IMapLocationListener l) {
		locationListener = l;
	}

	/**
	 * Adds listener to control when map is dragging
	 */
	public IMapLocationListener setMapLocationListener() {
		return locationListener;
	}

	public void setOnDrawMapListener(OnDrawMapListener onDrawMapListener) {
		this.onDrawMapListener = onDrawMapListener;
	}

	// ////////////////////////////// DRAWING MAP PART /////////////////////////////////////////////
	public BaseMapLayer getMainLayer() {
		return mainLayer;
	}

	public void setMainLayer(BaseMapLayer mainLayer) {
		this.mainLayer = mainLayer;
		int zoom = currentViewport.getZoom();

		if (getMaxZoom() < zoom) {
			zoom = getMaxZoom();
		}
		if (getMinZoom() > zoom) {
			zoom = getMinZoom();
		}
		currentViewport.setZoomAndAnimation(zoom, 0, 0);
		refreshMap();
	}

	public int getMapPosition() {
		return mapPosition;
	}

	public void setMapPosition(int type) {
		this.mapPosition = type;
	}

	public void setMapPositionX(int type) {
		this.mapPositionX = type;
	}

	public void setCustomMapRatio(float ratioX, float ratioY) {
		this.mapRatioX = ratioX;
		this.mapRatioY = ratioY;
	}

	public void restoreMapRatio() {
		RotatedTileBox box = currentViewport.copy();
		float rx = (float)box.getCenterPixelX() / box.getPixWidth();
		float ry = (float)box.getCenterPixelY() / box.getPixHeight();
		if (mapPosition == OsmandSettings.BOTTOM_CONSTANT) {
			ry -= 0.35;
		}
		box.setCenterLocation(rx, ry);
		LatLon screenCenter = box.getLatLonFromPixel(box.getPixWidth() / 2f, box.getPixHeight() / 2f);
		mapRatioX = 0;
		mapRatioY = 0;
		setLatLon(screenCenter.getLatitude(), screenCenter.getLongitude());
	}

	public boolean hasCustomMapRatio() {
		return mapRatioX != 0 && mapRatioY != 0;
	}

	public OsmandSettings getSettings() {
		return settings;
	}

	public int getMaxZoom() {
		if (mainLayer != null) {
			return mainLayer.getMaximumShownMapZoom();
		}
		return BaseMapLayer.DEFAULT_MAX_ZOOM;
	}

	public int getMinZoom() {
		if (mainLayer != null) {
			return mainLayer.getMinimumShownMapZoom() + 1;
		}
		return BaseMapLayer.DEFAULT_MIN_ZOOM;
	}

	private void drawBasemap(Canvas canvas) {
		if (bufferImgLoc != null) {
			float rot = -bufferImgLoc.getRotate();
			canvas.rotate(rot, currentViewport.getCenterPixelX(), currentViewport.getCenterPixelY());
			final RotatedTileBox calc = currentViewport.copy();
			calc.setRotate(bufferImgLoc.getRotate());

			int cz = getZoom();
			QuadPointDouble lt = bufferImgLoc.getLeftTopTile(cz);
			QuadPointDouble rb = bufferImgLoc.getRightBottomTile(cz);
			final float x1 = calc.getPixXFromTile(lt.x, lt.y, cz);
			final float x2 = calc.getPixXFromTile(rb.x, rb.y, cz);
			final float y1 = calc.getPixYFromTile(lt.x, lt.y, cz);
			final float y2 = calc.getPixYFromTile(rb.x, rb.y, cz);
//			LatLon lt = bufferImgLoc.getLeftTopLatLon();
//			LatLon rb = bufferImgLoc.getRightBottomLatLon();
//			final float x1 = calc.getPixXFromLatLon(lt.getLatitude(), lt.getLongitude());
//			final float x2 = calc.getPixXFromLatLon(rb.getLatitude(), rb.getLongitude());
//			final float y1 = calc.getPixYFromLatLon(lt.getLatitude(), lt.getLongitude());
//			final float y2 = calc.getPixYFromLatLon(rb.getLatitude(), rb.getLongitude());
			if (!bufferBitmap.isRecycled()) {
				RectF rct = new RectF(x1, y1, x2, y2);
				canvas.drawBitmap(bufferBitmap, null, rct, paintImg);
			}
			canvas.rotate(-rot, currentViewport.getCenterPixelX(), currentViewport.getCenterPixelY());
		}
	}

	private void refreshBaseMapInternal(RotatedTileBox tileBox, DrawSettings drawSettings) {
		if (tileBox.getPixHeight() == 0 || tileBox.getPixWidth() == 0) {
			return;
		}
		if (bufferBitmapTmp == null || tileBox.getPixHeight() != bufferBitmapTmp.getHeight()
				|| tileBox.getPixWidth() != bufferBitmapTmp.getWidth()) {
			bufferBitmapTmp = Bitmap.createBitmap(tileBox.getPixWidth(), tileBox.getPixHeight(), Config.ARGB_8888);
		}
		long start = SystemClock.elapsedRealtime();
		final QuadPoint c = tileBox.getCenterPixelPoint();
		Canvas canvas = new Canvas(bufferBitmapTmp);
		fillCanvas(canvas, drawSettings);
		for (int i = 0; i < layers.size(); i++) {
			try {
				OsmandMapLayer layer = layers.get(i);
				canvas.save();
				// rotate if needed
				if (!layer.drawInScreenPixels()) {
					canvas.rotate(tileBox.getRotate(), c.x, c.y);
				}
				layer.onPrepareBufferImage(canvas, tileBox, drawSettings);
				canvas.restore();
			} catch (IndexOutOfBoundsException e) {
				// skip it
				canvas.restore();
			}
		}
		Bitmap t = bufferBitmap;
		synchronized (this) {
			bufferImgLoc = tileBox;
			bufferBitmap = bufferBitmapTmp;
			bufferBitmapTmp = t;
		}
		long end = SystemClock.elapsedRealtime();
		additional.calculateFPS(start, end);
	}

	private void refreshMapInternal(DrawSettings drawSettings) {
		if (view == null) {
			return;
		}
		final float ratioy;
		if (mapRatioY != 0) {
			ratioy = mapRatioY;
		} else if (mapPosition == OsmandSettings.BOTTOM_CONSTANT) {
			ratioy = 0.85f;
		} else if (mapPosition == OsmandSettings.MIDDLE_BOTTOM_CONSTANT) {
			ratioy = 0.70f;
		} else if (mapPosition == OsmandSettings.MIDDLE_TOP_CONSTANT) {
			ratioy = 0.25f;
		} else {
			ratioy = 0.5f;
		}
		final float ratiox;
		if (mapRatioX != 0) {
			ratiox = mapRatioX;
		} else if (mapPosition == OsmandSettings.LANDSCAPE_MIDDLE_RIGHT_CONSTANT) {
			ratiox = 0.7f;
		} else {
			ratiox = mapPositionX == 0 ? 0.5f : 0.75f;
		}
		final int cy = (int) (ratioy * view.getHeight());
		final int cx = (int) (ratiox * view.getWidth());
		if (currentViewport.getPixWidth() != view.getWidth() || currentViewport.getPixHeight() != view.getHeight() ||
				currentViewport.getCenterPixelY() != cy ||
				currentViewport.getCenterPixelX() != cx) {
			currentViewport.setPixelDimensions(view.getWidth(), view.getHeight(), ratiox, ratioy);
			refreshBufferImage(drawSettings);
		}
		if (view instanceof SurfaceView) {
			SurfaceHolder holder = ((SurfaceView) view).getHolder();
			long ms = SystemClock.elapsedRealtime();
			synchronized (holder) {
				Canvas canvas = holder.lockCanvas();
				if (canvas != null) {
					try {
						// make copy to avoid concurrency
						RotatedTileBox viewportToDraw = currentViewport.copy();
						drawOverMap(canvas, viewportToDraw, drawSettings);
					} finally {
						holder.unlockCanvasAndPost(canvas);
					}
				}
				if (MEASURE_FPS) {
					main.calculateFPS(ms, SystemClock.elapsedRealtime());
				}
			}
		} else {
			view.invalidate();
		}
	}

	private void fillCanvas(Canvas canvas, DrawSettings drawSettings) {
		int color = MAP_DEFAULT_COLOR;
		CanvasColors canvasColors = this.canvasColors;
		if (canvasColors == null) {
			canvasColors = updateCanvasColors();
			this.canvasColors = canvasColors;
		}
		if (canvasColors != null) {
			color = drawSettings.isNightMode() ? canvasColors.colorNight : canvasColors.colorDay;
		}
		canvas.drawColor(color);
	}

	public void resetDefaultColor() {
		canvasColors = null;
	}

	private CanvasColors updateCanvasColors() {
		CanvasColors canvasColors = null;
		RenderingRulesStorage rrs = application.getRendererRegistry().getCurrentSelectedRenderer();
		if (rrs != null) {
			canvasColors = new CanvasColors();
			RenderingRuleSearchRequest req = new RenderingRuleSearchRequest(rrs);
			req.setBooleanFilter(rrs.PROPS.R_NIGHT_MODE, false);
			if (req.searchRenderingAttribute(RenderingRuleStorageProperties.A_DEFAULT_COLOR)) {
				canvasColors.colorDay = req.getIntPropertyValue(req.ALL.R_ATTR_COLOR_VALUE);
			}
			req = new RenderingRuleSearchRequest(rrs);
			req.setBooleanFilter(rrs.PROPS.R_NIGHT_MODE, true);
			if (req.searchRenderingAttribute(RenderingRuleStorageProperties.A_DEFAULT_COLOR)) {
				canvasColors.colorNight = req.getIntPropertyValue(req.ALL.R_ATTR_COLOR_VALUE);
			}
		}
		return canvasColors;
	}

	public boolean isMeasureFPS() {
		return MEASURE_FPS;
	}

	public void setMeasureFPS(boolean measureFPS) {
		MEASURE_FPS = measureFPS;
	}

	public float getFPS() {
		return main.fps;
	}

	public float getSecondaryFPS() {
		return additional.fps;
	}

	public boolean isAnimatingZoom() {
		return animatedDraggingThread.isAnimatingZoom();
	}

	@SuppressLint("WrongCall")
	public void drawOverMap(Canvas canvas, RotatedTileBox tileBox, DrawSettings drawSettings) {
		if (mapRenderer == null) {
			fillCanvas(canvas, drawSettings);
		}
		final QuadPoint c = tileBox.getCenterPixelPoint();
		synchronized (this) {
			if (bufferBitmap != null && !bufferBitmap.isRecycled() && mapRenderer == null) {
				canvas.save();
				canvas.rotate(tileBox.getRotate(), c.x, c.y);
				drawBasemap(canvas);
				canvas.restore();
			}
		}

		if (onDrawMapListener != null) {
			onDrawMapListener.onDrawOverMap();
		}

		for (int i = 0; i < layers.size(); i++) {
			try {
				OsmandMapLayer layer = layers.get(i);
				canvas.save();
				// rotate if needed
				if (!layer.drawInScreenPixels()) {
					canvas.rotate(tileBox.getRotate(), c.x, c.y);
				}
				if (mapRenderer != null) {
					layer.onPrepareBufferImage(canvas, tileBox, drawSettings);
				}
				layer.onDraw(canvas, tileBox, drawSettings);
				canvas.restore();
			} catch (IndexOutOfBoundsException e) {
				// skip it
			}
		}
		if (showMapPosition || animatedDraggingThread.isAnimatingZoom()) {
			drawMapPosition(canvas, c.x, c.y);
		} else if (multiTouchSupport.isInZoomMode()) {
			drawMapPosition(canvas, multiTouchSupport.getCenterPoint().x, multiTouchSupport.getCenterPoint().y);
		} else if (doubleTapScaleDetector.isInZoomMode()) {
			drawMapPosition(canvas, doubleTapScaleDetector.getCenterX(), doubleTapScaleDetector.getCenterY());
		}
	}


	protected void drawMapPosition(Canvas canvas, float x, float y) {
		canvas.drawCircle(x, y, 3 * dm.density, paintCenter);
		canvas.drawCircle(x, y, 7 * dm.density, paintCenter);
	}

	private void refreshBufferImage(final DrawSettings drawSettings) {
		if (mapRenderer != null) {
			return;
		}
		if (!baseHandler.hasMessages(BASE_REFRESH_MESSAGE) || drawSettings.isUpdateVectorRendering()) {
			Message msg = Message.obtain(baseHandler, new Runnable() {
				@Override
				public void run() {
					baseHandler.removeMessages(BASE_REFRESH_MESSAGE);
					try {
						DrawSettings param = drawSettings;
						Boolean currentNightMode = nightMode;
						if (currentNightMode != null && currentNightMode != param.isNightMode()) {
							param = new DrawSettings(currentNightMode, true);
							resetDefaultColor();
						}
						if (handler.hasMessages(MAP_FORCE_REFRESH_MESSAGE)) {
							if (!param.isUpdateVectorRendering()) {
								param = new DrawSettings(drawSettings.isNightMode(), true);
							}
							handler.removeMessages(MAP_FORCE_REFRESH_MESSAGE);
						}
						refreshBaseMapInternal(currentViewport.copy(), param);
						sendRefreshMapMsg(param, 0);
					} catch (Exception e) {
						LOG.error(e.getMessage(), e);
					}
				}
			});
			msg.what = drawSettings.isUpdateVectorRendering() ? MAP_FORCE_REFRESH_MESSAGE : BASE_REFRESH_MESSAGE;
			// baseHandler.sendMessageDelayed(msg, 0);
			baseHandler.sendMessage(msg);
		}
	}

	// this method could be called in non UI thread
	public void refreshMap() {
		refreshMap(false);
	}

	// this method could be called in non UI thread
	public void refreshMap(final boolean updateVectorRendering) {
		if (view != null && view.isShown()) {
			boolean nightMode = application.getDaynightHelper().isNightMode();
			Boolean currentNightMode = this.nightMode;
			boolean forceUpdateVectorDrawing = currentNightMode != null && currentNightMode != nightMode;
			if (forceUpdateVectorDrawing) {
				resetDefaultColor();
			}
			this.nightMode = nightMode;
			DrawSettings drawSettings = new DrawSettings(nightMode, updateVectorRendering || forceUpdateVectorDrawing);
			sendRefreshMapMsg(drawSettings, 20);
			refreshBufferImage(drawSettings);
		}
	}

	private void sendRefreshMapMsg(final DrawSettings drawSettings, int delay) {
		if (!handler.hasMessages(MAP_REFRESH_MESSAGE) || drawSettings.isUpdateVectorRendering()) {
			Message msg = Message.obtain(handler, new Runnable() {
				@Override
				public void run() {
					DrawSettings param = drawSettings;
					handler.removeMessages(MAP_REFRESH_MESSAGE);

					refreshMapInternal(param);
				}
			});
			msg.what = MAP_REFRESH_MESSAGE;
			if (delay > 0) {
				handler.sendMessageDelayed(msg, delay);
			} else {
				handler.sendMessage(msg);
			}
		}
	}

	@Override
	public void tileDownloaded(DownloadRequest request) {
		// force to refresh map because image can be loaded from different threads
		// and threads can block each other especially for sqlite images when they
		// are inserting into db they block main thread
		refreshMap();
	}

	// ///////////////////////////////// DRAGGING PART ///////////////////////////////////////

	public net.osmand.data.RotatedTileBox getCurrentRotatedTileBox() {
		return currentViewport;
	}

	public float getDensity() {
		return currentViewport.getDensity();
	}

	public float getScaleCoefficient() {
		float scaleCoefficient = getDensity();
		if (Math.min(dm.widthPixels / (dm.density * 160), dm.heightPixels / (dm.density * 160)) > 2.5f) {
			// large screen
			scaleCoefficient *= 1.5f;
		}
		return scaleCoefficient;
	}

	/**
	 * These methods do not consider rotating
	 */
	protected void dragToAnimate(float fromX, float fromY, float toX, float toY, boolean notify) {
		float dx = (fromX - toX);
		float dy = (fromY - toY);
		moveTo(dx, dy);
		if (locationListener != null && notify) {
			locationListener.locationChanged(getLatitude(), getLongitude(), this);
		}
	}

	protected void rotateToAnimate(float rotate) {
		if (isMapRotateEnabled()) {
			this.rotate = MapUtils.unifyRotationTo360(rotate);
			currentViewport.setRotate(this.rotate);
			refreshMap();
		}
	}

	protected void setLatLonAnimate(double latitude, double longitude, boolean notify) {
		currentViewport.setLatLonCenter(latitude, longitude);
		refreshMap();
		if (locationListener != null && notify) {
			locationListener.locationChanged(latitude, longitude, this);
		}
	}

	protected void setFractionalZoom(int zoom, double zoomPart, boolean notify) {
		currentViewport.setZoomAndAnimation(zoom, 0, zoomPart);
		refreshMap();
		if (locationListener != null && notify) {
			locationListener.locationChanged(getLatitude(), getLongitude(), this);
		}
	}

	// for internal usage
	protected void zoomToAnimate(int zoom, double zoomToAnimate, boolean notify) {
		if (mainLayer != null && getMaxZoom() >= zoom && getMinZoom() <= zoom) {
			currentViewport.setZoomAndAnimation(zoom, zoomToAnimate);
			if (zoom <= LOWEST_ZOOM_TO_ROTATE) {
				rotate = 0;
			}
			currentViewport.setRotate(rotate);
			refreshMap();
			if (notify && locationListener != null) {
				locationListener.locationChanged(getLatitude(), getLongitude(), this);
			}
		}
	}

	public void moveTo(float dx, float dy) {
		final QuadPoint cp = currentViewport.getCenterPixelPoint();
		final LatLon latlon = currentViewport.getLatLonFromPixel(cp.x + dx, cp.y + dy);
		currentViewport.setLatLonCenter(latlon.getLatitude(), latlon.getLongitude());
		refreshMap();
		// do not notify here listener

	}

	public void fitRectToMap(double left, double right, double top, double bottom,
							 int tileBoxWidthPx, int tileBoxHeightPx, int marginTopPx) {
		fitRectToMap(left, right, top, bottom, tileBoxWidthPx, tileBoxHeightPx, marginTopPx, 0);
	}

	public void fitRectToMap(double left, double right, double top, double bottom,
							 int tileBoxWidthPx, int tileBoxHeightPx, int marginTopPx, int marginLeftPx) {
		RotatedTileBox tb = currentViewport.copy();
		double border = 0.8;
		int dx = 0;
		int dy = 0;

		int tbw = (int) (tb.getPixWidth() * border);
		int tbh = (int) (tb.getPixHeight() * border);
		if (tileBoxWidthPx > 0) {
			tbw = (int) (tileBoxWidthPx * border);
			if (marginLeftPx > 0) {
				dx = (tb.getPixWidth() - tileBoxWidthPx) / 2 - marginLeftPx;
			}
		} else if (tileBoxHeightPx > 0) {
			tbh = (int) (tileBoxHeightPx * border);
			dy = (tb.getPixHeight() - tileBoxHeightPx) / 2 - marginTopPx;
		}
		tb.setPixelDimensions(tbw, tbh);

		double clat = bottom / 2 + top / 2;
		double clon = left / 2 + right / 2;
		tb.setLatLonCenter(clat, clon);
		while (tb.getZoom() < 17 && tb.containsLatLon(top, left) && tb.containsLatLon(bottom, right)) {
			tb.setZoom(tb.getZoom() + 1);
		}
		while (tb.getZoom() >= 7 && (!tb.containsLatLon(top, left) || !tb.containsLatLon(bottom, right))) {
			tb.setZoom(tb.getZoom() - 1);
		}
		if (dy != 0 || dx != 0) {
			clat = tb.getLatFromPixel(tb.getPixWidth() / 2, tb.getPixHeight() / 2 + dy);
			clon = tb.getLonFromPixel(tb.getPixWidth() / 2 + dx, tb.getPixHeight() / 2);
		}
		animatedDraggingThread.startMoving(clat, clon, tb.getZoom(), true);
	}

	public RotatedTileBox getTileBox(int tileBoxWidthPx, int tileBoxHeightPx, int marginTopPx) {
		RotatedTileBox tb = currentViewport.copy();
		double border = 0.8;
		int dy = 0;

		int tbw = (int) (tb.getPixWidth() * border);
		int tbh = (int) (tb.getPixHeight() * border);
		if (tileBoxWidthPx > 0) {
			tbw = (int) (tileBoxWidthPx * border);
		} else if (tileBoxHeightPx > 0) {
			tbh = (int) (tileBoxHeightPx * border);
			dy = (tb.getPixHeight() - tileBoxHeightPx) / 2 - marginTopPx;
		}
		tb.setPixelDimensions(tbw, tbh);

		if (dy != 0) {
			double clat = tb.getLatFromPixel(tb.getPixWidth() / 2, tb.getPixHeight() / 2 - dy);
			double clon = tb.getLonFromPixel(tb.getPixWidth() / 2, tb.getPixHeight() / 2);
			tb.setLatLonCenter(clat, clon);
		}
		return tb;
	}

	public void fitLocationToMap(double clat, double clon, int zoom,
								 int tileBoxWidthPx, int tileBoxHeightPx, int marginTopPx, boolean animated) {
		RotatedTileBox tb = currentViewport.copy();
		int dy = 0;

		int tbw = tb.getPixWidth();
		int tbh = tb.getPixHeight();
		if (tileBoxWidthPx > 0) {
			tbw = tileBoxWidthPx;
		} else if (tileBoxHeightPx > 0) {
			tbh = tileBoxHeightPx;
			dy = (tb.getPixHeight() - tileBoxHeightPx) / 2 - marginTopPx;
		}
		tb.setPixelDimensions(tbw, tbh);
		tb.setLatLonCenter(clat, clon);
		tb.setZoom(zoom);
		if (dy != 0) {
			clat = tb.getLatFromPixel(tb.getPixWidth() / 2, tb.getPixHeight() / 2 + dy);
			clon = tb.getLonFromPixel(tb.getPixWidth() / 2, tb.getPixHeight() / 2);
		}
		if (animated) {
			animatedDraggingThread.startMoving(clat, clon, tb.getZoom(), true);
		} else {
			setLatLon(clat, clon);
		}
	}

	public boolean onGenericMotionEvent(MotionEvent event) {
		if ((event.getSource() & InputDevice.SOURCE_CLASS_POINTER) != 0 &&
				event.getAction() == MotionEvent.ACTION_SCROLL &&
				event.getAxisValue(MotionEvent.AXIS_VSCROLL) != 0) {
			final RotatedTileBox tb = getCurrentRotatedTileBox();
			final double lat = tb.getLatFromPixel(event.getX(), event.getY());
			final double lon = tb.getLonFromPixel(event.getX(), event.getY());
			int zoomDir = event.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0 ? -1 : 1;
			getAnimatedDraggingThread().startMoving(lat, lon, getZoom() + zoomDir, true);
			return true;
		}
		return false;
	}

	public boolean onTouchEvent(MotionEvent event) {
		if (mapRenderer != null) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				mapRenderer.suspendSymbolsUpdate();
			} else if (event.getAction() == MotionEvent.ACTION_UP
					|| event.getAction() == MotionEvent.ACTION_CANCEL) {
				mapRenderer.resumeSymbolsUpdate();
			}
		}
		if (twoFingersTapDetector.onTouchEvent(event)) {
			ContextMenuLayer contextMenuLayer = getLayerByClass(ContextMenuLayer.class);
			if (contextMenuLayer != null) {
				contextMenuLayer.onTouchEvent(event, getCurrentRotatedTileBox());
			}
			return true;
		}

		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			animatedDraggingThread.stopAnimating();
		}
		final boolean isMultiTouch = multiTouchSupport.onTouchEvent(event);
		doubleTapScaleDetector.onTouchEvent(event);
		if (!isMultiTouch && !doubleTapScaleDetector.isInZoomMode()) {
			for (int i = layers.size() - 1; i >= 0; i--) {
				layers.get(i).onTouchEvent(event, getCurrentRotatedTileBox());
			}
		}
		if (!doubleTapScaleDetector.isInZoomMode() && !doubleTapScaleDetector.isDoubleTapping()) {
			gestureDetector.onTouchEvent(event);
		}
		return true;
	}

	public void setMapRender(MapRendererView mapRenderer) {
		this.mapRenderer = mapRenderer;
	}

	public MapRendererView getMapRenderer() {
		return mapRenderer;
	}

	public Boolean onTrackballEvent(MotionEvent event) {
		if (trackBallDelegate != null) {
			return trackBallDelegate.onTrackBallEvent(event);
		}
		return null;
	}

	public void setTrackBallDelegate(OnTrackBallListener trackBallDelegate) {
		this.trackBallDelegate = trackBallDelegate;
	}

	public void setOnLongClickListener(OnLongClickListener l) {
		this.onLongClickListener = l;
	}

	public void setOnClickListener(OnClickListener l) {
		this.onClickListener = l;
	}

	public void setAccessibilityActions(AccessibilityActionsProvider actions) {
		accessibilityActions = actions;
	}


	public AnimateDraggingMapThread getAnimatedDraggingThread() {
		return animatedDraggingThread;
	}

	public void showMessage(final String msg) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(application, msg, Toast.LENGTH_SHORT).show(); //$NON-NLS-1$
			}
		});
	}


	private class MapTileViewMultiTouchZoomListener implements MultiTouchZoomListener,
			DoubleTapScaleDetector.DoubleTapZoomListener {
		private PointF initialMultiTouchCenterPoint;
		private RotatedTileBox initialViewport;
		private float x1;
		private float y1;
		private float x2;
		private float y2;
		private LatLon initialCenterLatLon;
		private boolean startRotating = false;
		private static final float ANGLE_THRESHOLD = 30;

		@Override
		public void onZoomOrRotationEnded(double relativeToStart, float angleRelative) {
			// 1.5 works better even on dm.density=1 devices
			float dz = (float) (Math.log(relativeToStart) / Math.log(2)) * 1.5f;
			setIntZoom(Math.round(dz) + initialViewport.getZoom());
			if (Math.abs(angleRelative) < ANGLE_THRESHOLD * relativeToStart ||
					Math.abs(angleRelative) < ANGLE_THRESHOLD / relativeToStart) {
				angleRelative = 0;
			}
			rotateToAnimate(initialViewport.getRotate() + angleRelative);
			final int newZoom = getZoom();
			if (application.accessibilityEnabled()) {
				if (newZoom != initialViewport.getZoom()) {
					showMessage(application.getString(R.string.zoomIs) + " " + newZoom); //$NON-NLS-1$
				} else {
					final LatLon p1 = initialViewport.getLatLonFromPixel(x1, y1);
					final LatLon p2 = initialViewport.getLatLonFromPixel(x2, y2);
					showMessage(OsmAndFormatter.getFormattedDistance((float) MapUtils.getDistance(p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude()), application));
				}
			}
		}

		@Override
		public void onZoomEnded(double relativeToStart) {
			// 1.5 works better even on dm.density=1 devices
			float dz = (float) ((relativeToStart - 1) * DoubleTapScaleDetector.SCALE_PER_SCREEN);
			setIntZoom(Math.round(dz) + initialViewport.getZoom());
			final int newZoom = getZoom();
			if (application.accessibilityEnabled()) {
				if (newZoom != initialViewport.getZoom()) {
					showMessage(application.getString(R.string.zoomIs) + " " + newZoom); //$NON-NLS-1$
				} else {
					final LatLon p1 = initialViewport.getLatLonFromPixel(x1, y1);
					final LatLon p2 = initialViewport.getLatLonFromPixel(x2, y2);
					showMessage(OsmAndFormatter.getFormattedDistance((float) MapUtils.getDistance(p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude()), application));
				}
			}
		}


		@Override
		public void onGestureInit(float x1, float y1, float x2, float y2) {
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
			if (x1 != x2 || y1 != y2) {
				firstTouchPointLatLon = currentViewport.getLatLonFromPixel(x1, y1);
				secondTouchPointLatLon = currentViewport.getLatLonFromPixel(x2, y2);
				multiTouch = true;
				wasZoomInMultiTouch = false;
				multiTouchStartTime = System.currentTimeMillis();
			}
		}

		@Override
		public void onActionPointerUp() {
			multiTouch = false;
			if (isZooming()) {
				wasZoomInMultiTouch = true;
			} else {
				multiTouchEndTime = System.currentTimeMillis();
				wasZoomInMultiTouch = false;
			}
		}

		@Override
		public void onActionCancel() {
			multiTouch = false;
		}

		@Override
		public void onZoomStarted(PointF centerPoint) {
			initialMultiTouchCenterPoint = centerPoint;
			initialViewport = getCurrentRotatedTileBox().copy();
			initialCenterLatLon = initialViewport.getLatLonFromPixel(initialMultiTouchCenterPoint.x,
					initialMultiTouchCenterPoint.y);
			startRotating = false;
		}

		@Override
		public void onZoomingOrRotating(double relativeToStart, float relAngle) {
			double dz = (Math.log(relativeToStart) / Math.log(2)) * 1.5;
			if (Math.abs(dz) <= 0.1) {
				// keep only rotating
				dz = 0;
			}
			if (Math.abs(relAngle) < ANGLE_THRESHOLD && !startRotating) {
				relAngle = 0;
			} else {
				startRotating = true;
			}
			if (dz != 0 || relAngle != 0) {
				changeZoomPosition((float) dz, relAngle);
			}
		}

		@Override
		public void onZooming(double relativeToStart) {
			double dz = (relativeToStart - 1) * DoubleTapScaleDetector.SCALE_PER_SCREEN;
			changeZoomPosition((float) dz, 0);
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			LOG.debug("onDoubleTap getZoom()");
			if (!doubleTapScaleDetector.isInZoomMode()) {
				if (isZoomingAllowed(getZoom(), 1.1f)) {
					final RotatedTileBox tb = getCurrentRotatedTileBox();
					final double lat = tb.getLatFromPixel(e.getX(), e.getY());
					final double lon = tb.getLonFromPixel(e.getX(), e.getY());
					getAnimatedDraggingThread().startMoving(lat, lon, getZoom() + 1, true);
				}
				afterDoubleTap = true;
				return true;
			} else {
				return false;
			}
		}

		private void changeZoomPosition(float dz, float angle) {
			final RotatedTileBox calc = initialViewport.copy();
			calc.setLatLonCenter(initialCenterLatLon.getLatitude(), initialCenterLatLon.getLongitude());
			float calcRotate = calc.getRotate() + angle;
			calc.setRotate(calcRotate);
			calc.setZoomAndAnimation(initialViewport.getZoom(), dz, initialViewport.getZoomFloatPart());
			if (multiTouch) {
				wasZoomInMultiTouch = true;
			}

			final QuadPoint cp = initialViewport.getCenterPixelPoint();
			// Keep zoom center fixed or flexible
			LatLon r;
			if (multiTouchSupport.isInZoomMode()) {
				r = calc.getLatLonFromPixel(cp.x + cp.x - multiTouchSupport.getCenterPoint().x, cp.y + cp.y - multiTouchSupport.getCenterPoint().y);
			} else {
				r = calc.getLatLonFromPixel(cp.x + cp.x - initialMultiTouchCenterPoint.x, cp.y + cp.y - initialMultiTouchCenterPoint.y);
			}
			setLatLon(r.getLatitude(), r.getLongitude());

			int baseZoom = initialViewport.getZoom();
			while (initialViewport.getZoomFloatPart() + dz > 1 && isZoomingAllowed(baseZoom, dz)) {
				dz--;
				baseZoom++;
			}
			while (initialViewport.getZoomFloatPart() + dz < 0 && isZoomingAllowed(baseZoom, dz)) {
				dz++;
				baseZoom--;
			}
			if (!isZoomingAllowed(baseZoom, dz)) {
				dz = Math.signum(dz);
			}

			zoomToAnimate(baseZoom, dz, !(doubleTapScaleDetector.isInZoomMode()));
			rotateToAnimate(calcRotate);
		}

	}

	private boolean isZoomingAllowed(int baseZoom, float dz) {
		if (baseZoom > getMaxZoom()) {
			return false;
		}
		if (baseZoom > getMaxZoom() - 1 && dz > 1) {
			return false;
		}
		if (baseZoom < getMinZoom()) {
			return false;
		}
		if (baseZoom < getMinZoom() + 1 && dz < -1) {
			return false;
		}
		return true;
	}

	private class MapTileViewOnGestureListener extends SimpleOnGestureListener {
		@Override
		public boolean onDown(MotionEvent e) {
			// Facilitates better map re-linking for two finger tap zoom out
			wasMapLinkedBeforeGesture = ((MapActivity) activity).getMapViewTrackingUtilities().isMapLinkedToLocation();
			return false;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			animatedDraggingThread.startDragging(velocityX / 3, velocityY / 3,
					e1.getX(), e1.getY(), e2.getX(), e2.getY(), true);
			return true;
		}

		@Override
		public void onLongPress(MotionEvent e) {
			if (multiTouchSupport.isInZoomMode()
					|| doubleTapScaleDetector.isInZoomMode()
					|| doubleTapScaleDetector.isDoubleTapping()) {
				//	|| afterTwoFingersTap) {
				//afterTwoFingersTap = false;
				return;
			}
			if (LOG.isDebugEnabled()) {
				LOG.debug("On long click event " + e.getX() + " " + e.getY()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			PointF point = new PointF(e.getX(), e.getY());
			if ((accessibilityActions != null) && accessibilityActions.onLongClick(point, getCurrentRotatedTileBox())) {
				return;
			}
			for (int i = layers.size() - 1; i >= 0; i--) {
				if (layers.get(i).onLongPressEvent(point, getCurrentRotatedTileBox())) {
					return;
				}
			}
			if (onLongClickListener != null && onLongClickListener.onLongPressEvent(point)) {
				return;
			}
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			dragToAnimate(e2.getX() + distanceX, e2.getY() + distanceY, e2.getX(), e2.getY(), true);
			return true;
		}

		@Override
		public void onShowPress(MotionEvent e) {
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			if (doubleTapScaleDetector.isDoubleTapping() || afterDoubleTap) {
				// Needed to suppress false single tap detection if we mask MotionEvents for gestures on isDoubleTapping()
				afterDoubleTap = false;
				return true;
			}
			PointF point = new PointF(e.getX(), e.getY());
			if (LOG.isDebugEnabled()) {
				LOG.debug("On click event " + point.x + " " + point.y); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if ((accessibilityActions != null) && accessibilityActions.onClick(point, getCurrentRotatedTileBox())) {
				return true;
			}
			for (int i = layers.size() - 1; i >= 0; i--) {
				if (layers.get(i).onSingleTap(point, getCurrentRotatedTileBox())) {
					return true;
				}
			}
			if (onClickListener != null && onClickListener.onPressEvent(point)) {
				return true;
			}
			return false;
		}
	}

	public Resources getResources() {
		return application.getResources();
	}

	public Context getContext() {
		return activity;
	}
}

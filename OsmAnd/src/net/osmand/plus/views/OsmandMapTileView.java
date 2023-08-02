package net.osmand.plus.views;


import static net.osmand.plus.views.layers.base.BaseMapLayer.DEFAULT_MAX_ZOOM;
import static net.osmand.plus.views.layers.base.BaseMapLayer.DEFAULT_MIN_ZOOM;

import android.annotation.SuppressLint;
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
import android.os.Build;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.MapAnimator;
import net.osmand.core.jni.PointD;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.ZoomLevel;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.data.QuadPointDouble;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.RotatedTileBox.RotatedTileBoxBuilder;
import net.osmand.map.IMapLocationListener;
import net.osmand.map.MapTileDownloader.DownloadRequest;
import net.osmand.map.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.AppInitializer.AppInitializeListener;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.auto.CarSurfaceView;
import net.osmand.plus.auto.NavigationSession;
import net.osmand.plus.auto.SurfaceRenderer;
import net.osmand.plus.helpers.TwoFingerTapDetector;
import net.osmand.plus.measurementtool.MeasurementToolLayer;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.accessibility.AccessibilityActionsProvider;
import net.osmand.plus.plugins.weather.WeatherPlugin;
import net.osmand.plus.render.OsmandRenderer;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.CompassMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.DoubleTapScaleDetector.DoubleTapZoomListener;
import net.osmand.plus.views.MultiTouchSupport.MultiTouchZoomListener;
import net.osmand.plus.views.layers.ContextMenuLayer;
import net.osmand.plus.views.layers.base.BaseMapLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.layers.base.OsmandMapLayer.MapGestureType;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRuleStorageProperties;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OsmandMapTileView implements IMapDownloaderCallback {

	public static final float DEFAULT_ELEVATION_ANGLE = 90;
	public static final int MAP_DEFAULT_COLOR = 0xffebe7e4;

	private static final int SHOW_POSITION_MSG_ID = OsmAndConstants.UI_HANDLER_MAP_VIEW + 1;
	private static final int MAP_REFRESH_MESSAGE = OsmAndConstants.UI_HANDLER_MAP_VIEW + 4;
	private static final int MAP_FORCE_REFRESH_MESSAGE = OsmAndConstants.UI_HANDLER_MAP_VIEW + 5;
	private static final int BASE_REFRESH_MESSAGE = OsmAndConstants.UI_HANDLER_MAP_VIEW + 3;
	private static final int MIN_ZOOM_LIMIT = 1;
	private static final int MAX_ZOOM_LIMIT = 17;

	private boolean MEASURE_FPS;
	private final FPSMeasurement main = new FPSMeasurement();
	private final FPSMeasurement additional = new FPSMeasurement();

	private View view;
	private final Context ctx;
	private MapActivity mapActivity;
	private OsmandApplication application;
	protected OsmandSettings settings;

	private CanvasColors canvasColors;
	private Boolean nightMode;

	private static class CanvasColors {
		int colorDay = MAP_DEFAULT_COLOR;
		int colorNight = MAP_DEFAULT_COLOR;
	}

	private static class FPSMeasurement {
		int fpsMeasureCount;
		int fpsMeasureMs;
		long fpsFirstMeasurement;
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

	public interface OnTrackBallListener {
		boolean onTrackBallEvent(MotionEvent e);
	}

	public interface OnLongClickListener {
		boolean onLongPressEvent(PointF point);
	}

	public interface OnClickListener {
		boolean onPressEvent(PointF point);
	}

	public interface OnDrawMapListener {
		void onDrawOverMap();
	}

	protected static final Log LOG = PlatformUtil.getLog(OsmandMapTileView.class);

	private RotatedTileBox currentViewport;

	private float rotate; // accumulate

	private int mapPosition;
	private int mapPositionX;

	private float customMapRatioX;
	private float customMapRatioY;

	private boolean showMapPosition = true;

	private List<IMapLocationListener> locationListeners = new ArrayList<>();
	private List<ElevationListener> elevationListeners = new ArrayList<>();

	private OnLongClickListener onLongClickListener;

	private OnClickListener onClickListener;

	private OnTrackBallListener trackBallDelegate;

	private AccessibilityActionsProvider accessibilityActions;

	private final List<OsmandMapLayer> layersLegacy = new ArrayList<>();
	private final List<OsmandMapLayer> layersOpenGL = new ArrayList<>();

	private BaseMapLayer mainLayer;

	private final Map<OsmandMapLayer, Float> zOrdersLegacy = new HashMap<>();
	private final Map<OsmandMapLayer, Float> zOrdersOpenGL = new HashMap<>();

	private OnDrawMapListener onDrawMapListener;

	// UI Part
	// handler to refresh map (in ui thread - ui thread is not necessary, but msg queue is required).
	protected Handler handler;
	private Handler baseHandler;

	private AnimateDraggingMapThread animatedDraggingThread;
	private AnimateMapMarkersThread animatedMapMarkersThread;

	Paint paintGrayFill;
	Paint paintBlackFill;
	Paint paintWhiteFill;
	Paint paintCenter;

	private DisplayMetrics dm;
	private volatile MapRendererView mapRenderer;

	private Bitmap bufferBitmap;
	private RotatedTileBox bufferImgLoc;
	private Bitmap bufferBitmapTmp;

	private Paint paintImg;

	@Nullable
	private GestureDetector gestureDetector;
	@Nullable
	private MultiTouchSupport multiTouchSupport;
	@Nullable
	private DoubleTapScaleDetector doubleTapScaleDetector;
	@Nullable
	private TwoFingerTapDetector twoFingersTapDetector;
	//private boolean afterTwoFingersTap = false;
	private boolean afterDoubleTap;
	private boolean wasMapLinkedBeforeGesture;

	private LatLon firstTouchPointLatLon;
	private LatLon secondTouchPointLatLon;
	private boolean multiTouch;
	private long multiTouchStartTime;
	private long multiTouchEndTime;
	private boolean wasZoomInMultiTouch;
	private float elevationAngle;
	private boolean targetChanged;
	private int targetPixelX;
	private int targetPixelY;
	private int firstTouchLocationX;
	private int firstTouchLocationY;
	private float firstTouchLocationHeight;
	private int secondTouchLocationX;
	private int secondTouchLocationY;
	private float secondTouchLocationHeight;

	public OsmandMapTileView(@NonNull Context ctx, int width, int height) {
		this.ctx = ctx;
		init(ctx, width, height);
	}

	// ///////////////////////////// INITIALIZING UI PART ///////////////////////////////////
	public void init(@NonNull Context ctx, int width, int height) {
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
		animatedMapMarkersThread = new AnimateMapMarkersThread(this);

		WindowManager mgr = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
		dm = new DisplayMetrics();
		mgr.getDefaultDisplay().getMetrics(dm);
		LatLon ll = settings.getLastKnownMapLocation();
		currentViewport = new RotatedTileBoxBuilder()
				.setLocation(ll.getLatitude(), ll.getLongitude())
				.setZoom(settings.getLastKnownMapZoom())
				.setZoomFloatPart(settings.getLastKnownMapZoomFloatPart())
				.setRotate(settings.getLastKnownMapRotation())
				.setPixelDimensions(width, height)
				.build();
		currentViewport.setDensity(dm.density);
		setMapDensityImpl(getSettingsMapDensity());
		elevationAngle = settings.getLastKnownMapElevation();
	}

	@Nullable
	public MapActivity getMapActivity() {
		return mapActivity;
	}

	public void setMapActivity(@Nullable MapActivity mapActivity) {
		this.mapActivity = mapActivity;
	}

	public void setupTouchDetectors(@NonNull Context ctx) {
		gestureDetector = new GestureDetector(ctx, new MapTileViewOnGestureListener());
		multiTouchSupport = new MultiTouchSupport(application, new MapTileViewMultiTouchZoomListener());
		doubleTapScaleDetector = new DoubleTapScaleDetector(this, ctx, new MapTileViewMultiTouchZoomListener());
		twoFingersTapDetector = new TwoFingerTapDetector() {
			@Override
			public void onTwoFingerTap() {
				//afterTwoFingersTap = true;
				if (!mapGestureAllowed(MapGestureType.TWO_POINTERS_ZOOM_OUT)) {
					return;
				}

				Zoom zoom = new Zoom(getZoom(), getZoomFloatPart(), getMinZoom(), getMaxZoom());
				if (zoom.isZoomOutAllowed()) {
					zoom.zoomOut();
					getAnimatedDraggingThread().startZooming(zoom.getBaseZoom(), zoom.getZoomFloatPart(), null, false);
					if (wasMapLinkedBeforeGesture) {
						application.getMapViewTrackingUtilities().setMapLinkedToLocation(true);
					}
				}
			}
		};
	}

	public void clearTouchDetectors() {
		gestureDetector = null;
		multiTouchSupport = null;
		doubleTapScaleDetector = null;
		twoFingersTapDetector = null;
	}

	@NonNull
	public MapActivity requireMapActivity() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			throw new IllegalStateException(this + " not attached to MapActivity.");
		}
		return mapActivity;
	}

	public RotatedTileBox getRotatedTileBox() {
		return currentViewport.copy();
	}

	public void setView(@Nullable View view) {
		this.view = view;
		if (view != null) {
			view.setClickable(true);
			view.setLongClickable(true);
			view.setFocusable(true);
			if (Build.VERSION.SDK_INT >= 26) {
				view.setDefaultFocusHighlightEnabled(false);
			}
			refreshMap(true);
		}
	}

	public void setupRenderingView() {
		if (application.isApplicationInitializing()) {
			application.getAppInitializer().addListener(new AppInitializeListener() {
				@Override
				public void onFinish(@NonNull AppInitializer init) {
					application.getOsmandMap().setupRenderingView();
					application.getOsmandMap().refreshMap();
				}
			});
		} else {
			application.getOsmandMap().setupRenderingView();
		}
	}

	public void backToLocation() {
		application.getMapViewTrackingUtilities().backToLocationImpl();
	}

	public void zoomOut() {
		application.getOsmandMap().changeZoom(-1, System.currentTimeMillis());
	}

	public void zoomIn() {
		application.getOsmandMap().changeZoom(1, System.currentTimeMillis());
	}

	public void scrollMap(float dx, float dy) {
		moveTo(dx, dy, true);
	}

	public void flingMap(float x, float y, float velocityX, float velocityY) {
		animatedDraggingThread.startDragging(velocityX / 3, velocityY / 3,
				0, 0, x, y, true);
	}

	public Boolean onKeyDown(int keyCode, KeyEvent event) {
		return application.accessibilityEnabled() ? false : null;
	}

	public synchronized boolean isLayerExists(@NonNull OsmandMapLayer layer) {
		if (isUseOpenGL()) {
			return layersOpenGL.contains(layer);
		} else {
			return layersLegacy.contains(layer);
		}
	}

	public float getZorder(@NonNull OsmandMapLayer layer) {
		Float z = isUseOpenGL() ? zOrdersOpenGL.get(layer) : zOrdersLegacy.get(layer);
		if (z == null) {
			return 10;
		}
		return z;
	}

	public int getLayerIndex(@NonNull OsmandMapLayer layer) {
		float zOrder = getZorder(layer);
		return (int) (zOrder * 100.0f);
	}

	public synchronized void addLayer(@NonNull OsmandMapLayer layer, float zOrder) {
		addLayer(layer, zOrder, zOrder);
	}

	public synchronized void addLayer(@NonNull OsmandMapLayer layer, float zOrderLegacy, float zOrderOpenGL) {
		int i = 0;
		for (i = 0; i < layersLegacy.size(); i++) {
			if (zOrdersLegacy.get(layersLegacy.get(i)) > zOrderLegacy) {
				break;
			}
		}
		zOrdersLegacy.put(layer, zOrderLegacy);
		layersLegacy.add(i, layer);

		i = 0;
		for (i = 0; i < layersOpenGL.size(); i++) {
			if (zOrdersOpenGL.get(layersOpenGL.get(i)) > zOrderOpenGL) {
				break;
			}
		}
		zOrdersOpenGL.put(layer, zOrderOpenGL);
		layersOpenGL.add(i, layer);
		layer.initLayer(this);
	}

	public synchronized void removeLayer(@NonNull OsmandMapLayer layer) {
		layer.destroyLayer();
		while (layersLegacy.remove(layer)) ;
		while (layersOpenGL.remove(layer)) ;
		zOrdersLegacy.remove(layer);
		zOrdersOpenGL.remove(layer);
	}

	public List<OsmandMapLayer> getLayers() {
		if (isUseOpenGL()) {
			return layersOpenGL;
		} else {
			return layersLegacy;
		}
	}

	@SuppressWarnings("unchecked")
	public <T extends OsmandMapLayer> T getLayerByClass(Class<T> cl) {
		for (OsmandMapLayer lr : getLayers()) {
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

	@NonNull
	public OsmandApplication getApplication() {
		return application;
	}

	// ///////////////////////// NON UI PART (could be extracted in common) /////////////////////////////
	@NonNull
	public LatLon getFirstTouchPointLatLon() {
		return firstTouchPointLatLon;
	}

	@NonNull
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
		for (OsmandMapLayer layer : getLayers()) {
			if (!layer.isMapGestureAllowed(type)) {
				return false;
			}
		}
		return true;
	}

	public void setIntZoom(int zoom) {
		setZoomWithFloatPart(zoom, 0);
	}

	public void setZoomWithFloatPart(int baseZoom, float zoomFloatPart) {
		if (!isSteplessZoomSupported()) {
			zoomFloatPart = 0;
		}
		Zoom zoom = Zoom.checkZoomBounds(baseZoom, zoomFloatPart, getMinZoom(), getMaxZoom());
		if (mainLayer != null) {
			animatedDraggingThread.stopAnimating();
			setZoomAndAnimationImpl(zoom.getBaseZoom(), 0, zoom.getZoomFloatPart());
			setRotateImpl(rotate);
			refreshMap();
		}
	}

	public void setComplexZoom(int zoom, double mapDensity) {
		if (mainLayer != null && zoom <= getMaxZoom() && zoom >= getMinZoom()) {
			animatedDraggingThread.stopAnimating();
			setZoomAndAnimationImpl(zoom, 0);
			setMapDensityImpl(mapDensity);
			setRotateImpl(rotate);
			refreshMap();
		}
	}

	public void setMapDensity(double mapDensity) {
		if (mainLayer != null) {
			setMapDensityImpl(mapDensity);
		}
	}

	public void initMapRotationByCompassMode() {
		CompassMode compassMode = settings.getCompassMode();
		if (compassMode == CompassMode.NORTH_IS_UP) {
			resetRotation();
		} else if (compassMode == CompassMode.MANUALLY_ROTATED) {
			restoreManualMapRotation();
		} else if (compassMode == CompassMode.MOVEMENT_DIRECTION) {
			restoreLastKnownMapRotation();
		}
	}

	private void resetRotation() {
		setRotate(0, true);
	}

	private void restoreManualMapRotation() {
		float manualMapRotation = settings.getManuallyMapRotation();
		setRotate(manualMapRotation, true);
	}

	private void restoreLastKnownMapRotation() {
		if (settings.ROTATE_MAP.get() != OsmandSettings.ROTATE_MAP_COMPASS) {
			float lastKnownMapRotation = settings.getLastKnownMapRotation();
			setRotate(lastKnownMapRotation, true);
		}
	}

	public void setRotate(float rotate, boolean force) {
		if (multiTouch || Float.isNaN(rotate)) {
			return;
		}
		MapRendererView mapRenderer = getMapRenderer();
		float diff = MapUtils.unifyRotationDiff(rotate, mapRenderer != null ? mapActivity.getMapRotateTarget() : getRotate());
		if (Math.abs(diff) > 5 || force) { // check smallest rotation
			animatedDraggingThread.startRotate(rotate);
		}
	}

	public boolean isShowMapPosition() {
		return showMapPosition;
	}

	public void setShowMapPosition(boolean showMapPosition) {
		this.showMapPosition = showMapPosition;
	}

	public void showAndHideMapPosition() {
		setShowMapPosition(true);
		getApplication().runMessageInUIThreadAndCancelPrevious(SHOW_POSITION_MSG_ID, () -> {
			if (isShowMapPosition()) {
				setShowMapPosition(false);
				refreshMap();
			}
		}, 2500);
	}

	public float getRotate() {
		return currentViewport.getRotate();
	}

	public void setLatLon(double latitude, double longitude) {
		setLatLon(latitude, longitude, false);
	}

	public void setLatLon(LatLon mapLocation, float heightInMeters, LatLon mapLocationShifted) {
		if (!animatedDraggingThread.isAnimatingMapTilt()) {
			animatedDraggingThread.stopAnimating();
		}
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			PointI target31 = new PointI(MapUtils.get31TileNumberX(mapLocation.getLongitude()),
					MapUtils.get31TileNumberY(mapLocation.getLatitude()));
			mapRenderer.setTarget(target31, heightInMeters);
		}
		if (heightInMeters == 0.0f)
			currentViewport.setLatLonCenter(mapLocation.getLatitude(), mapLocation.getLongitude());
		else {
			currentViewport.setLatLonCenter(mapLocationShifted.getLatitude(), mapLocationShifted.getLongitude());
			animatedDraggingThread.invalidateMapTarget();
		}
		refreshMap();
	}

	public void setLatLon(double latitude, double longitude, float ratiox, float ratioy) {
		setLatLon(latitude, longitude, ratiox, ratioy, false);
	}

	public void setTarget31(int x31, int y31) {
		setTarget31(x31, y31, false);
	}

	public void setLatLon(double latitude, double longitude, boolean notify) {
		if (!animatedDraggingThread.isAnimatingMapTilt()) {
			animatedDraggingThread.stopAnimating();
		}
		setLatLonImpl(latitude, longitude);
		refreshMap();
		if (notify) {
			notifyLocationListeners(getLatitude(), getLongitude());
		}
	}

	public void setLatLon(double latitude, double longitude, float ratiox, float ratioy, boolean notify) {
		if (!animatedDraggingThread.isAnimatingMapTilt()) {
			animatedDraggingThread.stopAnimating();
		}
		setLatLonImpl(latitude, longitude, ratiox, ratioy);
		refreshMap();
		if (notify) {
			notifyLocationListeners(getLatitude(), getLongitude());
		}
	}

	public void setTarget31(int x31, int y31, boolean notify) {
		animatedDraggingThread.stopAnimating();
		setTarget31Impl(x31, y31);
		refreshMap();
		if (notify) {
			notifyLocationListeners(getLatitude(), getLongitude());
		}
	}

	public double getLatitude() {
		return currentViewport.getLatitude();
	}

	public double getLongitude() {
		return currentViewport.getLongitude();
	}

	public float getHeight() {
		float height = 0.0f;
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			height = mapRenderer.getMapTargetHeightInMeters();
		}
		return height;
	}

	public LatLon getTargetLatLon(LatLon coordinates) {
		LatLon result = coordinates;
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			PointI target31 = mapRenderer.getState().getTarget31();
			result = new LatLon(MapUtils.get31LatitudeY(target31.getY()), MapUtils.get31LongitudeX(target31.getX()));
		}
		return result;
	}

	public int getZoom() {
		return currentViewport.getZoom();
	}

	public boolean isPinchZoomingOrRotating() {
		return multiTouchSupport != null && multiTouchSupport.isInZoomAndRotationMode();
	}

	public float getElevationAngle() {
		return elevationAngle;
	}

	public float getZoomFloatPart() {
		return (float) currentViewport.getZoomFloatPart();
	}

	public float getZoomAnimation() {
		return (float) currentViewport.getZoomAnimation();
	}

	public double getSettingsMapDensity() {
		OsmandMap map = application.getOsmandMap();
		return (map != null ? map.getMapDensity() : getSettings().MAP_DENSITY.get())
				* Math.max(1, getDensity());
	}


	public boolean isZooming() {
		return currentViewport.isZoomAnimated();
	}

	public void addMapLocationListener(@NonNull IMapLocationListener listener) {
		if (!locationListeners.contains(listener)) {
			locationListeners = Algorithms.addToList(locationListeners, listener);
		}
	}

	public void removeMapLocationListener(@NonNull IMapLocationListener listener) {
		locationListeners = Algorithms.removeFromList(locationListeners, listener);
	}

	public void addElevationListener(@NonNull ElevationListener listener) {
		if (!elevationListeners.contains(listener)) {
			elevationListeners = Algorithms.addToList(elevationListeners, listener);
		}
	}

	public void removeElevationListener(@NonNull ElevationListener listener) {
		elevationListeners = Algorithms.removeFromList(elevationListeners, listener);
	}

	public void setOnDrawMapListener(@Nullable OnDrawMapListener listener) {
		this.onDrawMapListener = listener;
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
		setZoomAndAnimationImpl(zoom, 0, 0);
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
		this.customMapRatioX = ratioX;
		this.customMapRatioY = ratioY;
	}

	public void restoreMapRatio() {
		customMapRatioX = 0;
		customMapRatioY = 0;
	}

	public void restoreScreenCenter() {
		restoreMapRatio();
		RotatedTileBox box = currentViewport.copy();
		LatLon screenCenter = NativeUtilities.getLatLonFromElevatedPixel(mapRenderer, box,
				box.getPixWidth() / 2f, box.getPixHeight() / 2f);
		PointF ratio = calculateRatio();
		setLatLon(screenCenter.getLatitude(), screenCenter.getLongitude(), ratio.x, ratio.y);
	}

	public boolean hasCustomMapRatio() {
		return customMapRatioX != 0 && customMapRatioY != 0;
	}

	public OsmandSettings getSettings() {
		return settings;
	}

	public int getMaxZoom() {
		int customizedZoom = application.getAppCustomization().getMaxZoom();
		int maxSupportedZoom = mainLayer != null ? mainLayer.getMaximumShownMapZoom() : DEFAULT_MAX_ZOOM;
		if (customizedZoom > 0) {
			return Math.min(customizedZoom, maxSupportedZoom);
		}
		return maxSupportedZoom;
	}

	public int getMinZoom() {
		int customizedZoom = application.getAppCustomization().getMinZoom();
		int minSupportedZoom = mainLayer != null ? mainLayer.getMinimumShownMapZoom() + 1 : DEFAULT_MIN_ZOOM;
		return customizedZoom > 0
				? Math.max(customizedZoom, minSupportedZoom)
				: minSupportedZoom;
	}

	public boolean isCarView() {
		return view instanceof CarSurfaceView;
	}

	public float getCarViewDensity() {
		if (view instanceof CarSurfaceView) {
			return ((CarSurfaceView) view).getDensity();
		}
		return 0;
	}

	private void drawBasemap(Canvas canvas) {
		if (bufferImgLoc != null) {
			float rot = -bufferImgLoc.getRotate();
			canvas.rotate(rot, currentViewport.getCenterPixelX(), currentViewport.getCenterPixelY());
			RotatedTileBox calc = currentViewport.copy();
			calc.setRotate(bufferImgLoc.getRotate());

			int cz = getZoom();
			QuadPointDouble lt = bufferImgLoc.getLeftTopTile(cz);
			QuadPointDouble rb = bufferImgLoc.getRightBottomTile(cz);
			float x1 = calc.getPixXFromTile(lt.x, lt.y, cz);
			float x2 = calc.getPixXFromTile(rb.x, rb.y, cz);
			float y1 = calc.getPixYFromTile(lt.x, lt.y, cz);
			float y2 = calc.getPixYFromTile(rb.x, rb.y, cz);
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

	private void refreshBaseMapInternal(@NonNull RotatedTileBox tileBox, @NonNull DrawSettings drawSettings) {
		if (tileBox.getPixHeight() == 0 || tileBox.getPixWidth() == 0 || mapRenderer != null) {
			return;
		}
		if (bufferBitmapTmp == null || tileBox.getPixHeight() != bufferBitmapTmp.getHeight()
				|| tileBox.getPixWidth() != bufferBitmapTmp.getWidth()) {
			bufferBitmapTmp = Bitmap.createBitmap(tileBox.getPixWidth(), tileBox.getPixHeight(), Config.ARGB_8888);
		}
		long start = SystemClock.elapsedRealtime();
		QuadPoint c = tileBox.getCenterPixelPoint();
		Canvas canvas = new Canvas(bufferBitmapTmp);
		fillCanvas(canvas, drawSettings);
		List<OsmandMapLayer> layers = getLayers();
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

	@NonNull
	private PointF calculateRatio() {
		float ratioy = customMapRatioY != 0 ? customMapRatioY : getDefaultRatioY();

		float ratiox;
		if (customMapRatioX != 0) {
			ratiox = customMapRatioX;
		} else if (mapPosition == OsmandSettings.LANDSCAPE_MIDDLE_RIGHT_CONSTANT) {
			ratiox = 0.7f;
		} else {
			ratiox = mapPositionX == 0 ? 0.5f : (isLayoutRtl() ? 0.25f : 0.75f);
		}
		return new PointF(ratiox, ratioy);
	}

	public float getDefaultRatioY() {
		if (mapPosition == OsmandSettings.BOTTOM_CONSTANT) {
			return 0.85f;
		} else if (mapPosition == OsmandSettings.MIDDLE_BOTTOM_CONSTANT) {
			return 0.70f;
		} else if (mapPosition == OsmandSettings.MIDDLE_TOP_CONSTANT) {
			return 0.25f;
		} else {
			return 0.5f;
		}
	}

	private void refreshMapInternal(@NonNull DrawSettings drawSettings) {
		if (view == null) {
			return;
		}
		PointF ratio = calculateRatio();
		int cy = (int) (ratio.y * view.getHeight());
		int cx = (int) (ratio.x * view.getWidth());
		boolean updateMapRenderer = false;
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			PointI fixedPixel = mapRenderer.getState().getFixedPixel();
			updateMapRenderer = fixedPixel.getX() < 0 || fixedPixel.getY() < 0;
		}
		if (updateMapRenderer || currentViewport.getPixWidth() != view.getWidth() || currentViewport.getPixHeight() != view.getHeight() ||
				currentViewport.getCenterPixelY() != cy || currentViewport.getCenterPixelX() != cx) {
			currentViewport.setPixelDimensions(view.getWidth(), view.getHeight(), ratio.x, ratio.y);
			if (mapRenderer != null) {
				mapRenderer.setMapTarget(new PointI(cx, cy), mapRenderer.getTarget());
			}
			setElevationAngle(elevationAngle);
			setMapDensityImpl(getSettingsMapDensity());
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
		} else if (view instanceof CarSurfaceView) {
			SurfaceRenderer renderer = ((CarSurfaceView) view).getSurfaceRenderer();
			long ms = SystemClock.elapsedRealtime();
			synchronized (renderer) {
				try {
					// make copy to avoid concurrency
					RotatedTileBox viewportToDraw = currentViewport.copy();
					renderer.renderFrame(viewportToDraw, drawSettings);
				} catch (Exception e) {
					// ignore
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

	public boolean isAnimatingMapZoom() {
		return animatedDraggingThread.isAnimatingMapZoom();
	}

	public boolean isAnimatingMapMove() {
		return animatedDraggingThread.isAnimatingMapMove();
	}

	public boolean isAnimatingMapRotation() {
		return animatedDraggingThread.isAnimatingMapRotation();
	}

	@SuppressLint("WrongCall")
	public void drawOverMap(Canvas canvas, RotatedTileBox tileBox, DrawSettings drawSettings) {
		if (mapRenderer == null) {
			fillCanvas(canvas, drawSettings);
		}
		QuadPoint c = tileBox.getCenterPixelPoint();
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

		List<OsmandMapLayer> layers = getLayers();
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
		WeatherPlugin plugin = PluginsHelper.getActivePlugin(WeatherPlugin.class);
		if (showMapPosition || animatedDraggingThread.isAnimatingMapZoom() || (plugin != null && plugin.hasCustomForecast())) {
			drawMapPosition(canvas, c.x, c.y);
		} else if (multiTouchSupport != null && multiTouchSupport.isInZoomAndRotationMode()) {
			drawMapPosition(canvas, multiTouchSupport.getCenterPoint().x, multiTouchSupport.getCenterPoint().y);
		} else if (doubleTapScaleDetector != null && doubleTapScaleDetector.isInZoomMode()) {
			drawMapPosition(canvas, doubleTapScaleDetector.getCenterX(), doubleTapScaleDetector.getCenterY());
		}
	}

	protected void drawMapPosition(Canvas canvas, float x, float y) {
		canvas.drawCircle(x, y, 3 * dm.density, paintCenter);
		canvas.drawCircle(x, y, 7 * dm.density, paintCenter);
	}

	private void refreshBufferImage(@NonNull DrawSettings drawSettings) {
		if (mapRenderer == null && (!baseHandler.hasMessages(BASE_REFRESH_MESSAGE) || drawSettings.isUpdateVectorRendering())) {
			Message msg = Message.obtain(baseHandler, () -> {
				baseHandler.removeMessages(BASE_REFRESH_MESSAGE);
				if (mapRenderer != null) {
					handler.removeMessages(MAP_FORCE_REFRESH_MESSAGE);
					return;
				}
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
					param.mapRefreshTimestamp = System.currentTimeMillis();
					refreshBaseMapInternal(currentViewport.copy(), param);
					sendRefreshMapMsg(param, 0);
				} catch (Exception e) {
					LOG.error(e.getMessage(), e);
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
	public void refreshMap(boolean updateVectorRendering) {
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

	private void sendRefreshMapMsg(@NonNull DrawSettings drawSettings, int delay) {
		if (!handler.hasMessages(MAP_REFRESH_MESSAGE) || drawSettings.isUpdateVectorRendering()) {
			Message msg = Message.obtain(handler, () -> {
				DrawSettings param = drawSettings;
				handler.removeMessages(MAP_REFRESH_MESSAGE);

				refreshMapInternal(param);
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
	@NonNull
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
		//float dx = (fromX - toX);
		//float dy = (fromY - toY);
		//moveTo(dx, dy, false);
		moveTo(fromX, fromY, toX, toY, false);
		if (notify) {
			notifyLocationListeners(getLatitude(), getLongitude());
		}
	}

	protected void dragToAnimate(int toX31, int toY31, boolean notify) {
		moveTo(toX31, toY31, false);
		if (notify) {
			notifyLocationListeners(getLatitude(), getLongitude());
		}
	}

	public void rotateToAnimate(float rotate) {
		this.rotate = MapUtils.unifyRotationTo360(rotate);
		setRotateImpl(this.rotate);
		refreshMap();
	}

	public void rotateToAnimate(float rotate, int centerX, int centerY) {
		this.rotate = MapUtils.unifyRotationTo360(rotate);
		setRotateImpl(this.rotate, centerX, centerY);
		refreshMap();
	}

	protected void setLatLonAnimate(double latitude, double longitude, boolean notify) {
		setLatLonImpl(latitude, longitude);
		refreshMap();
		if (notify) {
			notifyLocationListeners(latitude, longitude);
		}
	}

	protected void setFractionalZoom(int zoom, double zoomPart, boolean notify) {
		setZoomAndAnimationImpl(zoom, 0, zoomPart);
		refreshMap();
		if (notify) {
			notifyLocationListeners(getLatitude(), getLongitude());
		}
	}

	// for internal usage
	private void setLatLonImpl(double latitude, double longitude) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			NativeUtilities.calculateTarget31(mapRenderer, latitude, longitude, true);
		}
		currentViewport.setLatLonCenter(latitude, longitude);
	}

	private void setLatLonImpl(double latitude, double longitude, float ratiox, float ratioy) {
		int cx = (int) (ratiox * view.getWidth());
		int cy = (int) (ratioy * view.getHeight());
		if (currentViewport.getCenterPixelY() == cy && currentViewport.getCenterPixelX() == cx) {
			setLatLonImpl(latitude, longitude);
		} else {
			currentViewport.setPixelDimensions(view.getWidth(), view.getHeight(), ratiox, ratioy);
			if (mapRenderer != null) {
				PointI target31 = NativeUtilities.calculateTarget31(mapRenderer, latitude, longitude, false);
				mapRenderer.setMapTarget(new PointI(cx, cy), target31);
			}
			currentViewport.setLatLonCenter(latitude, longitude);
		}
	}

	private void setTarget31Impl(int x31, int y31) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			mapRenderer.setTarget(new PointI(x31, y31));
		}
		currentViewport.setLatLonCenter(MapUtils.get31LatitudeY(y31), MapUtils.get31LongitudeX(x31));
	}

	private void setRotateImpl(float rotate) {
		RotatedTileBox tb = currentViewport.copy();
		setRotateImpl(rotate, tb.getCenterPixelX(), tb.getCenterPixelY());
	}

	private void setRotateImpl(float rotate, int centerX, int centerY) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			int centerX31 = 0;
			int centerY31 = 0;
			PointI center31 = new PointI();
			if (mapRenderer.getLocationFromScreenPoint(new PointI(centerX, centerY), center31)) {
				centerX31 = center31.getX();
				centerY31 = center31.getY();
			}
			PointI target31 = mapRenderer.getTarget();
			float azimuth = mapRenderer.getAzimuth();
			int targetX = target31.getX() - centerX31;
			int targetY = target31.getY() - centerY31;
			double angleR = Math.toRadians(-azimuth - rotate);
			double cosAngle = Math.cos(angleR);
			double sinAngle = Math.sin(angleR);
			int newTargetX = (int) (targetX * cosAngle - targetY * sinAngle + centerX31);
			int newTargetY = (int) (targetX * sinAngle + targetY * cosAngle + centerY31);
			mapRenderer.setTarget(new PointI(newTargetX, newTargetY));
			mapRenderer.setAzimuth(-rotate);
		}
		currentViewport.setRotate(rotate);
	}

	private void setZoomAndAnimationImpl(int zoom, double zoomAnimation) {
		setZoomAndAnimationImpl(zoom, zoomAnimation, currentViewport.getZoomFloatPart());
	}

	private void setZoomAndAnimationImpl(int zoom, double zoomAnimation, int centerX, int centerY) {
		setZoomAndAnimationImpl(zoom, zoomAnimation, currentViewport.getZoomFloatPart(), centerX, centerY);
	}

	private void setZoomAndAnimationImpl(int zoom, double zoomAnimation, double zoomFloatPart) {
		RotatedTileBox tb = currentViewport.copy();
		setZoomAndAnimationImpl(zoom, zoomAnimation, zoomFloatPart, tb.getCenterPixelX(), tb.getCenterPixelY());
	}

	private void setZoomAndAnimationImpl(int zoom, double zoomAnimation, double zoomFloatPart, int centerX, int centerY) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			int centerX31Before = 0;
			int centerY31Before = 0;

			// Get map center in 31
			PointI center31 = new PointI();
			if (mapRenderer.getLocationFromScreenPoint(new PointI(centerX, centerY), center31)) {
				centerX31Before = center31.getX();
				centerY31Before = center31.getY();
			}

			// Zoom
			float finalZoomFloatPart = (float) (zoomAnimation + zoomFloatPart);
			float visualZoom = finalZoomFloatPart >= 0
					? 1 + finalZoomFloatPart
					: 1 + 0.5f * finalZoomFloatPart;
			mapRenderer.setZoom(ZoomLevel.swigToEnum(zoom), visualZoom);
			float zoomMagnifier = application.getOsmandMap().getMapDensity();
			mapRenderer.setVisualZoomShift(zoomMagnifier - 1.0f);

			// Shift map to new center
			center31 = new PointI();
			// Get new map center in 31
			if (mapRenderer.getLocationFromScreenPoint(new PointI(centerX, centerY), center31)) {
				int centerX31After = center31.getX();
				int centerY31After = center31.getY();
				PointI target31 = mapRenderer.getTarget();
				int targetX = target31.getX() - (centerX31After - centerX31Before);
				int targetY = target31.getY() - (centerY31After - centerY31Before);
				// Shift map
				mapRenderer.setTarget(new PointI(targetX, targetY));
			}
		}
		currentViewport.setZoomAndAnimation(zoom, zoomAnimation, zoomFloatPart);
		setElevationAngle(normalizeElevationAngle(this.elevationAngle));
	}

	private void setMapDensityImpl(double mapDensity) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			float zoomMagnifier = application.getOsmandMap().getMapDensity();
			mapRenderer.setVisualZoomShift(zoomMagnifier - 1.0f);
		}
		currentViewport.setMapDensity(mapDensity);
	}

	public float normalizeElevationAngle(float elevationAngle) {
		return elevationAngle > 90 ? 90f : Math.max(getMinAllowedElevationAngle(), elevationAngle);
	}

	public float getMinAllowedElevationAngle() {
		if (true) {
			return 10;
		}
		int verticalTilesCount = currentViewport.getPixHeight() / OsmandRenderer.TILE_SIZE;
		if (verticalTilesCount < 8) {
			return 33;
		} else if (verticalTilesCount < 9) {
			return 35;
		} else if (verticalTilesCount < 10) {
			return 40;
		}
		return 45;
	}

	protected void zoomToAnimate(int zoom, double zoomToAnimate, int centerX, int centerY, boolean notify) {
		if (mainLayer != null && getMaxZoom() >= zoom && getMinZoom() <= zoom) {
			setZoomAndAnimationImpl(zoom, zoomToAnimate, centerX, centerY);
			setRotateImpl(rotate, centerX, centerY);
			refreshMap();
			if (notify) {
				notifyLocationListeners(getLatitude(), getLongitude());
			}
		}
	}

	private void zoomToAnimate(@NonNull RotatedTileBox initialViewport, float deltaZoom, int centerX, int centerY) {
		Zoom zoom = new Zoom(initialViewport.getZoom(), (float) initialViewport.getZoomFloatPart(), getMinZoom(), getMaxZoom());
		zoom.calculateAnimatedZoom(currentViewport.getZoom(), deltaZoom);
		boolean notify = !(doubleTapScaleDetector != null && doubleTapScaleDetector.isInZoomMode());
		zoomToAnimate(zoom.getBaseZoom(), zoom.getZoomAnimation(), centerX, centerY, notify);
	}

	private void zoomAndRotateToAnimate(boolean startZooming, boolean startRotating) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null && multiTouchSupport != null) {
			PointF firstPoint = multiTouchSupport.getFirstPoint();
			PointI firstPosition = new PointI((int) firstPoint.x, (int) firstPoint.y);
			PointF secondPoint = multiTouchSupport.getSecondPoint();
			PointI secondPosition = new PointI((int) secondPoint.x, (int) secondPoint.y);
			PointD zoomAndRotation = new PointD();
			boolean canChange = mapRenderer.getZoomAndRotationAfterPinch(
					new PointI(firstTouchLocationX, firstTouchLocationY), firstTouchLocationHeight, firstPosition,
					new PointI(secondTouchLocationX, secondTouchLocationY), secondTouchLocationHeight, secondPosition,
					zoomAndRotation);
			if (canChange) {
				if (startZooming) {
					float zoomShift = (float) zoomAndRotation.getX();
					Zoom zoom = new Zoom(currentViewport.getZoom(), (float) currentViewport.getZoomFloatPart(), getMinZoom(), getMaxZoom());
					zoom.calculateAnimatedZoom(currentViewport.getZoom(), zoomShift);
					int zoomLevel = zoom.getBaseZoom();
					double zoomAnimation = zoom.getZoomAnimation();
					double zoomFloatPart = currentViewport.getZoomFloatPart();

					float finalZoomFloatPart = (float) (zoomAnimation + zoomFloatPart);
					float visualZoom = finalZoomFloatPart >= 0
							? 1 + finalZoomFloatPart
							: 1 + 0.5f * finalZoomFloatPart;
					mapRenderer.setZoom(ZoomLevel.swigToEnum(zoomLevel), visualZoom);
					float zoomMagnifier = application.getOsmandMap().getMapDensity();
					mapRenderer.setVisualZoomShift(zoomMagnifier - 1.0f);

					currentViewport.setZoomAndAnimation(zoomLevel, 0.0, finalZoomFloatPart);
				}
				if (startRotating) {
					float angleShift = (float) zoomAndRotation.getY();
					this.rotate = MapUtils.unifyRotationTo360(this.rotate - angleShift);
					mapRenderer.setAzimuth(-this.rotate);
					currentViewport.setRotate(this.rotate);
				}
				refreshMap();
				boolean notify = !(doubleTapScaleDetector != null && doubleTapScaleDetector.isInZoomMode());
				if (notify)
					notifyLocationListeners(getLatitude(), getLongitude());
			}
		}
	}

	public void moveTo(float dx, float dy, boolean notify) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			RotatedTileBox tb = currentViewport.copy();
			PointI windowSize = mapRenderer.getState().getWindowSize();
			PointI translationPoint31 = NativeUtilities.get31FromPixel(mapRenderer, tb,
					(int) (windowSize.getX() / 2 + dx), (int) (windowSize.getY() / 2 + dy));
			if (translationPoint31 != null) {
				setTarget31Impl(translationPoint31.getX(), translationPoint31.getY());
			}
		} else {
			QuadPoint cp = currentViewport.getCenterPixelPoint();
			LatLon latlon = currentViewport.getLatLonFromPixel(cp.x + dx, cp.y + dy);
			setLatLonImpl(latlon.getLatitude(), latlon.getLongitude());
		}
		refreshMap();
		if (notify) {
			notifyLocationListeners(getLatitude(), getLongitude());
		}
	}

	public void moveTo(float fromX, float fromY, float toX, float toY, boolean notify) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			RotatedTileBox tb = currentViewport.copy();
			PointI from31 = NativeUtilities.get31FromPixel(mapRenderer, tb, (int) (fromX), (int) (fromY));
			PointI to31 = NativeUtilities.get31FromPixel(mapRenderer, tb, (int) (toX), (int) (toY));
			if (from31 != null && to31 != null) {
				PointI target31 = mapRenderer.getTarget();
				setTarget31Impl(target31.getX() - (to31.getX() - from31.getX()),
						target31.getY() - (to31.getY() - from31.getY()));
			}
		} else {
			float dx = (fromX - toX);
			float dy = (fromY - toY);
			QuadPoint cp = currentViewport.getCenterPixelPoint();
			LatLon latlon = currentViewport.getLatLonFromPixel(cp.x + dx, cp.y + dy);
			setLatLonImpl(latlon.getLatitude(), latlon.getLongitude());
		}
		refreshMap();
		if (notify) {
			notifyLocationListeners(getLatitude(), getLongitude());
		}
	}

	public void moveTo(int toX31, int toY31, boolean notify) {
		setTarget31Impl(toX31, toY31);
		refreshMap();
		if (notify) {
			notifyLocationListeners(getLatitude(), getLongitude());
		}
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
				int offset = (tb.getPixWidth() - tileBoxWidthPx) / 2 - marginLeftPx;
				dx = isLayoutRtl() ? -offset : offset;
			}
		} else if (tileBoxHeightPx > 0) {
			tbh = (int) (tileBoxHeightPx * border);
			dy = (tb.getPixHeight() - tileBoxHeightPx) / 2 - marginTopPx;
		}
		dy += tb.getCenterPixelY() - tb.getPixHeight() / 2;
		tb.setPixelDimensions(tbw, tbh);

		double clat = bottom / 2 + top / 2;
		double clon = left / 2 + right / 2;
		tb.setLatLonCenter(clat, clon);

		int minZoom = Math.max(getMinZoom(), MIN_ZOOM_LIMIT);
		int maxZoom = Math.min(getMaxZoom(), MAX_ZOOM_LIMIT);
		Zoom zoom = new Zoom(tb.getZoom(), (float) tb.getZoomFloatPart(), minZoom, maxZoom);

		while (zoom.isZoomInAllowed() && tb.containsRectInRotatedRect(left, top, right, bottom)) {
			zoom.zoomIn();
			tb.setZoomAndAnimation(zoom.getBaseZoom(), 0, zoom.getZoomFloatPart());
		}
		while (zoom.isZoomOutAllowed() && !tb.containsRectInRotatedRect(left, top, right, bottom)) {
			zoom.zoomOut();
			tb.setZoomAndAnimation(zoom.getBaseZoom(), 0, zoom.getZoomFloatPart());
		}

		if (dy != 0 || dx != 0) {
			float x = tb.getPixWidth() / 2f + dx;
			float y = tb.getPixHeight() / 2f + dy;
			clat = tb.getLatFromPixel(x, y);
			clon = tb.getLonFromPixel(x, y);
		}

		animatedDraggingThread.startMoving(clat, clon, zoom.getBaseZoom(), zoom.getZoomFloatPart(),
				true, false, null, null);
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
		dy += tb.getCenterPixelY() - tb.getPixHeight() / 2;
		tb.setPixelDimensions(tbw, tbh);

		if (dy != 0) {
			float x = tb.getPixWidth() / 2f;
			float y = tb.getPixHeight() / 2f - dy;
			double clat = tb.getLatFromPixel(x, y);
			double clon = tb.getLonFromPixel(x, y);
			tb.setLatLonCenter(clat, clon);
		}
		return tb;
	}

	public void fitLocationToMap(double clat, double clon, int zoom,
	                             int tileBoxWidthPx, int tileBoxHeightPx, int marginTopPx, boolean animated) {
		RotatedTileBox tb = currentViewport.copy();
		int dy = 0;

		int tbw = tileBoxWidthPx > 0 ? tileBoxWidthPx : tb.getPixWidth();
		int tbh = tb.getPixHeight();
		if (tileBoxHeightPx > 0) {
			tbh = tileBoxHeightPx;
			dy = (tb.getPixHeight() - tileBoxHeightPx) / 2 - marginTopPx;
		}
		dy += tb.getCenterPixelY() - tb.getPixHeight() / 2;
		tb.setPixelDimensions(tbw, tbh);
		tb.setLatLonCenter(clat, clon);
		tb.setZoom(zoom);
		if (dy != 0) {
			float x = tb.getPixWidth() / 2f;
			float y = tb.getPixHeight() / 2f + dy;
			clat = tb.getLatFromPixel(x, y);
			clon = tb.getLonFromPixel(x, y);
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
			RotatedTileBox tb = getCurrentRotatedTileBox();
			double lat = tb.getLatFromPixel(event.getX(), event.getY());
			double lon = tb.getLonFromPixel(event.getX(), event.getY());
			int zoomDir = event.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0 ? -1 : 1;
			getAnimatedDraggingThread().startMoving(lat, lon, getZoom() + zoomDir, true);
			return true;
		}
		return false;
	}

	private void findFirstTouchMapLocation(float touchPointX, float touchPointY) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			PointI touchPosition = new PointI((int) touchPointX, (int) touchPointY);
			PointI touchLocation31 = mapRenderer.getTarget();
			float height = mapRenderer.getHeightAndLocationFromElevatedPoint(touchPosition, touchLocation31);
			firstTouchLocationX = touchLocation31.getX();
			firstTouchLocationY = touchLocation31.getY();
			firstTouchLocationHeight = height > NativeUtilities.MIN_ALTITUDE_VALUE ? height : 0.0f;
		}
	}

	private void findSecondTouchMapLocation(float touchPointX, float touchPointY) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			PointI touchPosition = new PointI((int) touchPointX, (int) touchPointY);
			PointI touchLocation31 = mapRenderer.getTarget();
			float height = mapRenderer.getHeightAndLocationFromElevatedPoint(touchPosition, touchLocation31);
			secondTouchLocationX = touchLocation31.getX();
			secondTouchLocationY = touchLocation31.getY();
			secondTouchLocationHeight = height > NativeUtilities.MIN_ALTITUDE_VALUE ? height : 0.0f;
		}
	}

	public boolean onTouchEvent(MotionEvent event) {
		if (mapRenderer != null) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				mapRenderer.suspendSymbolsUpdate();
				targetChanged = false;
			} else if (event.getAction() == MotionEvent.ACTION_UP
					|| event.getAction() == MotionEvent.ACTION_CANCEL) {
				mapRenderer.resumeSymbolsUpdate();
				if (targetChanged) {
					targetChanged = false;
					// Restore previous target screen position after map gesture
					mapRenderer.resetMapTargetPixelCoordinates(new PointI(targetPixelX, targetPixelY));
					PointI target31 = mapRenderer.getTarget();
					currentViewport.setLatLonCenter(MapUtils.get31LatitudeY(target31.getY()),
							MapUtils.get31LongitudeX(target31.getX()));
					refreshMap();
					notifyLocationListeners(getLatitude(), getLongitude());
				}
			}
		}
		if (twoFingersTapDetector != null && twoFingersTapDetector.onTouchEvent(event)) {
			ContextMenuLayer contextMenuLayer = getLayerByClass(ContextMenuLayer.class);
			if (contextMenuLayer != null) {
				contextMenuLayer.onTouchEvent(event, getCurrentRotatedTileBox());
			}
			return true;
		}

		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			animatedDraggingThread.stopAnimating();
		}
		boolean wasInTiltMode = multiTouchSupport != null && multiTouchSupport.isInTiltMode();
		boolean isMultiTouch = multiTouchSupport != null && multiTouchSupport.onTouchEvent(event);

		MeasurementToolLayer layer = getMeasurementToolLayer();
		if (mapRenderer != null && multiTouchSupport != null && (layer == null || !layer.isInMeasurementMode())) {
			int actionCode = event.getActionMasked();
			if (actionCode != MotionEvent.ACTION_DOWN
					&& actionCode != MotionEvent.ACTION_UP
					&& actionCode != MotionEvent.ACTION_CANCEL) {
				int actionIndex = event.getActionIndex();
				boolean primaryTouch = actionCode == MotionEvent.ACTION_POINTER_DOWN && actionIndex == 0;
				boolean primaryClear = actionCode == MotionEvent.ACTION_POINTER_UP && actionIndex == 0;
				boolean secondaryTouch = actionCode == MotionEvent.ACTION_POINTER_DOWN && actionIndex == 1;
				boolean secondaryClear = actionCode == MotionEvent.ACTION_POINTER_UP && actionIndex == 1;
				if (primaryTouch) {
					// Keep map location of previous touch for map gestures
					secondTouchLocationX = firstTouchLocationX;
					secondTouchLocationY = firstTouchLocationY;
					secondTouchLocationHeight = firstTouchLocationHeight;
					findFirstTouchMapLocation(event.getX(), event.getY());
					rotate = MapUtils.unifyRotationTo360(-mapRenderer.getAzimuth());
				} else if (secondaryTouch) {
					// Find map location of second touch for map gestures
					PointF touchPoint = multiTouchSupport.getSecondPoint();
					findSecondTouchMapLocation(touchPoint.x, touchPoint.y);
					if (!targetChanged) {
						targetChanged = true;
						// Remember last target position before it is changed with map gesture
						PointI targetPixelPosition = mapRenderer.getTargetScreenPosition();
						targetPixelX = targetPixelPosition.getX();
						targetPixelY = targetPixelPosition.getY();
						touchPoint = multiTouchSupport.getFirstPoint();
						findFirstTouchMapLocation(touchPoint.x, touchPoint.y);
						rotate = MapUtils.unifyRotationTo360(-mapRenderer.getAzimuth());
					}
					rotate = MapUtils.unifyRotationTo360(-mapRenderer.getAzimuth());
				} else if (primaryClear) {
					// Use map location of second touch for map gestures
					if (wasInTiltMode && !multiTouchSupport.isInTiltMode()) {
						PointF touchPoint = multiTouchSupport.getSecondPoint();
						findSecondTouchMapLocation(touchPoint.x, touchPoint.y);
					}
					firstTouchLocationX = secondTouchLocationX;
					firstTouchLocationY = secondTouchLocationY;
					firstTouchLocationHeight = secondTouchLocationHeight;
				} else if (secondaryClear && wasInTiltMode && !multiTouchSupport.isInTiltMode()) {
					PointF touchPoint = multiTouchSupport.getFirstPoint();
					findFirstTouchMapLocation(touchPoint.x, touchPoint.y);
				}
			}
		}
		if (doubleTapScaleDetector != null) {
			doubleTapScaleDetector.onTouchEvent(event);
		}
		if (!isMultiTouch && doubleTapScaleDetector != null && !doubleTapScaleDetector.isInZoomMode()) {
			List<OsmandMapLayer> layers = getLayers();
			for (int i = layers.size() - 1; i >= 0; i--) {
				layers.get(i).onTouchEvent(event, getCurrentRotatedTileBox());
			}
		}
		if (doubleTapScaleDetector != null && !doubleTapScaleDetector.isInZoomMode()
				&& !doubleTapScaleDetector.isDoubleTapping() && gestureDetector != null) {
			gestureDetector.onTouchEvent(event);
		}
		return true;
	}

	public void setMapRenderer(@Nullable MapRendererView mapRenderer) {
		List<OsmandMapLayer> layers = getLayers();
		for (OsmandMapLayer layer : layers) {
			layer.onMapRendererChange(this.mapRenderer, mapRenderer);
		}
		if (this.mapRenderer != null) {
			// Trying to avoid possible crash if some layer did not cleanup providers
			this.mapRenderer.removeAllSymbolsProviders();
			this.mapRenderer.resetElevationDataProvider();
		}
		this.mapRenderer = mapRenderer;
		if (!isSteplessZoomSupported()) {
			setZoomWithFloatPart(getZoom(), 0);
		}
	}

	@Nullable
	public MapRendererView getMapRenderer() {
		return mapRenderer;
	}

	public boolean hasMapRenderer() {
		return mapRenderer != null;
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

	public AnimateMapMarkersThread getAnimatedMapMarkersThread() {
		return animatedMapMarkersThread;
	}

	public void showMessage(String msg) {
		handler.post(() -> Toast.makeText(application, msg, Toast.LENGTH_SHORT).show());
	}

	private class MapTileViewMultiTouchZoomListener implements MultiTouchZoomListener, DoubleTapZoomListener {

		private static final float ZONE_0_ANGLE_THRESHOLD = 5;
		private static final float ZONE_1_ANGLE_THRESHOLD = 20;
		private static final float ZONE_2_ANGLE_THRESHOLD = 30;
		private static final float ZONE_3_ANGLE_THRESHOLD = 60;
		private static final float ZONE_0_ZOOM_THRESHOLD = 0.15f;
		private static final float ZONE_1_ZOOM_THRESHOLD = 0.6f;
		private static final float ZONE_2_ZOOM_THRESHOLD = 1.5f;
		private static final float MAX_DELTA_ZOOM = 4;

		private PointF initialMultiTouchCenterPoint;
		private RotatedTileBox initialViewport;
		private float x1;
		private float y1;
		private float x2;
		private float y2;
		private LatLon initialCenterLatLon;
		private boolean startRotating;
		private boolean startZooming;
		private float initialElevation;
		private float prevAngle;

		@Override
		public void onZoomOrRotationEnded(double relativeToStart, float angleRelative) {
			MeasurementToolLayer layer = getMeasurementToolLayer();
			MapRendererView mapRenderer = getMapRenderer();
			boolean finished = mapRenderer != null && (layer == null || !layer.isInMeasurementMode());
			// 1.5 works better even on dm.density=1 devices
			if (!finished)
				finishPinchZoom();
			if (startRotating) {
				if (!finished)
					rotateToAnimate(initialViewport.getRotate() + angleRelative);
				if (angleRelative != 0) {
					application.getMapViewTrackingUtilities().checkAndUpdateManualRotationMode();
				}
			}
			int newZoom = getZoom();
			if (application.accessibilityEnabled()) {
				if (newZoom != initialViewport.getZoom()) {
					showMessage(application.getString(R.string.zoomIs) + " " + newZoom);
				} else {
					LatLon p1 = NativeUtilities.getLatLonFromElevatedPixel(mapRenderer, initialViewport, x1, y1);
					LatLon p2 = NativeUtilities.getLatLonFromElevatedPixel(mapRenderer, initialViewport, x2, y2);
					showMessage(OsmAndFormatter.getFormattedDistance((float) MapUtils.getDistance(
							p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude()), application));
				}
			}
		}

		@Override
		public void onZoomEnded(double relativeToStart) {
			// 1.5 works better even on dm.density=1 devices
			finishPinchZoom();
			int newZoom = getZoom();
			if (application.accessibilityEnabled()) {
				if (newZoom != initialViewport.getZoom()) {
					showMessage(application.getString(R.string.zoomIs) + " " + newZoom);
				} else {
					MapRendererView mapRenderer = getMapRenderer();
					LatLon p1 = NativeUtilities.getLatLonFromElevatedPixel(mapRenderer, initialViewport, x1, y1);
					LatLon p2 = NativeUtilities.getLatLonFromElevatedPixel(mapRenderer, initialViewport, x2, y2);
					showMessage(OsmAndFormatter.getFormattedDistance((float) MapUtils.getDistance(
							p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude()), application));
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
				MapRendererView mapRenderer = getMapRenderer();
				if (mapRenderer != null) {
					MapAnimator animator = mapRenderer.getMapAnimator();
					if (animator != null) {
						animator.pause();
						animator.cancelAllAnimations();
					}
				}
				firstTouchPointLatLon = NativeUtilities.getLatLonFromElevatedPixel(mapRenderer, currentViewport, x1, y1);
				secondTouchPointLatLon = NativeUtilities.getLatLonFromElevatedPixel(mapRenderer, currentViewport, x2, y2);
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
		public void onChangingViewAngle(float angle) {
			if (mapGestureAllowed(MapGestureType.TWO_POINTERS_TILT)) {
				setElevationAngle(initialElevation - angle);
			}
		}

		@Override
		public void onChangeViewAngleStarted() {
			if (mapGestureAllowed(MapGestureType.TWO_POINTERS_TILT)) {
				initialElevation = elevationAngle;

				MapRendererView mapRenderer = getMapRenderer();
				MeasurementToolLayer measurementToolLayer = getMeasurementToolLayer();
				boolean measurementMode = measurementToolLayer != null && measurementToolLayer.isInMeasurementMode();

				if (mapRenderer != null && !measurementMode && multiTouchSupport != null) {
					if (!targetChanged) {
						targetChanged = true;
						// Remember last target position before it is changed with map gesture
						PointI targetPixelPosition = mapRenderer.getTargetScreenPosition();
						targetPixelX = targetPixelPosition.getX();
						targetPixelY = targetPixelPosition.getY();
						rotate = MapUtils.unifyRotationTo360(-mapRenderer.getAzimuth());
					}

					PointF firstTouchPoint = multiTouchSupport.getFirstPoint();
					PointF secondTouchPoint = multiTouchSupport.getSecondPoint();
					int middleX = (int) ((firstTouchPoint.x + secondTouchPoint.x) / 2f);
					int middleY = (int) ((firstTouchPoint.y + secondTouchPoint.y) / 2f);
					PointI middlePoint31 = NativeUtilities.get31FromElevatedPixel(mapRenderer, middleX, middleY);
					if (middlePoint31 == null) {
						middlePoint31 = mapRenderer.getState().getTarget31();
					}

					mapRenderer.setMapTarget(new PointI(middleX, middleY), middlePoint31);
				}
			}
		}

		@Override
		public void onZoomStarted(PointF centerPoint) {
			initialMultiTouchCenterPoint = centerPoint;
			initialViewport = getCurrentRotatedTileBox().copy();
			MapRendererView mapRenderer = getMapRenderer();
			initialCenterLatLon = NativeUtilities.getLatLonFromElevatedPixel(mapRenderer, initialViewport,
					initialMultiTouchCenterPoint.x, initialMultiTouchCenterPoint.y);
			startRotating = false;
			startZooming = false;
			notifyLocationListeners(getLatitude(), getLongitude());
		}

		@Override
		public void onZoomingOrRotating(double relativeToStart, float relAngle) {
			double deltaZoom = calculateDeltaZoom(relativeToStart);
			if (Math.abs(deltaZoom) <= ZONE_0_ZOOM_THRESHOLD && !startZooming) {
				deltaZoom = 0; // keep only rotating
			} else {
				startZooming = true;
			}
			if (mapGestureAllowed(MapGestureType.TWO_POINTERS_ROTATION) && isAngleOverThreshold(Math.abs(relAngle), Math.abs(deltaZoom))) {
				startRotating = true;
			} else {
				relAngle = 0;
			}

			if (deltaZoom != 0 || relAngle != 0) {
				changeZoomPosition((float) deltaZoom, relAngle);
			}
		}

		private boolean isAngleOverThreshold(float angle, double deltaZoom) {
			if (startRotating) {
				return true;
			} else if (!startZooming) {
				return Math.abs(angle) >= ZONE_0_ANGLE_THRESHOLD;
			} else if (deltaZoom >= ZONE_2_ZOOM_THRESHOLD) {
				return Math.abs(angle) >= ZONE_3_ANGLE_THRESHOLD;
			} else if (deltaZoom >= ZONE_1_ZOOM_THRESHOLD) {
				return Math.abs(angle) >= ZONE_2_ANGLE_THRESHOLD;
			} else {
				return Math.abs(angle) >= ZONE_1_ANGLE_THRESHOLD;
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
			if (doubleTapScaleDetector != null && !doubleTapScaleDetector.isInZoomMode()) {
				Zoom zoom = new Zoom(getZoom(), getZoomFloatPart(), getMinZoom(), getMaxZoom());
				if (zoom.isZoomInAllowed()) {
					zoom.zoomIn();

					RotatedTileBox tb = getCurrentRotatedTileBox();
					LatLon latlon = NativeUtilities.getLatLonFromElevatedPixel(mapRenderer, tb, e.getX(), e.getY());
					if (hasMapRenderer()) {
						getAnimatedDraggingThread().startZooming(zoom.getBaseZoom(), zoom.getZoomFloatPart(), latlon, true);
					} else {
						getAnimatedDraggingThread().startMoving(
								latlon.getLatitude(), latlon.getLongitude(), zoom.getBaseZoom(), zoom.getZoomFloatPart(),
								true, false, null, null);
					}
				}
				afterDoubleTap = true;
				return true;
			} else {
				return false;
			}
		}

		private double calculateDeltaZoom(double relativeToStart) {
			// 1.5/Math.log(2) = 2.1640
			double deltaZoom = Math.log(relativeToStart) * 2.164;
			if (deltaZoom > MAX_DELTA_ZOOM) {
				return MAX_DELTA_ZOOM;
			} else if (deltaZoom < -MAX_DELTA_ZOOM) {
				return -MAX_DELTA_ZOOM;
			}
			return deltaZoom;
		}

		private void changeZoomPosition(float deltaZoom, float angle) {
			RotatedTileBox calc = initialViewport.copy();
			QuadPoint cp = initialViewport.getCenterPixelPoint();
			int multiTouchCenterX;
			int multiTouchCenterY;
			if (multiTouchSupport != null && multiTouchSupport.isInZoomAndRotationMode()) {
				multiTouchCenterX = (int) multiTouchSupport.getCenterPoint().x;
				multiTouchCenterY = (int) multiTouchSupport.getCenterPoint().y;
			} else {
				multiTouchCenterX = (int) initialMultiTouchCenterPoint.x;
				multiTouchCenterY = (int) initialMultiTouchCenterPoint.y;
			}

			calc.setLatLonCenter(initialCenterLatLon.getLatitude(), initialCenterLatLon.getLongitude());
			float calcRotate = calc.getRotate() + angle;
			calc.setRotate(calcRotate);
			calc.setZoomAndAnimation(initialViewport.getZoom(), deltaZoom, initialViewport.getZoomFloatPart());
			if (multiTouch) {
				wasZoomInMultiTouch = true;
			}
			// Keep zoom center fixed or flexible
			if (mapRenderer != null) {
				MeasurementToolLayer layer = getMeasurementToolLayer();
				if ((layer == null || !layer.isInMeasurementMode()) &&
						(doubleTapScaleDetector == null || !doubleTapScaleDetector.isInZoomMode()))
					zoomAndRotateToAnimate(startZooming, startRotating);
				else {
					zoomToAnimate(initialViewport, deltaZoom, multiTouchCenterX, multiTouchCenterY);
					rotateToAnimate(calcRotate, multiTouchCenterX, multiTouchCenterY);
				}
			} else {
				LatLon r = calc.getLatLonFromPixel(cp.x + cp.x - multiTouchCenterX, cp.y + cp.y - multiTouchCenterY);
				setLatLon(r.getLatitude(), r.getLongitude());
				zoomToAnimate(initialViewport, deltaZoom, multiTouchCenterX, multiTouchCenterY);
				rotateToAnimate(calcRotate);
			}
			prevAngle = angle;
		}

		public void finishPinchZoom() {
			double newZoomFloatPart = isSteplessZoomSupported()
					? currentViewport.getZoomAnimation() + currentViewport.getZoomFloatPart()
					: 0;
			if (mainLayer != null) {
				animatedDraggingThread.stopAnimating();
				int intZoom = currentViewport.getZoom();
				setZoomAndAnimationImpl(intZoom, 0, newZoomFloatPart);
				setRotateImpl(rotate);
				refreshMap();
			}
		}
	}

	public void setElevationAngle(float angle) {
		angle = normalizeElevationAngle(angle);
		this.elevationAngle = angle;
		application.getOsmandMap().setMapElevation(angle);
		notifyOnElevationChanging(angle);
	}

	private void notifyOnElevationChanging(float angle) {
		for (ElevationListener listener : elevationListeners) {
			listener.onElevationChanging(angle);
		}
	}

	private void notifyLocationListeners(double lat, double lon) {
		for (IMapLocationListener listener : locationListeners) {
			listener.locationChanged(lat, lon, this);
		}
	}

	private class MapTileViewOnGestureListener extends SimpleOnGestureListener {
		@Override
		public boolean onDown(MotionEvent e) {
			MapRendererView mapRenderer = getMapRenderer();
			MapAnimator animator = mapRenderer != null ? mapRenderer.getMapAnimator() : null;
			if (animator != null) {
				animator.pause();
				animator.cancelAllAnimations();
			}
			// Facilitates better map re-linking for two finger tap zoom out
			wasMapLinkedBeforeGesture = application.getMapViewTrackingUtilities().isMapLinkedToLocation();
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
			if (multiTouchSupport != null && multiTouchSupport.isInZoomAndRotationMode()
					|| doubleTapScaleDetector != null && doubleTapScaleDetector.isInZoomMode()
					|| doubleTapScaleDetector != null && doubleTapScaleDetector.isDoubleTapping()) {
				//	|| afterTwoFingersTap) {
				//afterTwoFingersTap = false;
				return;
			}
			if (LOG.isDebugEnabled()) {
				LOG.debug("On long click event " + e.getX() + " " + e.getY());  //$NON-NLS-2$
			}
			PointF point = new PointF(e.getX(), e.getY());
			if ((accessibilityActions != null) && accessibilityActions.onLongClick(point, getCurrentRotatedTileBox())) {
				return;
			}
			List<OsmandMapLayer> layers = getLayers();
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
			if (multiTouchSupport != null && !multiTouchSupport.isInTiltMode()) {
				MeasurementToolLayer layer = getMeasurementToolLayer();
				MapRendererView mapRenderer = getMapRenderer();
				if (mapRenderer != null && (layer == null || !layer.isInMeasurementMode())) {
					if (!targetChanged) {
						targetChanged = true;
						// Remember last target position before it is changed with map gesture
						PointI targetPixelPosition = mapRenderer.getTargetScreenPosition();
						targetPixelX = targetPixelPosition.getX();
						targetPixelY = targetPixelPosition.getY();
						findFirstTouchMapLocation(e1.getX(), e1.getY());
						rotate = MapUtils.unifyRotationTo360(-mapRenderer.getAzimuth());
					}
					PointI touchPoint = new PointI((int) e2.getX(), (int) e2.getY());
					mapRenderer.setMapTarget(touchPoint, new PointI(firstTouchLocationX, firstTouchLocationY));
					PointI target31 = mapRenderer.getState().getTarget31();
					currentViewport.setLatLonCenter(MapUtils.get31LatitudeY(target31.getY()), MapUtils.get31LongitudeX(target31.getX()));
					refreshMap();
					notifyLocationListeners(getLatitude(), getLongitude());
				} else
					dragToAnimate(e2.getX() + distanceX, e2.getY() + distanceY, e2.getX(), e2.getY(), true);
			}
			return true;
		}

		@Override
		public void onShowPress(MotionEvent e) {
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			if (doubleTapScaleDetector != null && doubleTapScaleDetector.isDoubleTapping() || afterDoubleTap) {
				// Needed to suppress false single tap detection if we mask MotionEvents for gestures on isDoubleTapping()
				afterDoubleTap = false;
				return true;
			}
			PointF point = new PointF(e.getX(), e.getY());
			if (LOG.isDebugEnabled()) {
				LOG.debug("On click event " + point.x + " " + point.y);  //$NON-NLS-2$
			}
			if ((accessibilityActions != null) && accessibilityActions.onClick(point, getCurrentRotatedTileBox())) {
				return true;
			}
			List<OsmandMapLayer> layers = getLayers();
			for (int i = layers.size() - 1; i >= 0; i--) {
				if (layers.get(i).onSingleTap(point, getCurrentRotatedTileBox())) {
					return true;
				}
			}
			return onClickListener != null && onClickListener.onPressEvent(point);
		}
	}

	public Resources getResources() {
		return application.getResources();
	}

	public Context getContext() {
		return ctx;
	}

	public boolean isLayoutRtl() {
		return AndroidUtils.isLayoutRtl(application);
	}

	@Nullable
	private MeasurementToolLayer getMeasurementToolLayer() {
		return application.getOsmandMap().getMapLayers().getMeasurementToolLayer();
	}

	private boolean isUseOpenGL() {
		NavigationSession carNavigationSession = getApplication().getCarNavigationSession();
		boolean androidAutoAttached = carNavigationSession != null && carNavigationSession.hasStarted();
		return getApplication().useOpenGlRenderer() && !androidAutoAttached;
	}

	private boolean isSteplessZoomSupported() {
		return hasMapRenderer();
	}

	public interface ElevationListener {
		void onElevationChanging(float angle);
	}
}

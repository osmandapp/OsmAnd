package net.osmand.plus.views;


import static net.osmand.plus.AppInitEvents.NATIVE_INITIALIZED;
import static net.osmand.plus.AppInitEvents.NATIVE_OPEN_GL_INITIALIZED;
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
import android.view.*;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.StateChangedListener;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.MapAnimator;
import net.osmand.core.jni.MapRendererDebugSettings;
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
import net.osmand.plus.AppInitEvents;
import net.osmand.plus.AppInitializeListener;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.auto.SurfaceRenderer;
import net.osmand.plus.auto.views.CarSurfaceView;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.helpers.MapDisplayPositionManager;
import net.osmand.plus.helpers.TwoFingerTapDetector;
import net.osmand.plus.measurementtool.MeasurementToolLayer;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.accessibility.AccessibilityActionsProvider;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.CompassMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.DoubleTapScaleDetector.DoubleTapZoomListener;
import net.osmand.plus.views.MultiTouchSupport.MultiTouchZoomListener;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.views.layers.ContextMenuLayer;
import net.osmand.plus.views.layers.base.BaseMapLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.layers.base.OsmandMapLayer.MapGestureType;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRuleStorageProperties;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.CollectionUtils;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OsmandMapTileView implements IMapDownloaderCallback {

	public static final float DEFAULT_ELEVATION_ANGLE = 90;
	public static final float MIN_ALLOWED_ELEVATION_ANGLE = 10;
	public static final int MAP_DEFAULT_COLOR = 0xffebe7e4;
	public static final int FOG_DEFAULT_COLOR = 0xffebe7e4;
	public static final int SKY_DEFAULT_COLOR = 0xffffffff;
	public static final int FOG_NIGHTMODE_COLOR = 0xff243060;
	public static final int SKY_NIGHTMODE_COLOR = 0xff304080;

	private static final int SHOW_POSITION_MSG_ID = OsmAndConstants.UI_HANDLER_MAP_VIEW + 1;
	private static final int MAP_REFRESH_MESSAGE = OsmAndConstants.UI_HANDLER_MAP_VIEW + 4;
	private static final int MAP_FORCE_REFRESH_MESSAGE = OsmAndConstants.UI_HANDLER_MAP_VIEW + 5;
	private static final int BASE_REFRESH_MESSAGE = OsmAndConstants.UI_HANDLER_MAP_VIEW + 3;
	private static final int MIN_ZOOM_LIMIT = 1;
	private static final int MIN_ZOOM_LEVEL_TO_ADJUST_CAMERA_TILT = 3;
	private static final int MAX_ZOOM_LIMIT = 17;

	private boolean MEASURE_FPS;
	private final FPSMeasurement main = new FPSMeasurement();
	private final FPSMeasurement additional = new FPSMeasurement();
	private final MapRenderFPSMeasurement renderFPSMeasurement = new MapRenderFPSMeasurement();

	private boolean DISABLE_MAP_LAYERS;
	private StateChangedListener<Boolean> disableMapLayersListener;

	private View view;
	private final Context ctx;
	private MapActivity mapActivity;
	private OsmandApplication application;
	protected OsmandSettings settings;
	private MapViewTrackingUtilities mapViewTrackingUtilities;

	private CanvasColors canvasColors;
	private Boolean nightMode;

	private float minAllowedElevationAngle = MIN_ALLOWED_ELEVATION_ANGLE;


	private static class CanvasColors {
		int colorDay = MAP_DEFAULT_COLOR;
		int colorNight = MAP_DEFAULT_COLOR;
	}

	public interface OnTrackBallListener {
		boolean onTrackBallEvent(MotionEvent e);
	}

	public interface TouchListener {
		void onTouchEvent(@NonNull MotionEvent event);
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

	public interface ManualZoomListener {
		void onManualZoomChange();
	}

	public interface ViewportListener {
		void onViewportChanged();
	}

	protected static final Log LOG = PlatformUtil.getLog(OsmandMapTileView.class);

	private RotatedTileBox currentViewport;

	private float rotate; // accumulate

	private boolean showMapPosition = true;

	private List<IMapLocationListener> locationListeners = new ArrayList<>();
	private List<ElevationListener> elevationListeners = new ArrayList<>();
	private List<ManualZoomListener> manualZoomListeners = new ArrayList<>();
	private List<ViewportListener> viewportListeners = new ArrayList<>();
	private List<TouchListener> touchListeners = new ArrayList<>();

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
	private boolean blockTwoFingersTap;

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
	private float scrollDistanceX;
	private float scrollDistanceY;

	public OsmandMapTileView(@NonNull Context ctx, int width, int height) {
		this.ctx = ctx;
		init(ctx, width, height);
	}

	// ///////////////////////////// INITIALIZING UI PART ///////////////////////////////////
	public void init(@NonNull Context ctx, int width, int height) {
		application = (OsmandApplication) ctx.getApplicationContext();
		settings = application.getSettings();
		mapViewTrackingUtilities = application.getMapViewTrackingUtilities();

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

		DisplayMetrics dm = new DisplayMetrics();
		AndroidUtils.getDisplay(ctx).getMetrics(dm);

		updateDisplayMetrics(dm, width, height);
		elevationAngle = settings.getLastKnownMapElevation();

		DISABLE_MAP_LAYERS = settings.DISABLE_MAP_LAYERS.get();
		disableMapLayersListener = change -> DISABLE_MAP_LAYERS = change;
		settings.DISABLE_MAP_LAYERS.addListener(disableMapLayersListener);
	}

	public void updateDisplayMetrics(DisplayMetrics dm, int width, int height) {
		this.dm = dm;
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
	}

	private float getCurrentDensity() {
		return isCarView() ? getCarViewDensity() : dm.density;
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
			public boolean onTouchEvent(MotionEvent event) {
				int action = event.getAction();
				int actionCode = action & MotionEvent.ACTION_MASK;
				switch (actionCode) {
					case MotionEvent.ACTION_DOWN:
						blockTwoFingersTap = false;
				}
				return super.onTouchEvent(event);
			}

			@Override
			public void onTwoFingerTap() {
				//afterTwoFingersTap = true;
				if (!mapGestureAllowed(MapGestureType.TWO_POINTERS_ZOOM_OUT) || blockTwoFingersTap) {
					return;
				}

				Zoom zoom = getCurrentZoom();
				if (zoom.isZoomOutAllowed()) {
					zoom.zoomOut();
					getAnimatedDraggingThread().startZooming(zoom.getBaseZoom(), zoom.getZoomFloatPart(), null, false);
					if (wasMapLinkedBeforeGesture) {
						mapViewTrackingUtilities.setMapLinkedToLocation(true);
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

				private boolean renderingViewSetup;

				@Override
				public void onProgress(@NonNull AppInitializer init, @NonNull AppInitEvents event) {
					boolean openGlInitialized = event == NATIVE_OPEN_GL_INITIALIZED && NativeCoreContext.isInit();
					if ((openGlInitialized || event == NATIVE_INITIALIZED) && !renderingViewSetup) {
						application.getOsmandMap().setupRenderingView();
						renderingViewSetup = true;
					}
				}

				@Override
				public void onFinish(@NonNull AppInitializer init) {
					if (!renderingViewSetup) {
						application.getOsmandMap().setupRenderingView();
					}
					application.getOsmandMap().refreshMap();
				}
			});
		} else {
			application.getOsmandMap().setupRenderingView();
		}
	}

	public void backToLocation() {
		mapViewTrackingUtilities.backToLocationImpl();
	}

	public void zoomOut() {
		changeZoomManually(-1);
	}

	public void zoomIn() {
		changeZoomManually(1);
	}

	public void zoomOutAndAdjustTiltAngle() {
		changeZoomManually(-1, is3DMode());
	}

	public void zoomInAndAdjustTiltAngle() {
		changeZoomManually(1, is3DMode());
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
		if (type == MapGestureType.TWO_POINTERS_ROTATION &&
				settings.getCompassMode() == CompassMode.NORTH_IS_UP) {
			return false;
		}
		for (OsmandMapLayer layer : getLayers()) {
			if (!layer.isMapGestureAllowed(type)) {
				return false;
			}
		}
		return true;
	}

	public void changeZoomManually(int zoomStep) {
		changeZoomManually(zoomStep, false);
	}

	public void changeZoomManually(int zoomStep, boolean adjustTiltAngle) {
		if (animatedDraggingThread.isAnimatingMapZoom()) {
			animatedDraggingThread.stopAnimatingSync();
		}

		Zoom zoom = getCurrentZoom();
		int previousZoom = zoom.getBaseZoom();

		if (zoomStep > 0 && !zoom.isZoomInAllowed()) {
			Toast.makeText(application, R.string.edit_tilesource_maxzoom, Toast.LENGTH_SHORT).show();
			return;
		} else if (zoomStep < 0 && !zoom.isZoomOutAllowed()) {
			Toast.makeText(application, R.string.edit_tilesource_minzoom, Toast.LENGTH_SHORT).show();
			return;
		}

		zoom.changeZoom(zoomStep);
		animatedDraggingThread.startZooming(zoom.getBaseZoom(), zoom.getZoomFloatPart(), null, false);
		if (adjustTiltAngle && MultiTouchSupport.isTiltSupportEnabled(application)) {
			adjustTiltAngle(zoom, previousZoom);
		}

		mapViewTrackingUtilities.setZoomTime(System.currentTimeMillis());
		boolean linkedToLocation = application.getMapViewTrackingUtilities().isMapLinkedToLocation();
		if (!linkedToLocation) {
			showAndHideMapPosition();
		}
		if (application.accessibilityEnabled()) {
			Toast.makeText(application, application.getString(R.string.zoomIs) + " " + zoom.getBaseZoom(), Toast.LENGTH_SHORT).show();
		}

		for (ManualZoomListener listener : manualZoomListeners) {
			listener.onManualZoomChange();
		}
	}

	private boolean is3DMode() {
		return elevationAngle != DEFAULT_ELEVATION_ANGLE;
	}

	public void adjustTiltAngle() {
		int baseZoom = getZoom();
		int angle = getAdjustedTiltAngle(baseZoom, true);
		setElevationAngle(angle);
	}

	private void adjustTiltAngle(@NonNull Zoom zoom, int previousZoom) {
		int baseZoom = zoom.getBaseZoom();
		if (baseZoom >= MIN_ZOOM_LEVEL_TO_ADJUST_CAMERA_TILT && baseZoom <= MAX_ZOOM_LIMIT) {
			float previousAngle = getElevationAngle();
			int angle = getAdjustedTiltAngle(baseZoom, false);
			if (baseZoom >= previousZoom || angle >= previousAngle) {
				animatedDraggingThread.startTilting(angle, AnimateDraggingMapThread.ZOOM_ANIMATION_TIME);
			}
		}
	}

	public int getAdjustedTiltAngle(int baseZoom, boolean enforceZoomRange) {
		if (enforceZoomRange) {
			baseZoom = Math.max(MIN_ZOOM_LEVEL_TO_ADJUST_CAMERA_TILT, Math.min(baseZoom, MAX_ZOOM_LIMIT));
		}
		int angle = 90 - (baseZoom - 2) * 5;
		return (int) Math.max(minAllowedElevationAngle, Math.min(angle, DEFAULT_ELEVATION_ANGLE));
	}

	public float getMinAllowedElevationAngle() {
		return minAllowedElevationAngle;
	}

	public void setMinAllowedElevationAngle(float minAllowedElevationAngle) {
		this.minAllowedElevationAngle = minAllowedElevationAngle;
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

	public void resetRotation() {
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
		float diff = MapUtils.unifyRotationDiff(rotate, mapRenderer != null && mapActivity != null ? mapActivity.getMapRotateTarget() : getRotate());
		if (Math.abs(diff) > 5 || force) { // check smallest rotation
			animatedDraggingThread.startRotate(rotate);
		}
	}

	public void setHeight(float height) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null)
			mapRenderer.restoreFlatZoom(height);
		else
			currentViewport.setHeight(height);
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

	public int getZoom() {
		return currentViewport.getZoom();
	}

	@NonNull
	public Zoom getCurrentZoom() {
		return new Zoom(getZoom(), getZoomFloatPart(), getMinZoom(), getMaxZoom());
	}

	public int getBaseZoom() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			return mapRenderer.getState().getZoomLevel().ordinal() + mapRenderer.getTileZoomOffset();
		}
		return currentViewport.getZoom();
	}

	public boolean isPinchZoomingOrRotating() {
		return multiTouchSupport != null && multiTouchSupport.isInZoomAndRotationMode();
	}

	public boolean isAfterDoubleTap() {
		return afterDoubleTap;
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
			locationListeners = CollectionUtils.addToList(locationListeners, listener);
		}
	}

	public void removeMapLocationListener(@NonNull IMapLocationListener listener) {
		locationListeners = CollectionUtils.removeFromList(locationListeners, listener);
	}

	public void addManualZoomChangeListener(@NonNull ManualZoomListener listener) {
		if (!manualZoomListeners.contains(listener)) {
			manualZoomListeners = CollectionUtils.addToList(manualZoomListeners, listener);
		}
	}

	public void removeManualZoomListener(@NonNull ManualZoomListener listener) {
		manualZoomListeners = CollectionUtils.removeFromList(manualZoomListeners, listener);
	}

	public void addElevationListener(@NonNull ElevationListener listener) {
		if (!elevationListeners.contains(listener)) {
			elevationListeners = CollectionUtils.addToList(elevationListeners, listener);
		}
	}

	public void removeElevationListener(@NonNull ElevationListener listener) {
		elevationListeners = CollectionUtils.removeFromList(elevationListeners, listener);
	}

	public void addViewportListener(@NonNull ViewportListener listener) {
		if (!viewportListeners.contains(listener)) {
			viewportListeners = CollectionUtils.addToList(viewportListeners, listener);
		}
	}

	public void removeViewportListener(@NonNull ViewportListener listener) {
		viewportListeners = CollectionUtils.removeFromList(viewportListeners, listener);
	}

	public void setOnDrawMapListener(@Nullable OnDrawMapListener listener) {
		this.onDrawMapListener = listener;
	}

	public void addTouchListener(@NonNull TouchListener listener) {
		if (!touchListeners.contains(listener)) {
			touchListeners = CollectionUtils.addToList(touchListeners, listener);
		}
	}

	public void removeTouchListener(@NonNull TouchListener listener) {
		touchListeners = CollectionUtils.removeFromList(touchListeners, listener);
	}

	// ////////////////////////////// DRAWING MAP PART /////////////////////////////////////////////
	public BaseMapLayer getMainLayer() {
		return mainLayer;
	}

	public void setMainLayer(BaseMapLayer mainLayer) {
		this.mainLayer = mainLayer;
		int zoom = currentViewport.getZoom();
		double zoomFloatPart = currentViewport.getZoomFloatPart();
		if (getMaxZoom() < zoom) {
			zoom = getMaxZoom();
			zoomFloatPart = 0;
		}
		if (getMinZoom() > zoom) {
			zoom = getMinZoom();
			zoomFloatPart = 0;
		}
		setZoomAndAnimationImpl(zoom, 0, zoomFloatPart);
		refreshMap();
	}

	public void restoreScreenCenter() {
		MapDisplayPositionManager displayPositionManager = mapViewTrackingUtilities.getMapDisplayPositionManager();
		displayPositionManager.restoreMapRatio();
		RotatedTileBox box = currentViewport.copy();
		LatLon screenCenter = NativeUtilities.getLatLonFromElevatedPixel(mapRenderer, box,
				box.getPixWidth() / 2f, box.getPixHeight() / 2f);
		PointF ratio = displayPositionManager.getMapRatio();
		setLatLon(screenCenter.getLatitude(), screenCenter.getLongitude(), ratio.x, ratio.y);
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

	private void refreshMapInternal(@NonNull DrawSettings drawSettings) {
		if (view == null) {
			return;
		}
		PointF ratio = mapViewTrackingUtilities.getMapDisplayPositionManager().getMapRatio();
		int cy = (int) (ratio.y * view.getHeight());
		int cx = (int) (ratio.x * view.getWidth());
		boolean updateMapRenderer = false;
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			PointI fixedPixel = mapRenderer.getState().getFixedPixel();
			updateMapRenderer = fixedPixel.getX() < 0 || fixedPixel.getY() < 0;
		}

		boolean viewportChanged = currentViewport.getPixWidth() != view.getWidth()
				|| currentViewport.getPixHeight() != view.getHeight();
		boolean centerChanged = currentViewport.getCenterPixelY() != cy
				|| currentViewport.getCenterPixelX() != cx;

		if (updateMapRenderer || viewportChanged || centerChanged) {
			currentViewport.setPixelDimensions(view.getWidth(), view.getHeight(), ratio.x, ratio.y);

			if (viewportChanged) {
				notifyViewportChanged();
			}

			if (mapRenderer != null) {
				mapRenderer.setMapTarget(new PointI(cx, cy), mapRenderer.getTarget());
			}
			setElevationAngle(elevationAngle);
			setMapDensityImpl(getSettingsMapDensity());
			refreshBufferImage(drawSettings);
			float height = currentViewport.getHeight();
			if (height > 0.0f && mapRenderer != null) {
				currentViewport.setHeight(0.0f);
				mapRenderer.restoreFlatZoom(height);
			}
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
		return main.getFps();
	}

	public float getSecondaryFPS() {
		return additional.getFps();
	}

	public float calculateRenderFps() {
		MapRendererView renderer = getMapRenderer();
		if (renderer != null) {
			renderFPSMeasurement.calculateFPS(renderer.getFrameId());
		}
		return renderFPSMeasurement.getFps();
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
		if (DISABLE_MAP_LAYERS) {
			return;
		}
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
		if (showMapPosition || PluginsHelper.isMapPositionIconNeeded()) {
			drawMapPosition(canvas, c.x, c.y);
		} else if (multiTouchSupport != null && multiTouchSupport.isInZoomAndRotationMode()) {
			drawMapPosition(canvas, multiTouchSupport.getCenterPoint().x, multiTouchSupport.getCenterPoint().y);
		} else if (doubleTapScaleDetector != null && doubleTapScaleDetector.isInZoomMode()) {
			drawMapPosition(canvas, doubleTapScaleDetector.getCenterX(), doubleTapScaleDetector.getCenterY());
		}
	}

	protected void drawMapPosition(Canvas canvas, float x, float y) {
		canvas.drawCircle(x, y, 3 * getCurrentDensity(), paintCenter);
		canvas.drawCircle(x, y, 7 * getCurrentDensity(), paintCenter);
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
			PointI center31 = new PointI();
			if (mapRenderer.getLocationFromScreenPoint(new PointI(centerX, centerY), center31)) {
				int centerX31 = center31.getX();
				int centerY31 = center31.getY();
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
		zoom = normalizeZoomWithLimits(zoom);
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

	public void setCurrentZoom() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			int zoomLevel = mapRenderer.getZoomLevel().ordinal();
			float visualZoom = mapRenderer.getVisualZoom();
			float zoomFloatPart = visualZoom >= 1.0f
					? visualZoom - 1.0f
					: (visualZoom - 1.0f) * 2.0f;
			currentViewport.setZoomAndAnimation(zoomLevel, 0, zoomFloatPart);
		}
	}

	public float normalizeElevationAngle(float elevationAngle) {
		return elevationAngle > 90 ? 90f : Math.max(minAllowedElevationAngle, elevationAngle);
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

	private void zoomToAnimate(@NonNull RotatedTileBox tileBox, float deltaZoom, int centerX, int centerY) {
		Zoom zoom = new Zoom(tileBox.getZoom(), (float) tileBox.getZoomFloatPart(), getMinZoom(), getMaxZoom());
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
			scrollDistanceX = 0.0f;
			scrollDistanceY = 0.0f;
			mapRenderer.setMapTarget(firstPosition, new PointI(firstTouchLocationX, firstTouchLocationY));
			PointD zoomAndRotation = new PointD();
			boolean canChange = mapRenderer.getZoomAndRotationAfterPinch(
					new PointI(firstTouchLocationX, firstTouchLocationY), firstTouchLocationHeight, firstPosition,
					new PointI(secondTouchLocationX, secondTouchLocationY), secondTouchLocationHeight, secondPosition,
					zoomAndRotation);
			if (canChange) {
				if (startZooming) {
					float relativeZoomAnimation = (float) zoomAndRotation.getX();
					int flatZoomLevel = mapRenderer.getFlatZoomLevel().ordinal();
					float flatVisualZoom = mapRenderer.getFlatVisualZoom();
					float flatZoomFloatPart = flatVisualZoom >= 1.0f
							? flatVisualZoom - 1.0f
							: (flatVisualZoom - 1.0f) * 2.0f;
					Zoom zoom = new Zoom(flatZoomLevel, flatZoomFloatPart, getMinZoom(), getMaxZoom());
					zoom.calculateAnimatedZoom(flatZoomLevel, relativeZoomAnimation);
					int zoomLevel = zoom.getBaseZoom();
					double zoomAnimation = zoom.getZoomAnimation();
					double zoomFloatPart = zoom.getZoomFloatPart();

					float finalZoomFloatPart = (float) (zoomAnimation + zoomFloatPart);
					float visualZoom = finalZoomFloatPart >= 0
							? 1 + finalZoomFloatPart
							: 1 + 0.5f * finalZoomFloatPart;
					mapRenderer.setFlatZoom(ZoomLevel.swigToEnum(zoomLevel), visualZoom);
					float zoomMagnifier = application.getOsmandMap().getMapDensity();
					mapRenderer.setVisualZoomShift(zoomMagnifier - 1.0f);

					int surfaceZoomLevel = mapRenderer.getZoomLevel().ordinal();
					float surfaceVisualZoom = mapRenderer.getVisualZoom();
					float surfaceZoomFloatPart = surfaceVisualZoom >= 1.0f
							? surfaceVisualZoom - 1.0f
							: (surfaceVisualZoom - 1.0f) * 2.0f;

					currentViewport.setZoomAndAnimation(surfaceZoomLevel, zoomAnimation, surfaceZoomFloatPart);
				}
				if (startRotating) {
					float angleShift = (float) zoomAndRotation.getY();
					this.rotate = MapUtils.unifyRotationTo360(this.rotate - angleShift);
					mapRenderer.setAzimuth(-this.rotate);
					currentViewport.setRotate(this.rotate);
				}
			}
			PointI target31 = mapRenderer.getState().getTarget31();
			currentViewport.setLatLonCenter(MapUtils.get31LatitudeY(target31.getY()), MapUtils.get31LongitudeX(target31.getX()));
			refreshMap();
			notifyLocationListeners(getLatitude(), getLongitude());
		}
	}

	public void moveTo(float dx, float dy, boolean notify) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			PointI point31 = new PointI();
			PointI center = mapRenderer.getTargetScreenPosition();
			if (center.getX() < 0 || center.getY() < 0) {
				PointD newCenter = new PointD(
						(double) mapRenderer.getWindowWidth() / 2.0 + (double) dx,
						(double) mapRenderer.getWindowHeight() / 2.0 + (double) dy);
				if (mapRenderer.getLocationFromScreenPoint(newCenter, point31)) {
					mapRenderer.setTarget(point31, false, false);
					currentViewport.setLatLonCenter(MapUtils.get31LatitudeY(point31.getY()), MapUtils.get31LongitudeX(point31.getX()));
				}
			} else {
				PointD newCenter = new PointD(
						(double) center.getX() + (double) dx,
						(double) center.getY() + (double) dy);
				PointI prevPoint31 = new PointI();
				if (mapRenderer.getLocationFromScreenPoint(center, prevPoint31)
						&& mapRenderer.getLocationFromScreenPoint(newCenter, point31)) {
					point31 = NativeUtilities.calculateNewTarget31(mapRenderer.getTarget(),
							new PointI(point31.getX() - prevPoint31.getX(), point31.getY() - prevPoint31.getY()));
					mapRenderer.setTarget(point31);
					currentViewport.setLatLonCenter(MapUtils.get31LatitudeY(point31.getY()), MapUtils.get31LongitudeX(point31.getX()));
				}
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

		animatedDraggingThread.startMoving(clat, clon, zoom.getBaseZoom(), zoom.getZoomFloatPart());
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
			animatedDraggingThread.startMoving(clat, clon, tb.getZoom());
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
			int endZoom = normalizeZoomWithLimits(getZoom() + zoomDir);
			getAnimatedDraggingThread().startMoving(lat, lon, endZoom);
			return true;
		}
		return false;
	}

	private int normalizeZoomWithLimits(int targetZoom) {
		int minZoom = getMinZoom();
		int maxZoom = getMaxZoom();
		Zoom zoom = new Zoom(targetZoom, getZoomFloatPart(), minZoom, maxZoom);
		if (!zoom.isZoomOutAllowed()) {
			return minZoom;
		} else if (!zoom.isZoomInAllowed()) {
			return maxZoom;
		}
		return targetZoom;
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
					setCurrentZoom();
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

		for (TouchListener listener : touchListeners) {
			listener.onTouchEvent(event);
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

		private static final float ZONE_0_ANGLE_THRESHOLD = 2;
		private static final float ZONE_1_ANGLE_THRESHOLD = 10;
		private static final float ZONE_2_ANGLE_THRESHOLD = 15;
		private static final float ZONE_3_ANGLE_THRESHOLD = 30;
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

		@Override
		public void onZoomOrRotationEnded(double relativeToStart, float angleRelative) {
			MeasurementToolLayer layer = getMeasurementToolLayer();
			MapRendererView mapRenderer = getMapRenderer();
			boolean completedAnimation = mapRenderer != null && (layer == null || !layer.isInMeasurementMode());
			int newIntZoom = getZoom();

			if (completedAnimation) {
				float newZoomFloatPart = isSteplessZoomSupported()
						? (float) (currentViewport.getZoomAnimation() + currentViewport.getZoomFloatPart())
						: 0.0f;
				currentViewport.setZoomAndAnimation(newIntZoom, 0.0, newZoomFloatPart);
				refreshMap();
			} else {
				finishZoomAndRotationGesture();
			}

			if (startRotating) {
				if (!completedAnimation) {
					rotateToAnimate(initialViewport.getRotate() + angleRelative);
				}
				if (angleRelative != 0) {
					mapViewTrackingUtilities.checkAndUpdateManualRotationMode();
				}
			}
			if (application.accessibilityEnabled()) {
				if (newIntZoom != initialViewport.getZoom()) {
					showMessage(application.getString(R.string.zoomIs) + " " + newIntZoom);
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
			finishZoomAndRotationGesture();
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
		public void onStopChangingViewAngle() {
			if (mapGestureAllowed(MapGestureType.TWO_POINTERS_TILT)) {
				MapRendererView mapRenderer = getMapRenderer();
				if (mapRenderer != null) {
					notifyOnStopChangingElevation(mapRenderer.getElevationAngle());
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
			if (mapGestureAllowed(MapGestureType.TWO_POINTERS_ROTATION)) {
				startRotating = true;
			} else {
				relAngle = 0;
			}

			if (deltaZoom != 0 || relAngle != 0) {
				changeZoomPosition((float) deltaZoom, relAngle);
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
				Zoom zoom = getCurrentZoom();
				if (zoom.isZoomInAllowed()) {
					zoom.zoomIn();

					RotatedTileBox tb = getCurrentRotatedTileBox();
					LatLon latlon = NativeUtilities.getLatLonFromElevatedPixel(mapRenderer, tb, e.getX(), e.getY());
					if (hasMapRenderer()) {
						getAnimatedDraggingThread().startZooming(zoom.getBaseZoom(), zoom.getZoomFloatPart(), latlon, true);
					} else {
						getAnimatedDraggingThread().startMoving(
								latlon.getLatitude(), latlon.getLongitude(), zoom.getBaseZoom(), zoom.getZoomFloatPart());
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
						(doubleTapScaleDetector == null || !doubleTapScaleDetector.isInZoomMode())) {
					if (startZooming) {
						blockTwoFingersTap = true;
					}
					zoomAndRotateToAnimate(startZooming, startRotating);
				} else {
					zoomToAnimate(initialViewport, deltaZoom, multiTouchCenterX, multiTouchCenterY);
					rotateToAnimate(calcRotate, multiTouchCenterX, multiTouchCenterY);
				}
			} else {
				LatLon r = calc.getLatLonFromPixel(cp.x + cp.x - multiTouchCenterX, cp.y + cp.y - multiTouchCenterY);
				setLatLon(r.getLatitude(), r.getLongitude());
				zoomToAnimate(initialViewport, deltaZoom, multiTouchCenterX, multiTouchCenterY);
				rotateToAnimate(calcRotate);
			}
		}

		public void finishZoomAndRotationGesture() {
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
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			mapRenderer.setElevationAngle(angle);
		}
		notifyOnElevationChanging(angle);
	}

	private void notifyOnElevationChanging(float angle) {
		for (ElevationListener listener : elevationListeners) {
			listener.onElevationChanging(angle);
		}
	}

	private void notifyOnStopChangingElevation(float angle) {
		for (ElevationListener listener : elevationListeners) {
			listener.onStopChangingElevation(angle);
		}
	}

	private void notifyLocationListeners(double lat, double lon) {
		for (IMapLocationListener listener : locationListeners) {
			listener.locationChanged(lat, lon, this);
		}
	}

	private void notifyViewportChanged() {
		for (ViewportListener listener : viewportListeners) {
			listener.onViewportChanged();
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
			wasMapLinkedBeforeGesture = mapViewTrackingUtilities.isMapLinkedToLocation();
			return false;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			if (e1 != null) {
				animatedDraggingThread.startDragging(velocityX / 3, velocityY / 3,
						e1.getX() + scrollDistanceX, e1.getY() + scrollDistanceY,
						e2.getX() + scrollDistanceX, e2.getY() + scrollDistanceY, true);
			}
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
		public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
			if (multiTouchSupport == null || (!multiTouchSupport.isInTiltMode() && !multiTouchSupport.isInZoomAndRotationMode())) {
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
					scrollDistanceX = distanceX;
					scrollDistanceY = distanceY;
					PointI touchPoint = new PointI((int) (e2.getX() + scrollDistanceX), (int) (e2.getY() + scrollDistanceY));
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

	public void applyBatterySavingModeSetting(MapRendererView mapRenderer) {
		if (settings.BATTERY_SAVING_MODE.get()) {
			mapRenderer.enableBatterySavingMode();
		} else {
			mapRenderer.disableBatterySavingMode();
		}
	}

	public void applyDebugSettings(MapRendererView mapRenderer) {
		OsmandDevelopmentPlugin plugin = PluginsHelper.getPlugin(OsmandDevelopmentPlugin.class);
		if (plugin != null) {
			boolean show = plugin.SHOW_SYMBOLS_DEBUG_INFO.get();
			boolean allow = plugin.ALLOW_SYMBOLS_DISPLAY_ON_TOP.get();
			MapRendererDebugSettings debugSettings = mapRenderer.getDebugSettings();
			debugSettings.setDebugStageEnabled(show);
			debugSettings.setShowSymbolsMarksRejectedByViewpoint(show);
			debugSettings.setShowSymbolsBBoxesRejectedByIntersectionCheck(show);
			debugSettings.setShowSymbolsBBoxesRejectedByMinDistanceToSameContentFromOtherSymbolCheck(show);
			debugSettings.setShowSymbolsBBoxesRejectedByPresentationMode(show);
			debugSettings.setShowTooShortOnPathSymbolsRenderablesPaths(show);
			debugSettings.setSkipSymbolsIntersectionCheck(allow);
			mapRenderer.setDebugSettings(debugSettings);
		}
	}

	private boolean isUseOpenGL() {
		return getApplication().useOpenGlRenderer();
	}

	private boolean isSteplessZoomSupported() {
		return hasMapRenderer();
	}

	public interface ElevationListener {
		void onElevationChanging(float angle);

		void onStopChangingElevation(float angle);
	}
}

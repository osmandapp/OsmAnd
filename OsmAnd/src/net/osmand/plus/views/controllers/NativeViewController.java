package net.osmand.plus.views.controllers;

import android.content.Context;
import android.opengl.EGL14;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.FrameLayout;
import net.osmand.access.MapAccessibilityActions;
import net.osmand.core.android.CoreResourcesFromAndroidAssetsCustom;
import net.osmand.core.jni.*;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.MapTileDownloader;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityLayers;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.helpers.TwoFingerTapDetector;
import net.osmand.plus.render.NativeCppLibrary;
import net.osmand.plus.render.NativeOsmandLibrary;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;

import javax.microedition.khronos.egl.*;
import java.io.File;
import java.util.List;

/**
 * Created by Denis on 01.10.2014.
 */
public class NativeViewController extends MapViewBaseController {

	private GLSurfaceView glSurfaceView;
	private OsmandSettings settings;
	private MapActivity mapActivity;
	private float displayDensityFactor;
	private int referenceTileSize;
	private int rasterTileSize;
	private IMapStylesCollection mapStylesCollection;
	private ResolvedMapStyle mapStyle;
	private ObfsCollection obfsCollection;
	private MapPresentationEnvironment mapPresentationEnvironment;
	private Primitiviser primitiviser;
	private BinaryMapDataProvider binaryMapDataProvider;
	private BinaryMapPrimitivesProvider binaryMapPrimitivesProvider;
	private BinaryMapStaticSymbolsProvider binaryMapStaticSymbolsProvider;
	private BinaryMapRasterBitmapTileProvider binaryMapRasterBitmapTileProvider;
	private OnlineRasterMapTileProvider onlineMapRasterBitmapTileProvider;
	private IMapRenderer mapRenderer;
	private GpuWorkerThreadPrologue gpuWorkerThreadPrologue;
	private GpuWorkerThreadEpilogue gpuWorkerThreadEpilogue;
	private RenderRequestCallback renderRequestCallback;
	private QIODeviceLogSink fileLogSink;
	private RotatedTileBox currentViewport = null;

	private boolean offlineMap = true;

	private GestureDetector gestureDetector;

	public static final String NATIVE_TAG = "NativeRender";
	private CoreResourcesFromAndroidAssetsCustom coreResources;

	TwoFingerTapDetector twoFingerTapDetector = new TwoFingerTapDetector() {
		@Override
		public void onTwoFingerTap() {
			currentViewport.setZoom(currentViewport.getZoom() - 1);
			updateView();
		}
	};


	public NativeViewController(GLSurfaceView surfaceView, MapActivity activity) {
		this.glSurfaceView = surfaceView;
		this.settings = activity.getMyApplication().getSettings();
		this.mapActivity = activity;
		loadLibraries();
		setupView();
	}

	private void loadLibraries() {
		NativeCppLibrary.loadLibrary("gnustl_shared");
		NativeCppLibrary.loadLibrary("Qt5Core");
		NativeCppLibrary.loadLibrary("Qt5Network");
		NativeCppLibrary.loadLibrary("Qt5Sql");
		NativeCppLibrary.loadLibrary("OsmAndCoreWithJNI");
	}

	private void setupView() {
		WindowManager mgr = (WindowManager)mapActivity.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics dm = new DisplayMetrics();
		mgr.getDefaultDisplay().getMetrics(dm);
		currentViewport = new RotatedTileBox.RotatedTileBoxBuilder().
				setLocation(settings.getLastKnownMapLocation().getLatitude(),
						settings.getLastKnownMapLocation().getLongitude()).setZoomAndScale(settings.getLastKnownMapZoom(), 0).
				setPixelDimensions(dm.widthPixels, dm.heightPixels).build();
		currentViewport.setDensity(dm.density);



		gestureDetector = new GestureDetector(mapActivity, new GestureDetector.OnGestureListener() {

			@Override
			public boolean onSingleTapUp(MotionEvent e) {
				return false;
			}

			@Override
			public void onShowPress(MotionEvent e) {
			}

			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
				final QuadPoint cp = currentViewport.getCenterPixelPoint();
				final LatLon latlon = currentViewport.getLatLonFromPixel(cp.x + distanceX, cp.y + distanceY);
				currentViewport.setLatLonCenter(latlon.getLatitude(), latlon.getLongitude());
				updateView();
				return false;
			}

			@Override
			public void onLongPress(MotionEvent e) {
			}

			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
				return false;
			}

			@Override
			public boolean onDown(MotionEvent e) {
				return false;
			}
		});
		gestureDetector.setOnDoubleTapListener(new GestureDetector.OnDoubleTapListener() {
			@Override
			public boolean onSingleTapConfirmed(MotionEvent e) {
				return false;
			}

			@Override
			public boolean onDoubleTapEvent(MotionEvent e) {
				return false;
			}

			@Override
			public boolean onDoubleTap(MotionEvent e) {
				currentViewport.setZoom(currentViewport.getZoom() + 1);
				updateView();
				return true;
			}
		});

		// Get device display density factor
		DisplayMetrics displayMetrics = new DisplayMetrics();
		mapActivity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		displayDensityFactor = displayMetrics.densityDpi / 160.0f;
		referenceTileSize = (int)(256 * displayDensityFactor);
		rasterTileSize = Integer.highestOneBit(referenceTileSize - 1) * 2;
		Log.i(NATIVE_TAG, "displayDensityFactor = " + displayDensityFactor);
		Log.i(NATIVE_TAG, "referenceTileSize = " + referenceTileSize);
		Log.i(NATIVE_TAG, "rasterTileSize = " + rasterTileSize);

		Log.i(NATIVE_TAG, "Initializing core...");
		coreResources = CoreResourcesFromAndroidAssetsCustom.loadFromCurrentApplication(mapActivity.getMyApplication());
		OsmAndCore.InitializeCore(coreResources);

		File directory =mapActivity.getMyApplication().getAppPath("");
		fileLogSink = QIODeviceLogSink.createFileLogSink(directory.getAbsolutePath() + "osmandcore.log");
		Logger.get().addLogSink(fileLogSink);

		Log.i(NATIVE_TAG, "Going to resolve default embedded style...");
		mapStylesCollection = new MapStylesCollection();
		mapStyle = mapStylesCollection.getResolvedStyleByName("default");
		if (mapStyle == null)
		{
			Log.e(NATIVE_TAG, "Failed to resolve style 'default'");
			System.exit(0);
		}

		Log.i(NATIVE_TAG, "Going to prepare OBFs collection");
		obfsCollection = new ObfsCollection();

		Log.i(NATIVE_TAG, "Will load OBFs from " + Environment.getExternalStorageDirectory() + "/osmand");

		Log.i(NATIVE_TAG, "Will load OBFs from " + directory.getAbsolutePath());
		obfsCollection.addDirectory(directory.getAbsolutePath(), false);

		Log.i(NATIVE_TAG, "Going to prepare all resources for renderer");
		mapPresentationEnvironment = new MapPresentationEnvironment(
				mapStyle,
				displayDensityFactor,
				"en"); //TODO: here should be current locale
		//mapPresentationEnvironment->setSettings(configuration.styleSettings);
		primitiviser = new Primitiviser(
				mapPresentationEnvironment);
		binaryMapDataProvider = new BinaryMapDataProvider(
				obfsCollection);
		binaryMapPrimitivesProvider = new BinaryMapPrimitivesProvider(
				binaryMapDataProvider,
				primitiviser,
				rasterTileSize);
		binaryMapStaticSymbolsProvider = new BinaryMapStaticSymbolsProvider(
				binaryMapPrimitivesProvider,
				rasterTileSize);
		binaryMapRasterBitmapTileProvider = new BinaryMapRasterBitmapTileProvider_Software(
				binaryMapPrimitivesProvider);

		onlineMapRasterBitmapTileProvider = OnlineTileSources.getBuiltIn().createProviderFor("Mapnik (OsmAnd)");

		Log.i(NATIVE_TAG, "Going to create renderer");
		mapRenderer = OsmAndCore.createMapRenderer(MapRendererClass.AtlasMapRenderer_OpenGLES2);
		if (mapRenderer == null)
		{
			Log.e(NATIVE_TAG, "Failed to create map renderer 'AtlasMapRenderer_OpenGLES2'");
			System.exit(0);
		}

		AtlasMapRendererConfiguration atlasRendererConfiguration = AtlasMapRendererConfiguration.Casts.upcastFrom(mapRenderer.getConfiguration());
		atlasRendererConfiguration.setReferenceTileSizeOnScreenInPixels(referenceTileSize);
		mapRenderer.setConfiguration(AtlasMapRendererConfiguration.Casts.downcastTo_MapRendererConfiguration(atlasRendererConfiguration));

		mapRenderer.addSymbolProvider(binaryMapStaticSymbolsProvider);
		updateView();
        /*
        IMapRasterBitmapTileProvider mapnik = OnlineTileSources.getBuiltIn().createProviderFor("Mapnik (OsmAnd)");
        if (mapnik == null)
            Log.e(NATIVE_TAG, "Failed to create mapnik");
        */
		if (offlineMap){
			mapRenderer.setRasterLayerProvider(RasterMapLayerId.BaseLayer, binaryMapRasterBitmapTileProvider);
		} else {
			mapRenderer.setRasterLayerProvider(RasterMapLayerId.BaseLayer, onlineMapRasterBitmapTileProvider);
		}

		//TODO:_glSurfaceView.setPreserveEGLContextOnPause(true);
		glSurfaceView.setEGLContextClientVersion(2);
		glSurfaceView.setEGLContextFactory(new EGLContextFactory());
		glSurfaceView.setRenderer(new NativeRenderer(mapRenderer));
		glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}

	protected void updateView() {
		mapRenderer.setAzimuth(0.0f);
		mapRenderer.setElevationAngle(90);
		mapRenderer.setTarget(new PointI(currentViewport.getCenter31X(), currentViewport.getCenter31Y()));
		mapRenderer.setZoom((float)currentViewport.getZoom() + (float)currentViewport.getZoomScale());
	}

	private class EGLContextFactory implements GLSurfaceView.EGLContextFactory {
		private EGLContext gpuWorkerContext;
		private EGLSurface gpuWorkerFakeSurface;

		public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig) {
			final String eglExtensions = egl.eglQueryString(display, EGL10.EGL_EXTENSIONS);
			Log.i(NATIVE_TAG, "EGL extensions: " + eglExtensions);
			final String eglVersion = egl.eglQueryString(display, EGL10.EGL_VERSION);
			Log.i(NATIVE_TAG, "EGL version: " + eglVersion);

			Log.i(NATIVE_TAG, "Creating main context...");
			final int[] contextAttribList = {
					EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
					EGL10.EGL_NONE };

			EGLContext mainContext = null;
			try {
				mainContext = egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, contextAttribList);
			} catch (Exception e) {
				Log.e(NATIVE_TAG, "Failed to create main context", e);
			}
			if (mainContext == null || mainContext == EGL10.EGL_NO_CONTEXT) {
				Log.e(NATIVE_TAG, "Failed to create main context: " + egl.eglGetError());
				mainContext = null;
				System.exit(0);
			}

			Log.i(NATIVE_TAG, "Creating GPU worker context...");
			try {
				gpuWorkerContext = egl.eglCreateContext(
						display,
						eglConfig,
						mainContext,
						contextAttribList);
			} catch (Exception e) {
				Log.e(NATIVE_TAG, "Failed to create GPU worker context", e);
			}
			if (gpuWorkerContext == null || gpuWorkerContext == EGL10.EGL_NO_CONTEXT)
			{
				Log.e(NATIVE_TAG, "Failed to create GPU worker context: " + egl.eglGetError());
				gpuWorkerContext = null;
			}

			if (gpuWorkerContext != null)
			{
				Log.i(NATIVE_TAG, "Creating GPU worker fake surface...");
				try {
					final int[] surfaceAttribList = {
							EGL10.EGL_WIDTH, 1,
							EGL10.EGL_HEIGHT, 1,
							EGL10.EGL_NONE };
					gpuWorkerFakeSurface = egl.eglCreatePbufferSurface(display, eglConfig, surfaceAttribList);
				} catch (Exception e) {
					Log.e(NATIVE_TAG, "Failed to create GPU worker fake surface", e);
				}
				if (gpuWorkerFakeSurface == null || gpuWorkerFakeSurface == EGL10.EGL_NO_SURFACE)
				{
					Log.e(NATIVE_TAG, "Failed to create GPU worker fake surface: " + egl.eglGetError());
					gpuWorkerFakeSurface = null;
				}
			}

			MapRendererSetupOptions rendererSetupOptions = new MapRendererSetupOptions();
			if (gpuWorkerContext != null && gpuWorkerFakeSurface != null) {
				rendererSetupOptions.setGpuWorkerThreadEnabled(true);
				gpuWorkerThreadPrologue = new GpuWorkerThreadPrologue(egl, display, gpuWorkerContext, gpuWorkerFakeSurface);
				rendererSetupOptions.setGpuWorkerThreadPrologue(gpuWorkerThreadPrologue.getBinding());
				gpuWorkerThreadEpilogue = new GpuWorkerThreadEpilogue(egl);
				rendererSetupOptions.setGpuWorkerThreadEpilogue(gpuWorkerThreadEpilogue.getBinding());
			} else {
				rendererSetupOptions.setGpuWorkerThreadEnabled(false);
			}
			renderRequestCallback = new RenderRequestCallback();
			rendererSetupOptions.setFrameUpdateRequestCallback(renderRequestCallback.getBinding());
			mapRenderer.setup(rendererSetupOptions);

			return mainContext;
		}

		public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
			egl.eglDestroyContext(display, context);

			if (gpuWorkerContext != null) {
				egl.eglDestroyContext(display, gpuWorkerContext);
				gpuWorkerContext = null;
			}

			if (gpuWorkerFakeSurface != null) {
				egl.eglDestroySurface(display, gpuWorkerFakeSurface);
				gpuWorkerFakeSurface = null;
			}
		}
	}

	private class GpuWorkerThreadEpilogue extends MapRendererSetupOptions.IGpuWorkerThreadEpilogue {
		public GpuWorkerThreadEpilogue(EGL10 egl) {
			_egl = egl;
		}

		private final EGL10 _egl;

		@Override
		public void method(IMapRenderer mapRenderer) {
			try {
				if (!_egl.eglWaitGL())
					Log.e(NATIVE_TAG, "Failed to wait for GPU worker context: " + _egl.eglGetError());
			} catch (Exception e) {
				Log.e(NATIVE_TAG, "Failed to wait for GPU worker context", e);
			}
		}
	}

	private class GpuWorkerThreadPrologue extends MapRendererSetupOptions.IGpuWorkerThreadPrologue {
		public GpuWorkerThreadPrologue(EGL10 egl, EGLDisplay eglDisplay, EGLContext context, EGLSurface surface) {
			_egl = egl;
			_eglDisplay = eglDisplay;
			_context = context;
			_eglSurface = surface;
		}

		private final EGL10 _egl;
		private final EGLDisplay _eglDisplay;
		private final EGLContext _context;
		private final EGLSurface _eglSurface;

		@Override
		public void method(IMapRenderer mapRenderer) {
			try {
				if (!_egl.eglMakeCurrent(_eglDisplay, _eglSurface, _eglSurface, _context))
					Log.e(NATIVE_TAG, "Failed to set GPU worker context active: " + _egl.eglGetError());
			} catch (Exception e) {
				Log.e(NATIVE_TAG, "Failed to set GPU worker context active", e);
			}
		}
	}

	private class RenderRequestCallback extends MapRendererSetupOptions.IFrameUpdateRequestCallback {
		@Override
		public void method(IMapRenderer mapRenderer) {
			glSurfaceView.requestRender();
		}
	}

	@Override
	public void setTrackBallDelegate(OnTrackBallListener trackBallDelegate) {
		super.setTrackBallDelegate(trackBallDelegate);
	}

	@Override
	public void setAccessibilityActions(MapAccessibilityActions accessibilityActions) {
		super.setAccessibilityActions(accessibilityActions);
	}

	@Override
	public void refreshMap(boolean b) {
		updateView();
	}

	@Override
	public void createLayers(MapActivityLayers mapLayers) {
		mapLayers.createLayers(glSurfaceView);
	}

	@Override
	public void setLatLon(double latitude, double longitude) {
		super.setLatLon(latitude, longitude);
	}

	@Override
	public void setIntZoom(int i) {
		super.setIntZoom(i);
	}

	@Override
	public void addView(FrameLayout view) {
		super.addView(view);
	}

	@Override
	public void setTrackingUtilities(MapViewTrackingUtilities mapViewTrackingUtilities) {
		super.setTrackingUtilities(mapViewTrackingUtilities);
	}

	@Override
	public void tileDownloaded(MapTileDownloader.DownloadRequest request) {
		super.tileDownloaded(request);
	}

	@Override
	public ViewParent getParentView() {
		return glSurfaceView.getParent();
	}

	@Override
	public List<OsmandMapLayer> getLayers() {
		return super.getLayers();
	}

	@Override
	public double getLatitude() {
		return super.getLatitude();
	}

	@Override
	public double getLongitude() {
		return super.getLongitude();
	}

	@Override
	public void startMoving(double latitude, double longitude, int mapZoomToShow, boolean b) {
		super.startMoving(latitude, longitude, mapZoomToShow, b);
	}

	@Override
	public int getZoom() {
		return super.getZoom();
	}

	@Override
	public void startZooming(int newZoom, boolean changeLocation) {
		super.startZooming(newZoom, changeLocation);
	}

	@Override
	public boolean isZooming() {
		return super.isZooming();
	}

	@Override
	public RotatedTileBox getCurrentRotatedTileBox() {
		return super.getCurrentRotatedTileBox();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void updateLayers(MapActivityLayers mapLayers) {
		super.updateLayers(mapLayers);
	}

	@Override
	public void setComplexZoom() {
		super.setComplexZoom();
	}

	@Override
	public void showAndHideMapPosition() {
		super.showAndHideMapPosition();
	}

	@Override
	public OsmandMapTileView getMapTileView() {
		return super.getMapTileView();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (twoFingerTapDetector.onTouchEvent(event)) {
			return true;
		}
		return gestureDetector.onTouchEvent(event);
	}

	@Override
	public void resume() {
		glSurfaceView.onResume();
	}

	@Override
	public void destroy() {
		if (mapStylesCollection != null) {
			mapStylesCollection.delete();
			mapStylesCollection = null;
		}

		if (mapStyle != null) {
			mapStyle.delete();
			mapStyle = null;
		}

		if (obfsCollection != null) {
			obfsCollection.delete();
			obfsCollection = null;
		}

		if (mapPresentationEnvironment != null) {
			mapPresentationEnvironment.delete();
			mapPresentationEnvironment = null;
		}

		if (primitiviser != null) {
			primitiviser.delete();
			primitiviser = null;
		}

		if (binaryMapDataProvider != null) {
			binaryMapDataProvider.delete();
			binaryMapDataProvider = null;
		}

		if (binaryMapPrimitivesProvider != null) {
			binaryMapPrimitivesProvider.delete();
			binaryMapPrimitivesProvider = null;
		}

		if (binaryMapStaticSymbolsProvider != null) {
			binaryMapStaticSymbolsProvider.delete();
			binaryMapStaticSymbolsProvider = null;
		}

		if (binaryMapRasterBitmapTileProvider != null) {
			binaryMapRasterBitmapTileProvider.delete();
			binaryMapRasterBitmapTileProvider = null;
		}

		if (mapRenderer != null) {
			mapRenderer.delete();
			mapRenderer = null;
		}

		OsmAndCore.ReleaseCore();
	}
}

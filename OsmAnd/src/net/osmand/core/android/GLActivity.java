package net.osmand.core.android;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

import net.osmand.core.android.CoreResourcesFromAndroidAssets;
import net.osmand.core.jni.AreaI;
import net.osmand.core.jni.AtlasMapRendererConfiguration;
import net.osmand.core.jni.BinaryMapDataProvider;
import net.osmand.core.jni.BinaryMapPrimitivesProvider;
import net.osmand.core.jni.BinaryMapRasterBitmapTileProvider;
import net.osmand.core.jni.BinaryMapRasterBitmapTileProvider_Software;
import net.osmand.core.jni.BinaryMapStaticSymbolsProvider;
import net.osmand.core.jni.IMapRenderer;
import net.osmand.core.jni.IMapStylesCollection;
import net.osmand.core.jni.Logger;
import net.osmand.core.jni.MapPresentationEnvironment;
import net.osmand.core.jni.MapRendererClass;
import net.osmand.core.jni.MapRendererSetupOptions;
import net.osmand.core.jni.MapStyle;
import net.osmand.core.jni.MapStylesCollection;
import net.osmand.core.jni.ObfsCollection;
import net.osmand.core.jni.OsmAndCore;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.Primitiviser;
import net.osmand.core.jni.QIODeviceLogSink;
import net.osmand.core.jni.RasterMapLayerId;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.helpers.SimpleTwoFingerTapDetector;
import net.osmand.plus.render.NativeOsmandLibrary;
import android.app.Activity;
import android.content.Context;
import android.opengl.EGL14;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;

public class GLActivity extends Activity {

	static {
		NativeOsmandLibrary.loadLibrary("gnustl_shared");
		NativeOsmandLibrary.loadLibrary("Qt5Core");
		NativeOsmandLibrary.loadLibrary("Qt5Network");
		NativeOsmandLibrary.loadLibrary("Qt5Sql");
		NativeOsmandLibrary.loadLibrary("OsmAndCoreWithJNI");
	}
    private static final String TAG = "OsmAndCoreSample";

    private CoreResourcesFromAndroidAssets _coreResources;

    private float _displayDensityFactor;
    private int _referenceTileSize;
    private int _rasterTileSize;
    private IMapStylesCollection _mapStylesCollection;
    private MapStyle _mapStyle;
    private ObfsCollection _obfsCollection;
    private MapPresentationEnvironment _mapPresentationEnvironment;
    private Primitiviser _primitiviser;
    private BinaryMapDataProvider _binaryMapDataProvider;
    private BinaryMapPrimitivesProvider _binaryMapPrimitivesProvider;
    private BinaryMapStaticSymbolsProvider _binaryMapStaticSymbolsProvider;
    private BinaryMapRasterBitmapTileProvider _binaryMapRasterBitmapTileProvider;
    private IMapRenderer _mapRenderer;
    private GpuWorkerThreadPrologue _gpuWorkerThreadPrologue;
    private GpuWorkerThreadEpilogue _gpuWorkerThreadEpilogue;
    private RenderRequestCallback _renderRequestCallback;
    private QIODeviceLogSink _fileLogSink;
    private RotatedTileBox currentViewport = null;
    
	private GestureDetector gestureDetector;

    
    protected OsmandApplication getApp() {
    	return (OsmandApplication) getApplication();
    }
    
    private boolean afterTwoFingerTap = false;
	SimpleTwoFingerTapDetector twoFingerTapDetector = new SimpleTwoFingerTapDetector() {
		@Override
		public void onTwoFingerTap() {
			afterTwoFingerTap = true;
			currentViewport.setZoom(currentViewport.getZoom() - 1);
			updateView();
		}
	};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        OsmandSettings st = getApp().getSettings();
        WindowManager mgr = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics dm = new DisplayMetrics();
		mgr.getDefaultDisplay().getMetrics(dm);
		currentViewport = new RotatedTileBox.RotatedTileBoxBuilder().
				setLocation(st.getLastKnownMapLocation().getLatitude(), 
						st.getLastKnownMapLocation().getLongitude()).setZoomAndScale(st.getLastKnownMapZoom(), 0).
						setPixelDimensions(dm.widthPixels, dm.heightPixels).build();
		currentViewport.setDensity(dm.density);
		
		
		
		gestureDetector = new GestureDetector(this, new android.view.GestureDetector.OnGestureListener() {
			
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
		gestureDetector.setOnDoubleTapListener(new OnDoubleTapListener() {
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
        		
        setContentView(R.layout.activity_gl);

        // Get device display density factor
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        _displayDensityFactor = displayMetrics.densityDpi / 160.0f;
        _referenceTileSize = (int)(256 * _displayDensityFactor);
        _rasterTileSize = Integer.highestOneBit(_referenceTileSize - 1) * 2;
        Log.i(TAG, "displayDensityFactor = " + _displayDensityFactor);
        Log.i(TAG, "referenceTileSize = " + _referenceTileSize);
        Log.i(TAG, "rasterTileSize = " + _rasterTileSize);

        Log.i(TAG, "Initializing core...");
        _coreResources = CoreResourcesFromAndroidAssets.loadFromCurrentApplication(this);
        OsmAndCore.InitializeCore(_coreResources);

        _fileLogSink = QIODeviceLogSink.createFileLogSink(Environment.getExternalStorageDirectory() + "/osmand/osmandcore.log");
        Logger.get().addLogSink(_fileLogSink);

        Log.i(TAG, "Going to resolve default embedded style...");
        _mapStylesCollection = new MapStylesCollection();
        _mapStyle = _mapStylesCollection.getBakedStyle("default");
        if (_mapStyle == null)
        {
            Log.e(TAG, "Failed to resolve style 'default'");
            System.exit(0);
        }

        Log.i(TAG, "Going to prepare OBFs collection");
        _obfsCollection = new ObfsCollection();
        Log.i(TAG, "Will load OBFs from " + Environment.getExternalStorageDirectory() + "/osmand");
        _obfsCollection.addDirectory(Environment.getExternalStorageDirectory() + "/osmand", false);

        Log.i(TAG, "Going to prepare all resources for renderer");
        _mapPresentationEnvironment = new MapPresentationEnvironment(
                _mapStyle,
                _displayDensityFactor,
                "en"); //TODO: here should be current locale
        //mapPresentationEnvironment->setSettings(configuration.styleSettings);
        _primitiviser = new Primitiviser(
                _mapPresentationEnvironment);
        _binaryMapDataProvider = new BinaryMapDataProvider(
                _obfsCollection);
        _binaryMapPrimitivesProvider = new BinaryMapPrimitivesProvider(
                _binaryMapDataProvider,
                _primitiviser,
                _rasterTileSize);
        _binaryMapStaticSymbolsProvider = new BinaryMapStaticSymbolsProvider(
                _binaryMapPrimitivesProvider,
                _rasterTileSize);
        _binaryMapRasterBitmapTileProvider = new BinaryMapRasterBitmapTileProvider_Software(
                _binaryMapPrimitivesProvider);

        Log.i(TAG, "Going to create renderer");
        _mapRenderer = OsmAndCore.createMapRenderer(MapRendererClass.AtlasMapRenderer_OpenGLES2);
        if (_mapRenderer == null)
        {
            Log.e(TAG, "Failed to create map renderer 'AtlasMapRenderer_OpenGLES2'");
            System.exit(0);
        }

        AtlasMapRendererConfiguration atlasRendererConfiguration = AtlasMapRendererConfiguration.Casts.upcastFrom(_mapRenderer.getConfiguration());
        atlasRendererConfiguration.setReferenceTileSizeOnScreenInPixels(_referenceTileSize);
        _mapRenderer.setConfiguration(AtlasMapRendererConfiguration.Casts.downcastTo_MapRendererConfiguration(atlasRendererConfiguration));

        _mapRenderer.addSymbolProvider(_binaryMapStaticSymbolsProvider);
        updateView();
        /*
        IMapRasterBitmapTileProvider mapnik = OnlineTileSources.getBuiltIn().createProviderFor("Mapnik (OsmAnd)");
        if (mapnik == null)
            Log.e(TAG, "Failed to create mapnik");
        */
        _mapRenderer.setRasterLayerProvider(RasterMapLayerId.BaseLayer, _binaryMapRasterBitmapTileProvider);

        _glSurfaceView = (GLSurfaceView) findViewById(R.id.glSurfaceView);
        //TODO:_glSurfaceView.setPreserveEGLContextOnPause(true);
        _glSurfaceView.setEGLContextClientVersion(2);
        _glSurfaceView.setEGLContextFactory(new EGLContextFactory());
        _glSurfaceView.setRenderer(new Renderer());
        _glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

	protected void updateView() {
		_mapRenderer.setAzimuth(0.0f);
		_mapRenderer.setElevationAngle(90);
		_mapRenderer.setTarget(new PointI(currentViewport.getCenter31X(), currentViewport.getCenter31Y()));
		_mapRenderer.setZoom(currentViewport.getZoom() + currentViewport.getZoomScale());
	}

	private GLSurfaceView _glSurfaceView;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	if (twoFingerTapDetector.onTouchEvent(event)) {
			return true;
		}
    	return gestureDetector.onTouchEvent(event);
    }
    @Override
    protected void onPause() {
        super.onPause();
        _glSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        _glSurfaceView.onResume();
    }

    @Override
    protected void onDestroy() {
        if (_mapStylesCollection != null) {
            _mapStylesCollection.delete();
            _mapStylesCollection = null;
        }

        if (_mapStyle != null) {
            _mapStyle.delete();
            _mapStyle = null;
        }

        if (_obfsCollection != null) {
            _obfsCollection.delete();
            _obfsCollection = null;
        }

        if (_mapPresentationEnvironment != null) {
            _mapPresentationEnvironment.delete();
            _mapPresentationEnvironment = null;
        }

        if (_primitiviser != null) {
            _primitiviser.delete();
            _primitiviser = null;
        }

        if (_binaryMapDataProvider != null) {
            _binaryMapDataProvider.delete();
            _binaryMapDataProvider = null;
        }

        if (_binaryMapPrimitivesProvider != null) {
            _binaryMapPrimitivesProvider.delete();
            _binaryMapPrimitivesProvider = null;
        }

        if (_binaryMapStaticSymbolsProvider != null) {
            _binaryMapStaticSymbolsProvider.delete();
            _binaryMapStaticSymbolsProvider = null;
        }

        if (_binaryMapRasterBitmapTileProvider != null) {
            _binaryMapRasterBitmapTileProvider.delete();
            _binaryMapRasterBitmapTileProvider = null;
        }

        if (_mapRenderer != null) {
            _mapRenderer.delete();
            _mapRenderer = null;
        }

        OsmAndCore.ReleaseCore();

        super.onDestroy();
    }

    private class RenderRequestCallback extends MapRendererSetupOptions.IFrameUpdateRequestCallback {
        @Override
        public void method(IMapRenderer mapRenderer) {
            _glSurfaceView.requestRender();
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
                    Log.e(TAG, "Failed to set GPU worker context active: " + _egl.eglGetError());
            } catch (Exception e) {
                Log.e(TAG, "Failed to set GPU worker context active", e);
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
                    Log.e(TAG, "Failed to wait for GPU worker context: " + _egl.eglGetError());
            } catch (Exception e) {
                Log.e(TAG, "Failed to wait for GPU worker context", e);
            }
        }
    }

    private class EGLContextFactory implements GLSurfaceView.EGLContextFactory {
        private EGLContext _gpuWorkerContext;
        private EGLSurface _gpuWorkerFakeSurface;

        public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig) {
            final String eglExtensions = egl.eglQueryString(display, EGL10.EGL_EXTENSIONS);
            Log.i(TAG, "EGL extensions: " + eglExtensions);
            final String eglVersion = egl.eglQueryString(display, EGL10.EGL_VERSION);
            Log.i(TAG, "EGL version: " + eglVersion);

            Log.i(TAG, "Creating main context...");
            final int[] contextAttribList = {
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL10.EGL_NONE };

            EGLContext mainContext = null;
            try {
                mainContext = egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, contextAttribList);
            } catch (Exception e) {
                Log.e(TAG, "Failed to create main context", e);
            }
            if (mainContext == null || mainContext == EGL10.EGL_NO_CONTEXT) {
                Log.e(TAG, "Failed to create main context: " + egl.eglGetError());
                mainContext = null;
                System.exit(0);
            }

            Log.i(TAG, "Creating GPU worker context...");
            try {
                _gpuWorkerContext = egl.eglCreateContext(
                        display,
                        eglConfig,
                        mainContext,
                        contextAttribList);
            } catch (Exception e) {
                Log.e(TAG, "Failed to create GPU worker context", e);
            }
            if (_gpuWorkerContext == null || _gpuWorkerContext == EGL10.EGL_NO_CONTEXT)
            {
                Log.e(TAG, "Failed to create GPU worker context: " + egl.eglGetError());
                _gpuWorkerContext = null;
            }

            if (_gpuWorkerContext != null)
            {
                Log.i(TAG, "Creating GPU worker fake surface...");
                try {
                    final int[] surfaceAttribList = {
                            EGL10.EGL_WIDTH, 1,
                            EGL10.EGL_HEIGHT, 1,
                            EGL10.EGL_NONE };
                    _gpuWorkerFakeSurface = egl.eglCreatePbufferSurface(display, eglConfig, surfaceAttribList);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to create GPU worker fake surface", e);
                }
                if (_gpuWorkerFakeSurface == null || _gpuWorkerFakeSurface == EGL10.EGL_NO_SURFACE)
                {
                    Log.e(TAG, "Failed to create GPU worker fake surface: " + egl.eglGetError());
                    _gpuWorkerFakeSurface = null;
                }
            }

            MapRendererSetupOptions rendererSetupOptions = new MapRendererSetupOptions();
            if (_gpuWorkerContext != null && _gpuWorkerFakeSurface != null) {
                rendererSetupOptions.setGpuWorkerThreadEnabled(true);
                _gpuWorkerThreadPrologue = new GpuWorkerThreadPrologue(egl, display, _gpuWorkerContext, _gpuWorkerFakeSurface);
                rendererSetupOptions.setGpuWorkerThreadPrologue(_gpuWorkerThreadPrologue.getBinding());
                _gpuWorkerThreadEpilogue = new GpuWorkerThreadEpilogue(egl);
                rendererSetupOptions.setGpuWorkerThreadEpilogue(_gpuWorkerThreadEpilogue.getBinding());
            } else {
                rendererSetupOptions.setGpuWorkerThreadEnabled(false);
            }
            _renderRequestCallback = new RenderRequestCallback();
            rendererSetupOptions.setFrameUpdateRequestCallback(_renderRequestCallback.getBinding());
            _mapRenderer.setup(rendererSetupOptions);

            return mainContext;
        }

        public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
            egl.eglDestroyContext(display, context);

            if (_gpuWorkerContext != null) {
                egl.eglDestroyContext(display, _gpuWorkerContext);
                _gpuWorkerContext = null;
            }

            if (_gpuWorkerFakeSurface != null) {
                egl.eglDestroySurface(display, _gpuWorkerFakeSurface);
                _gpuWorkerFakeSurface = null;
            }
        }
    }

    private class Renderer implements GLSurfaceView.Renderer {
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            Log.i(TAG, "onSurfaceCreated");
            if (_mapRenderer.isRenderingInitialized())
                _mapRenderer.releaseRendering();
        }

        public void onSurfaceChanged(GL10 gl, int width, int height) {
            Log.i(TAG, "onSurfaceChanged");
            _mapRenderer.setViewport(new AreaI(0, 0, height, width));
            _mapRenderer.setWindowSize(new PointI(width, height));

            if (!_mapRenderer.isRenderingInitialized())
            {
                if (!_mapRenderer.initializeRendering())
                    Log.e(TAG, "Failed to initialize rendering");
            }
        }

        public void onDrawFrame(GL10 gl) {
            _mapRenderer.update();

            if (_mapRenderer.prepareFrame())
                _mapRenderer.renderFrame();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
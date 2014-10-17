package net.osmand.plus.views.corenative;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import net.osmand.core.android.CoreResourcesFromAndroidAssetsCustom;
import net.osmand.core.jni.AtlasMapRendererConfiguration;
import net.osmand.core.jni.BinaryMapDataProvider;
import net.osmand.core.jni.BinaryMapPrimitivesProvider;
import net.osmand.core.jni.BinaryMapRasterLayerProvider_Software;
import net.osmand.core.jni.BinaryMapStaticSymbolsProvider;
import net.osmand.core.jni.IMapRenderer;
import net.osmand.core.jni.Logger;
import net.osmand.core.jni.MapPresentationEnvironment;
import net.osmand.core.jni.MapRendererClass;
import net.osmand.core.jni.MapRendererSetupOptions;
import net.osmand.core.jni.MapStylesCollection;
import net.osmand.core.jni.ObfsCollection;
import net.osmand.core.jni.OnlineRasterMapLayerProvider;
import net.osmand.core.jni.OnlineTileSources;
import net.osmand.core.jni.OsmAndCore;
import net.osmand.core.jni.Primitiviser;
import net.osmand.core.jni.QIODeviceLogSink;
import net.osmand.core.jni.RasterMapLayerId;
import net.osmand.core.jni.ResolvedMapStyle;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.render.NativeCppLibrary;
import android.content.Context;
import android.opengl.EGL14;
import android.opengl.GLSurfaceView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

/**
 * Created by Denis on 01.10.2014.
 */
public class NativeQtLibrary {
	
	private static boolean OFFLINE_MAP = true;
	private static IMapRenderer mapRenderer;
	public static final String NATIVE_TAG = "NativeRender";
	
	private static List<Object> doNotGc = new ArrayList<Object>();
	private static boolean init;
	

	private static <T> T notGc(T obj){
		doNotGc.add(obj);
		return obj;
	}
	
	private static <T> T notGcFor1Egl(T obj){
		doNotGc.add(obj);
		return obj;
	}
	
	public static void initView(GLSurfaceView glSurfaceView) {
		System.out.println("Init GL View");
		//TODO:_glSurfaceView.setPreserveEGLContextOnPause(true);
		glSurfaceView.setEGLContextClientVersion(2);
		glSurfaceView.setEGLContextFactory(new EGLContextFactory(glSurfaceView));
		glSurfaceView.setRenderer(new NativeRenderer(mapRenderer));
		glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		mapRenderer.setAzimuth(0.0f);
		mapRenderer.setElevationAngle(90);
	}
	
	public static IMapRenderer getMapRenderer() {
		return mapRenderer;
	}
	
	public static boolean isInit() {
		return init;
	}
	
	public static boolean tryCatchInit(OsmandApplication app) {
		try {
			init(app);
			return true;
		} catch(Throwable t) {
			t.printStackTrace();
			Log.e(NATIVE_TAG, "Failed to initialize");
			return false;
		}
		
	}
	
	public static void init(OsmandApplication app) {
		if (!init) {
			loadLibraries();
			initRenderer(app);
			init = true;
		}
	}
	
	private static void loadLibraries()  {
		NativeCppLibrary.loadLibrary("gnustl_shared");
		NativeCppLibrary.loadLibrary("Qt5Core");
		NativeCppLibrary.loadLibrary("Qt5Network");
		NativeCppLibrary.loadLibrary("Qt5Sql");
		NativeCppLibrary.loadLibrary("OsmAndCoreWithJNI");
	}

	private static void initRenderer(OsmandApplication app) {
		WindowManager mgr = (WindowManager)app.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics dm = new DisplayMetrics();
		mgr.getDefaultDisplay().getMetrics(dm);

		// Get device display density factor
//		DisplayMetrics displayMetrics = new DisplayMetrics();
//		act.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		DisplayMetrics displayMetrics = app.getResources().getDisplayMetrics();
		float displayDensityFactor = displayMetrics.densityDpi / 160.0f;
		int referenceTileSize = (int)(256 * displayDensityFactor);
		int rasterTileSize = Integer.highestOneBit(referenceTileSize - 1) * 2;
		Log.i(NATIVE_TAG, "displayDensityFactor = " + displayDensityFactor + 
				" referenceTileSize = " + referenceTileSize + " rasterTileSize = " + rasterTileSize);
		Log.i(NATIVE_TAG, "Initializing core...");
		CoreResourcesFromAndroidAssetsCustom coreResources = notGc(CoreResourcesFromAndroidAssetsCustom.loadFromCurrentApplication(app));
		OsmAndCore.InitializeCore(coreResources.instantiateProxy());

		// initialize log
		File directory = app.getAppPath("");
		QIODeviceLogSink fileLogSink = 
				notGc(QIODeviceLogSink.createFileLogSink(directory.getAbsolutePath() + "osmandcore.log"));
		Logger.get().addLogSink(fileLogSink);

		Log.i(NATIVE_TAG, "Going to resolve default embedded style...");
		MapStylesCollection mapStylesCollection = notGc(new MapStylesCollection());
		ResolvedMapStyle mapStyle = mapStylesCollection.getResolvedStyleByName("default");
		if (mapStyle == null) {
			throw new IllegalStateException("Failed to resolve style 'default'");
			
		}
		Log.i(NATIVE_TAG, "Will load OBFs from " + directory.getAbsolutePath());
		ObfsCollection obfsCollection = notGc(new ObfsCollection());
		obfsCollection.addDirectory(directory.getAbsolutePath(), false);

		Log.i(NATIVE_TAG, "Going to prepare all resources for renderer");
		Log.i(NATIVE_TAG, "Going to create renderer");
		mapRenderer = OsmAndCore.createMapRenderer(MapRendererClass.AtlasMapRenderer_OpenGLES2);
		if (mapRenderer == null) {
			throw new IllegalArgumentException("Failed to create map renderer 'AtlasMapRenderer_OpenGLES2'");
		}

		AtlasMapRendererConfiguration atlasRendererConfiguration = AtlasMapRendererConfiguration.Casts.upcastFrom(mapRenderer.getConfiguration());
		atlasRendererConfiguration.setReferenceTileSizeOnScreenInPixels(referenceTileSize);
		mapRenderer.setConfiguration(AtlasMapRendererConfiguration.Casts.downcastTo_MapRendererConfiguration(atlasRendererConfiguration));
		
		if (OFFLINE_MAP){
			MapPresentationEnvironment presentation = notGc(new MapPresentationEnvironment(mapStyle, displayDensityFactor, "en")); 
			//TODO: here should be current locale
			//mapPresentationEnvironment->setSettings(configuration.styleSettings);
			BinaryMapPrimitivesProvider binaryMapPrimitivesProvider = notGc(new BinaryMapPrimitivesProvider(
					notGc(new BinaryMapDataProvider(obfsCollection)), 
					notGc(new Primitiviser(presentation)), rasterTileSize));
			BinaryMapRasterLayerProvider_Software binaryMapRasterLayerProvider = notGc(new BinaryMapRasterLayerProvider_Software(
					binaryMapPrimitivesProvider));
			mapRenderer.setRasterLayerProvider(RasterMapLayerId.BaseLayer, binaryMapRasterLayerProvider);
//			BinaryMapStaticSymbolsProvider binaryMapStaticSymbolsProvider = notGc(new BinaryMapStaticSymbolsProvider(
//					binaryMapPrimitivesProvider, rasterTileSize));
//			mapRenderer.addSymbolProvider(binaryMapStaticSymbolsProvider);
		} else {
			OnlineRasterMapLayerProvider onlineMapRasterLayerProvider = notGc(OnlineTileSources.getBuiltIn()
					.createProviderFor("Mapnik (OsmAnd)"));
			mapRenderer.setRasterLayerProvider(RasterMapLayerId.BaseLayer, onlineMapRasterLayerProvider);
		}

	}

	
	
	private static class EGLContextFactory implements GLSurfaceView.EGLContextFactory {
		private EGLContext gpuWorkerContext;
		private EGLSurface gpuWorkerFakeSurface;
		private GLSurfaceView glSurfaceView;

		public EGLContextFactory(GLSurfaceView glSurfaceView) {
			this.glSurfaceView = glSurfaceView;
		}

		public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig) {
			final String eglExtensions = egl.eglQueryString(display, EGL10.EGL_EXTENSIONS);
			Log.i(NATIVE_TAG, "EGL extensions: " + eglExtensions);
			final String eglVersion = egl.eglQueryString(display, EGL10.EGL_VERSION);
			Log.i(NATIVE_TAG, "EGL version: " + eglVersion);

			Log.i(NATIVE_TAG, "Creating main context...");
			final int[] contextAttribList = { EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE };

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
				gpuWorkerContext = egl.eglCreateContext(display, eglConfig, mainContext, contextAttribList);
			} catch (Exception e) {
				Log.e(NATIVE_TAG, "Failed to create GPU worker context", e);
			}
			if (gpuWorkerContext == null || gpuWorkerContext == EGL10.EGL_NO_CONTEXT) {
				Log.e(NATIVE_TAG, "Failed to create GPU worker context: " + egl.eglGetError());
				gpuWorkerContext = null;
			}

			if (gpuWorkerContext != null) {
				Log.i(NATIVE_TAG, "Creating GPU worker fake surface...");
				try {
					final int[] surfaceAttribList = { EGL10.EGL_WIDTH, 1, EGL10.EGL_HEIGHT, 1, EGL10.EGL_NONE };
					gpuWorkerFakeSurface = egl.eglCreatePbufferSurface(display, eglConfig, surfaceAttribList);
				} catch (Exception e) {
					Log.e(NATIVE_TAG, "Failed to create GPU worker fake surface", e);
				}
				if (gpuWorkerFakeSurface == null || gpuWorkerFakeSurface == EGL10.EGL_NO_SURFACE) {
					Log.e(NATIVE_TAG, "Failed to create GPU worker fake surface: " + egl.eglGetError());
					gpuWorkerFakeSurface = null;
				}
			}

			MapRendererSetupOptions rendererSetupOptions = notGcFor1Egl(new MapRendererSetupOptions());
			if (gpuWorkerContext != null && gpuWorkerFakeSurface != null) {
				rendererSetupOptions.setGpuWorkerThreadEnabled(true);
				GpuWorkerThreadPrologue gpuWorkerThreadPrologue = notGcFor1Egl(new GpuWorkerThreadPrologue(egl,
						display, gpuWorkerContext, gpuWorkerFakeSurface));
				rendererSetupOptions.setGpuWorkerThreadPrologue(gpuWorkerThreadPrologue.getBinding());
				GpuWorkerThreadEpilogue gpuWorkerThreadEpilogue = notGcFor1Egl(new GpuWorkerThreadEpilogue(egl));
				rendererSetupOptions.setGpuWorkerThreadEpilogue(gpuWorkerThreadEpilogue.getBinding());
			} else {
				rendererSetupOptions.setGpuWorkerThreadEnabled(false);
			}
			RenderRequestCallback renderRequestCallback = notGcFor1Egl(new RenderRequestCallback(glSurfaceView));
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

	private static class GpuWorkerThreadEpilogue extends MapRendererSetupOptions.IGpuWorkerThreadEpilogue {
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

	private static class GpuWorkerThreadPrologue extends MapRendererSetupOptions.IGpuWorkerThreadPrologue {
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

	private static class RenderRequestCallback extends MapRendererSetupOptions.IFrameUpdateRequestCallback {
		private GLSurfaceView glSurfaceView;

		public RenderRequestCallback(GLSurfaceView glSurfaceView) {
			this.glSurfaceView = glSurfaceView;
		}

		@Override
		public void method(IMapRenderer mapRenderer) {
			glSurfaceView.requestRender();
		}
	}

}

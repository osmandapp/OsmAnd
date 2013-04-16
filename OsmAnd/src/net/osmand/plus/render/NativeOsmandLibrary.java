package net.osmand.plus.render;


import net.osmand.NativeLibrary;
import net.osmand.PlatformUtil;
import net.osmand.plus.ClientContext;
import net.osmand.plus.Version;
import net.osmand.plus.render.OsmandRenderer.RenderingContext;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;

import org.apache.commons.logging.Log;

import android.graphics.Bitmap;

public class NativeOsmandLibrary extends NativeLibrary {
	private static final Log log = PlatformUtil.getLog(NativeOsmandLibrary.class);
	
	private static NativeOsmandLibrary library;
	private static Boolean isNativeSupported = null;
	
	public static NativeOsmandLibrary getLoadedLibrary(){
		synchronized (NativeOsmandLibrary.class) {
			return library;
		}
	}

	public static NativeOsmandLibrary getLibrary(RenderingRulesStorage storage, ClientContext ctx) {
		if (!isLoaded()) {
			synchronized (NativeOsmandLibrary.class) {
				if (!isLoaded()) {
					try {
						log.debug("Loading native gnustl_shared..."); //$NON-NLS-1$
						System.loadLibrary("gnustl_shared");
						log.debug("Loading native cpufeatures_proxy..."); //$NON-NLS-1$
						System.loadLibrary("cpufeatures_proxy");
						/*(if (PlatformUtil.AVIAN_LIBRARY) {
							log.debug("Loading load routing test..."); //$NON-NLS-1$
							System.loadLibrary("routing_test");
							testRoutingPing();
						}*/
						if(android.os.Build.VERSION.SDK_INT >= 8) {
							log.debug("Loading jnigraphics, since Android >= 2.2 ..."); //$NON-NLS-1$
							System.loadLibrary("jnigraphics");
						}
						final String libCpuSuffix = cpuHasNeonSupport() ? "_neon" : "";
						log.debug("Loading native libraries..."); //$NON-NLS-1$
						if(Version.isOldCoreVersion(ctx)) {
							System.loadLibrary("osmand" + libCpuSuffix);
						} else {
							System.loadLibrary("Qt5Core" + libCpuSuffix);
							System.loadLibrary("Qt5Network" + libCpuSuffix);
							System.loadLibrary("Qt5Concurrent" + libCpuSuffix);
							System.loadLibrary("Qt5Sql" + libCpuSuffix);
							System.loadLibrary("Qt5Xml" + libCpuSuffix);
							System.loadLibrary("OsmAndCore" + libCpuSuffix);
							System.loadLibrary("OsmAndCoreUtils" + libCpuSuffix);
							System.loadLibrary("OsmAndJNI" + libCpuSuffix);
						}
						log.debug("Creating NativeOsmandLibrary instance..."); //$NON-NLS-1$
						library = new NativeOsmandLibrary();
						log.debug("Initializing rendering rules storage..."); //$NON-NLS-1$
						NativeOsmandLibrary.initRenderingRulesStorage(storage);
						isNativeSupported = true;
						log.debug("Native library loaded successfully"); //$NON-NLS-1$
					} catch (Throwable e) {
						log.error("Failed to load native library", e); //$NON-NLS-1$
						isNativeSupported = false;
					}
				}
			}
		}
		return library;
	}
	
	public static boolean isSupported()
	{
		return isNativeSupported != null && isNativeSupported;
	}
	
	public static boolean isLoaded() {
		return isNativeSupported != null;  
	}
	
	public static boolean isNativeSupported(RenderingRulesStorage storage, ClientContext ctx) {
		if(storage != null) {
			getLibrary(storage, ctx);
		}
		return isSupported();
	}
	
	public boolean useDirectRendering(){
		return android.os.Build.VERSION.SDK_INT >= 8;
	}

	public RenderingGenerationResult generateRendering(RenderingContext rc, NativeSearchResult searchResultHandler,
			Bitmap bitmap, boolean isTransparent, RenderingRuleSearchRequest render) {
		if (searchResultHandler == null) {
			log.error("Error searchresult = null"); //$NON-NLS-1$
			return new RenderingGenerationResult(null);
		}
		
		// Android 2.2+
		if(android.os.Build.VERSION.SDK_INT >= 8) { 
			return generateRenderingDirect(rc, searchResultHandler.nativeHandler, bitmap, render);
		} else {
			return generateRenderingIndirect(rc, searchResultHandler.nativeHandler, isTransparent, render, false);
		}
	}

	
	private static native RenderingGenerationResult generateRenderingDirect(RenderingContext rc, long searchResultHandler,
			Bitmap bitmap, RenderingRuleSearchRequest render);
			
	public static native int getCpuCount();
	public static native boolean cpuHasNeonSupport();
	
}

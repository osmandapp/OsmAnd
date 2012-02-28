package net.osmand.plus.render;


import org.apache.commons.logging.Log;

import net.osmand.LogUtil;
import net.osmand.plus.render.OsmandRenderer.RenderingContext;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import android.graphics.Bitmap;

public class NativeOsmandLibrary {
	private static final Log log = LogUtil.getLog(NativeOsmandLibrary.class);
	
	private static NativeOsmandLibrary library;
	private static Boolean isNativeSupported = null;

	public static NativeOsmandLibrary getLibrary(RenderingRulesStorage storage) {
		if (!isLoaded()) {
			synchronized (NativeOsmandLibrary.class) {
				if (!isLoaded()) {
					try {
						log.debug("Loading native stlport_shared..."); //$NON-NLS-1$
						System.loadLibrary("stlport_shared");
						log.debug("Loading native osmand..."); //$NON-NLS-1$
						System.loadLibrary("osmand");
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
	
	public static boolean isNativeSupported(RenderingRulesStorage storage) {
		if(storage != null) {
			getLibrary(storage);
		}
		return isSupported();
	}

	public String generateRendering(RenderingContext rc, NativeSearchResult searchResultHandler, Bitmap bmp,
			boolean useEnglishNames, RenderingRuleSearchRequest render, int defaultColor) {
		if (searchResultHandler == null) {
			return "Error searchresult = null";
		}
		return generateRendering(rc, searchResultHandler.nativeHandler, bmp, useEnglishNames, render, defaultColor);
	}

	/**
	 * @param searchResultHandle
	 *            - must be null if there is no need to append to previous results returns native handle to results
	 */
	public NativeSearchResult searchObjectsForRendering(int sleft, int sright, int stop, int sbottom, int zoom, String mapName,
			RenderingRuleSearchRequest request, boolean skipDuplicates, NativeSearchResult searchResultHandler,
			Object objectWithInterruptedField) {
		if (searchResultHandler == null) {
			return new NativeSearchResult(searchObjectsForRendering(sleft, sright, stop, sbottom, zoom, mapName, request, skipDuplicates,
					0, objectWithInterruptedField));
		} else {
			int res = searchObjectsForRendering(sleft, sright, stop, sbottom, zoom, mapName, request, skipDuplicates,
					searchResultHandler.nativeHandler, objectWithInterruptedField);
			if (res == searchResultHandler.nativeHandler) {
				return searchResultHandler;
			}
			return new NativeSearchResult(res);
		}

	}

	public void deleteSearchResult(NativeSearchResult searchResultHandler) {
		if (searchResultHandler.nativeHandler != 0) {
			deleteSearchResult(searchResultHandler.nativeHandler);
			searchResultHandler.nativeHandler = 0;
		}
	}
	
	public boolean initMapFile(String filePath) {
		return initBinaryMapFile(filePath);
	}

	public static class NativeSearchResult {
		private int nativeHandler;

		private NativeSearchResult(int nativeHandler) {
			this.nativeHandler = nativeHandler;
		}

		@Override
		protected void finalize() throws Throwable {
			if (nativeHandler != 0) {
				super.finalize();
			}
			deleteSearchResult(nativeHandler);
		}
	}

	private static native void deleteSearchResult(int searchResultHandle);

	private static native boolean initBinaryMapFile(String filePath);
	
	private static native boolean initRenderingRulesStorage(RenderingRulesStorage storage);

	private static native String generateRendering(RenderingContext rc, int searchResultHandler, Bitmap bmp, boolean useEnglishNames,
			RenderingRuleSearchRequest render, int defaultColor);

	private static native int searchObjectsForRendering(int sleft, int sright, int stop, int sbottom, int zoom, String mapnaem,
			RenderingRuleSearchRequest request, boolean skipDuplicates, int searchResultHandler, Object objectWithInterruptedField);
}

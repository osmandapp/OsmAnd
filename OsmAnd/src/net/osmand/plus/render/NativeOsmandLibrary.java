package net.osmand.plus.render;


import net.osmand.plus.render.OsmandRenderer.RenderingContext;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import android.graphics.Bitmap;

public class NativeOsmandLibrary {

	private static NativeOsmandLibrary library;
	private static Boolean isNativeSupported = null;

	public static NativeOsmandLibrary getLibrary(RenderingRulesStorage storage) {
		if (!isLoaded()) {
			synchronized (NativeOsmandLibrary.class) {
				if (!isLoaded()) {
					try {
						System.loadLibrary("libstlport_shared");
						System.loadLibrary("osmand");
						library = new NativeOsmandLibrary();
						NativeOsmandLibrary.initRenderingRulesStorage(storage);
						isNativeSupported = true;
					} catch (Throwable e) {
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

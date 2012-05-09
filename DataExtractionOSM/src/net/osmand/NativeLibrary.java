package net.osmand;

import java.nio.ByteBuffer;

import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;

public class NativeLibrary {
	
	public static class RenderingGenerationResult {
		public RenderingGenerationResult(ByteBuffer bitmap) {
			bitmapBuffer = bitmap;
		}
	
		public final ByteBuffer bitmapBuffer;
	}
	
	public static class NativeSearchResult {

		public int nativeHandler;
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
	
	/**
	 * @param searchResultHandle
	 *            - must be null if there is no need to append to previous results returns native handle to results
	 */
	public NativeSearchResult searchObjectsForRendering(int sleft, int sright, int stop, int sbottom, int zoom,
			RenderingRuleSearchRequest request, boolean skipDuplicates, Object objectWithInterruptedField, String msgIfNothingFound) {
		return new NativeSearchResult(searchNativeObjectsForRendering(sleft, sright, stop, sbottom, zoom, request, skipDuplicates,
				objectWithInterruptedField, msgIfNothingFound));
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
	
	public boolean closeMapFile(String filePath) {
		return closeBinaryMapFile(filePath);
	}


	protected static native void deleteSearchResult(int searchResultHandle);

	protected static native boolean initBinaryMapFile(String filePath);
	
	protected static native boolean closeBinaryMapFile(String filePath);
	
	protected static native void initRenderingRulesStorage(RenderingRulesStorage storage);


	protected static native RenderingGenerationResult generateRendering_Indirect(RenderingContext rc, int searchResultHandler,
			int requestedBitmapWidth, int requestedBitmapHeight, int rowBytes, boolean isTransparent, boolean useEnglishNames,
			RenderingRuleSearchRequest render, int defaultColor);
	
	protected static native int searchNativeObjectsForRendering(int sleft, int sright, int stop, int sbottom, int zoom, 
			RenderingRuleSearchRequest request, boolean skipDuplicates, Object objectWithInterruptedField, String msgIfNothingFound);

}

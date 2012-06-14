package net.osmand;

import java.nio.ByteBuffer;

import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.RouteDataObject;
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

		public long nativeHandler;
		private NativeSearchResult(long nativeHandler) {
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
	
	public RouteDataObject[] loadRouteRegion(RouteRegion reg, int left, int right, int top, int bottom) {
		return loadRoutingData(reg, reg.getName(), reg.getFilePointer(), left, right, top, bottom);
	}

	
	protected static native RouteDataObject[] loadRoutingData(RouteRegion reg, String regName, int fpointer, int left, int right, int top, int bottom); 

	protected static native void deleteSearchResult(long searchResultHandle);

	protected static native boolean initBinaryMapFile(String filePath);
	
	protected static native boolean closeBinaryMapFile(String filePath);
	
	protected static native void initRenderingRulesStorage(RenderingRulesStorage storage);


	protected static native RenderingGenerationResult generateRenderingIndirect(RenderingContext rc, long searchResultHandler,
			boolean isTransparent, RenderingRuleSearchRequest render, boolean encodePng);
	
	protected static native long searchNativeObjectsForRendering(int sleft, int sright, int stop, int sbottom, int zoom, 
			RenderingRuleSearchRequest request, boolean skipDuplicates, Object objectWithInterruptedField, String msgIfNothingFound);

}
